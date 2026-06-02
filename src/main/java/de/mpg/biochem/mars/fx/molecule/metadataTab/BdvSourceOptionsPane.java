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

package de.mpg.biochem.mars.fx.molecule.metadataTab;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import de.mpg.biochem.mars.n5.*;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.concurrent.Task;
import javafx.util.Duration;
import org.controlsfx.control.ToggleSwitch;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.ij.N5Importer.N5BasePathFun;
import org.janelia.saalfeldlab.n5.metadata.imagej.ImagePlusLegacyMetadataParser;
import org.janelia.saalfeldlab.n5.ui.DatasetSelectorDialog;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import org.janelia.saalfeldlab.n5.DatasetAttributes;

import org.janelia.saalfeldlab.n5.ui.N5DatasetTreeCellRenderer;
import org.janelia.saalfeldlab.n5.universe.metadata.*;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadataParser;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.metadata.MarsBdvSource;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class BdvSourceOptionsPane extends VBox {

	private TextField m00, m01, m02, m10, m11, m12, cField, tField, pathField, n5Dataset;
	private Label datasetInfo;
	private ToggleSwitch driftCorrectSwitch, oneTimePointSwitch;
	private BooleanProperty driftCorrect = new SimpleBooleanProperty();
	private BooleanProperty singleTimePoint = new SimpleBooleanProperty();
	private Button pathButton;
	// private Label pathValidation;
	private MarsBdvSource marsBdvSource;

	private HBox n5OptionsHBox;

	// Icons reused across validation updates.
	private Text times, check, times2, check2;

	// Spinning refresh icons shown while a background check is running.
	private Text pathRefresh, datasetRefresh;
	private RotateTransition pathRefreshSpin, datasetRefreshSpin;

	// Validation labels (need to be reachable from the background task callbacks).
	private Label pathValidation, datasetValidation;

	// Single-thread daemon executor for all background validation work.
	private final ExecutorService validationExecutor = Executors
			.newSingleThreadExecutor(new ThreadFactory() {

				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r, "BdvSource-validation");
					t.setDaemon(true);
					return t;
				}
			});

	// The currently running (or last-launched) validation task. We keep a single
	// shared reference for both fields so that whichever field fired last wins,
	// and we cancel any in-flight task before launching a new one. Combined with
	// the 500 ms PauseTransition this guarantees at most one validation in flight.
	private Task<ValidationResult> currentValidationTask;

	public BdvSourceOptionsPane() {
		setPadding(new Insets(15, 20, 15, 20));
		setSpacing(5);
		setFillWidth(true);

		getStyleClass().add("bdv-source-options");

		GridPane gridpane1 = new GridPane();

		// For reference...
		// Insets(double top, double right, double bottom, double left)

		Label m00Label = new Label("m00");
		gridpane1.add(m00Label, 0, 0);
		GridPane.setMargin(m00Label, new Insets(0, 5, 10, 5));

		m00 = new TextField();
		m00.textProperty().addListener((observable, oldValue,
										newValue) -> setAffineTransform3D(m00.getText(), 0, 0));
		m00.setPrefWidth(80);
		m00.setMaxWidth(80);
		gridpane1.add(m00, 1, 0);
		GridPane.setMargin(m00, new Insets(0, 5, 10, 5));

		Label m01Label = new Label("m01");
		gridpane1.add(m01Label, 2, 0);
		GridPane.setMargin(m01Label, new Insets(0, 5, 10, 5));

		m01 = new TextField();
		m01.textProperty().addListener((observable, oldValue,
										newValue) -> setAffineTransform3D(m01.getText(), 0, 1));
		m01.setPrefWidth(80);
		m01.setMaxWidth(80);
		gridpane1.add(m01, 3, 0);
		GridPane.setMargin(m01, new Insets(0, 5, 10, 5));

		Label m02Label = new Label("m02");
		gridpane1.add(m02Label, 4, 0);
		GridPane.setMargin(m02Label, new Insets(0, 5, 10, 5));

		m02 = new TextField();
		m02.textProperty().addListener((observable, oldValue,
										newValue) -> setAffineTransform3D(m02.getText(), 0, 3));
		m02.setPrefWidth(80);
		m02.setMaxWidth(80);
		gridpane1.add(m02, 5, 0);
		GridPane.setMargin(m02, new Insets(0, 5, 10, 5));

		getChildren().add(gridpane1);

		// Option to lock Y-range
		GridPane gridpane2 = new GridPane();

		Label m10Label = new Label("m10");
		gridpane2.add(m10Label, 0, 0);
		GridPane.setMargin(m10Label, new Insets(0, 5, 10, 5));

		m10 = new TextField();
		m10.textProperty().addListener((observable, oldValue,
										newValue) -> setAffineTransform3D(m10.getText(), 1, 0));
		m10.setPrefWidth(80);
		m10.setMaxWidth(80);
		gridpane2.add(m10, 1, 0);
		GridPane.setMargin(m10, new Insets(0, 5, 10, 5));

		Label m11Label = new Label("m11");
		gridpane2.add(m11Label, 2, 0);
		GridPane.setMargin(m11Label, new Insets(0, 5, 10, 5));

		m11 = new TextField();
		m11.textProperty().addListener((observable, oldValue,
										newValue) -> setAffineTransform3D(m11.getText(), 1, 1));
		m11.setPrefWidth(80);
		m11.setMaxWidth(80);
		gridpane2.add(m11, 3, 0);
		GridPane.setMargin(m11, new Insets(0, 5, 10, 5));

		Label m12Label = new Label("m12");
		gridpane2.add(m12Label, 4, 0);
		GridPane.setMargin(m12Label, new Insets(0, 5, 10, 5));

		m12 = new TextField();
		m12.textProperty().addListener((observable, oldValue,
										newValue) -> setAffineTransform3D(m12.getText(), 1, 3));
		m12.setPrefWidth(80);
		m12.setMaxWidth(80);
		gridpane2.add(m12, 5, 0);
		GridPane.setMargin(m12, new Insets(0, 5, 10, 5));

		getChildren().add(gridpane2);

		GridPane gridpane3 = new GridPane();

		driftCorrectSwitch = new ToggleSwitch();
		driftCorrect.setValue(false);
		driftCorrectSwitch.selectedProperty().bindBidirectional(driftCorrect);
		driftCorrect.addListener((observable, oldValue, newValue) -> {
			if (marsBdvSource != null) marsBdvSource.setCorrectDrift(newValue);
		});

		Label driftCorrectLabel = new Label("Drift Correct");
		gridpane3.add(driftCorrectLabel, 0, 0);
		GridPane.setMargin(driftCorrectLabel, new Insets(0, 5, 10, 5));

		gridpane3.add(driftCorrectSwitch, 1, 0);
		GridPane.setMargin(driftCorrectSwitch, new Insets(0, 5, 10, 5));

		getChildren().add(gridpane3);

		HBox pathBox = new HBox();
		pathBox.setAlignment(Pos.CENTER_LEFT);

		Label filePathLabel = new Label("Path");
		filePathLabel.setPrefWidth(45);
		filePathLabel.setMaxWidth(45);
		HBox.setMargin(filePathLabel, new Insets(0, 5, 10, 5));
		pathBox.getChildren().add(filePathLabel);

		pathField = new TextField();
		HBox.setMargin(pathField, new Insets(0, 5, 10, 5));
		HBox.setHgrow(pathField, Priority.ALWAYS);
		pathBox.getChildren().add(pathField);

		times = FontAwesomeIconFactory.get().createIcon(
				de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TIMES, "1.5em");
		times.setStyle(times.getStyle() + "-fx-fill: red !important;");

		check = FontAwesomeIconFactory.get().createIcon(
				de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CHECK, "1.5em");
		check.setStyle(check.getStyle() + "-fx-fill: green !important;");

		// Spinning refresh icon for the Path field.
		pathRefresh = FontAwesomeIconFactory.get().createIcon(
				de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.REFRESH, "1.5em");
		pathRefreshSpin = new RotateTransition(Duration.millis(800), pathRefresh);
		pathRefreshSpin.setByAngle(360);
		pathRefreshSpin.setCycleCount(RotateTransition.INDEFINITE);
		pathRefreshSpin.setInterpolator(javafx.animation.Interpolator.LINEAR);

		pathValidation = new Label("");
		pathValidation.setGraphic(times);
		HBox.setMargin(pathValidation, new Insets(0, 5, 10, 5));
		pathBox.getChildren().add(pathValidation);

		pathButton = new Button("Browse");
		pathButton.setPrefWidth(70);
		pathButton.setMaxWidth(70);
		pathButton.setOnAction(e -> {
			if (marsBdvSource.isN5()) {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						DatasetSelectorDialog selectionDialog = new DatasetSelectorDialog(
								new MarsN5ViewerReaderFun(), new N5BasePathFun(),
								pathField.getText(),
								new N5MetadataParser[]{ new OmeNgffMetadataParser() }, // need the ngff parser because it's where the metadata are
								new N5MetadataParser[] { new ImagePlusLegacyMetadataParser(),
										new N5CosemMetadataParser(),
										new N5SingleScaleMetadataParser(),
										new CanonicalMetadataParser(),
										new N5GenericSingleScaleMetadataParser() });

						selectionDialog.setVirtualOption(false);
						selectionDialog.setCropOption(false);

						selectionDialog.setTreeRenderer(new N5DatasetTreeCellRenderer(
								true));

						// Prevents NullPointerException
						selectionDialog.setContainerPathUpdateCallback(x -> {});

						final Consumer<DataSelection> callback = (
								DataSelection dataSelection) -> {
							Platform.runLater(new Runnable() {

								@Override
								public void run() {
									String baseDialogPath = selectionDialog.getN5RootPath();
									String datasetPath = dataSelection.metadata.get(0).getPath();

									//This mess is so that the n5rootPath ends with the n5 directory
									//and the dataset is the path after that
									//the dialog datasetPath that is returned can include the n5 directory otherwise.
									StringBuilder n5RootPath = new StringBuilder(baseDialogPath);
									StringBuilder n5DatasetPath = new StringBuilder(datasetPath);
									if (!baseDialogPath.endsWith(".n5")) {
										String[] parts = datasetPath.split("/");

										for (int i = 0; i < parts.length; i++) {
											if (!n5RootPath.toString().endsWith("/")) n5RootPath.append("/");
											n5RootPath.append(parts[i]);
											if (parts[i].endsWith(".n5")) {
												n5DatasetPath = new StringBuilder();
												n5DatasetPath.append(parts[i + 1]);
												for (int j = i + 2; j < parts.length; j++) {
													n5DatasetPath.append("/").append(parts[j]);
												}
												break;
											}
										}
									}

									//Update the source
									marsBdvSource.setPath(n5RootPath.toString());
									marsBdvSource.setN5Dataset(n5DatasetPath.toString());
									String info = getDatasetInfo(
											((N5DatasetMetadata) dataSelection.metadata.get(0))
													.getAttributes());
									marsBdvSource.setProperty("info", info);

									//Update the fields
									pathField.setText(n5RootPath.toString());
									n5Dataset.setText(n5DatasetPath.toString());
									datasetInfo.setText(info);
								}
							});
						};

						selectionDialog.run(callback);
					}
				});
			}
			else {
				final File path = (pathField.getText().trim().equals("")) ? new File(
						"") : new File(pathField.getText().trim());
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Select xml");
				if (path.getParent() != null) {
					File startingDirectory = new File(path.getParent());
					if (startingDirectory.exists()) fileChooser.setInitialDirectory(
							startingDirectory);
				}
				fileChooser.getExtensionFilters().add(new ExtensionFilter("xml file",
						"*.xml"));
				File file = fileChooser.showOpenDialog(getScene().getWindow());

				if (file != null) {
					marsBdvSource.setPath(file.getAbsolutePath());
					pathField.setText(file.getAbsolutePath());
					datasetInfo.setText("");
				}
			}
		});
		HBox.setMargin(pathButton, new Insets(0, 5, 10, 5));
		pathBox.getChildren().add(pathButton);

		getChildren().add(pathBox);

		n5OptionsHBox = new HBox();
		n5OptionsHBox.setAlignment(Pos.CENTER_LEFT);

		Label n5Label = new Label("Dataset");
		n5Label.setPrefWidth(45);
		n5Label.setMaxWidth(45);
		HBox.setMargin(n5Label, new Insets(0, 5, 10, 5));
		n5OptionsHBox.getChildren().add(n5Label);

		n5Dataset = new TextField();
		n5Dataset.setPrefWidth(150);
		n5Dataset.setMaxWidth(150);
		HBox.setMargin(n5Dataset, new Insets(0, 5, 10, 5));
		n5OptionsHBox.getChildren().add(n5Dataset);

		times2 = FontAwesomeIconFactory.get().createIcon(
				de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TIMES, "1.5em");
		times2.setStyle(times.getStyle() + "-fx-fill: red !important;");

		check2 = FontAwesomeIconFactory.get().createIcon(
				de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CHECK, "1.5em");
		check2.setStyle(check.getStyle() + "-fx-fill: green !important;");

		// Spinning refresh icon for the Dataset field.
		datasetRefresh = FontAwesomeIconFactory.get().createIcon(
				de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.REFRESH, "1.5em");
		datasetRefreshSpin = new RotateTransition(Duration.millis(800),
				datasetRefresh);
		datasetRefreshSpin.setByAngle(360);
		datasetRefreshSpin.setCycleCount(RotateTransition.INDEFINITE);
		datasetRefreshSpin.setInterpolator(javafx.animation.Interpolator.LINEAR);

		datasetValidation = new Label("");
		datasetValidation.setGraphic(times2);
		HBox.setMargin(datasetValidation, new Insets(0, 5, 10, 5));
		n5OptionsHBox.getChildren().add(datasetValidation);

		// Increased to 500 ms before validation begins. The PauseTransition
		// collapses rapid typing into a single trigger; cancelling the previous
		// task (see launchValidation) prevents parallel checks if a task is still
		// running when the next one fires.
		PauseTransition pathDetectionPause = new PauseTransition(Duration.millis(
				500));
		pathField.textProperty().addListener((observable, oldValue, newValue) -> {
			pathDetectionPause.setOnFinished(event -> {
				if (marsBdvSource == null) return;
				marsBdvSource.setPath(pathField.getText());
				datasetInfo.setText("");
				launchValidation(pathField.getText(), n5Dataset.getText());
			});
			pathDetectionPause.playFromStart();
		});

		PauseTransition datasetDetectionPause = new PauseTransition(Duration.millis(
				500));
		n5Dataset.textProperty().addListener((observable, oldValue, newValue) -> {
			datasetDetectionPause.setOnFinished(event -> {
				if (marsBdvSource == null) return;
				marsBdvSource.setN5Dataset(n5Dataset.getText());
				datasetInfo.setText("");
				launchValidation(pathField.getText(), n5Dataset.getText());
			});
			datasetDetectionPause.playFromStart();
		});

		Label cLabel = new Label("C");
		HBox.setMargin(cLabel, new Insets(0, 5, 10, 5));
		n5OptionsHBox.getChildren().add(cLabel);

		cField = new TextField();
		cField.textProperty().addListener((observable, oldValue, newValue) -> {
			if (marsBdvSource != null) {
				try {
					marsBdvSource.setChannel(Integer.valueOf(cField.getText()));
				}
				catch (NumberFormatException e) {
					marsBdvSource.setChannel(0);
				}
			}
		});
		cField.setPrefWidth(50);
		cField.setMaxWidth(50);
		HBox.setMargin(cField, new Insets(0, 5, 10, 5));
		n5OptionsHBox.getChildren().add(cField);

		oneTimePointSwitch = new ToggleSwitch();
		singleTimePoint.setValue(false);
		oneTimePointSwitch.selectedProperty().bindBidirectional(singleTimePoint);
		singleTimePoint.addListener((observable, oldValue, newValue) -> {
			if (newValue.booleanValue()) {
				marsBdvSource.setSingleTimePointMode(true);
			}
			else marsBdvSource.setSingleTimePointMode(false);
		});

		Label singleTimePointLabel = new Label("Single Time Point Overlay");
		HBox.setMargin(singleTimePointLabel, new Insets(0, 5, 10, 5));
		n5OptionsHBox.getChildren().add(singleTimePointLabel);

		HBox.setMargin(oneTimePointSwitch, new Insets(0, 5, 10, 5));
		n5OptionsHBox.getChildren().add(oneTimePointSwitch);

		Label tLabel = new Label("T");
		HBox.setMargin(tLabel, new Insets(0, 5, 10, 5));
		n5OptionsHBox.getChildren().add(tLabel);

		tField = new TextField();
		tField.textProperty().addListener((observable, oldValue, newValue) -> {
			if (marsBdvSource != null) marsBdvSource.setSingleTimePoint(Integer
					.valueOf(tField.getText()));
		});
		tField.setPrefWidth(50);
		tField.setMaxWidth(50);
		HBox.setMargin(tField, new Insets(0, 5, 10, 5));
		n5OptionsHBox.getChildren().add(tField);

		getChildren().add(n5OptionsHBox);

		GridPane infoGridpane = new GridPane();

		datasetInfo = new Label();
		infoGridpane.add(datasetInfo, 0, 0);
		GridPane.setMargin(datasetInfo, new Insets(0, 5, 10, 5));

		getChildren().add(infoGridpane);
	}

	/**
	 * Immutable result carried back from the background validation task to the
	 * JavaFX thread.
	 */
	private static final class ValidationResult {

		final boolean pathExists;
		final boolean datasetExists;
		final String info; // null if no attributes were available

		ValidationResult(boolean pathExists, boolean datasetExists, String info) {
			this.pathExists = pathExists;
			this.datasetExists = datasetExists;
			this.info = info;
		}
	}

	/**
	 * Launches a background validation of the given path and dataset. Any
	 * previously running task is cancelled first, so at most one validation is
	 * ever in flight. All N5 I/O happens off the JavaFX Application Thread; UI
	 * updates are applied in the task's succeeded handler (which runs on the FX
	 * thread).
	 */
	private void launchValidation(final String path, final String dataset) {
		// Cancel any in-flight (or queued) validation so we never run parallel
		// checks even if the user types fast enough to start one before the
		// previous finishes.
		if (currentValidationTask != null && currentValidationTask.isRunning()) {
			currentValidationTask.cancel();
		}

		// Show spinning refresh icons while the check runs.
		startSpin();

		final Task<ValidationResult> task = new Task<ValidationResult>() {

			@Override
			protected ValidationResult call() throws Exception {
				boolean pathExists;
				boolean datasetExists;
				String info = null;
				N5Reader reader = null;
				try {
					reader = new MarsN5ViewerReaderFun().apply(path);
					if (isCancelled()) return null;
					pathExists = reader.exists("/");
					datasetExists = reader.exists(dataset);
					if (pathExists && reader != null) {
						DatasetAttributes attributes = reader.getDatasetAttributes(
								dataset);
						if (attributes != null) info = getDatasetInfo(attributes);
					}
				}
				catch (Exception e) {
					pathExists = false;
					datasetExists = false;
				}
				return new ValidationResult(pathExists, datasetExists, info);
			}
		};

		task.setOnSucceeded(e -> {
			// Ignore stale results: only apply if this is still the current task.
			if (task != currentValidationTask) return;
			stopSpin();

			ValidationResult result = task.getValue();
			if (result == null) return;

			if (result.pathExists) {
				pathValidation.setGraphic(check);
				if (result.datasetExists) datasetValidation.setGraphic(check2);
				else datasetValidation.setGraphic(times2);

				if (result.info != null) {
					if (marsBdvSource != null) marsBdvSource.setProperty("info",
							result.info);
					datasetInfo.setText(result.info);
				}
			}
			else {
				pathValidation.setGraphic(times);
				datasetValidation.setGraphic(times2);
			}
		});

		task.setOnFailed(e -> {
			if (task != currentValidationTask) return;
			stopSpin();
			pathValidation.setGraphic(times);
			datasetValidation.setGraphic(times2);
		});

		task.setOnCancelled(e -> {
			// A newer task has taken over; it will manage the spinner and icons.
		});

		currentValidationTask = task;
		validationExecutor.submit(task);
	}

	/** Show and start spinning the refresh icons on both validation labels. */
	private void startSpin() {
		pathValidation.setGraphic(pathRefresh);
		datasetValidation.setGraphic(datasetRefresh);
		pathRefreshSpin.playFromStart();
		datasetRefreshSpin.playFromStart();
	}

	/** Stop spinning the refresh icons. */
	private void stopSpin() {
		pathRefreshSpin.stop();
		datasetRefreshSpin.stop();
		pathRefresh.setRotate(0);
		datasetRefresh.setRotate(0);
	}

	/**
	 * Shuts down the background executor. Call this when the pane is being
	 * disposed of (optional, since the executor uses daemon threads).
	 */
	public void shutdown() {
		validationExecutor.shutdownNow();
	}

	private void setAffineTransform3D(String value, int m, int n) {
		if (marsBdvSource == null) return;

		double number;
		try {
			number = Double.valueOf(value);
		}
		catch (NumberFormatException e) {
			return;
		}

		marsBdvSource.getAffineTransform3D().set(number, m, n);
	}

	public static String getDatasetInfo(DatasetAttributes attributes) {
		final String dimString = String.join(" x ", Arrays.stream(attributes
				.getDimensions()).mapToObj(d -> Long.toString(d)).collect(Collectors
				.toList()));
		return "Dimensions " + dimString + ", " + attributes.getDataType();
	}

	public void setMarsBdvSource(MarsBdvSource marsBdvSource) {
		if (marsBdvSource == null) {
			this.marsBdvSource = null;

			m00.setText("1.0");
			m01.setText("0.0");
			m02.setText("0.0");
			m10.setText("0.0");
			m11.setText("1.0");
			m12.setText("0.0");
			driftCorrect.set(false);
			pathField.setText("");

			cField.setText("0");
			tField.setText("0");
			n5Dataset.setText("");
			datasetInfo.setText("");

			for (Node node : getChildren())
				node.setDisable(true);
		}
		else {
			for (Node node : getChildren())
				node.setDisable(false);

			this.marsBdvSource = marsBdvSource;
			m00.setText(String.valueOf(marsBdvSource.getAffineTransform3D().get(0,
					0)));
			m01.setText(String.valueOf(marsBdvSource.getAffineTransform3D().get(0,
					1)));
			m02.setText(String.valueOf(marsBdvSource.getAffineTransform3D().get(0,
					3)));
			m10.setText(String.valueOf(marsBdvSource.getAffineTransform3D().get(1,
					0)));
			m11.setText(String.valueOf(marsBdvSource.getAffineTransform3D().get(1,
					1)));
			m12.setText(String.valueOf(marsBdvSource.getAffineTransform3D().get(1,
					3)));
			driftCorrect.set(marsBdvSource.getCorrectDrift());
			pathField.setText(marsBdvSource.getPath());

			if (marsBdvSource.isN5()) {
				if (!getChildren().contains(n5OptionsHBox)) getChildren().add(
						getChildren().size() - 1, n5OptionsHBox);
				cField.setText(String.valueOf(marsBdvSource.getChannel()));
				n5Dataset.setText(marsBdvSource.getN5Dataset());

				if (marsBdvSource.getSingleTimePointMode()) singleTimePoint.setValue(
						true);
				else singleTimePoint.setValue(false);

				tField.setText(String.valueOf(marsBdvSource.getSingleTimePoint()));

				// Dataset information
				if (marsBdvSource.getProperties().containsKey("info")) datasetInfo
						.setText(marsBdvSource.getProperties().get("info"));
				else datasetInfo.setText("");
			}
			else {
				getChildren().remove(n5OptionsHBox);
				datasetInfo.setText("");
			}
		}
	}
}
