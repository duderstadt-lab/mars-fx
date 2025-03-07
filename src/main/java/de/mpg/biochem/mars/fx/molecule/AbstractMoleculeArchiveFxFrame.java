/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2025 Karl Duderstadt
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

package de.mpg.biochem.mars.fx.molecule;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.FLOPPY_ALT;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.controls.JFXTabPane;

import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import de.mpg.biochem.mars.fx.dialogs.*;
import de.mpg.biochem.mars.io.MoleculeArchiveAmazonS3Source;
import de.mpg.biochem.mars.io.MoleculeArchiveIOFactory;
import de.mpg.biochem.mars.io.MoleculeArchiveSource;
import de.mpg.biochem.mars.molecule.*;
import de.mpg.biochem.mars.n5.*;
import de.mpg.biochem.mars.swingUI.MoleculeArchiveSelector.MoleculeArchiveSaveDialog;
import de.mpg.biochem.mars.swingUI.MoleculeArchiveSelector.MoleculeArchiveSelection;
import de.mpg.biochem.mars.swingUI.MoleculeArchiveSelector.MoleculeArchiveTreeCellRenderer;
import javafx.stage.Window;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.controlsfx.control.MaskerPane;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.prefs.PrefService;
import org.scijava.ui.UIService;

import bdv.util.BdvHandle;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.bdv.MarsBdvFrame;
import de.mpg.biochem.mars.fx.bdv.ViewerTransformSyncStarter;
import de.mpg.biochem.mars.fx.bdv.ViewerTransformSyncStopper;
import de.mpg.biochem.mars.fx.event.InitializeMoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MetadataTagsChangedEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveLockEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveSavedEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveSavingEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveUnlockEvent;
import de.mpg.biochem.mars.fx.event.MoleculeTagsChangedEvent;
import de.mpg.biochem.mars.fx.event.RefreshMetadataEvent;
import de.mpg.biochem.mars.fx.event.RefreshMetadataPropertiesEvent;
import de.mpg.biochem.mars.fx.event.RefreshMoleculeEvent;
import de.mpg.biochem.mars.fx.event.RefreshMoleculePropertiesEvent;
import de.mpg.biochem.mars.fx.event.RunMoleculeArchiveTaskEvent;
import de.mpg.biochem.mars.fx.molecule.metadataTab.MetadataSubPane;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.fx.util.HotKeyEntry;
import de.mpg.biochem.mars.fx.util.IJStage;
import de.mpg.biochem.mars.fx.util.MarsAnimation;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.object.MartianObject;
import de.mpg.biochem.mars.util.DefaultJsonConverter;
import de.mpg.biochem.mars.util.MarsUtil;
import ij.WindowManager;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.JFXPanel;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public abstract class AbstractMoleculeArchiveFxFrame<I extends MarsMetadataTab<? extends MetadataSubPane, ? extends MetadataSubPane>, M extends MoleculesTab<? extends MoleculeSubPane, ? extends MoleculeSubPane>>
	extends AbstractJsonConvertibleRecord implements MoleculeArchiveWindow
{

	@Parameter
	protected MoleculeArchiveService moleculeArchiveService;

	@Parameter
	protected UIService uiService;

	@Parameter
	protected PrefService prefService;

	@Parameter
	protected Context context;

	@Parameter
	protected LogService logService;

	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;

	protected String title;
	protected Stage stage;
	protected IJStage ijStage;

	protected StackPane maskerStackPane;
	protected MaskerPane masker;
	protected MarsAnimation marsSpinning;

	protected BorderPane borderPane;
	protected JFXTabPane tabsContainer;

	// protected TextArea lockLogArea;
	protected ListView<String> lockLogArea;
	protected ObservableList<String> lockLogAreaStrings = FXCollections
		.observableArrayList();

	protected MenuBar menuBar;
	protected HBox menuHBox;
	protected Button showPropertiesButton;

	protected Menu fileMenu, toolsMenu;
	protected BooleanProperty showProperties = new SimpleBooleanProperty(true);

	protected DashboardTab dashboardTab;
	protected CommentsTab commentsTab;

	protected I imageMetadataTab;
	protected M moleculesTab;
	protected SettingsTab settingsTab;

	protected boolean windowStateLoaded = false;

	protected static JsonFactory jfactory;

	protected Set<MoleculeArchiveTab> tabSet;

	protected MarsBdvFrame[] marsBdvFrames;
	protected byte[] roverFileBackground;

	protected double tabWidth = 50.0;

	protected final AtomicBoolean archiveLocked = new AtomicBoolean(false);

	public AbstractMoleculeArchiveFxFrame(
		MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive,
		final Context context)
	{
		super();
		context.inject(this);

		this.title = (archive.getSource() != null && archive.getSource() instanceof MoleculeArchiveAmazonS3Source) ? archive.getName() + " (minio)" : archive.getName();
		this.archive = archive;

		archive.setWindow(this);
	}

	/**
	 * JFXPanel creates a link between Swing and JavaFX.
	 */
	public void init() {
		new JFXPanel(); // initializes JavaFX environment

		// The call to runLater() avoid a mix between JavaFX thread and Swing
		// thread.
		// Allows multiple runLaters in the same session...
		// Suggested here -
		// https://stackoverflow.com/questions/29302837/javafx-platform-runlater-never-running
		Platform.setImplicitExit(false);

		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				stage = new Stage();
				stage.setTitle(title);
				stage.setOnHidden(e -> SwingUtilities.invokeLater(() -> {
					close();
				}));

				ijStage = new IJStage(stage);

				SwingUtilities.invokeLater(() -> WindowManager.addWindow(ijStage));
				buildScene();
			}
		});
	}

	protected Scene buildScene() {
		borderPane = new BorderPane();

		lockLogArea = new ListView<String>();
		lockLogArea.getStyleClass().add("log-text-area");
		lockLogArea.setStyle("-fx-font-family: \"monospace\"; -fx-font-size: 10pt");
		lockLogArea.setItems(lockLogAreaStrings);
		lockLogArea.setVisible(false);
		lockLogArea.setFixedCellSize(30);

		masker = new MaskerPane();
		masker.setVisible(false);

		marsSpinning = new MarsAnimation();

		masker.setProgressNode(marsSpinning);

		maskerStackPane = new StackPane();
		maskerStackPane.getStylesheets().add(
			"de/mpg/biochem/mars/fx/molecule/MoleculeArchiveFxFrame.css");
		masker.setStyle("-fx-accent: #f5f5f5; -fx-text-fill: #f5f5f5;");
		maskerStackPane.getChildren().add(borderPane);
		maskerStackPane.getChildren().add(lockLogArea);
		maskerStackPane.getChildren().add(masker);

		tabsContainer = new JFXTabPane();
		tabsContainer.prefHeight(128.0);
		tabsContainer.prefWidth(308.0);
		tabsContainer.setSide(Side.LEFT);
		tabsContainer.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		tabsContainer.setTabMinWidth(tabWidth);
		tabsContainer.setTabMaxWidth(tabWidth);
		tabsContainer.setTabMinHeight(tabWidth);
		tabsContainer.setTabMaxHeight(tabWidth);
		tabsContainer.setRotateGraphic(true);

		if (jfactory == null) jfactory = new JsonFactory();

		buildMenuBar();
		buildTabs();

		showProperties.addListener((observable, oldValue, newValue) -> {
			if (newValue.booleanValue()) {
				imageMetadataTab.showProperties();
				moleculesTab.showProperties();
			}
			else {
				imageMetadataTab.hideProperties();
				moleculesTab.hideProperties();
			}
		});

		// Now add tabs to container
		tabSet.forEach(maTab -> tabsContainer.getTabs().add(maTab.getTab()));

		fireEvent(new InitializeMoleculeArchiveEvent(archive));

		borderPane.setCenter(tabsContainer);
		Scene scene = new Scene(maskerStackPane);

		stage.setScene(scene);

		try {
			loadState();
		}
		catch (IOException e) {
			e.printStackTrace();
			logService.warn("A problem was encountered when loading the rover file " +
				archive.getSource().getPath() + ".rover" +
				" containing the mars-fx display settings. \n" +
				"Please check the file to make sure the syntax is correct." +
				"Aborting and opening with the default settings.");
		}
		
		if (!windowStateLoaded) {
			stage.setWidth(800);
			stage.setHeight(600);
			stage.show();
		}

		updateAccelerators();

		getNode().addEventFilter(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT,
			new EventHandler<MoleculeArchiveEvent>()
			{

				@Override
				public void handle(MoleculeArchiveEvent e) {
					if (e.getEventType().getName().equals("RUN_MOLECULE_ARCHIVE_TASK")) {
						runTask(((RunMoleculeArchiveTaskEvent) e).getTask(),
							((RunMoleculeArchiveTaskEvent) e).getMessage());
						e.consume();
					}
				}
			});
		return scene;
	}

	protected void buildTabs() {
		tabSet = new LinkedHashSet<MoleculeArchiveTab>();

		dashboardTab = new DashboardTab(context);
		dashboardTab.getTab().setStyle("-fx-background-color: -fx-focus-color;");
		tabSet.add(dashboardTab);
				
		imageMetadataTab = createImageMetadataTab(context);
		tabSet.add(imageMetadataTab);
		
		moleculesTab = createMoleculesTab(context);
		tabSet.add(moleculesTab);

		commentsTab = new CommentsTab(context);
		tabSet.add(commentsTab);

		settingsTab = new SettingsTab(context);
		tabSet.add(settingsTab);

		// fire save events for tabs as they are left and update events for new tabs
		tabsContainer.getSelectionModel().selectedItemProperty().addListener(
			new ChangeListener<Tab>()
			{
				@Override
				public void changed(ObservableValue<? extends Tab> observable,
					Tab oldValue, Tab newValue)
			{
					tabSet.stream().filter(maTab -> newValue == maTab.getTab())
						.findFirst().ifPresent(maTab -> updateMenus(maTab.getMenus()));

					if (oldValue == commentsTab.getTab()) {
						commentsTab.setEditMode(false);
						commentsTab.saveComments();
					}
					else if (oldValue == imageMetadataTab.getTab()) {
						imageMetadataTab.saveCurrentRecord();
					}
					else if (oldValue == moleculesTab.getTab()) {
						moleculesTab.saveCurrentRecord();
					}
					else if (oldValue == settingsTab.getTab()) {
						settingsTab.save();
						updateAccelerators();
					}

					if (newValue == imageMetadataTab.getTab()) {
						imageMetadataTab.fireEvent(new RefreshMetadataEvent());
						showPropertiesButton();
					}
					else if (newValue == moleculesTab.getTab()) {
						moleculesTab.fireEvent(new RefreshMoleculeEvent());
						showPropertiesButton();
					}
					else {
						hidePropertiesButton();
					}
				}
			});
	}

	protected void updateAccelerators() {
		// Update global accelerators...
		for (HotKeyEntry hotKeyEntry : settingsTab.getHotKeyList()) {
			Runnable rn = () -> {
				if (tabsContainer.getSelectionModel().getSelectedItem() == moleculesTab
					.getTab())
				{
					moleculesTab.getSelectedMolecule().addTag(hotKeyEntry.getTag());
					moleculesTab.fireEvent(new RefreshMoleculePropertiesEvent());
					moleculesTab.fireEvent(new MoleculeTagsChangedEvent(moleculesTab
						.getSelectedMolecule()));
				}
				else if (tabsContainer.getSelectionModel()
					.getSelectedItem() == imageMetadataTab.getTab())
				{
					imageMetadataTab.getSelectedMetadata().addTag(hotKeyEntry.getTag());
					imageMetadataTab.fireEvent(new RefreshMetadataPropertiesEvent());
					imageMetadataTab.fireEvent(new MetadataTagsChangedEvent(
						imageMetadataTab.getSelectedMetadata()));
				}
			};
			getNode().getScene().getAccelerators().put(hotKeyEntry.getShortcut(), rn);
		}
	}

	protected void buildMenuBar() {
		// Build file menu
		Action fileSaveAction = new Action("Save", "Shortcut+S", null,
			e -> save());
		Action fileSaveCopyAction = new Action("Save a Copy...", null, null,
			e -> saveCopy());
		Action fileSaveJsonCopyAction = new Action("Save a Json Copy...", null,
			null, e -> saveJsonCopy());
		Action fileSaveVirtualStoreAction = new Action(
			"Save a Virtual Store Copy...", null, null, e -> saveVirtualStoreCopy());
		Action fileSaveJsonVirtualStoreAction = new Action(
			"Save a Json Virtual Store Copy...", null, null,
			e -> saveJsonVirtualStoreCopy());
		Action fileSaveActionCloudStorageAction = new Action("Save a Cloud Storage Copy...", null, null,
				e -> saveToCloudStorage());
		// Comment this out for now since it doesn't clear the settings before
		// loading the new ones and
		// I have to check on BDV...
		// Action importRoverSettingsAction = new Action("Import Rover Settings...",
		// null, null, e -> importRoverSettings());
		Action fileCloseAction = new Action("Close", null, null, e -> close());

		fileMenu = ActionUtils.createMenu("File", fileSaveAction,
			fileSaveCopyAction, fileSaveJsonCopyAction, fileSaveVirtualStoreAction,
			fileSaveJsonVirtualStoreAction, fileSaveActionCloudStorageAction,
			// null,
			// importRoverSettingsAction,
			null, fileCloseAction);

		// Build tools menu
		Action showVideoAction = new Action("Show Video", null, null,
			e -> showVideo());
		Action deleteMoleculesAction = new Action("Delete Molecules", null, null,
			e -> deleteMolecules());
		Action deleteMoleculeTagsAction = new Action("Delete Molecule Tags", null,
			null, e -> deleteMoleculeTags());
		Action deleteMoleculeParametersAction = new Action(
			"Delete Molecule Parameters", null, null,
			e -> deleteMoleculeParameters());
		Action deleteMoleculeRegionsAction = new Action("Delete Molecule Regions",
			null, null, e -> deleteMoleculeRegions());
		Action deleteMoleculePositionsAction = new Action(
			"Delete Molecule Positions", null, null, e -> deleteMoleculePositions());
		Action deleteSegmentTablesAction = new Action("Delete Segment Tables", null,
			null, e -> deleteSegmentTables());

		Action mergeMoleculesAction = new Action("Merge Molecules", null, null,
			e -> mergeMolecules());

		Action rebuildIndexesAction = new Action("Rebuild Indexes", null, null,
			e -> {
				runTask(() -> {
					try {
						archive.rebuildIndexes();
					}
					catch (IOException e1) {
						e1.printStackTrace();
					}
				}, "Rebuilding Indexes...");
			});

		toolsMenu = ActionUtils.createMenu("Tools", deleteMoleculesAction,
			deleteMoleculeTagsAction, deleteMoleculeParametersAction,
			deleteMoleculeRegionsAction, deleteMoleculePositionsAction,
			deleteSegmentTablesAction, mergeMoleculesAction, null, showVideoAction,
			null, rebuildIndexesAction);

		menuBar = new MenuBar(fileMenu, toolsMenu);

		// Setup show properties button but don't add it yet...
		showPropertiesButton = new Button("");
		showPropertiesButton.setStyle(
			"-fx-background-color: -fx-outer-border, -fx-inner-border, -fx-body-color;-fx-background-insets: 0, 1, 2;-fx-background-radius: 5, 4, 3;");
		Text caretRight = FontAwesomeIconFactory.get().createIcon(
			de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CARET_RIGHT, "1.4em");
		caretRight.setStyle(caretRight.getStyle() + "-fx-fill: gray;");
		Text caretLeft = FontAwesomeIconFactory.get().createIcon(
			de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CARET_LEFT, "1.4em");
		caretLeft.setStyle(caretLeft.getStyle() + "-fx-fill: gray;");
		showPropertiesButton.setGraphic(caretRight);

		showPropertiesButton.setOnAction(e -> {
			if (showProperties.get()) {
				showProperties.set(false);
				showPropertiesButton.setGraphic(caretLeft);
			}
			else {
				showProperties.set(true);
				showPropertiesButton.setGraphic(caretRight);
			}
		});

		menuHBox = new HBox(menuBar);
		menuHBox.setAlignment(Pos.CENTER);
		HBox.setHgrow(menuBar, Priority.ALWAYS);
		HBox.setHgrow(showPropertiesButton, Priority.NEVER);

		borderPane.setTop(menuHBox);
	}

	public void showPropertiesButton() {
		menuHBox.getChildren().clear();
		menuHBox.getChildren().add(menuBar);
		menuHBox.getChildren().add(showPropertiesButton);
	}

	public void hidePropertiesButton() {
		menuHBox.getChildren().clear();
		menuHBox.getChildren().add(menuBar);
	}

	protected void showVideo() {
		for (String metaUID : archive.getMetadataUIDs()) {
			MarsMetadata meta = archive.getMetadata(metaUID);
			for (String name : meta.getBdvSourceNames()) {
				boolean exists;
				try {
					exists = new MarsN5ViewerReaderFun().apply(meta.getBdvSource(name).getPath()).exists("/");
				} catch(Exception e) {
					exists = false;
				}
				if (!exists) {
					RoverErrorDialog alert = new RoverErrorDialog(getNode().getScene()
						.getWindow(), "The Bdv source path " + meta.getBdvSource(name).getPath() +
							" of metadata record " + meta.getUID() + " does not exist. " +
							"Please correct the path and try again.");
					alert.show();
					return;
				}
			}
		}

		boolean discoveredBdvFrameSettings = false;
		if (archive.getSource() != null && archive.getSource().getPath() != null) {
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode jsonNode = mapper.readTree(archive.getSource().getRoverInputStream());
				if (jsonNode.get("bdvFrames") != null) discoveredBdvFrameSettings =
					true;
			}
			catch (IOException e) {}
		}

		// Check if there are settings for MarsBdvFrames in the rover file...
		if (discoveredBdvFrameSettings) {
			RoverConfirmationDialog useBdvSettingsDialog =
				new RoverConfirmationDialog(getNode().getScene().getWindow(),
					"Video window settings were discovered.\n" +
						"Would you like to use them?", "Yes", "No");
			useBdvSettingsDialog.showAndWait().ifPresent(result -> {
				if (result.getButtonData().isDefaultButton()) {
					loadBdvSettings();
				}
				else {
					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							if (prefService
									.getBoolean(SettingsTab.class, "activateSynchronizedBdvWindows", false)) {
								ShowVideoDialog dialog = new ShowVideoDialog(getNode().getScene()
										.getWindow());
								dialog.showAndWait().ifPresent(result2 -> buildBdvFrames(result2
										.getViewNumber()));
							} else buildBdvFrames(1);
						}
					});
				}
			});
		}
		else {
			if (prefService
					.getBoolean(SettingsTab.class, "activateSynchronizedBdvWindows", false)) {
				ShowVideoDialog dialog = new ShowVideoDialog(getNode().getScene()
						.getWindow());
				dialog.showAndWait().ifPresent(result2 -> buildBdvFrames(result2
						.getViewNumber()));
			} else buildBdvFrames(1);
		}
	}

	private void buildBdvFrames(int views) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				if (archive != null) {
					BdvHandle[] handles = new BdvHandle[views];
					marsBdvFrames = new MarsBdvFrame[views];
					for (int i = 0; i < views; i++) {
						MarsBdvFrame marsBdvFrame = createMarsBdvFrame(prefService
							.getBoolean(SettingsTab.class, "useN5VolatileViews", true));
						marsBdvFrames[i] = marsBdvFrame;
						handles[i] = marsBdvFrame.getBdvHandle();
					}

					ViewerTransformSyncStarter sync = new ViewerTransformSyncStarter(
						handles, true);

					for (int i = 0; i < marsBdvFrames.length; i++) {
						marsBdvFrames[i].getFrame().addWindowListener(new WindowAdapter() {
				      @Override
				      public void windowClosing(WindowEvent e) {
									for (int i = 0; i < marsBdvFrames.length; i++)
										if (marsBdvFrames[i] != null) {
											marsBdvFrames[i].releaseMemory();
											marsBdvFrames[i].getFrame().dispose();
											marsBdvFrames[i] = null;
										}

									marsBdvFrames = null;
									new ViewerTransformSyncStopper(sync.getSynchronizers(), sync
										.getTimeSynchronizers()).run();
								}
							});
					}
					sync.run();

					moleculesTab.setMarsBdvFrames(marsBdvFrames);
					imageMetadataTab.setMarsBdvFrames(marsBdvFrames);
				}
			}
		});
	}

	protected void deleteMolecules() {
		PropertySelectionDialog dialog = new PropertySelectionDialog(getNode()
			.getScene().getWindow(), archive.properties().getTagSet(),
			"Delete Molecules", "Delete molecules with tags:",
			"Delete molecules with no tags");

		dialog.showAndWait().ifPresent(result -> {
			runTask(() -> {
				ArrayList<String> deleteUIDs = (ArrayList<String>) archive
					.getMoleculeUIDs().parallelStream().filter(UID -> {
						if (result.removeAll() && archive.get(UID).getTags().size() == 0) {
							return true;
						}

						boolean hasTag = false;
						List<String> tagList = result.getList();
						for (int i = 0; i < tagList.size(); i++) {
							for (String tag : archive.get(UID).getTags()) {
								if (tagList.get(i).equals(tag)) {
									hasTag = true;
								}
							}
						}
						return hasTag;
					}).collect(toList());

				deleteUIDs.parallelStream().forEach(UID -> archive.remove(UID));
			}, "Deleting Molecules...");
		});
	}

	protected void deleteMoleculeTags() {
		PropertySelectionDialog dialog = new PropertySelectionDialog(getNode()
			.getScene().getWindow(), archive.properties().getTagSet(),
			"Delete Molecule Tags", "Delete molecule tags:", "Delete all tags");

		dialog.showAndWait().ifPresent(result -> {
			runTask(() -> {
				List<String> tagList = result.getList();
				archive.getMoleculeUIDs().parallelStream().forEach(UID -> {
					Molecule molecule = archive.get(UID);
					if (result.removeAll()) {
						molecule.removeAllTags();
					}
					else {
						for (int i = 0; i < tagList.size(); i++) {
							molecule.removeTag(tagList.get(i));
						}
					}
					archive.put(molecule);
				});
			}, "Deleting Molecule Tags...");
		});
	}

	protected void deleteMoleculeParameters() {
		PropertySelectionDialog dialog = new PropertySelectionDialog(getNode()
			.getScene().getWindow(), archive.properties().getParameterSet(),
			"Delete Molecule Parameters", "Delete molecule parameters:",
			"Delete all parameters");

		dialog.showAndWait().ifPresent(result -> {
			runTask(() -> {
				List<String> parameterList = result.getList();
				archive.getMoleculeUIDs().parallelStream().forEach(UID -> {
					Molecule molecule = archive.get(UID);
					if (result.removeAll()) {
						molecule.removeAllParameters();
					}
					else {
						for (int i = 0; i < parameterList.size(); i++) {
							molecule.removeParameter(parameterList.get(i));
						}
					}
					archive.put(molecule);
				});
			}, "Deleting Molecule Parameters...");
		});
	}

	protected void deleteMoleculeRegions() {
		PropertySelectionDialog dialog = new PropertySelectionDialog(getNode()
			.getScene().getWindow(), archive.properties().getRegionSet(),
			"Delete Molecule Regions", "Delete molecule regions:",
			"Delete all regions");

		dialog.showAndWait().ifPresent(result -> {
			runTask(() -> {
				List<String> regionList = result.getList();
				archive.getMoleculeUIDs().parallelStream().forEach(UID -> {
					Molecule molecule = archive.get(UID);
					if (result.removeAll()) {
						molecule.removeAllRegions();
					}
					else {
						for (int i = 0; i < regionList.size(); i++) {
							molecule.removeRegion(regionList.get(i));
						}
					}
					archive.put(molecule);
				});
			}, "Deleting Molecule Regions...");
		});
	}

	protected void deleteMoleculePositions() {
		PropertySelectionDialog dialog = new PropertySelectionDialog(getNode()
			.getScene().getWindow(), archive.properties().getPositionSet(),
			"Delete Molecule Positions", "Delete molecule positions:",
			"Delete all positions");

		dialog.showAndWait().ifPresent(result -> {
			runTask(() -> {
				List<String> regionList = result.getList();
				archive.getMoleculeUIDs().parallelStream().forEach(UID -> {
					Molecule molecule = archive.get(UID);
					if (result.removeAll()) {
						molecule.removeAllPositions();
					}
					else {
						for (int i = 0; i < regionList.size(); i++) {
							molecule.removePosition(regionList.get(i));
						}
					}
					archive.put(molecule);
				});
			}, "Deleting Molecule Positions...");
		});
	}

	protected void deleteSegmentTables() {
		SegmentTableSelectionDialog dialog = new SegmentTableSelectionDialog(
			getNode().getScene().getWindow(), archive.properties()
				.getSegmentsTableNames(), "Delete segments table");

		dialog.showAndWait().ifPresent(result -> {
			runTask(() -> {
				List<String> segmentTableName = result.getSegmentTableName();
				archive.getMoleculeUIDs().parallelStream().forEach(UID -> {
					Molecule molecule = archive.get(UID);
					molecule.removeSegmentsTable(segmentTableName);
					archive.put(molecule);
				});

				archive.properties().getSegmentsTableNames().remove(segmentTableName);
			}, "Deleting Segments Tables...");
		});
	}

	protected void mergeMolecules() {
		PropertySelectionDialog dialog = new PropertySelectionDialog(getNode()
			.getScene().getWindow(), archive.properties().getTagSet(),
			"Merge Molecules", "Merge molecules with tag:");

		dialog.showAndWait().ifPresent(result -> {
			runTask(() -> {
				if (result.getList().size() == 0) return;

				String tag = result.getList().get(0);

				List<String> mergeUIDs = (ArrayList<String>) archive.getMoleculeUIDs()
					.stream().filter(UID -> archive.moleculeHasTag(UID, tag)).collect(
						toList());

				if (mergeUIDs.size() < 2) return;

				for (int i = 1; i < mergeUIDs.size(); i++) {

					if (archive.get(mergeUIDs.get(0)) instanceof MartianObject)
						((MartianObject) archive.get(mergeUIDs.get(0))).merge(
							(MartianObject) archive.get(mergeUIDs.get(i)));
					else archive.get(mergeUIDs.get(0)).merge(archive.get(mergeUIDs.get(
						i)));

					archive.remove(mergeUIDs.get(i));
				}

			}, "Merging Molecules...");
		});
	}

	public void runTask(Runnable process, String message) {
		lockFX(message);

		Task<Void> task = new Task<Void>() {

			@Override
			public Void call() {
				process.run();
				Platform.runLater(new Runnable() {

					@Override
					public void run() {
						unlockFX();
					}
				});
				return null;
			}
		};
		task.setOnFailed(event -> {
			RoverErrorDialog alert = new RoverErrorDialog(getNode().getScene()
				.getWindow(), message + " did not finish normally.");
			alert.show();
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					unlockFX();
				}
			});
		});

		new Thread(task).start();
	}

	public
		MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>>
		getArchive()
	{
		return archive;
	}

	public Stage getStage() {
		return stage;
	}

	public String getTitle() {
		return title;
	}

	public void updateMenus(ArrayList<Menu> menus) {
		while (menuBar.getMenus().size() > 1)
			menuBar.getMenus().remove(1);
		if (menus != null && menus.size() > 0) {
			for (Menu menu : menus)
				menuBar.getMenus().add(menu);
		}
		menuBar.getMenus().add(toolsMenu);
	}

	public void save() {
		if (archive.getSource() != null) {
			if (!archive.getSource().isReachable()) {
				RoverErrorDialog alert = new RoverErrorDialog(getNode().getScene()
						.getWindow(), "MoleculeArchive source is unreachable.");
				alert.show();
				return;
			}
			//if (archive.getSource().getName().equals(archive.getName())) {
				runTask(() -> {
					fireEvent(new MoleculeArchiveSavingEvent(archive));
					try {
						archive.save();
						saveState(archive.getSource().getRoverOutputStream());
					}
					catch (IOException e) {
						e.printStackTrace();
					}
					fireEvent(new MoleculeArchiveSavedEvent(archive));
				}, "Saving...");
			//} else {
				// the archive name has changed... so let's check with the user about
				// the new name...
				//saveAs(new File(archive.getName()));
			//}
		} else {
			saveAs(new File(archive.getName()));
		}
		settingsTab.save();
	}

	public void saveCopy() {
		String fileName = archive.getName();
		if (fileName.endsWith(".store")) fileName = fileName.substring(0, fileName
			.length() - 6);

		if (archive.getSource() != null && new File(archive.getSource().getPath()).getParentFile().exists()) {
			saveAsCopy(new File(new File(archive.getSource().getPath()).getParentFile(), fileName));
		}
		else {
			saveAsCopy(new File(System.getProperty("user.home"), fileName));
		}
	}

	public void saveJsonCopy() {
		String fileName = archive.getName();
		if (fileName.endsWith(".store")) fileName = fileName.substring(0, fileName
			.length() - 6);

		if (archive.getSource() != null) {
			saveAsCopy(new File(new File(archive.getSource().getPath()).getParentFile(), fileName));
		}
		else {
			saveAsJsonCopy(new File(System.getProperty("user.home"), fileName));
		}
	}

	private boolean saveAs(File saveAsFile) {
		FileChooser fileChooser = new FileChooser();

		saveAsFile = ArchiveUtils.yamaFileExtensionFixer(saveAsFile);

		fileChooser.setInitialDirectory(saveAsFile.getParentFile());
		fileChooser.setInitialFileName(saveAsFile.getName());

		File newFile = fileChooser.showSaveDialog(this.tabsContainer.getScene()
			.getWindow());

		if (newFile != null) {
			final File newFileWithExtension = ArchiveUtils.yamaFileExtensionFixer(
				newFile);

			runTask(() -> {
				fireEvent(new MoleculeArchiveSavingEvent(archive));
				try {
					archive.saveAs(newFileWithExtension);
					saveState(newFileWithExtension.getAbsolutePath());

					/*
					if (moleculeArchiveService.contains(archive.getName()))
						moleculeArchiveService.removeArchive(archive);

					archive.getSource().setPath(newFileWithExtension.getAbsolutePath());
					archive.setName(newFileWithExtension.getName());
					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							stage.setTitle(newFileWithExtension.getName());
						}
					});

					moleculeArchiveService.addArchive(archive);
					*/
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				fireEvent(new MoleculeArchiveSavedEvent(archive));
			}, "Saving...");
			return true;
		}
		return false;
	}

	private boolean saveAsCopy(File saveAsFile) {
		FileChooser fileChooser = new FileChooser();

		saveAsFile = ArchiveUtils.yamaFileExtensionFixer(saveAsFile);

		fileChooser.setInitialDirectory(saveAsFile.getParentFile());
		fileChooser.setInitialFileName(saveAsFile.getName());

		File file = fileChooser.showSaveDialog(this.tabsContainer.getScene()
			.getWindow());

		if (file != null) {
			final File newFileWithExtension = ArchiveUtils.yamaFileExtensionFixer(
				file);
			runTask(() -> {
				fireEvent(new MoleculeArchiveSavingEvent(archive));
				try {
					archive.saveAs(newFileWithExtension);
					saveState(newFileWithExtension.getAbsolutePath());
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				fireEvent(new MoleculeArchiveSavedEvent(archive));
			}, "Saving...");
			return true;
		}
		return false;
	}

	private boolean saveAsJsonCopy(File saveAsFile) {
		FileChooser fileChooser = new FileChooser();

		saveAsFile = ArchiveUtils.jsonFileExtensionFixer(saveAsFile);

		fileChooser.setInitialDirectory(saveAsFile.getParentFile());
		fileChooser.setInitialFileName(saveAsFile.getName());

		File file = fileChooser.showSaveDialog(this.tabsContainer.getScene()
			.getWindow());

		if (file != null) {
			final File newFileWithExtension = ArchiveUtils.jsonFileExtensionFixer(
				file);

			runTask(() -> {
				fireEvent(new MoleculeArchiveSavingEvent(archive));
				try {
					archive.saveAsJson(newFileWithExtension);
					saveState(newFileWithExtension.getAbsolutePath());
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				fireEvent(new MoleculeArchiveSavedEvent(archive));
			}, "Saving...");
			return true;
		}
		return false;
	}

	public void saveVirtualStoreCopy() {
		String name = archive.getName();

		if (name.endsWith(".yama")) {
			name += ".store";
		}
		else if (!name.endsWith(".yama.store")) {
			name += ".yama.store";
		}
		saveAsVirtualStore(new File(name));
	}

	public void saveJsonVirtualStoreCopy() {
		String name = archive.getName();

		if (name.endsWith(".yama")) {
			name += ".store";
		}
		else if (!name.endsWith(".yama.store")) {
			name += ".yama.store";
		}
		saveAsJsonVirtualStore(new File(name));
	}

	private void saveAsVirtualStore(File saveAsFile) {
		FileChooser fileChooser = new FileChooser();

		saveAsFile = ArchiveUtils.storeFileExtensionFixer(saveAsFile);

		fileChooser.setInitialDirectory(saveAsFile.getParentFile());
		fileChooser.setInitialFileName(saveAsFile.getName());

		File virtualDirectory = fileChooser.showSaveDialog(this.tabsContainer
			.getScene().getWindow());

		if (virtualDirectory != null) {
			final File newFileWithExtension = ArchiveUtils.storeFileExtensionFixer(
				virtualDirectory);
			runTask(() -> {
				fireEvent(new MoleculeArchiveSavingEvent(archive));
				try {
					archive.saveAsVirtualStore(newFileWithExtension);
					saveState(newFileWithExtension.getAbsolutePath());
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				fireEvent(new MoleculeArchiveSavedEvent(archive));
			}, "Saving Virtual Store Copy...");
		}
	}

	private void saveAsJsonVirtualStore(File saveAsFile) {
		FileChooser fileChooser = new FileChooser();

		saveAsFile = ArchiveUtils.storeFileExtensionFixer(saveAsFile);

		fileChooser.setInitialDirectory(saveAsFile.getParentFile());
		fileChooser.setInitialFileName(saveAsFile.getName());

		File virtualDirectory = fileChooser.showSaveDialog(this.tabsContainer
			.getScene().getWindow());

		if (virtualDirectory != null) {
			final File newFileWithExtension = ArchiveUtils.storeFileExtensionFixer(
				virtualDirectory);
			runTask(() -> {
				fireEvent(new MoleculeArchiveSavingEvent(archive));
				try {
					archive.saveAsJsonVirtualStore(newFileWithExtension);
					saveState(newFileWithExtension.getAbsolutePath());
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				fireEvent(new MoleculeArchiveSavedEvent(archive));
			}, "Saving Virtual Store Copy...");
		}
	}

	private void saveToCloudStorage() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (archive != null) {
					MoleculeArchiveSaveDialog cloudSaveDialog = new MoleculeArchiveSaveDialog(context);
					cloudSaveDialog.setTreeRenderer(new MoleculeArchiveTreeCellRenderer(true));
					// Prevents NullPointerException
					cloudSaveDialog.setContainerPathUpdateCallback(x -> {});

					final Consumer<MoleculeArchiveSelection> callback = (MoleculeArchiveSelection dataSelection) -> {
						Platform.runLater(() -> {
							runTask(() -> {
								fireEvent(new MoleculeArchiveSavingEvent(archive));
								try {
									String updatedURL = (dataSelection.url.endsWith("." + MoleculeArchiveSource.MOLECULE_ARCHIVE_STORE_ENDING)) ?
										archive.saveAsVirtualStore(dataSelection.url) : archive.saveAs(dataSelection.url);
									saveState(new MoleculeArchiveIOFactory().openSource(updatedURL).getRoverOutputStream());
								}
								catch (IOException e) {
									e.printStackTrace();
								}
								fireEvent(new MoleculeArchiveSavedEvent(archive));
							}, "Saving...");
						});
					};

					cloudSaveDialog.run(callback);
				}
			}
		});


	}

	private void importRoverSettings() {
		FileChooser fileChooser = new FileChooser();

		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
			"rover files (*.rover)", "*.rover");
		fileChooser.getExtensionFilters().add(extFilter);

		File stateFile = fileChooser.showOpenDialog(this.tabsContainer.getScene()
			.getWindow());

		if (stateFile == null || !stateFile.exists()) return;

		runTask(() -> {
			try {
				InputStream inputStream = new BufferedInputStream(new FileInputStream(
					stateFile));
				JsonParser jParser = jfactory.createParser(inputStream);
				fromJSON(jParser);
				jParser.close();
				inputStream.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}, "Loading Rover Settings...");
	}

	public Node getNode() {
		return maskerStackPane;
	}

	public abstract I createImageMetadataTab(final Context context);
	
	public I getMetadataTab() {
		return imageMetadataTab;
	}

	public abstract M createMoleculesTab(final Context context);
	
	public M getMoleculesTab() {
		return moleculesTab;
	}

	public abstract MarsBdvFrame createMarsBdvFrame(boolean useVolatile);

	public abstract MarsBdvFrame createMarsBdvFrame(JsonParser jParser,
		boolean useVolatile);

	public DashboardTab getDashboard() {
		return dashboardTab;
	}

	// Lock, unlock and update event might be called by swing threads
	// so we use Platform.runLater to ensure they are executed on
	// the javafx thread.

	public void lock(String message) {
		if (archiveLocked.get()) return;

		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				lockFX(message);
			}
		});

		// Make sure we block the calling (swing) thread until
		// the archive has actually been locked...
		while (!archiveLocked.get()) {}
	}

	private void lockFX(String message) {
		masker.setText(message);
		masker.setVisible(true);
		lockLogAreaStrings.clear();
		lockLogArea.setVisible(true);
		marsSpinning.play();
		marsSpinning.setProgress(-1);
		fireEvent(new MoleculeArchiveLockEvent(archive));
		archiveLocked.set(true);
	}

	public void lock() {
		if (archiveLocked.get()) return;

		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				lockFX();
			}
		});

		// Make sure we block the calling (swing) thread until
		// the archive has actually been locked...
		while (!archiveLocked.get()) {}
	}

	private void lockFX() {
		lockFX("Please Wait...");
	}

	@Override
	public void updateLockMessage(String message) {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				masker.setText(message);
			}
		});
	}

	@Override
	public void logln(String message) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				lockLogAreaStrings.addAll(message.split("\\n"));
				if (lockLogAreaStrings.size() > 0) lockLogArea.scrollTo(lockLogAreaStrings.size() - 1);
				ScrollBar scroll = (ScrollBar) lockLogArea.lookup(
					".scroll-bar:vertical");
				if (scroll != null) scroll.setDisable(true);
			}
		});

	}

	@Override
	public void log(String message) {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				if (lockLogAreaStrings.size() > 0) {
					int index = lockLogAreaStrings.size() - 1;
					String oldMessage = lockLogAreaStrings.get(index);
					lockLogAreaStrings.set(index, oldMessage + message);
					lockLogArea.scrollTo(index);
				}
				else lockLogAreaStrings.addAll(message.split("\\n"));

				ScrollBar scroll = (ScrollBar) lockLogArea.lookup(
					".scroll-bar:vertical");
				if (scroll != null) scroll.setDisable(true);
			}
		});
	}

	// Not really ideal since a Task and updateProgress would be the best
	// But this is the only way for direct interaction through swing threads.
	@Override
	public void setProgress(double progress) {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				if (masker.isVisible()) {
					marsSpinning.setProgress(progress);
				}
			}
		});
	}

	@Override
	public void unlock() {
		if (!archiveLocked.get()) return;

		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				unlockFX();
			}
		});

		// Make sure we block the calling (swing) thread until
		// the archive has actually been unlocked...
		while (archiveLocked.get()) {}
	}

	private void unlockFX() {
		fireEvent(new MoleculeArchiveUnlockEvent(archive));
		masker.setVisible(false);
		lockLogArea.setVisible(false);
		marsSpinning.stop();
		archiveLocked.set(false);
	}

	public void close() {
		if (moleculeArchiveService.contains(archive.getName()))
			moleculeArchiveService.removeArchive(archive);

		if (ijStage != null) WindowManager.removeWindow(ijStage);

		Platform.runLater(() -> {
			if (stage.isShowing()) stage.hide();
			stage.close();
			
			//Remove all references to the archive in all subpanes. Otherwise, there is a memory leak
			//See issue #75
			fireEvent(new InitializeMoleculeArchiveEvent(null));
		});

		if (marsBdvFrames != null) {
			for (int i = 0; i < marsBdvFrames.length; i++)
				if (marsBdvFrames[i] != null) {
					marsBdvFrames[i].releaseMemory();
					marsBdvFrames[i].getFrame().dispose();
					marsBdvFrames[i] = null;
				}
			marsBdvFrames = null;
		}
		
		ijStage = null;
		archive.setWindow(null);
		archive = null;
	}

	// Creates settings input and output maps to save the current state of the
	// program.
	@Override
	protected void createIOMaps() {

		setJsonField("window", jGenerator -> {
			jGenerator.writeObjectFieldStart("window");
			jGenerator.writeNumberField("x", stage.getX());
			jGenerator.writeNumberField("y", stage.getY());
			jGenerator.writeNumberField("width", stage.getWidth());
			jGenerator.writeNumberField("height", stage.getHeight());
			jGenerator.writeEndObject();
		}, jParser -> {
			Rectangle rect = new Rectangle(0, 0, 800, 600);
			while (jParser.nextToken() != JsonToken.END_OBJECT) {
				if ("x".equals(jParser.getCurrentName())) {
					jParser.nextToken();
					rect.x = jParser.getIntValue();
				}
				if ("y".equals(jParser.getCurrentName())) {
					jParser.nextToken();
					rect.y = jParser.getIntValue();
				}
				if ("width".equals(jParser.getCurrentName())) {
					jParser.nextToken();
					rect.width = jParser.getIntValue();
				}
				if ("height".equals(jParser.getCurrentName())) {
					jParser.nextToken();
					rect.height = jParser.getIntValue();
				}
			}

			windowStateLoaded = true;

			stage.setX(rect.x);
			stage.setY(rect.y);
			stage.setWidth(rect.width);
			stage.setHeight(rect.height);
			stage.show();
		});

		for (MoleculeArchiveTab moleculeArchiveTab : tabSet)
			setJsonField(moleculeArchiveTab.getName(), jGenerator -> {
				jGenerator.writeFieldName(moleculeArchiveTab.getName());
				moleculeArchiveTab.toJSON(jGenerator);
			}, jParser -> {
				moleculeArchiveTab.fromJSON(jParser);
			});

		setJsonField("bdvFrames", jGenerator -> {
			if (marsBdvFrames != null && marsBdvFrames.length > 0) {
				jGenerator.writeObjectFieldStart("bdvFrames");
				jGenerator.writeNumberField("numberViews", marsBdvFrames.length);
				jGenerator.writeArrayFieldStart("views");
				for (MarsBdvFrame bdvFrame : marsBdvFrames)
					if (bdvFrame != null) bdvFrame.toJSON(jGenerator);
				jGenerator.writeEndArray();
				jGenerator.writeEndObject();
			}
			else if (roverFileBackground != null) {
				// We should check if there were settings already saved and restore
				// them!
				ObjectMapper mapper = new ObjectMapper();
				JsonNode jsonNode = mapper.readTree(roverFileBackground);
				JsonNode bdvFrameNode = jsonNode.get("bdvFrames");
				if (bdvFrameNode != null) {
					jGenerator.writeFieldName("bdvFrames");
					mapper.writeTree(jGenerator, bdvFrameNode);
				}
			}
		}, jParser -> {
			// The settings were discovered but will not be loaded until showVideo is
			// called.
			MarsUtil.passThroughUnknownObjects(jParser);
		});

		/*
		 * 
		 * The fields below are needed for backwards compatibility.
		 * 
		 * Please remove for a future release.
		 * 
		 */

		for (MoleculeArchiveTab moleculeArchiveTab : tabSet) {
			String name = moleculeArchiveTab.getName();
			if (moleculeArchiveTab.getName().equals("dashboardTab")) name =
				"DashboardTab";
			else if (moleculeArchiveTab.getName().equals("commentsTab")) name =
				"CommentsTab";
			else if (moleculeArchiveTab.getName().equals("settingsTab")) name =
				"SettingsTab";
			else if (moleculeArchiveTab.getName().equals("metadataTab")) name =
				"MetadataTab";
			else if (moleculeArchiveTab.getName().equals("moleculesTab")) name =
				"MoleculesTab";

			setJsonField(name, null, jParser -> {
				moleculeArchiveTab.fromJSON(jParser);
			});
		}

		setJsonField("Window", null, jParser -> {
			Rectangle rect = new Rectangle(0, 0, 800, 600);
			while (jParser.nextToken() != JsonToken.END_OBJECT) {
				if ("x".equals(jParser.getCurrentName())) {
					jParser.nextToken();
					rect.x = jParser.getIntValue();
				}
				if ("y".equals(jParser.getCurrentName())) {
					jParser.nextToken();
					rect.y = jParser.getIntValue();
				}
				if ("width".equals(jParser.getCurrentName())) {
					jParser.nextToken();
					rect.width = jParser.getIntValue();
				}
				if ("height".equals(jParser.getCurrentName())) {
					jParser.nextToken();
					rect.height = jParser.getIntValue();
				}
			}

			windowStateLoaded = true;

			stage.setX(rect.x);
			stage.setY(rect.y);
			stage.setWidth(rect.width);
			stage.setHeight(rect.height);
			stage.show();
		});
	}

	protected void saveState(OutputStream outputStream) throws IOException {
		if (marsBdvFrames == null && archive.getSource().exists(archive.getSource().getPath() + ".rover"))
			roverFileBackground = IOUtils.toByteArray(archive.getSource().getRoverInputStream());

		logln("Saving archive window settings to rover file...");

		JsonGenerator jGenerator = jfactory.createGenerator(outputStream);
		jGenerator.useDefaultPrettyPrinter();
		toJSON(jGenerator);
		jGenerator.close();
		outputStream.flush();
		outputStream.close();
	}

	protected void saveState(String path) throws IOException {
		if (marsBdvFrames == null && new File(path + ".rover").exists())
			roverFileBackground = FileUtils.readFileToByteArray(new File(path +
				".rover"));

		logln("Saving archive window settings to rover file...");

		OutputStream stream = new BufferedOutputStream(new FileOutputStream(
			new File(path + ".rover")));
		JsonGenerator jGenerator = jfactory.createGenerator(stream);
		jGenerator.useDefaultPrettyPrinter();
		toJSON(jGenerator);
		jGenerator.close();
		stream.flush();
		stream.close();
	}

	protected void loadState() throws IOException {
		if (archive.getSource() == null) return;
		if (!archive.getSource().exists(archive.getSource().getPath() + ".rover")) return;

		InputStream inputStream = new BufferedInputStream(archive.getSource().getRoverInputStream());
		JsonParser jParser = jfactory.createParser(inputStream);
		fromJSON(jParser);
		jParser.close();
		inputStream.close();
	}

	protected void loadBdvSettings() {
		if (archive.getSource() == null) return;
		try {
			if (!archive.getSource().exists(archive.getSource().getPath() + ".rover")) return;
		} catch (IOException e) {}

		// Build default parser
		DefaultJsonConverter defaultParser = new DefaultJsonConverter();
		defaultParser.setShowWarnings(false);
		defaultParser.setJsonField("bdvFrames", null, jParser -> {
			jParser.nextToken();
			jParser.nextToken();
			final int views = jParser.getNumberValue().intValue();
			BdvHandle[] handles = new BdvHandle[views];
			marsBdvFrames = new MarsBdvFrame[views];
			int index = 0;
			jParser.nextToken();
			while (jParser.nextToken() != JsonToken.END_ARRAY) {
				MarsBdvFrame marsBdvFrame = createMarsBdvFrame(jParser, prefService
					.getBoolean(SettingsTab.class, "useN5VolatileViews", true));
				marsBdvFrames[index] = marsBdvFrame;
				handles[index] = marsBdvFrame.getBdvHandle();
				index++;
			}

			// Make sure we move out of the bdvFrames field to the end of the
			// object...
			jParser.nextToken();

			ViewerTransformSyncStarter sync = new ViewerTransformSyncStarter(handles,
				true);

			for (int i = 0; i < marsBdvFrames.length; i++) {
				marsBdvFrames[i].getFrame().addWindowListener(
					new java.awt.event.WindowAdapter()
					{

						@Override
						public void windowClosing(java.awt.event.WindowEvent windowEvent) {
							super.windowClosing(windowEvent);
							for (int i = 0; i < marsBdvFrames.length; i++)
								if (marsBdvFrames[i] != null) {
									marsBdvFrames[i].getFrame().dispose();
									marsBdvFrames[i] = null;
								}
							marsBdvFrames = null;
							new ViewerTransformSyncStopper(sync.getSynchronizers(), sync
								.getTimeSynchronizers()).run();
						}
					});
			}
			sync.run();

			if (imageMetadataTab.getSelectedMetadata() != null) imageMetadataTab
				.setMarsBdvFrames(marsBdvFrames);
			if (moleculesTab.getSelectedMolecule() != null) moleculesTab
				.setMarsBdvFrames(marsBdvFrames);
		});

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				try {
					InputStream inputStream = new BufferedInputStream(archive.getSource().getRoverInputStream());
					JsonParser jParser = jfactory.createParser(inputStream);
					defaultParser.fromJSON(jParser);
					jParser.close();
					inputStream.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

	}

	public void fireEvent(Event event) {
		dashboardTab.fireEvent(event);
		imageMetadataTab.fireEvent(event);
		moleculesTab.fireEvent(event);
		commentsTab.fireEvent(event);
		settingsTab.fireEvent(event);
	}
}
