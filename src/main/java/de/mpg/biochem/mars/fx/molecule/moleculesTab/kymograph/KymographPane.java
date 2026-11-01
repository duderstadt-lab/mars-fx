/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2026 Karl Duderstadt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package de.mpg.biochem.mars.fx.molecule.moleculesTab.kymograph;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.controlsfx.control.RangeSlider;
import org.controlsfx.control.SearchableComboBox;
import org.scijava.Context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.mpg.biochem.mars.fx.event.AddDocumentEvent;
import de.mpg.biochem.mars.fx.dialogs.RoverErrorDialog;
import de.mpg.biochem.mars.fx.event.FigureChangedEvent;
import de.mpg.biochem.mars.fx.event.RunMoleculeArchiveTaskEvent;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.kymograph.KymographSettings.ChannelSettings;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.kymograph.KymographSettings.Filter;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.kymograph.KymographSettings.FrameReduction;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.kymograph.KymographSettings.Mode;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveWindow;
import de.mpg.biochem.mars.util.MarsFigure;
import ij.IJ;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 * Center pane tab for creating kymographs and montages of the selected
 * molecule. Settings are edited on the left and the figure is shown in the
 * center. Figures are generated in the background by running the script made
 * by the {@link KymographScriptBuilder} and are stored in the molecule record
 * together with the settings used, so they are shown again when the molecule
 * is selected.
 * <p>
 * Only one figure is generated at a time. Every generation is tied to the
 * molecule it was started for. If the selection changes, Generate is pressed
 * again or the archive is locked, the result of the running generation is
 * discarded.
 */
public class KymographPane {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final DateTimeFormatter CREATED_FORMAT = DateTimeFormatter
		.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
	private static final String[] BUILT_IN_LUTS = { "Grays", "Red", "Green",
		"Blue", "Cyan", "Magenta", "Yellow" };

	private static File lastExportDirectory;

	private final Context context;
	private final KymographScriptRunner runner;

	private final SplitPane rootPane;
	private final VBox sectionsBox;
	private final StackPane figurePane;
	private final ImageView imageView;
	private final ScrollPane imageScrollPane;
	private final Label placeholderLabel;
	private final TextArea errorArea;
	private final VBox busyBox;
	private final Label busyLabel;
	private final Label statusLabel;
	private final ToggleButton fitButton;
	private final Button generateButton;
	private final MenuButton exportButton;
	private final Button addToCommentsButton;
	private final Button batchButton;
	private final CheckBox autoGenerateCheckBox;
	private final CheckBox keepSettingsCheckBox;

	private MoleculeArchive<?, ?, ?, ?> archive;
	private KymographArchiveType archiveType;
	private Molecule molecule;
	private KymographSettings settings = new KymographSettings();

	// Bumped whenever a running generation should be discarded.
	private long generation = 0;
	private Thread worker;
	private boolean busy = false;
	private final PauseTransition autoGenerateDelay = new PauseTransition(
		Duration.millis(300));

	// Pushes settings into the controls. Listeners ignore changes while pushing.
	private final List<Runnable> pushers = new ArrayList<>();
	private boolean pushing = false;
	private final List<Runnable> visibilityUpdaters = new ArrayList<>();
	private final List<Section> sections = new ArrayList<>();

	/** How the time points of a montage are arranged. */
	private enum Arrangement {
			SINGLE_ROW, SINGLE_COLUMN, GRID
	}

	// Columns used when switching the montage to a grid.
	private int gridColumns = 5;

	private static final double SPINNER_WIDTH = 76;
	private static final double COMBO_WIDTH = 116;

	private RangeSlider frameSlider;
	private Spinner<Integer> frameMinSpinner;
	private Spinner<Integer> frameMaxSpinner;
	private boolean syncingFrames = false;
	private int numberOfFrames = 1;
	private ComboBox<Integer> filterChannelComboBox;
	private VBox channelsBox;
	private TextField xAxisLabelField;

	public KymographPane(final Context context) {
		this.context = context;
		this.runner = new KymographScriptRunner(context);

		// Left side settings
		sectionsBox = new VBox();
		buildSections();

		ScrollPane settingsScrollPane = new ScrollPane(sectionsBox);
		settingsScrollPane.setFitToWidth(true);
		settingsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

		autoGenerateCheckBox = new CheckBox("Auto-generate on select");
		autoGenerateCheckBox.setTooltip(new Tooltip(
			"Generate a figure when a molecule without one is selected."));

		keepSettingsCheckBox = new CheckBox("Keep settings when switching molecules");
		keepSettingsCheckBox.setTooltip(new Tooltip(
			"When on, the current settings stay when another molecule is selected.\nWhen off, the settings stored with that molecule's figure are loaded."));

		generateButton = new Button("Generate");
		generateButton.setDefaultButton(true);
		generateButton.setMaxWidth(Double.MAX_VALUE);
		generateButton.setOnAction(e -> {
			if (busy) cancel();
			else generate();
		});

		MenuItem exportPNG = new MenuItem("PNG...");
		exportPNG.setOnAction(e -> exportPNG());
		MenuItem exportSVG = new MenuItem("SVG...");
		exportSVG.setOnAction(e -> exportSVG());
		exportButton = new MenuButton("Export", null, exportPNG, exportSVG);
		exportButton.setMaxWidth(Double.MAX_VALUE);

		addToCommentsButton = new Button("Add to Comments");
		addToCommentsButton.setTooltip(new Tooltip(
			"Add the script for the current settings to a new Comments document."));
		addToCommentsButton.setMaxWidth(Double.MAX_VALUE);
		addToCommentsButton.setOnAction(e -> addToComments());

		batchButton = new Button("Batch...");
		batchButton.setTooltip(new Tooltip(
			"Generate figures for many molecules. The archive is locked while they are generated."));
		batchButton.setOnAction(e -> showBatchDialog());

		HBox buttonRow = new HBox(5, exportButton, addToCommentsButton,
			batchButton);
		HBox.setHgrow(exportButton, Priority.ALWAYS);
		HBox.setHgrow(addToCommentsButton, Priority.ALWAYS);

		VBox actionsBox = new VBox(6, autoGenerateCheckBox, keepSettingsCheckBox,
			generateButton, buttonRow);
		actionsBox.setPadding(new Insets(8));
		autoGenerateCheckBox.setAlignment(Pos.CENTER_LEFT);

		BorderPane settingsPane = new BorderPane();
		settingsPane.setCenter(settingsScrollPane);
		settingsPane.setBottom(actionsBox);
		settingsPane.setMinWidth(260);
		settingsPane.setPrefWidth(330);

		// Center figure
		imageView = new ImageView();
		imageView.setPreserveRatio(true);
		imageView.setSmooth(true);
		StackPane imageHolder = new StackPane(imageView);
		imageScrollPane = new ScrollPane(imageHolder);
		imageScrollPane.setPannable(true);
		imageHolder.minWidthProperty().bind(imageScrollPane.widthProperty().subtract(
			2));
		imageHolder.minHeightProperty().bind(imageScrollPane.heightProperty()
			.subtract(2));

		fitButton = new ToggleButton("Fit");
		fitButton.setSelected(true);
		fitButton.setTooltip(new Tooltip("Fit the figure to the window."));
		ToggleButton actualSizeButton = new ToggleButton("1:1");
		actualSizeButton.setTooltip(new Tooltip("Show the figure at actual size."));
		ToggleGroup zoomGroup = new ToggleGroup();
		fitButton.setToggleGroup(zoomGroup);
		actualSizeButton.setToggleGroup(zoomGroup);
		zoomGroup.selectedToggleProperty().addListener((ob, o, n) -> {
			if (n == null) o.setSelected(true);
			updateZoom();
		});
		imageScrollPane.viewportBoundsProperty().addListener((ob, o,
			n) -> updateZoom());

		placeholderLabel = new Label();
		placeholderLabel.setWrapText(true);
		placeholderLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 1.2em;");

		errorArea = new TextArea();
		errorArea.setEditable(false);
		errorArea.setWrapText(true);
		errorArea.setStyle("-fx-text-fill: #c62828; -fx-font-family: monospace;");

		ProgressIndicator progressIndicator = new ProgressIndicator();
		progressIndicator.setMaxSize(60, 60);
		busyLabel = new Label();
		busyLabel.setWrapText(true);
		Button cancelButton = new Button("Cancel");
		cancelButton.setOnAction(e -> cancel());
		busyBox = new VBox(12, progressIndicator, busyLabel, cancelButton);
		busyBox.setAlignment(Pos.CENTER);
		busyBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		busyBox.setStyle("-fx-background-color: rgba(127, 127, 127, 0.25);");
		busyBox.setVisible(false);

		figurePane = new StackPane(imageScrollPane, placeholderLabel, errorArea,
			busyBox);

		statusLabel = new Label();
		statusLabel.setStyle("-fx-text-fill: gray;");
		HBox.setHgrow(statusLabel, Priority.ALWAYS);
		statusLabel.setMaxWidth(Double.MAX_VALUE);
		HBox statusBar = new HBox(6, statusLabel, fitButton, actualSizeButton);
		statusBar.setAlignment(Pos.CENTER_LEFT);
		statusBar.setPadding(new Insets(4, 8, 4, 8));

		BorderPane centerPane = new BorderPane();
		centerPane.setCenter(figurePane);
		centerPane.setBottom(statusBar);

		rootPane = new SplitPane(settingsPane, centerPane);
		rootPane.setDividerPositions(0.25);
		SplitPane.setResizableWithParent(settingsPane, Boolean.FALSE);

		autoGenerateDelay.setOnFinished(e -> generate());

		pushSettings();
		showFigure(null);
	}

	public Node getNode() {
		return rootPane;
	}

	// ---------------------------------------------------------------------
	// Archive and selection

	/**
	 * Set the archive. Returns false if the archive type is not supported or
	 * mars-kymograph is not installed, in which case the tab should not be
	 * shown.
	 *
	 * @param archive The archive or null.
	 * @return True if figures can be created for this archive.
	 */
	public boolean setArchive(MoleculeArchive<?, ?, ?, ?> archive) {
		cancel();
		this.archive = archive;
		this.molecule = null;
		KymographArchiveType type = KymographArchiveType.of(archive);
		archiveType = (type != null && type.isAvailable()) ? type : null;
		if (archiveType != null && !archiveType.supportsKymograph())
			settings.mode = Mode.MONTAGE;
		pushSettings();
		showFigure(null);
		return archiveType != null;
	}

	public String getTabName() {
		return (archiveType == null || archiveType.supportsKymograph())
			? "Kymograph" : "Montage";
	}

	/**
	 * Called whenever the molecule selection changes. Any running generation is
	 * discarded. The figure is shown by {@link #refresh()} once the tab is
	 * visible.
	 *
	 * @param molecule The selected molecule or null.
	 */
	public void setMolecule(Molecule molecule) {
		autoGenerateDelay.stop();
		if (busy) cancel();
		this.molecule = molecule;
	}

	/**
	 * Show the stored figure for the current molecule, loading its settings,
	 * or generate one if auto-generate is on.
	 */
	public void refresh() {
		if (archiveType == null) return;
		updateMoleculeInfo();
		MarsFigure figure = getFigure();
		if (figure != null && !keepSettingsCheckBox.isSelected()) {
			KymographSettings stored = KymographSettings.fromJson(figure
				.getSettings());
			if (!archiveType.supportsKymograph()) stored.mode = Mode.MONTAGE;
			settings = stored;
			updateMoleculeInfo();
		}
		pushSettings();
		showFigure(figure);
		if (figure == null && molecule != null && autoGenerateCheckBox
			.isSelected()) autoGenerateDelay.playFromStart();
	}

	/**
	 * Called when the archive is locked for a command or script. A running
	 * generation is discarded so it can't write into the archive while a
	 * command is running.
	 */
	public void onArchiveLocked() {
		autoGenerateDelay.stop();
		if (busy) cancel();
	}

	private MarsFigure getFigure() {
		if (molecule == null) return null;
		return molecule.getFigure();
	}

	/**
	 * Update the channel and frame controls for the metadata of the current
	 * molecule.
	 */
	private void updateMoleculeInfo() {
		if (archive == null || molecule == null) return;
		MarsMetadata metadata = archive.getMetadata(molecule.getMetadataUID());
		if (metadata == null) return;
		settings.setNumberOfChannels(numberOfChannels(metadata));
		numberOfFrames = Math.max(1, metadata.getImage(0).getSizeT());
	}

	/**
	 * Figures are made from the N5 sources of the metadata, one channel per
	 * source.
	 */
	private static int numberOfChannels(MarsMetadata metadata) {
		int channels = (int) metadata.getBdvSources().stream().filter(s -> s
			.isN5()).count();
		return channels > 0 ? channels : Math.max(1, metadata.getImage(0)
			.getSizeC());
	}

	// ---------------------------------------------------------------------
	// Generation

	public void generate() {
		autoGenerateDelay.stop();
		if (archive == null || archiveType == null || molecule == null) return;
		if (busy) cancel();

		final String UID = molecule.getUID();
		final KymographSettings jobSettings = settings.copy();
		final long job = ++generation;
		final String script = new KymographScriptBuilder(jobSettings, archiveType,
			UID).reportDisplayRanges(true).build();
		final MoleculeArchive<?, ?, ?, ?> jobArchive = archive;

		setBusy(true, "Generating " + figureType(jobSettings) + " for " + UID +
			"...");
		worker = new Thread(() -> {
			KymographScriptRunner.Result result;
			try {
				result = runner.run(jobArchive, script);
			}
			catch (InterruptedException e) {
				return;
			}
			catch (Exception e) {
				result = new KymographScriptRunner.Result(null, null, e.toString());
			}
			final KymographScriptRunner.Result finalResult = result;
			Platform.runLater(() -> finish(job, UID, jobSettings, finalResult));
		}, "Mars kymograph " + UID);
		worker.setDaemon(true);
		worker.start();
	}

	private void finish(long job, String UID, KymographSettings jobSettings,
		KymographScriptRunner.Result result)
	{
		// The selection changed or the generation was cancelled.
		if (job != generation) return;
		setBusy(false, null);
		worker = null;

		if (!result.isSuccess()) {
			showError(result.getError());
			return;
		}

		// Keep the display ranges that were calculated so they can be adjusted.
		applyDisplayRanges(jobSettings, result);
		applyDisplayRanges(settings, result);
		pushSettings();

		if (molecule == null || !molecule.getUID().equals(UID)) return;

		// Stored on the selected record, which is saved like any other edit.
		MarsFigure figure = new MarsFigure(figureType(jobSettings), jobSettings
			.toJson(), result.getImage());
		figure.setGenerator(generatorName());
		molecule.setFigure(figure);
		rootPane.fireEvent(new FigureChangedEvent(figure));
		showFigure(figure);
	}

	private static void applyDisplayRanges(KymographSettings s,
		KymographScriptRunner.Result result)
	{
		for (Map.Entry<Integer, double[]> range : result.getDisplayRanges()
			.entrySet())
		{
			ChannelSettings channel = s.getChannel(range.getKey());
			if (channel.autoDisplayRange) {
				channel.displayRangeMin = range.getValue()[0];
				channel.displayRangeMax = range.getValue()[1];
			}
		}
	}

	// ---------------------------------------------------------------------
	// Batch

	/**
	 * Ask which molecules to generate figures for and generate them while the
	 * archive is locked.
	 */
	public void showBatchDialog() {
		if (archive == null || archiveType == null) {
			new RoverErrorDialog(rootPane.getScene().getWindow(),
				"Figures are not available for this archive. Kymographs and montages need mars-kymograph and a DNA, single-molecule or transverse flow archive.")
					.show();
			return;
		}
		GenerateFiguresDialog dialog = new GenerateFiguresDialog(rootPane
			.getScene().getWindow(), archive.properties().getTagSet(), getTabName()
				.toLowerCase());
		dialog.showAndWait().ifPresent(this::generateBatch);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void generateBatch(GenerateFiguresDialog.Options options) {
		final KymographSettings current = settings.copy();
		final MoleculeArchive<?, ?, ?, ?> batchArchive = archive;
		final KymographArchiveType type = archiveType;

		Runnable task = () -> {
			MoleculeArchiveWindow window = batchArchive.getWindow();
			List<String> UIDs = batchArchive.getMoleculeUIDs().stream().filter(
				UID -> options.getTags().isEmpty() || options.getTags().stream()
					.anyMatch(tag -> batchArchive.moleculeHasTag(UID, tag))).collect(
						Collectors.toList());

			int generated = 0;
			int skipped = 0;
			List<String> failed = new ArrayList<>();
			for (int i = 0; i < UIDs.size(); i++) {
				String UID = UIDs.get(i);
				if (window != null) {
					window.updateLockMessage("Generating " + (i + 1) + " / " + UIDs
						.size() + "...");
					window.setProgress((double) i / UIDs.size());
				}

				Molecule batchMolecule = batchArchive.get(UID);
				MarsFigure existing = batchMolecule.getFigure();
				if (existing != null && !options.overwrite()) {
					skipped++;
					continue;
				}

				KymographSettings s = (options.useStoredSettings() &&
					existing != null) ? KymographSettings.fromJson(existing
						.getSettings()) : current.copy();
				if (!type.supportsKymograph()) s.mode = Mode.MONTAGE;
				MarsMetadata metadata = batchArchive.getMetadata(batchMolecule
					.getMetadataUID());
				if (metadata != null) s.setNumberOfChannels(numberOfChannels(
					metadata));

				KymographScriptRunner.Result result;
				try {
					result = runner.run(batchArchive, new KymographScriptBuilder(s, type,
						UID).reportDisplayRanges(true).build());
				}
				catch (InterruptedException e) {
					break;
				}
				catch (Exception e) {
					result = new KymographScriptRunner.Result(null, null, e.toString());
				}

				if (!result.isSuccess()) {
					failed.add(UID);
					if (window != null) window.logln("Could not generate a figure for " +
						UID + ": " + result.getError());
					continue;
				}
				applyDisplayRanges(s, result);
				MarsFigure figure = new MarsFigure(s.mode == Mode.KYMOGRAPH && type.supportsKymograph() ? "kymograph"
						: "montage", s.toJson(), result.getImage());
				figure.setGenerator(generatorName());
				batchMolecule.setFigure(figure);
				((MoleculeArchive) batchArchive).put(batchMolecule);
				generated++;
			}

			if (window != null) window.logln("Generated " + generated +
				" figures" + (skipped > 0 ? ", skipped " + skipped +
					" molecules that already had one" : "") + (failed.isEmpty() ? "."
						: ", " + failed.size() + " failed: " + String.join(", ",
							failed)));
			Platform.runLater(() -> rootPane.fireEvent(new FigureChangedEvent(
				null)));
		};

		rootPane.fireEvent(new RunMoleculeArchiveTaskEvent((MoleculeArchive) batchArchive,
			task, "Generating " + getTabName().toLowerCase() + "s..."));
	}

	public void cancel() {
		generation++;
		if (worker != null) worker.interrupt();
		worker = null;
		if (busy) {
			setBusy(false, null);
			showFigure(getFigure());
		}
	}

	private void setBusy(boolean busy, String message) {
		this.busy = busy;
		busyBox.setVisible(busy);
		// Keep the busy message clear of the placeholder or error text behind it.
		if (busy) errorArea.setVisible(false);
		placeholderLabel.setVisible(!busy && imageView.getImage() == null &&
			!errorArea.isVisible());
		busyLabel.setText(message);
		generateButton.setText(busy ? "Cancel" : "Generate");
		exportButton.setDisable(busy || getFigure() == null);
	}

	private String figureType(KymographSettings s) {
		return (s.mode == Mode.KYMOGRAPH && archiveType.supportsKymograph())
			? "kymograph" : "montage";
	}

	private static String generatorName() {
		String version = null;
		try {
			version = Class.forName(KymographArchiveType.KYMOGRAPH_PACKAGE +
				".ImageFormatter").getPackage().getImplementationVersion();
		}
		catch (ClassNotFoundException | LinkageError e) {
			// Leave the version out.
		}
		return "mars-kymograph" + (version != null ? " " + version : "");
	}

	// ---------------------------------------------------------------------
	// Display

	private void showFigure(MarsFigure figure) {
		errorArea.setVisible(false);
		Image image = figure != null ? decode(figure.getImage()) : null;
		imageView.setImage(image);
		imageScrollPane.setVisible(image != null);
		placeholderLabel.setVisible(image == null);
		if (archiveType == null) placeholderLabel.setText(
			"Figures are not available for this archive.");
		else if (molecule == null) placeholderLabel.setText(
			"Select a molecule to create a " + getTabName().toLowerCase() + ".");
		else placeholderLabel.setText("No " + getTabName().toLowerCase() +
			" has been generated for this molecule yet. Press Generate.");

		if (figure != null) statusLabel.setText(figure.getType() + " · " +
			(figure.getCreated() != null ? CREATED_FORMAT.format(figure
				.getCreated()) : "") + (image != null ? " · " + (int) image
					.getWidth() + " x " + (int) image.getHeight() + " px" : ""));
		else statusLabel.setText("");

		exportButton.setDisable(busy || figure == null);
		generateButton.setDisable(archiveType == null || molecule == null);
		batchButton.setDisable(archiveType == null);
		addToCommentsButton.setDisable(archiveType == null || molecule == null);
		updateZoom();
	}

	private void showError(String error) {
		imageScrollPane.setVisible(false);
		placeholderLabel.setVisible(false);
		errorArea.setText("The figure could not be generated.\n\n" + error);
		errorArea.setVisible(true);
	}

	private void updateZoom() {
		Image image = imageView.getImage();
		if (image == null) return;
		if (fitButton.isSelected()) {
			double width = imageScrollPane.getViewportBounds().getWidth() - 10;
			double height = imageScrollPane.getViewportBounds().getHeight() - 10;
			imageView.setFitWidth(Math.max(10, width));
			imageView.setFitHeight(Math.max(10, height));
		}
		else {
			imageView.setFitWidth(0);
			imageView.setFitHeight(0);
		}
	}

	private static Image decode(String dataURI) {
		byte[] bytes = decodeBytes(dataURI);
		if (bytes == null) return null;
		Image image = new Image(new ByteArrayInputStream(bytes));
		return image.isError() ? null : image;
	}

	private static byte[] decodeBytes(String dataURI) {
		if (dataURI == null || dataURI.isEmpty()) return null;
		int comma = dataURI.indexOf(',');
		try {
			return Base64.getDecoder().decode(comma >= 0 ? dataURI.substring(comma +
				1) : dataURI);
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}

	// ---------------------------------------------------------------------
	// Export and scripts

	private File chooseFile(String extension, String description) {
		MarsFigure figure = getFigure();
		if (figure == null) return null;
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Export " + figure.getType());
		if (lastExportDirectory != null && lastExportDirectory.exists())
			fileChooser.setInitialDirectory(lastExportDirectory);
		fileChooser.setInitialFileName(molecule.getUID() + "_" + figure.getType() +
			"." + extension);
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
			description, "*." + extension));
		File file = fileChooser.showSaveDialog(rootPane.getScene().getWindow());
		if (file != null) lastExportDirectory = file.getParentFile();
		return file;
	}

	private void exportPNG() {
		MarsFigure figure = getFigure();
		File file = chooseFile("png", "PNG image");
		if (file == null) return;
		try {
			Files.write(file.toPath(), decodeBytes(figure.getImage()));
		}
		catch (IOException e) {
			showError("Could not write " + file.getAbsolutePath() + "\n" + e
				.getMessage());
		}
	}

	/**
	 * SVG files are created by generating the figure again with the settings
	 * stored with it.
	 */
	private void exportSVG() {
		MarsFigure figure = getFigure();
		File file = chooseFile("svg", "SVG vector image");
		if (file == null) return;
		if (busy) cancel();

		final long job = ++generation;
		final String script = new KymographScriptBuilder(KymographSettings
			.fromJson(figure.getSettings()), archiveType, molecule.getUID())
				.saveAsSVG(file.getAbsolutePath()).build();
		final MoleculeArchive<?, ?, ?, ?> jobArchive = archive;
		setBusy(true, "Exporting " + file.getName() + "...");
		worker = new Thread(() -> {
			KymographScriptRunner.Result result;
			try {
				result = runner.run(jobArchive, script);
			}
			catch (InterruptedException e) {
				return;
			}
			catch (Exception e) {
				result = new KymographScriptRunner.Result(null, null, e.toString());
			}
			final KymographScriptRunner.Result finalResult = result;
			Platform.runLater(() -> {
				if (job != generation) return;
				setBusy(false, null);
				worker = null;
				if (!finalResult.isSuccess()) showError(finalResult.getError());
			});
		}, "Mars kymograph export");
		worker.setDaemon(true);
		worker.start();
	}

	private void addToComments() {
		if (molecule == null || archiveType == null) return;
		String markdown = "# " + getTabName() + " " + molecule.getUID() + "\n\n" +
			new KymographScriptBuilder(settings, archiveType, molecule.getUID())
				.buildFencedCodeBlock();
		rootPane.fireEvent(new AddDocumentEvent(getTabName() + " " + molecule
			.getUID(), markdown));
	}

	// ---------------------------------------------------------------------
	// State saved in the rover file

	public String getState() {
		ObjectNode state = MAPPER.createObjectNode();
		try {
			state.set("settings", MAPPER.readTree(settings.toJson()));
		}
		catch (IOException e) {
			// Settings are always valid json.
		}
		state.put("autoGenerate", autoGenerateCheckBox.isSelected());
		state.put("keepSettings", keepSettingsCheckBox.isSelected());
		ObjectNode expanded = state.putObject("expandedSections");
		for (Section section : sections)
			expanded.put(section.titledPane.getText(), section.titledPane
				.isExpanded());
		return state.toString();
	}

	public void setState(String json) {
		try {
			JsonNode state = MAPPER.readTree(json);
			if (state.has("settings")) settings = KymographSettings.fromJson(state
				.get("settings").toString());
			if (state.has("autoGenerate")) autoGenerateCheckBox.setSelected(state
				.get("autoGenerate").asBoolean());
			if (state.has("keepSettings")) keepSettingsCheckBox.setSelected(state.get(
				"keepSettings").asBoolean());
			JsonNode expanded = state.get("expandedSections");
			if (expanded != null) for (Section section : sections)
				if (expanded.has(section.titledPane.getText())) section.titledPane
					.setExpanded(expanded.get(section.titledPane.getText()).asBoolean());
			pushSettings();
		}
		catch (IOException e) {
			// Keep the defaults.
		}
	}

	// ---------------------------------------------------------------------
	// Settings controls

	private void pushSettings() {
		pushing = true;
		try {
			if (archiveType != null && !archiveType.supportsKymograph())
				settings.mode = Mode.MONTAGE;
			pushers.forEach(Runnable::run);
			rebuildChannels();
			updateFrameSlider();
			visibilityUpdaters.forEach(Runnable::run);
		}
		finally {
			pushing = false;
		}
	}

	private boolean isKymograph() {
		return settings.mode == Mode.KYMOGRAPH && (archiveType == null ||
			archiveType.supportsKymograph());
	}

	private boolean isSingleMolecule() {
		return archiveType == KymographArchiveType.SINGLE_MOLECULE;
	}

	private void buildSections() {
		// Figure
		Section figure = new Section("Figure", true);
		ToggleGroup modeGroup = new ToggleGroup();
		ToggleButton kymographButton = new ToggleButton("Kymograph");
		ToggleButton montageButton = new ToggleButton("Montage");
		kymographButton.setToggleGroup(modeGroup);
		montageButton.setToggleGroup(modeGroup);
		modeGroup.selectedToggleProperty().addListener((ob, o, n) -> {
			if (n == null) {
				o.setSelected(true);
				return;
			}
			if (pushing) return;
			settings.mode = n == kymographButton ? Mode.KYMOGRAPH : Mode.MONTAGE;
			visibilityUpdaters.forEach(Runnable::run);
		});
		pushers.add(() -> (settings.mode == Mode.KYMOGRAPH ? kymographButton
			: montageButton).setSelected(true));
		HBox modeBox = new HBox(kymographButton, montageButton);
		figure.add("Type", modeBox, () -> archiveType == null || archiveType
			.supportsKymograph());

		TextField titleField = new TextField();
		titleField.setPromptText("Molecule UID");
		titleField.textProperty().addListener((ob, o, n) -> {
			if (!pushing) settings.title = n.isEmpty() ? null : n;
		});
		pushers.add(() -> titleField.setText(settings.title == null ? ""
			: settings.title));
		figure.add("Title", titleField);

		// Source
		Section source = new Section("Source", true);
		source.add("Projection width", row(intField(1, 1000,
			() -> settings.projectionWidth, v -> settings.projectionWidth = v),
			pixels()), this::isKymograph);
		source.add("Border", row(new Label("W"), intField(0, 1000,
			() -> settings.borderWidth, v -> settings.borderWidth = v), new Label(
				"H"), intField(0, 1000, () -> settings.borderHeight,
					v -> settings.borderHeight = v), pixels()), () -> !isKymograph() &&
						!isSingleMolecule());
		source.add("Spot radius", row(intField(1, 1000, () -> settings.spotRadius,
			v -> settings.spotRadius = v), pixels()), this::isSingleMolecule);
		TextField xLocation = textField(() -> settings.xLocationSource,
			v -> settings.xLocationSource = v);
		TextField yLocation = textField(() -> settings.yLocationSource,
			v -> settings.yLocationSource = v);
		xLocation.setPrefColumnCount(4);
		yLocation.setPrefColumnCount(4);
		source.add("Location", row(new Label("X"), xLocation, new Label("Y"),
			yLocation), this::isSingleMolecule);
		source.add("", checkBox("From parameters",
			() -> settings.locationFromParameters,
			v -> settings.locationFromParameters = v), this::isSingleMolecule);

		// Time points
		Section frames = new Section("Time points", true);
		frameSlider = new RangeSlider(0, 1, 0, 1);
		// Not snapping to ticks, so every time point can be reached.
		frameSlider.setSnapToTicks(false);
		frameSlider.setBlockIncrement(1);
		frameSlider.setMaxWidth(Double.MAX_VALUE);
		frameMinSpinner = new Spinner<>(0, 1, 0);
		frameMaxSpinner = new Spinner<>(0, 1, 1);
		for (Spinner<Integer> spinner : List.of(frameMinSpinner, frameMaxSpinner)) {
			spinner.setEditable(true);
			compact(spinner);
			commitOnFocusLost(spinner);
		}
		frameMinSpinner.setTooltip(new Tooltip("First time point"));
		frameMaxSpinner.setTooltip(new Tooltip("Last time point"));
		ChangeListener<Number> frameListener = (ob, o, n) -> {
			if (syncingFrames) return;
			setFrameRange((int) Math.round(frameSlider.getLowValue()), (int) Math
				.round(frameSlider.getHighValue()));
		};
		frameSlider.lowValueProperty().addListener(frameListener);
		frameSlider.highValueProperty().addListener(frameListener);
		ChangeListener<Integer> spinnerListener = (ob, o, n) -> {
			if (syncingFrames || n == null) return;
			setFrameRange(frameMinSpinner.getValue(), frameMaxSpinner.getValue());
		};
		frameMinSpinner.valueProperty().addListener(spinnerListener);
		frameMaxSpinner.valueProperty().addListener(spinnerListener);
		frames.add("Range", frameSlider);
		frames.add("", row(new Label("from"), frameMinSpinner, new Label("to"),
			frameMaxSpinner));
		Label reductionBy = new Label("by");
		Spinner<Integer> reductionFactor = intField(1, 10000,
			() -> settings.frameReductionFactor,
			v -> settings.frameReductionFactor = v);
		reductionFactor.setTooltip(new Tooltip(
			"Number of time points to skip, average or sum."));
		showWhen(() -> settings.frameReduction != FrameReduction.NONE, reductionBy,
			reductionFactor);
		frames.add("Reduction", row(enumField(FrameReduction.values(),
			() -> settings.frameReduction, v -> settings.frameReduction = v),
			reductionBy, reductionFactor));

		// Processing
		Section processing = new Section("Processing", false);
		Label filterSizeLabel = new Label("size");
		Spinner<Double> filterSize = doubleField(0.1, 1000, 0.5,
			() -> settings.filterSize, v -> settings.filterSize = v);
		showWhen(() -> settings.filter != Filter.NONE, filterSizeLabel, filterSize);
		processing.add("Filter", row(enumField(Filter.values(),
			() -> settings.filter, v -> settings.filter = v), filterSizeLabel,
			filterSize));
		filterChannelComboBox = new ComboBox<>();
		filterChannelComboBox.setPrefWidth(COMBO_WIDTH);
		filterChannelComboBox.setMaxWidth(Region.USE_PREF_SIZE);
		filterChannelComboBox.setConverter(new StringConverter<Integer>() {

			@Override
			public String toString(Integer c) {
				return c == null || c == 0 ? "All" : String.valueOf(c);
			}

			@Override
			public Integer fromString(String string) {
				return null;
			}
		});
		filterChannelComboBox.valueProperty().addListener((ob, o, n) -> {
			if (!pushing && n != null) settings.filterChannel = n;
		});
		processing.add("Filter channel", filterChannelComboBox,
			() -> !isKymograph() && settings.filter != Filter.NONE);
		processing.add("Interpolation", doubleField(1, 20, 0.5,
			() -> settings.interpolation, v -> settings.interpolation = v));

		// Layout
		Section layout = new Section("Layout", false);
		ComboBox<Arrangement> arrangement = enumField(Arrangement.values(),
			() -> settings.columns > 0 ? Arrangement.GRID : settings.horizontalLayout
				? Arrangement.SINGLE_ROW : Arrangement.SINGLE_COLUMN, v -> {
					settings.horizontalLayout = v != Arrangement.SINGLE_COLUMN;
					settings.columns = v == Arrangement.GRID ? gridColumns : -1;
				});
		arrangement.setTooltip(new Tooltip(
			"How the time points of the montage are arranged."));
		Label columnsLabel = new Label("columns");
		Spinner<Integer> columns = intField(1, 10000, () -> settings.columns > 0
			? settings.columns : gridColumns, v -> {
				gridColumns = v;
				if (settings.columns > 0) settings.columns = v;
			});
		showWhen(() -> settings.columns > 0, columnsLabel, columns);
		layout.add("Arrange", row(arrangement, columnsLabel, columns),
			() -> !isKymograph());
		layout.add("Spacing", row(intField(0, 1000, () -> settings.spacing,
			v -> settings.spacing = v), pixels()), () -> !isKymograph());
		ComboBox<KymographSettings.Orientation> orientation = enumField(
			KymographSettings.Orientation.values(), () -> settings.orientation,
			v -> settings.orientation = v);
		orientation.setTooltip(new Tooltip(
			"Horizontal keeps time running left to right. The vertical options rotate the figure by 90 degrees to the left or right."));
		layout.add("Orientation", orientation);
		CheckBox horizontalReflection = checkBox("Horizontal",
			() -> settings.horizontalReflection,
			v -> settings.horizontalReflection = v);
		showWhen(() -> !isKymograph(), horizontalReflection);
		layout.add("Reflect", row(checkBox("Vertical",
			() -> settings.verticalReflection, v -> settings.verticalReflection = v),
			horizontalReflection));

		// Channels
		Section channels = new Section("Channels", true);
		channelsBox = new VBox(4);
		channels.addFull(channelsBox);

		// Axes
		Section axes = new Section("Axes", false);
		axes.add("", checkBox("Show x axis", () -> settings.showXAxis,
			v -> settings.showXAxis = v));
		xAxisLabelField = textField(() -> settings.xAxisLabel,
			v -> settings.xAxisLabel = v);
		axes.add("X label", xAxisLabelField, () -> settings.showXAxis);
		axes.add("", checkBox("Automatic range from time",
			() -> settings.autoXAxisRange, v -> settings.autoXAxisRange = v),
			() -> settings.showXAxis);
		axes.add("Time unit", enumField(KymographSettings.TimeUnit.values(),
			() -> settings.timeUnit, v -> {
				// Follow the unit if the label is still one of the defaults.
				if (timeLabel(settings.timeUnit).equals(settings.xAxisLabel)) {
					settings.xAxisLabel = timeLabel(v);
					xAxisLabelField.setText(settings.xAxisLabel);
				}
				settings.timeUnit = v;
			}),
			() -> settings.showXAxis && settings.autoXAxisRange);
		axes.add("X range", row(doubleField(-1e9, 1e9, 1, () -> settings.xAxisMin,
			v -> settings.xAxisMin = v), new Label("to"), doubleField(-1e9, 1e9, 1,
				() -> settings.xAxisMax, v -> settings.xAxisMax = v)),
			() -> settings.showXAxis && !settings.autoXAxisRange);
		axes.add("X step", stepAndPrecision(() -> settings.xStepSize,
			v -> settings.xStepSize = v, () -> settings.xAxisPrecision,
			v -> settings.xAxisPrecision = v), () -> settings.showXAxis);

		axes.add("", checkBox("Show y axis", () -> settings.showYAxis,
			v -> settings.showYAxis = v));
		axes.add("Y label", textField(() -> settings.yAxisLabel,
			v -> settings.yAxisLabel = v), () -> settings.showYAxis);
		axes.add("", checkBox("Automatic range in pixels",
			() -> settings.autoYAxisRange, v -> settings.autoYAxisRange = v),
			() -> settings.showYAxis);
		axes.add("Y range", row(doubleField(-1e9, 1e9, 1, () -> settings.yAxisMin,
			v -> settings.yAxisMin = v), new Label("to"), doubleField(-1e9, 1e9, 1,
				() -> settings.yAxisMax, v -> settings.yAxisMax = v)),
			() -> settings.showYAxis && !settings.autoYAxisRange);
		axes.add("Y step", stepAndPrecision(() -> settings.yStepSize,
			v -> settings.yStepSize = v, () -> settings.yAxisPrecision,
			v -> settings.yAxisPrecision = v), () -> settings.showYAxis);

		// Style
		Section style = new Section("Style", false);
		Spinner<Integer> resolution = intField(1, 50, () -> settings.rescale,
			v -> settings.rescale = v);
		resolution.setTooltip(new Tooltip(
			"Output pixels per image pixel. Higher values give a larger, sharper image with the same layout."));
		style.add("Resolution", row(resolution, new Label("x")));
		style.add("", row(checkBox("Dark theme", () -> settings.darkTheme,
			v -> settings.darkTheme = v), checkBox("Stack channels",
				() -> settings.channelStack, v -> settings.channelStack = v)));
		style.add("Stack spacing", row(intField(0, 1000,
			() -> settings.channelStackSpacing,
			v -> settings.channelStackSpacing = v), pixels()),
			() -> settings.channelStack);
		style.add("Font size", row(new Label("Axis"), intField(4, 200,
			() -> settings.axisFontSize, v -> settings.axisFontSize = v), new Label(
				"Label"), intField(4, 200, () -> settings.labelFontSize,
					v -> settings.labelFontSize = v)));
		style.add("", row(new Label("Title"), intField(4, 200,
			() -> settings.titleFontSize, v -> settings.titleFontSize = v)));
		style.add("Axis line", row(doubleField(0.25, 50, 0.25,
			() -> settings.axisLineWidth, v -> settings.axisLineWidth = v),
			pixels()));
		style.add("Ticks", row(new Label("Length"), intField(0, 100,
			() -> settings.tickLength, v -> settings.tickLength = v), new Label(
				"Width"), doubleField(0.25, 50, 0.25, () -> settings.tickLineWidth,
					v -> settings.tickLineWidth = v)));

		for (Section section : sections)
			sectionsBox.getChildren().add(section.titledPane);
	}

	private HBox stepAndPrecision(DoubleSupplier stepGetter,
		DoubleConsumer stepSetter, IntSupplier precisionGetter,
		IntConsumer precisionSetter)
	{
		Spinner<Double> step = doubleField(0, 1e9, 1, stepGetter, stepSetter);
		step.setTooltip(new Tooltip("Distance between ticks. 0 chooses it automatically."));
		Spinner<Integer> precision = intField(0, 10, precisionGetter,
			precisionSetter);
		precision.setTooltip(new Tooltip("Number of decimals in the tick labels."));
		return row(step, new Label("decimals"), precision);
	}

	private static String timeLabel(KymographSettings.TimeUnit unit) {
		switch (unit) {
			case MINUTES:
				return "Time (min)";
			case FRAMES:
				return "Frame";
			default:
				return "Time (s)";
		}
	}

	private void updateFrameSlider() {
		int max = Math.max(1, numberOfFrames - 1);
		frameSlider.setMax(max);
		frameSlider.setMajorTickUnit(Math.max(1, max / 10.0));
		((SpinnerValueFactory.IntegerSpinnerValueFactory) frameMinSpinner
			.getValueFactory()).setMax(max);
		((SpinnerValueFactory.IntegerSpinnerValueFactory) frameMaxSpinner
			.getValueFactory()).setMax(max);
		int low = Math.max(0, Math.min(settings.minT, max));
		int high = settings.maxT < 0 ? max : Math.min(settings.maxT, max);
		setFrameRange(low, high);
	}

	/**
	 * Set the time point range on the slider, the spinners and, unless the
	 * settings are being pushed into the controls, the settings.
	 */
	private void setFrameRange(int low, int high) {
		int max = Math.max(1, numberOfFrames - 1);
		low = Math.max(0, Math.min(low, max));
		high = Math.max(low, Math.min(high, max));
		syncingFrames = true;
		try {
			// Order the updates so the low value never exceeds the high value.
			if (low > frameSlider.getHighValue()) {
				frameSlider.setHighValue(high);
				frameSlider.setLowValue(low);
			}
			else {
				frameSlider.setLowValue(low);
				frameSlider.setHighValue(high);
			}
			frameMinSpinner.getValueFactory().setValue(low);
			frameMaxSpinner.getValueFactory().setValue(high);
		}
		finally {
			syncingFrames = false;
		}
		if (pushing) return;
		settings.minT = low;
		settings.maxT = high >= max ? -1 : high;
	}

	private void rebuildChannels() {
		channelsBox.getChildren().clear();
		Integer filterChannel = settings.filterChannel;
		filterChannelComboBox.getItems().clear();
		filterChannelComboBox.getItems().add(0);

		Set<String> luts = new LinkedHashSet<>();
		for (String lut : BUILT_IN_LUTS)
			luts.add(lut);
		try {
			String[] installed = IJ.getLuts();
			if (installed != null) for (String lut : installed)
				luts.add(lut);
		}
		catch (Throwable e) {
			// ImageJ is not initialized, use the built in LUTs.
		}

		for (ChannelSettings channel : settings.channels) {
			filterChannelComboBox.getItems().add(channel.channel);

			CheckBox visible = new CheckBox("C" + channel.channel);
			visible.setMinWidth(Region.USE_PREF_SIZE);
			visible.setTooltip(new Tooltip("Show channel " + channel.channel));
			visible.setSelected(channel.visible);
			visible.selectedProperty().addListener((ob, o, n) -> channel.visible = n);

			// Only existing LUTs can be picked. Typing filters the list.
			SearchableComboBox<String> lut = new SearchableComboBox<>();
			lut.getItems().addAll(luts);
			if (channel.lut != null && !lut.getItems().contains(channel.lut)) lut
				.getItems().add(channel.lut);
			lut.setValue(channel.lut);
			lut.setPrefWidth(COMBO_WIDTH);
			lut.setMinWidth(60);
			lut.valueProperty().addListener((ob, o, n) -> {
				if (n != null && !n.isEmpty()) channel.lut = n;
			});

			CheckBox auto = new CheckBox("Auto");
			auto.setMinWidth(Region.USE_PREF_SIZE);
			auto.setSelected(channel.autoDisplayRange);
			auto.setTooltip(new Tooltip(
				"Calculate the display range automatically."));

			// The range used last time, shown while the range is automatic.
			Label autoRange = new Label(KymographScriptBuilder.number(
				channel.displayRangeMin) + " - " + KymographScriptBuilder.number(
					channel.displayRangeMax));
			autoRange.setStyle("-fx-opacity: 0.6;");
			autoRange.setTooltip(new Tooltip("Display range used last time"));

			TextField min = numberTextField(channel.displayRangeMin,
				v -> channel.displayRangeMin = v);
			TextField max = numberTextField(channel.displayRangeMax,
				v -> channel.displayRangeMax = v);
			HBox range = row(new Label("Min"), min, new Label("Max"), max);
			range.setPadding(new Insets(0, 0, 2, 24));

			Runnable updateAuto = () -> {
				boolean isAuto = channel.autoDisplayRange;
				autoRange.setVisible(isAuto && channel.displayRangeMax > channel.displayRangeMin &&
					!(channel.displayRangeMin == 0 && channel.displayRangeMax == 1));
				range.setVisible(!isAuto);
				range.setManaged(!isAuto);
			};
			updateAuto.run();
			auto.selectedProperty().addListener((ob, o, n) -> {
				channel.autoDisplayRange = n;
				updateAuto.run();
			});

			channelsBox.getChildren().add(new VBox(2, row(visible, lut, auto,
				autoRange), range));
		}
		filterChannelComboBox.setValue(filterChannelComboBox.getItems().contains(
			filterChannel) ? filterChannel : 0);
	}

	// ---------------------------------------------------------------------
	// Control helpers

	private Spinner<Integer> intField(int min, int max, IntSupplier getter,
		IntConsumer setter)
	{
		Spinner<Integer> spinner = new Spinner<>(min, max, Math.max(min, Math.min(
			max, getter.getAsInt())));
		spinner.setEditable(true);
		compact(spinner);
		commitOnFocusLost(spinner);
		spinner.valueProperty().addListener((ob, o, n) -> {
			if (!pushing && n != null) setter.accept(n);
		});
		pushers.add(() -> spinner.getValueFactory().setValue(Math.max(min, Math
			.min(max, getter.getAsInt()))));
		return spinner;
	}

	private Spinner<Double> doubleField(double min, double max, double step,
		DoubleSupplier getter, DoubleConsumer setter)
	{
		SpinnerValueFactory.DoubleSpinnerValueFactory factory =
			new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, getter
				.getAsDouble(), step);
		factory.setConverter(new StringConverter<Double>() {

			@Override
			public String toString(Double value) {
				return value == null ? "" : KymographScriptBuilder.number(value);
			}

			@Override
			public Double fromString(String string) {
				try {
					return Double.parseDouble(string.trim());
				}
				catch (NumberFormatException e) {
					return factory.getValue();
				}
			}
		});
		Spinner<Double> spinner = new Spinner<>(factory);
		spinner.setEditable(true);
		compact(spinner);
		commitOnFocusLost(spinner);
		spinner.valueProperty().addListener((ob, o, n) -> {
			if (!pushing && n != null) setter.accept(n);
		});
		pushers.add(() -> factory.setValue(getter.getAsDouble()));
		return spinner;
	}

	private static void compact(Spinner<?> spinner) {
		spinner.getEditor().setPrefColumnCount(3);
		spinner.setPrefWidth(SPINNER_WIDTH);
		spinner.setMinWidth(SPINNER_WIDTH);
		spinner.setMaxWidth(Region.USE_PREF_SIZE);
	}

	/**
	 * Controls in a single row, for related settings that belong together.
	 */
	private static HBox row(Node... nodes) {
		HBox row = new HBox(6, nodes);
		row.setAlignment(Pos.CENTER_LEFT);
		for (Node node : nodes)
			if (node instanceof Label) ((Label) node).setMinWidth(
				Region.USE_PREF_SIZE);
		return row;
	}

	/** A unit label for pixel values. */
	private static Label pixels() {
		Label label = new Label("px");
		label.setTooltip(new Tooltip("pixels"));
		return label;
	}

	/**
	 * Show the nodes only while the condition holds.
	 */
	private void showWhen(Supplier<Boolean> condition, Node... nodes) {
		visibilityUpdaters.add(() -> {
			boolean show = condition.get();
			for (Node node : nodes) {
				node.setVisible(show);
				node.setManaged(show);
			}
		});
	}

	private static <T> void commitOnFocusLost(Spinner<T> spinner) {
		spinner.focusedProperty().addListener((ob, o, focused) -> {
			if (!focused) {
				try {
					spinner.getValueFactory().setValue(spinner.getValueFactory()
						.getConverter().fromString(spinner.getEditor().getText()));
				}
				catch (RuntimeException e) {
					spinner.getEditor().setText(spinner.getValueFactory().getConverter()
						.toString(spinner.getValue()));
				}
			}
		});
	}

	private TextField textField(Supplier<String> getter,
		Consumer<String> setter)
	{
		TextField field = new TextField();
		field.textProperty().addListener((ob, o, n) -> {
			if (!pushing) setter.accept(n);
		});
		pushers.add(() -> field.setText(getter.get() == null ? "" : getter.get()));
		return field;
	}

	private TextField numberTextField(double value, DoubleConsumer setter) {
		TextField field = new TextField(KymographScriptBuilder.number(value));
		field.setPrefColumnCount(5);
		field.textProperty().addListener((ob, o, n) -> {
			try {
				setter.accept(Double.parseDouble(n.trim()));
			}
			catch (NumberFormatException e) {
				// Keep the last valid value.
			}
		});
		return field;
	}

	private CheckBox checkBox(String text, Supplier<Boolean> getter,
		Consumer<Boolean> setter)
	{
		CheckBox checkBox = new CheckBox(text);
		checkBox.selectedProperty().addListener((ob, o, n) -> {
			if (pushing) return;
			setter.accept(n);
			visibilityUpdaters.forEach(Runnable::run);
		});
		pushers.add(() -> checkBox.setSelected(getter.get()));
		return checkBox;
	}

	private <E extends Enum<E>> ComboBox<E> enumField(E[] values,
		Supplier<E> getter, Consumer<E> setter)
	{
		ComboBox<E> comboBox = new ComboBox<>();
		comboBox.getItems().addAll(values);
		comboBox.setPrefWidth(COMBO_WIDTH);
		comboBox.setMinWidth(COMBO_WIDTH);
		comboBox.setMaxWidth(Region.USE_PREF_SIZE);
		comboBox.setConverter(new StringConverter<E>() {

			@Override
			public String toString(E value) {
				if (value == null) return "";
				String name = value.name().replace('_', ' ').toLowerCase();
				return Character.toUpperCase(name.charAt(0)) + name.substring(1);
			}

			@Override
			public E fromString(String string) {
				return null;
			}
		});
		comboBox.valueProperty().addListener((ob, o, n) -> {
			if (pushing || n == null) return;
			setter.accept(n);
			visibilityUpdaters.forEach(Runnable::run);
		});
		pushers.add(() -> comboBox.setValue(getter.get()));
		return comboBox;
	}

	/**
	 * A collapsible section with a two column grid of labeled controls.
	 */
	private class Section {

		private final TitledPane titledPane;
		private final GridPane grid;
		private int row = 0;

		Section(String title, boolean expanded) {
			grid = new GridPane();
			grid.setHgap(8);
			grid.setVgap(4);
			grid.setPadding(new Insets(4, 6, 6, 6));
			ColumnConstraints labels = new ColumnConstraints();
			labels.setMinWidth(Region.USE_PREF_SIZE);
			ColumnConstraints controls = new ColumnConstraints();
			controls.setHgrow(Priority.ALWAYS);
			controls.setFillWidth(true);
			grid.getColumnConstraints().addAll(labels, controls);
			titledPane = new TitledPane(title, grid);
			titledPane.setAnimated(false);
			titledPane.setExpanded(expanded);
			sections.add(this);
		}

		void add(String label, Node control) {
			add(label, control, null);
		}

		void add(String label, Node control, Supplier<Boolean> visible) {
			Label labelNode = new Label(label);
			labelNode.setMinWidth(Region.USE_PREF_SIZE);
			grid.add(labelNode, 0, row);
			grid.add(control, 1, row);
			row++;
			if (visible != null) visibilityUpdaters.add(() -> {
				boolean show = visible.get();
				for (Node node : new Node[] { labelNode, control }) {
					node.setVisible(show);
					node.setManaged(show);
				}
			});
		}

		void addFull(Node node) {
			grid.add(node, 0, row, 2, 1);
			row++;
		}

		void setVisible(boolean visible) {
			titledPane.setVisible(visible);
			titledPane.setManaged(visible);
		}
	}
}
