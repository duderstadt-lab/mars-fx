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
package de.mpg.biochem.mars.fx.dialogs.s3.explorer;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import de.mpg.biochem.mars.n5.MarsS3Browser;
import de.mpg.biochem.mars.fx.util.MarsThemeManager;
import de.mpg.biochem.mars.fx.util.IJStage;
import com.jfoenix.controls.JFXChipView;
import com.jfoenix.controls.JFXTabPane;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import org.scijava.Context;

import javax.swing.SwingUtilities;
import ij.WindowManager;

/**
 * Dataset Explorer — a large, modeless {@link Stage} window that lists Molecule
 * Archives and N5 datasets discovered in an S3 bucket as searchable/filterable
 * cards, with per-dataset tags and rich-text comments.
 *
 * <p>Layout (mirrors the Mars archive window feel):
 * <pre>
 *  SplitPane
 *  ├── LEFT  (collapsible): connection (endpoint, bucket), theme, index folder
 *  ├── CENTER: search bar + type switch + date range + card list
 *  └── RIGHT (collapsible): dataset details + tag editor, then the notes
 *                           (markdown) editor below for the selected dataset
 * </pre>
 *
 * <p>This is a {@code Stage} + {@code IJStage} window (not a {@code Dialog}),
 * modeless, single-instance — exactly like {@code CloudArchiveOpenWindow}. The
 * few integration points that must match your codebase are marked
 * {@code // INTEGRATION:} below.
 */
public class DatasetExplorerWindow {

    // Single-instance guard, same pattern as CloudArchiveOpenWindow.
    private static DatasetExplorerWindow openInstance;

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(DatasetExplorerWindow.class);
    private static final String PREF_ENDPOINT = "explorer.endpoint";
    private static final String PREF_BUCKET = "explorer.bucket";
    private static final String PREF_INDEX_FOLDER = "explorer.indexFolder";
    private static final String PREF_LOCAL_PATH = "explorer.localPath";
    private static final String PREF_LOCAL_MODE = "explorer.localMode";
    private static final String PREF_LOCAL_PROJECTS = "explorer.localProjects";
    private static final String PREF_SORT = "explorer.sort";
    private static final String PREF_RECENT = "explorer.recentOnly";

    // Node-scoped stylesheet (data URI) that makes a ProgressIndicator's skin
    // sub-nodes transparent, so no pale backing box shows behind the spinner in
    // dark mode. Applied only to the status-bar busy indicator. Kept here rather
    // than in the master sheets so the fix is self-contained to this window.
    // Spaces are pre-encoded as %20 so no URLEncoder call is needed (and so this
    // stays a compile-time constant with no Java-version-specific overloads).
    private static final String SPINNER_TRANSPARENT_CSS =
            "data:text/css,"
                    + ".progress-indicator%20{%20-fx-background-color:%20transparent;%20}"
                    + ".progress-indicator%20>%20.determinate-indicator%20>%20.indicator,"
                    + ".progress-indicator%20>%20.spinner%20{%20-fx-background-color:%20transparent;%20-fx-border-color:%20transparent;%20}"
                    + ".progress-indicator%20.percentage%20{%20visibility:%20hidden;%20}";
    private static final String PREF_WIN_W = "explorer.windowWidth";
    private static final String PREF_WIN_H = "explorer.windowHeight";
    private static final String PREF_DIV_LEFT = "explorer.dividerLeft";
    private static final String PREF_DIV_RIGHT = "explorer.dividerRight";

    /**
     * Card ordering options shown in the sort dropdown on the second search row.
     * Each carries a human label and a comparator. Null timestamps sort last for
     * the date-based options, so never-modified / never-opened items sink to the
     * bottom rather than jumping to the top.
     */
    private enum SortOption {
        MODIFIED_DESC("Modified (newest)",
                Comparator.comparing(DatasetEntry::getModifiedEpochMillis,
                        Comparator.nullsLast(Comparator.reverseOrder()))),
        MODIFIED_ASC("Modified (oldest)",
                Comparator.comparing(DatasetEntry::getModifiedEpochMillis,
                        Comparator.nullsLast(Comparator.naturalOrder()))),
        OPENED_DESC("Opened (newest)",
                Comparator.comparing(DatasetEntry::getOpenedAtMillis,
                        Comparator.nullsLast(Comparator.reverseOrder()))),
        NAME_ASC("Name (A–Z)",
                Comparator.comparing(DatasetEntry::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))),
        NAME_DESC("Name (Z–A)",
                Comparator.comparing(DatasetEntry::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)).reversed());

        final String label;
        final Comparator<DatasetEntry> comparator;
        SortOption(String label, Comparator<DatasetEntry> comparator) {
            this.label = label; this.comparator = comparator;
        }
        @Override public String toString() { return label; }
    }

    private Stage stage;
    private IJStage ijStage;

    // Model
    private final ObservableList<DatasetEntry> allEntries = FXCollections.observableArrayList();
    private final FilteredList<DatasetEntry> filtered = new FilteredList<>(allEntries, e -> true);
    private DatasetEntry selected;
    private DatasetIndexStore store;

    // Left pane controls
    private TextField endpointField;
    private TextField indexFolderField;

    // Center controls
    private TextField searchField;
    private Label searchCountLabel;
    private javafx.animation.PauseTransition searchDebounce;
    private ToggleButton archiveToggle;
    private ToggleButton n5Toggle;
    private ToggleButton recentToggle;              // top row: filter to opened-only
    private ComboBox<SortOption> sortChoice;        // second row: card ordering
    private VBox cardsBox;               // holds DatasetCard nodes
    private ProgressIndicator busy;
    private Label statusLabel;
    private Button cancelIndexBtn;      // shown only while indexing

    // Right pane controls
    private VBox detailsContent;
    private JFXChipView<String> tagChipView;

    // Notes (now in the right pane, below details)
    private DatasetNotesPane notesPane;
    private DatasetScriptPane scriptPane;
    private Context context; // SciJava context for scripting (passed via show)
    private DatasetEntry notesBoundEntry; // dataset whose notes are currently loaded

    // Split panes + collapse state
    private SplitPane split;
    private Region leftPane;
    private Region rightPane;
    private Button toggleLeftBtn;
    private Button toggleRightBtn;
    // Remembered divider positions (persisted across sessions). Defaults match the
    // initial layout; overwritten from prefs on open and whenever the user drags.
    private double savedLeftDivider = 0.16;
    private double savedRightDivider = 0.80;

    private boolean darkMode = false;

    // -----------------------------------------------------------------
    // Entry point
    // -----------------------------------------------------------------

    /** Show the explorer (creating it if needed, focusing it if already open). */
    public static void show() {
        show(null, null);
    }

    public static void show(java.util.function.Consumer<String> onOpenArchive) {
        show(onOpenArchive, null);
    }

    /**
     * Show the explorer, wiring an open-archive handler (invoked with the archive
     * URL on double-click) and the SciJava {@link Context} used by the scripting
     * tab. Both may be null (scripting is disabled without a context).
     */
    public static void show(java.util.function.Consumer<String> onOpenArchive, Context context) {
        if (openInstance != null && openInstance.stage != null) {
            if (onOpenArchive != null) openInstance.setOnOpenArchive(onOpenArchive);
            openInstance.stage.toFront();
            openInstance.stage.requestFocus();
            return;
        }
        openInstance = new DatasetExplorerWindow();
        if (onOpenArchive != null) openInstance.setOnOpenArchive(onOpenArchive);
        openInstance.context = context;
        openInstance.build();
    }

    private void build() {
        // Reflect the app-wide theme so the explorer opens matching everything else.
        darkMode = MarsThemeManager.isDarkTheme();

        stage = new Stage();
        stage.setTitle("Dataset Explorer");
        // Restore last session's window size (fall back to defaults).
        stage.setWidth(PREFS.getDouble(PREF_WIN_W, 1200));
        stage.setHeight(PREFS.getDouble(PREF_WIN_H, 760));

        // Shadow AWT frame so Fiji's Window menu can enumerate this window, and so
        // the OS-gated forward focus-pull inside IJStage moves AWT focus off
        // ImageJ's toolbar when this window is focused — stopping Cmd/Ctrl
        // keystrokes from leaking into Fiji's menus. Built once, right after the
        // Stage, then registered with WindowManager on the EDT. Matches
        // CloudArchiveOpenWindow exactly.
        ijStage = new IJStage(stage);
        ijStage.buildShadowFrame();
        SwingUtilities.invokeLater(() -> WindowManager.addWindow(ijStage));

        BorderPane root = new BorderPane();
        split = new SplitPane();

        leftPane = buildLeftPane();
        Region center = buildCenterPane();
        rightPane = buildRightPane();

        // Restore last session's divider positions.
        savedLeftDivider = PREFS.getDouble(PREF_DIV_LEFT, 0.16);
        savedRightDivider = PREFS.getDouble(PREF_DIV_RIGHT, 0.80);

        split.getItems().addAll(leftPane, center, rightPane);
        split.setDividerPositions(savedLeftDivider, savedRightDivider);

        root.setCenter(split);
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root);
        applyTheme(scene); // MarsThemeManager applies the master light/dark sheet

        stage.setScene(scene);

        // Teardown goes in setOnCloseRequest (not setOnHidden), on the EDT, exactly
        // as CloudArchiveOpenWindow does — this avoids the close-lockup.
        stage.setOnCloseRequest(e -> {
            persistPrefs();
            if (currentIndexer != null) currentIndexer.cancel();
            if (currentLocalIndexer != null) currentLocalIndexer.cancel();
            if (scriptPane != null) scriptPane.cleanup();
            SwingUtilities.invokeLater(() -> {
                WindowManager.removeWindow(ijStage);
                ijStage.cleanup();
            });
        });

        // setOnHidden only clears the single-instance guard.
        stage.setOnHidden(e -> openInstance = null);

        // Load any cached index for the last endpoint/bucket immediately.
        loadPrefsIntoFields();
        tryLoadCachedIndex();

        stage.show();

        // Auto-populate the bucket list on open if we already have a server.
        if (!endpointField.getText().trim().isEmpty())
            Platform.runLater(this::loadBuckets);
    }

    // -----------------------------------------------------------------
    // LEFT PANE — connection + settings (collapsible)
    // -----------------------------------------------------------------

    private ListView<String> bucketListView;
    private TextField localPathField;
    private ListView<String> folderListView;
    private boolean localMode = false; // false = cloud source, true = local folder

    private Region buildLeftPane() {
        // ===== Cloud source tab content (server + buckets) =====
        endpointField = new TextField(PREFS.get(PREF_ENDPOINT, ""));
        endpointField.setPromptText("https://minio.example.tum.de");
        // Explicit border so the field is visible in light mode (the default
        // .text-field border can be too faint against the panel background) —
        // same treatment as the search and local-path fields.
        endpointField.setStyle("-fx-border-color: derive(-fx-text-inner-color, 55%);"
                + " -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");

        // Buckets are shown in a selectable list (like the Open Archive dialog),
        // each with an ARCHIVE icon. The list auto-populates when a server address
        // is present (on open and whenever the address changes). Selecting a bucket
        // loads its cached index; "Index selected bucket" re-indexes from the server.
        // Credentials are resolved by the AWS chain in MarsS3Browser.
        bucketListView = new ListView<>();
        bucketListView.setPrefHeight(320);
        VBox.setVgrow(bucketListView, Priority.ALWAYS);
        bucketListView.setCellFactory(lv -> bucketListCell());
        String savedBucket = PREFS.get(PREF_BUCKET, "");
        if (!savedBucket.isEmpty()) {
            bucketListView.getItems().add(savedBucket);
            bucketListView.getSelectionModel().select(savedBucket);
        }
        bucketListView.getSelectionModel().selectedItemProperty().addListener((o, was, sel) -> {
            if (sel != null) {
                PREFS.put(PREF_BUCKET, sel);
                tryLoadCachedIndex(); // show cached cards immediately on selection
            }
        });

        // Auto-load buckets when the server address changes (debounced by equality),
        // and once on open if an address is already present.
        endpointField.setOnAction(e -> loadBuckets());
        endpointField.focusedProperty().addListener((o, was, focused) -> {
            if (!focused && !endpointField.getText().trim().isEmpty()) loadBuckets();
        });

        Button indexBucketBtn = new Button("Index selected bucket");
        indexBucketBtn.setMaxWidth(Double.MAX_VALUE);
        indexBucketBtn.setOnAction(e -> startIndexing());

        VBox cloudBox = new VBox(6,
                labeled("Server address", endpointField),
                new Label("Bucket"),
                bucketListView,
                indexBucketBtn);
        cloudBox.setPadding(new Insets(8));
        VBox.setVgrow(bucketListView, Priority.ALWAYS);

        // ===== Local folder tab content (registered indexed projects) =====
        // The list holds project folders you've indexed (full paths, folder icon),
        // the local analogue of S3 buckets. Point at a folder via the field / "…"
        // browser, then "Index folder" walks it recursively AND registers it here.
        // Clicking a project loads its cached index; right-click removes it (and
        // deletes its cache).
        localPathField = new TextField(PREFS.get(PREF_LOCAL_PATH, ""));
        localPathField.setPromptText("/path/to/project/folder");
        // Explicit border so the field is visible in light mode (the default
        // .text-field border can be too faint against the panel background) —
        // same treatment as the search field.
        localPathField.setStyle("-fx-border-color: derive(-fx-text-inner-color, 55%);"
                + " -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");
        Button browseLocal = new Button("…");
        browseLocal.setOnAction(e -> chooseLocalPath());
        HBox pathRow = new HBox(4, localPathField, browseLocal);
        HBox.setHgrow(localPathField, Priority.ALWAYS);

        folderListView = new ListView<>();
        folderListView.setPrefHeight(320);
        VBox.setVgrow(folderListView, Priority.ALWAYS);
        folderListView.setCellFactory(lv -> projectListCell());
        folderListView.getItems().setAll(loadRegisteredProjects());
        // Clicking a registered project loads its path + cached index.
        folderListView.getSelectionModel().selectedItemProperty().addListener((o, was, sel) -> {
            if (sel != null) {
                localPathField.setText(sel);
                PREFS.put(PREF_LOCAL_PATH, sel);
                tryLoadCachedIndex();
            }
        });

        Button indexLocalBtn = new Button("Index folder");
        indexLocalBtn.setMaxWidth(Double.MAX_VALUE);
        indexLocalBtn.setOnAction(e -> startLocalIndexing());

        VBox folderBox = new VBox(6,
                labeled("Local path", pathRow),
                new Label("Indexed projects"),
                folderListView,
                indexLocalBtn);
        folderBox.setPadding(new Insets(8));
        VBox.setVgrow(folderListView, Priority.ALWAYS);

        // ===== Source tabs (cloud / folder), styled like the right-pane tabs =====
        double tabWidth = 50.0;
        JFXTabPane sourceTabs = new JFXTabPane();
        sourceTabs.setSide(javafx.geometry.Side.TOP);
        sourceTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        sourceTabs.setTabMinWidth(tabWidth);
        sourceTabs.setTabMaxWidth(tabWidth);
        sourceTabs.setTabMinHeight(30.0);
        sourceTabs.setTabMaxHeight(30.0);
        sourceTabs.disableAnimationProperty();

        Tab cloudTab = new Tab();
        cloudTab.setClosable(false);
        cloudTab.setGraphic(tabGraphic(FontAwesomeIcon.CLOUD, tabWidth));
        cloudTab.setContent(cloudBox);

        Tab folderTab = new Tab();
        folderTab.setClosable(false);
        folderTab.setGraphic(tabGraphic(FontAwesomeIcon.FOLDER, tabWidth));
        folderTab.setContent(folderBox);

        sourceTabs.getTabs().addAll(cloudTab, folderTab);
        // Track which source mode is active so indexing/loading route correctly.
        sourceTabs.getSelectionModel().selectedItemProperty().addListener((o, was, sel) -> {
            localMode = (sel == folderTab);
            tryLoadCachedIndex(); // show whichever source's cached datasets
        });
        // Restore last-used source tab.
        if (PREFS.getBoolean(PREF_LOCAL_MODE, false)) {
            sourceTabs.getSelectionModel().select(folderTab);
            localMode = true;
        }

        TitledPane sourcePane = new TitledPane("Source", sourceTabs);
        sourcePane.setCollapsible(false);
        VBox.setVgrow(sourceTabs, Priority.ALWAYS);

        // ===== Appearance (shared, below tabs) =====
        ToggleGroup themeGroup = new ToggleGroup();
        RadioButton lightRadio = new RadioButton("Light");
        RadioButton darkRadio = new RadioButton("Dark");
        lightRadio.setToggleGroup(themeGroup);
        darkRadio.setToggleGroup(themeGroup);
        lightRadio.setSelected(!darkMode);
        darkRadio.setSelected(darkMode);
        themeGroup.selectedToggleProperty().addListener((o, was, is) -> {
            darkMode = (is == darkRadio);
            MarsThemeManager.setDarkTheme(darkMode);
            regenerateAllIcons();
            if (notesPane != null) notesPane.refreshPreview();
        });
        HBox themeRow = new HBox(12, lightRadio, darkRadio);
        VBox apprBox = new VBox(6, themeRow);
        apprBox.setPadding(new Insets(8));
        TitledPane apprPane = new TitledPane("Appearance", apprBox);
        apprPane.setCollapsible(false);

        // ===== Index storage folder (shared, below tabs) =====
        indexFolderField = new TextField(PREFS.get(PREF_INDEX_FOLDER, ""));
        indexFolderField.setPromptText("local folder for index files");
        // Explicit border for light-mode visibility — matches the other fields.
        indexFolderField.setStyle("-fx-border-color: derive(-fx-text-inner-color, 55%);"
                + " -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");
        Button browseFolder = new Button("…");
        browseFolder.setOnAction(e -> chooseIndexFolder());
        HBox folderRow = new HBox(4, indexFolderField, browseFolder);
        HBox.setHgrow(indexFolderField, Priority.ALWAYS);
        VBox idxBox = new VBox(6, new Label("Index storage folder"), folderRow);
        idxBox.setPadding(new Insets(8));
        TitledPane idxPane = new TitledPane("Index", idxBox);
        idxPane.setCollapsible(false);

        VBox content = new VBox(8, sourcePane, apprPane, new Region(), idxPane);
        VBox.setVgrow(sourcePane, Priority.ALWAYS);
        content.setPadding(new Insets(8));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setMinWidth(180);
        scroll.setPrefWidth(200);
        return scroll;
    }

    // ---- registered local projects -------------------------------------

    /** Registered project paths are stored newline-joined in a single pref. */
    private java.util.List<String> loadRegisteredProjects() {
        String raw = PREFS.get(PREF_LOCAL_PROJECTS, "");
        java.util.List<String> list = new java.util.ArrayList<>();
        if (!raw.isEmpty())
            for (String p : raw.split("\n")) { p = p.trim(); if (!p.isEmpty()) list.add(p); }
        return list;
    }

    private void saveRegisteredProjects() {
        if (folderListView == null) return;
        PREFS.put(PREF_LOCAL_PROJECTS, String.join("\n", folderListView.getItems()));
    }

    /** Add a path to the registered-projects list (if not already present). */
    private void registerProject(String path) {
        if (folderListView == null || path == null || path.isEmpty()) return;
        if (!folderListView.getItems().contains(path)) {
            folderListView.getItems().add(path);
            java.util.List<String> sorted = new java.util.ArrayList<>(folderListView.getItems());
            sorted.sort(String.CASE_INSENSITIVE_ORDER);
            folderListView.getItems().setAll(sorted);
            saveRegisteredProjects();
        }
        folderListView.getSelectionModel().select(path);
    }

    /** Remove a project from the list and delete its cached index + user data. */
    private void removeProject(String path) {
        if (path == null) return;
        folderListView.getItems().remove(path);
        saveRegisteredProjects();
        // Delete the cache for this local source ("local:" + path).
        String folder = indexFolderField.getText().trim();
        if (!folder.isEmpty()) {
            try { new DatasetIndexStore(folder).deleteCache("local:", path); }
            catch (Exception ignore) {}
        }
        // If the removed project was showing, clear the view.
        if (path.equals(localPathField.getText().trim())) {
            localPathField.clear();
            allEntries.clear();
            setStatus("Removed project: " + path);
        }
    }

    /** A ListCell showing a FOLDER icon + full project path, with a right-click remove. */
    private ListCell<String> projectListCell() {
        return new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(null);
                } else {
                    setText(item);
                    setGraphic(FontAwesomeIconFactory.get().createIcon(
                            FontAwesomeIcon.FOLDER, "1.0em"));
                    javafx.scene.control.MenuItem remove =
                            new javafx.scene.control.MenuItem("Remove (and delete cache)");
                    remove.setOnAction(e -> removeProject(item));
                    setContextMenu(new javafx.scene.control.ContextMenu(remove));
                }
            }
        };
    }

    private void chooseLocalPath() {
        javafx.stage.DirectoryChooser chooser = new javafx.stage.DirectoryChooser();
        chooser.setTitle("Choose project folder to index");
        String p = localPathField.getText().trim();
        if (!p.isEmpty()) {
            File cur = new File(p);
            if (cur.isDirectory()) chooser.setInitialDirectory(cur);
        }
        File dir = chooser.showDialog(stage);
        if (dir != null) {
            localPathField.setText(dir.getAbsolutePath());
            PREFS.put(PREF_LOCAL_PATH, dir.getAbsolutePath());
            // If this path is already a registered project, load its cache.
            if (folderListView.getItems().contains(dir.getAbsolutePath()))
                tryLoadCachedIndex();
        }
    }

    /** The currently selected bucket, or empty string if none. */
    private String currentBucket() {
        String v = bucketListView == null ? null
                : bucketListView.getSelectionModel().getSelectedItem();
        return v == null ? "" : v.trim();
    }

    /** A ListCell that shows an ARCHIVE icon beside the bucket name (matching the S3 dialog). */
    private ListCell<String> bucketListCell() {
        return new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    // Same icon factory the archive dialog uses, so it themes identically.
                    setGraphic(FontAwesomeIconFactory.get().createIcon(
                            FontAwesomeIcon.ARCHIVE, "1.0em"));
                }
            }
        };
    }

    private volatile boolean bucketsLoading = false;

    /** List the buckets on the current server and populate the list. */
    private void loadBuckets() {
        String endpoint = endpointField.getText().trim();
        if (endpoint.isEmpty()) {
            setStatus("Enter a server address first.");
            return;
        }
        if (bucketsLoading) return; // avoid overlapping loads (focus + enter)
        bucketsLoading = true;
        PREFS.put(PREF_ENDPOINT, endpoint);
        busy.setVisible(true);
        setStatus("Loading buckets from " + endpoint + " …");

        new Thread(() -> {
            try (MarsS3Browser browser = new MarsS3Browser(endpoint)) {
                List<String> buckets = browser.listBuckets();
                Platform.runLater(() -> {
                    String previous = currentBucket();
                    bucketListView.getItems().setAll(buckets);
                    if (buckets.contains(previous))
                        bucketListView.getSelectionModel().select(previous);
                    // else leave unselected — let the user choose which to index
                    busy.setVisible(false);
                    bucketsLoading = false;
                    setStatus(buckets.size() + " bucket(s) found.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    busy.setVisible(false);
                    bucketsLoading = false;
                    setStatus("Could not list buckets: " + ex.getMessage());
                });
            }
        }, "LoadBuckets").start();
    }

    // -----------------------------------------------------------------
    // CENTER PANE — tabs (Datasets / Notes)
    // -----------------------------------------------------------------

    private Region buildCenterPane() {
        // Center is now just the search bar + cards (no tabs). The notes editor
        // moved into the right pane, below the dataset details and tags.
        return buildDatasetsContent();
    }

    private Region buildDatasetsContent() {
        // --- Row 1: collapse buttons, search, type toggles, date-filter toggle ---
        toggleLeftBtn = iconButton(FontAwesomeIcon.CHEVRON_LEFT, "Show/hide connection pane");
        toggleLeftBtn.setOnAction(e -> toggleLeftPane());

        toggleRightBtn = iconButton(FontAwesomeIcon.CHEVRON_RIGHT, "Show/hide details pane");
        toggleRightBtn.setOnAction(e -> toggleRightPane());

        searchField = new TextField();
        searchField.setPromptText("Search name, tags…  (comma = AND, ! = exclude)");
        // Explicit border so the field is visible in light mode (the default
        // .text-field border can be too faint against the top bar background).
        searchField.setStyle("-fx-border-color: derive(-fx-text-inner-color, 55%);"
                + " -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;"
                + " -fx-padding: 4 56 4 8;"); // right padding leaves room for the count
        // Result count floats at the right edge of the field.
        searchCountLabel = new Label("");
        searchCountLabel.setStyle("-fx-opacity: 0.6; -fx-font-size: 11px;");
        StackPane.setAlignment(searchCountLabel, Pos.CENTER_RIGHT);
        StackPane.setMargin(searchCountLabel, new Insets(0, 8, 0, 0));
        searchCountLabel.setMouseTransparent(true);
        StackPane searchStack = new StackPane(searchField, searchCountLabel);
        HBox.setHgrow(searchStack, Priority.ALWAYS);

        // Debounce: typing schedules a filter ~150 ms later, coalescing rapid
        // keystrokes so we don't re-filter (and rebuild cards) on every character.
        searchDebounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(150));
        searchDebounce.setOnFinished(ev -> applyFilter());
        searchField.textProperty().addListener((o, a, b) -> searchDebounce.playFromStart());

        archiveToggle = new ToggleButton("YAMA");
        archiveToggle.setSelected(true);
        n5Toggle = new ToggleButton("N5");
        n5Toggle.setSelected(true);
        // Make the pressed state unmistakable: selected = filled accent with white
        // text; unselected = muted/outlined. Applied now and on every toggle.
        styleFilterToggle(archiveToggle);
        styleFilterToggle(n5Toggle);
        archiveToggle.selectedProperty().addListener((o, a, b) -> { styleFilterToggle(archiveToggle); applyFilter(); });
        n5Toggle.selectedProperty().addListener((o, a, b) -> { styleFilterToggle(n5Toggle); applyFilter(); });

        // Recent toggle: when on, filters the cards to archives that have been
        // opened (a non-null opened-at stamp), hiding never-opened items. It's a
        // filter only — ordering is handled by the sort dropdown below. Sits next
        // to YAMA/N5 and composes with them and the search box.
        recentToggle = new ToggleButton("Recent");
        recentToggle.setSelected(PREFS.getBoolean(PREF_RECENT, false));
        recentToggle.setTooltip(new javafx.scene.control.Tooltip(
                "Show only archives you've opened"));
        styleFilterToggle(recentToggle);
        recentToggle.selectedProperty().addListener((o, a, b) -> {
            styleFilterToggle(recentToggle);
            PREFS.putBoolean(PREF_RECENT, recentToggle.isSelected());
            applyFilter();
        });

        // Sort toggle: reveals/hides the second row holding the sort dropdown.
        ToggleButton sortToggle = new ToggleButton();
        sortToggle.setGraphic(FontAwesomeIconFactory.get().createIcon(
                FontAwesomeIcon.SORT, "1.0em"));
        sortToggle.setTooltip(new javafx.scene.control.Tooltip("Sort options"));
        styleFilterToggle(sortToggle);
        sortToggle.selectedProperty().addListener((o, a, b) -> styleFilterToggle(sortToggle));

        HBox row1 = new HBox(6, toggleLeftBtn, searchStack, archiveToggle, n5Toggle,
                recentToggle, sortToggle, toggleRightBtn);
        row1.setAlignment(Pos.CENTER_LEFT);

        // --- Row 2 (hidden by default): sort dropdown ---
        sortChoice = new ComboBox<>();
        sortChoice.getItems().setAll(SortOption.values());
        sortChoice.setValue(loadSortPref());
        sortChoice.valueProperty().addListener((o, was, now) -> {
            if (now != null) PREFS.put(PREF_SORT, now.name());
            rebuildCards(); // ordering happens at render time
        });

        HBox row2 = new HBox(6, new Label("Sort:"), sortChoice);
        row2.setAlignment(Pos.CENTER_LEFT);
        row2.setPadding(new Insets(6, 0, 0, 0));
        row2.setVisible(false);
        row2.setManaged(false);
        sortToggle.selectedProperty().addListener((o, was, on) -> {
            row2.setVisible(on);
            row2.setManaged(on);
        });

        VBox topBar = new VBox(0, row1, row2);
        topBar.setPadding(new Insets(8));

        // --- Card list ---
        cardsBox = new VBox(8);
        cardsBox.setPadding(new Insets(8));
        ScrollPane scroll = new ScrollPane(cardsBox);
        scroll.setFitToWidth(true);

        // Keep the visible cards in sync with the filtered list.
        filtered.addListener((javafx.collections.ListChangeListener<DatasetEntry>) c -> rebuildCards());

        BorderPane pane = new BorderPane();
        pane.setTop(topBar);
        pane.setCenter(scroll);
        return pane;
    }

    /** A small icon-only button using a FontAwesome glyph. */
    private static Button iconButton(FontAwesomeIcon glyph, String tooltip) {
        Button b = new Button();
        b.setGraphic(FontAwesomeIconFactory.get().createIcon(glyph, "1.1em"));
        b.setTooltip(new javafx.scene.control.Tooltip(tooltip));
        return b;
    }

    private Label notesHeader;

    private Region buildNotesSection() {
        notesHeader = new Label("Notes");
        notesHeader.setStyle("-fx-font-weight: bold; -fx-padding: 8 0 4 0;");

        notesPane = new DatasetNotesPane();
        // Dropped images are stored under <indexFolder>/images/. Supplied lazily so
        // it always reflects the current index folder field value.
        notesPane.setImagesFolder(() -> {
            String folder = indexFolderField == null ? "" : indexFolderField.getText().trim();
            return folder.isEmpty() ? null : new java.io.File(folder, "images");
        });
        // Autosave: whenever the notes text changes, write it back to the bound
        // dataset and persist. Guarded by notesBoundEntry so edits are attributed
        // to the right dataset (and never fire while we're loading a new one).
        notesPane.setOnMarkdownChanged(md -> {
            if (notesBoundEntry != null) {
                notesBoundEntry.setCommentsMarkdown(md);
                persistUserData();
            }
        });
        notesPane.setDisable(true); // until a dataset is selected

        VBox section = new VBox(4, notesHeader, notesPane);
        VBox.setVgrow(notesPane, Priority.ALWAYS);
        return section;
    }

    // -----------------------------------------------------------------
    // RIGHT PANE — details + tag editor (collapsible)
    // -----------------------------------------------------------------

    private Region buildRightPane() {
        Label title = new Label("Details");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        detailsContent = new VBox(8);
        showNoSelection();

        // Details + tags are always fully visible at their natural height and push
        // the notes editor down; the notes editor then grows to fill whatever space
        // remains. No scroll pane around details, so nothing there gets clipped.
        Region notesSection = buildNotesSection();

        VBox infoContent = new VBox(8, title, detailsContent, new Separator(), notesSection);
        infoContent.setPadding(new Insets(8));
        VBox.setVgrow(notesSection, Priority.ALWAYS);

        // --- Tabbed right pane: info tab (details+notes) + code tab (scripting) ---
        // Styled like AbstractMoleculePropertiesPane: JFXTabPane, top side, fixed
        // 50x30 icon tabs, non-closable, glyph graphics.
        double tabWidth = 50.0;
        JFXTabPane tabs = new JFXTabPane();
        tabs.setSide(javafx.geometry.Side.TOP);
        tabs.setTabClosingPolicy(javafx.scene.control.TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setTabMinWidth(tabWidth);
        tabs.setTabMaxWidth(tabWidth);
        tabs.setTabMinHeight(30.0);
        tabs.setTabMaxHeight(30.0);
        tabs.disableAnimationProperty();

        Tab infoTab = new Tab();
        infoTab.setClosable(false);
        infoTab.setGraphic(tabGraphic(FontAwesomeIcon.INFO_CIRCLE, tabWidth));
        infoTab.setContent(infoContent);

        Tab codeTab = new Tab();
        codeTab.setClosable(false);
        // Code icon: octicon CODE, matching the scriptable-widget tab glyph.
        BorderPane codeGraphic = new BorderPane();
        codeGraphic.setMaxWidth(tabWidth);
        codeGraphic.setCenter(de.jensd.fx.glyphs.octicons.utils.OctIconFactory.get()
                .createIcon(de.jensd.fx.glyphs.octicons.OctIcon.CODE, "1.1em"));
        codeTab.setGraphic(codeGraphic);
        if (context != null) {
            scriptPane = new DatasetScriptPane(context);
            // Run against the currently selected dataset's archive URL.
            scriptPane.setArchiveUrlSupplier(() -> {
                if (selected == null || !selected.isArchive()) return null;
                return datasetUrl(selected);
            });
            // "All" mode: every archive currently shown by the filter.
            scriptPane.setAllArchiveUrlsSupplier(() -> {
                java.util.List<String> urls = new java.util.ArrayList<>();
                for (DatasetEntry e : filtered) {
                    if (e.isArchive()) urls.add(datasetUrl(e));
                }
                return urls;
            });
            // Single-archive script run stamps opened-at (fired on the FX thread by
            // the pane; "All" runs never fire this).
            scriptPane.setOnSingleArchiveOpened(url -> {
                DatasetEntry e = entryForUrl(url);
                if (e != null) stampOpened(e);
            });
            codeTab.setContent(scriptPane);
        } else {
            Label noCtx = new Label("Scripting unavailable (no SciJava context).");
            noCtx.setPadding(new Insets(16));
            codeTab.setContent(noCtx);
        }

        tabs.getTabs().addAll(infoTab, codeTab);

        BorderPane right = new BorderPane(tabs);
        right.setMinWidth(260);
        return right;
    }

    /** A BorderPane-centered FontAwesome glyph for a fixed-width icon tab. */
    private static BorderPane tabGraphic(FontAwesomeIcon glyph, double tabWidth) {
        BorderPane p = new BorderPane();
        p.setMaxWidth(tabWidth);
        p.setCenter(FontAwesomeIconFactory.get().createIcon(glyph, "1.1em"));
        return p;
    }

    private void showNoSelection() {
        detailsContent.getChildren().setAll(new Label("No dataset selected."));
    }

    private void showDetailsFor(DatasetEntry e) {
        detailsContent.getChildren().clear();

        Label name = new Label(e.getName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        name.setWrapText(true);

        Label type = new Label("Type: " + (e.isArchive() ? "Molecule Archive (YAMA)" : "N5 container"));
        Label path = new Label("Path: " + e.getPath());
        path.setWrapText(true);
        Label size = new Label("Size: " + (e.getSizeBytes() >= 0 ? humanBytes(e.getSizeBytes()) : "—"));
        Label mod = new Label("Modified: " + fmtMillis(e.getModifiedEpochMillis()));
        Label created = new Label("Created: " + fmtMillis(e.getCreatedEpochMillis()));

        // --- Tag editor (JFXChipView, matching the archive window) ---
        Label tagsHeader = new Label("Tags");
        tagsHeader.setStyle("-fx-font-weight: bold; -fx-padding: 8 0 0 0;");

        // Rebuild the chip view for this dataset. JFXChipView shows each tag as a
        // chip with an inline delete button and lets you type tags adjacently, just
        // like the molecule/archive tag editor.
        tagChipView = new JFXChipView<String>();
        tagChipView.setMinHeight(120);
        // Load existing tags without triggering the change listener.
        tagChipView.getChips().addAll(e.getTags());
        // Suggestions: all tags already used across the indexed datasets.
        tagChipView.getSuggestions().addAll(allKnownTags());

        // Sync chip changes back to the entry + card + filter + persistence.
        tagChipView.getChips().addListener((javafx.collections.ListChangeListener<String>) c -> {
            while (c.next()) {
                if (c.wasRemoved()) for (String t : c.getRemoved()) e.removeTag(t);
                if (c.wasAdded()) for (String t : c.getAddedSubList()) e.addTag(t);
            }
            refreshCardTags(e);
            persistUserData();
            applyFilter();
        });

        detailsContent.getChildren().addAll(name, type, path, size, mod, created,
                tagsHeader, tagChipView);
    }

    /** All distinct tags across indexed datasets, for chip-view autosuggest. */
    private java.util.List<String> allKnownTags() {
        java.util.TreeSet<String> set = new java.util.TreeSet<>();
        for (DatasetEntry e : allEntries) set.addAll(e.getTags());
        return new java.util.ArrayList<>(set);
    }

    // -----------------------------------------------------------------
    // Status bar
    // -----------------------------------------------------------------

    private Region buildStatusBar() {
        busy = new ProgressIndicator();
        busy.setPrefSize(16, 16);
        busy.setVisible(false);
        // The default ProgressIndicator skin paints a light backing box behind the
        // spinning arc, which shows as a pale square against the dark theme. Inline
        // styles can't reach the skin's inner sub-nodes (.percentage / .indicator),
        // so a tiny node-scoped stylesheet (data URI, no master-sheet edit needed)
        // forces those transparent. The -fx-progress-color keeps the arc on-accent.
        busy.setStyle("-fx-progress-color: -fx-accent;");
        busy.getStylesheets().add(SPINNER_TRANSPARENT_CSS);
        statusLabel = new Label("Ready");
        // Cancel button: visible only during an active index. Cancels whichever
        // indexer (S3 or local) is running. Useful when a mis-named folder (e.g. an
        // N5 missing its .n5 suffix) is being descended into and the walk would
        // otherwise run for a very long time.
        cancelIndexBtn = new Button("Cancel");
        cancelIndexBtn.setVisible(false);
        cancelIndexBtn.setManaged(false);
        cancelIndexBtn.setOnAction(e -> cancelIndexing());
        HBox bar = new HBox(8, busy, statusLabel, cancelIndexBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(4, 8, 4, 8));
        return bar;
    }

    /** Toggle the busy spinner and cancel button together for the indexing state. */
    private void setIndexingUi(boolean on) {
        if (busy != null) busy.setVisible(on);
        if (cancelIndexBtn != null) {
            cancelIndexBtn.setVisible(on);
            cancelIndexBtn.setManaged(on);
        }
    }

    /**
     * Cancel an in-progress index (S3 or local). The indexers stop their walk at
     * the next check and won't fire onFinished, so we reset the UI state here
     * rather than waiting on a callback. Any datasets already discovered are
     * discarded — a partial index isn't written.
     */
    private void cancelIndexing() {
        if (!indexing) return;
        indexCancelled = true;
        if (currentIndexer != null) currentIndexer.cancel();
        if (currentLocalIndexer != null) currentLocalIndexer.cancel();
        indexing = false;
        setIndexingUi(false);
        setStatus("Indexing cancelled.");
    }

    // -----------------------------------------------------------------
    // Collapse / expand
    // -----------------------------------------------------------------

    private void toggleLeftPane() {
        if (split.getItems().contains(leftPane)) {
            // Remember the left divider position before collapsing.
            savedLeftDivider = split.getDividerPositions()[0];
            split.getItems().remove(leftPane);
            setButtonIcon(toggleLeftBtn, FontAwesomeIcon.CHEVRON_RIGHT); // closed → point to content
        } else {
            split.getItems().add(0, leftPane);
            Platform.runLater(() -> restoreDividers());
            setButtonIcon(toggleLeftBtn, FontAwesomeIcon.CHEVRON_LEFT); // open → point to pane
        }
    }

    private void toggleRightPane() {
        if (split.getItems().contains(rightPane)) {
            // Remember the right divider position before collapsing. The right
            // divider is the LAST one in the split.
            double[] d = split.getDividerPositions();
            if (d.length > 0) savedRightDivider = d[d.length - 1];
            split.getItems().remove(rightPane);
            setButtonIcon(toggleRightBtn, FontAwesomeIcon.CHEVRON_LEFT); // closed → point to content
        } else {
            split.getItems().add(rightPane);
            Platform.runLater(() -> restoreDividers());
            setButtonIcon(toggleRightBtn, FontAwesomeIcon.CHEVRON_RIGHT); // open → point to pane
        }
    }

    /**
     * Restore both divider positions based on which panes are currently present.
     * With all three panes visible, dividers are [savedLeftDivider, savedRightDivider].
     * With one side collapsed there's a single divider to place.
     */
    private void restoreDividers() {
        boolean hasLeft = split.getItems().contains(leftPane);
        boolean hasRight = split.getItems().contains(rightPane);
        if (hasLeft && hasRight) {
            split.setDividerPositions(savedLeftDivider, savedRightDivider);
        } else if (hasLeft) {
            split.setDividerPositions(savedLeftDivider);
        } else if (hasRight) {
            // Only center + right: the single divider sits where the right pane starts.
            split.setDividerPositions(savedRightDivider);
        }
    }

    private static void setButtonIcon(Button b, FontAwesomeIcon glyph) {
        b.setGraphic(FontAwesomeIconFactory.get().createIcon(glyph, "1.1em"));
    }

    // -----------------------------------------------------------------
    // Indexing
    // -----------------------------------------------------------------

    private void startIndexing() {
        String endpoint = endpointField.getText().trim();
        String bucket = currentBucket();
        String folder = indexFolderField.getText().trim();
        if (endpoint.isEmpty() || bucket.isEmpty()) {
            setStatus("Enter a server address and bucket first.");
            return;
        }
        if (indexing) {
            setStatus("Already indexing — please wait…");
            return;
        }
        persistPrefs();
        store = folder.isEmpty() ? null : new DatasetIndexStore(folder);

        indexing = true;
        indexCancelled = false;
        setIndexingUi(true);
        setStatus("Indexing " + bucket + " …");
        allEntries.clear();

        // Build a MarsS3Browser for this endpoint. Credentials are resolved by
        // MarsS3Browser internally (DefaultAWSCredentialsProviderChain, falling
        // back to anonymous), so we pass only the server URL.
        final MarsS3Browser browser = new MarsS3Browser(endpoint);

        // Adapter: map the indexer's S3Access onto MarsS3Browser's real methods.
        DatasetIndexer.S3Access access = new DatasetIndexer.S3Access() {
            @Override
            public List<String> listFolders(String b, String prefix) throws Exception {
                return browser.listFolders(b, prefix);
            }
            @Override
            public List<String> listFiles(String b, String prefix) throws Exception {
                return browser.listFiles(b, prefix);
            }
            @Override
            public boolean isN5(String name) { return browser.isN5(name); }
            @Override
            public boolean isArchive(String name) { return browser.isArchive(name); }
            @Override
            public DatasetIndexer.ObjectMeta meta(String b, String key) throws Exception {
                // Fetch size + last-modified for each dataset. Runs on the indexer's
                // BACKGROUND thread (not the FX thread), so it doesn't block the UI.
                MarsS3Browser.MarsObjectMeta m = browser.getObjectMeta(b, key);
                return m == null ? null
                        : new DatasetIndexer.ObjectMeta(m.sizeBytes, m.lastModifiedMillis);
            }
        };

        DatasetIndexer indexer = new DatasetIndexer(access);
        currentIndexer = indexer;
        // Stream only a progress COUNT to the UI during the walk (cheap); defer all
        // card building to onFinished so we rebuild the card list exactly once,
        // instead of once per dataset (which was the real UI-lock cause).
        indexer.indexAsync(bucket, new DatasetIndexer.Listener() {
            @Override
            public void onProgress(int discovered, String currentPrefix) {
                Platform.runLater(() -> setStatus("Indexing… " + discovered + " found"));
            }
            @Override
            public void onFinished(List<DatasetEntry> all) {
                Platform.runLater(() -> {
                    finishIndexing(endpoint, bucket, all);
                    browser.close();
                });
            }
            @Override
            public void onError(Exception ex) {
                Platform.runLater(() -> {
                    indexing = false;
                    setIndexingUi(false);
                    setStatus("Error indexing: " + ex.getMessage());
                    browser.close();
                });
            }
        });
    }

    private volatile boolean indexing = false;
    private volatile boolean indexCancelled = false;
    private DatasetIndexer currentIndexer;
    private DatasetLocalIndexer currentLocalIndexer;

    /** Index a local directory tree (recurses under the path field's folder). */
    private void startLocalIndexing() {
        String path = localPathField.getText().trim();
        String folder = indexFolderField.getText().trim();
        if (path.isEmpty()) {
            setStatus("Enter or choose a local path first.");
            return;
        }
        File root = new File(path);
        if (!root.isDirectory()) {
            setStatus("Not a folder: " + path);
            return;
        }
        if (indexing) { setStatus("Already indexing — please wait…"); return; }

        PREFS.put(PREF_LOCAL_PATH, path);
        store = folder.isEmpty() ? null : new DatasetIndexStore(folder);

        indexing = true;
        indexCancelled = false;
        setIndexingUi(true);
        setStatus("Indexing " + path + " …");
        allEntries.clear();

        final String endpoint = "local:";
        final String bucket = path;

        DatasetLocalIndexer indexer = new DatasetLocalIndexer();
        currentLocalIndexer = indexer;
        indexer.indexAsync(root, new DatasetLocalIndexer.Listener() {
            @Override
            public void onProgress(int discovered, String currentPath) {
                Platform.runLater(() -> setStatus("Indexing… " + discovered + " found"));
            }
            @Override
            public void onFinished(List<DatasetEntry> all) {
                Platform.runLater(() -> {
                    if (indexCancelled) { indexCancelled = false; return; }
                    finishIndexing(endpoint, bucket, all);
                    registerProject(bucket); // add this path to the projects list
                });
            }
            @Override
            public void onError(Exception ex) {
                Platform.runLater(() -> {
                    indexing = false;
                    setIndexingUi(false);
                    setStatus("Error indexing: " + ex.getMessage());
                });
            }
        });
    }

    private void finishIndexing(String endpoint, String bucket, List<DatasetEntry> all) {
        // If the user cancelled after the indexer queued this callback but before
        // it ran on the FX thread, discard the result — don't repopulate or write
        // a cache for a run the user abandoned.
        if (indexCancelled) { indexCancelled = false; return; }
        indexing = false;
        setIndexingUi(false);

        // Merge cached user data (tags/comments) for the whole batch at once.
        if (store != null) {
            try {
                store.mergeUserData(endpoint, bucket, all);
            } catch (Exception ignore) {}
        }

        // Single bulk update — the FilteredList listener fires once, so rebuildCards
        // runs a single time for the whole result set.
        allEntries.setAll(all);
        setStatus(all.size() + " datasets indexed.");

        if (store != null) {
            try {
                store.writeIndex(endpoint, bucket, all);
            } catch (Exception ex) {
                setStatus("Indexed, but failed to write cache: " + ex.getMessage());
            }
        }
    }

    /** The store "endpoint" for the active source: real endpoint (cloud) or "local:". */
    private String sourceEndpoint() {
        return localMode ? "local:" : endpointField.getText().trim();
    }

    /** The store "bucket" for the active source: bucket name (cloud) or local path. */
    private String sourceBucket() {
        return localMode
                ? (localPathField == null ? "" : localPathField.getText().trim())
                : currentBucket();
    }

    private void tryLoadCachedIndex() {
        String endpoint = sourceEndpoint();
        String bucket = sourceBucket();
        String folder = indexFolderField.getText().trim();
        if (endpoint.isEmpty() || bucket.isEmpty() || folder.isEmpty()) return;
        store = new DatasetIndexStore(folder);
        try {
            List<DatasetEntry> cached = store.readIndex(endpoint, bucket);
            store.mergeUserData(endpoint, bucket, cached);
            allEntries.setAll(cached);
            if (!cached.isEmpty()) setStatus(cached.size() + " datasets loaded from cache.");
        } catch (Exception ex) {
            setStatus("Could not load cached index: " + ex.getMessage());
        }
    }

    // -----------------------------------------------------------------
    // Filtering
    // -----------------------------------------------------------------

    /** The persisted sort option, defaulting to Modified-newest. */
    private SortOption loadSortPref() {
        String name = PREFS.get(PREF_SORT, SortOption.MODIFIED_DESC.name());
        try { return SortOption.valueOf(name); }
        catch (IllegalArgumentException ex) { return SortOption.MODIFIED_DESC; }
    }

    private void applyFilter() {
        String raw = searchField == null ? "" : searchField.getText().trim();
        boolean showArchive = archiveToggle == null || archiveToggle.isSelected();
        boolean showN5 = n5Toggle == null || n5Toggle.isSelected();
        boolean recentOnly = recentToggle != null && recentToggle.isSelected();

        // Parse the query into include/exclude terms. Comma separates terms; a
        // leading '!' marks an exclusion. Case-insensitive. Semantics:
        //   - every include term must be found (AND over includes)
        //   - every exclusion term must NOT be found (AND over exclusions)
        List<String> includes = new ArrayList<>();
        List<String> excludes = new ArrayList<>();
        for (String part : raw.split(",")) {
            String t = part.trim().toLowerCase();
            if (t.isEmpty()) continue;
            if (t.startsWith("!")) {
                String ex = t.substring(1).trim();
                if (!ex.isEmpty()) excludes.add(ex);
            } else {
                includes.add(t);
            }
        }

        filtered.setPredicate(e -> {
            if (e.isArchive() && !showArchive) return false;
            if (e.isN5() && !showN5) return false;

            if (!includes.isEmpty() || !excludes.isEmpty()) {
                String hay = haystack(e);
                for (String ex : excludes) if (hay.contains(ex)) return false;
                // AND: every include term must be found.
                for (String in : includes) if (!hay.contains(in)) return false;
            }

            // Recent filter: hide anything never opened.
            if (recentOnly && e.getOpenedAtMillis() == null) return false;
            return true;
        });

        // Update the result count shown in the search field.
        if (searchCountLabel != null) {
            int shown = filtered.size();
            int total = allEntries.size();
            searchCountLabel.setText(shown == total ? String.valueOf(total)
                    : shown + " / " + total);
        }
    }

    /** Lowercased searchable text for one entry: name + path + tags. */
    private static String haystack(DatasetEntry e) {
        StringBuilder sb = new StringBuilder();
        if (e.getName() != null) sb.append(e.getName().toLowerCase()).append(' ');
        if (e.getPath() != null) sb.append(e.getPath().toLowerCase()).append(' ');
        for (String t : e.getTags()) sb.append(t.toLowerCase()).append(' ');
        return sb.toString();
    }

    // -----------------------------------------------------------------
    // Card rendering
    // -----------------------------------------------------------------

    // Cache of generated identicons, keyed by "seed|dark" so re-filtering reuses
    // them instead of regenerating (identicon generation is the expensive part of
    // rebuildCards). Cleared when the theme changes.
    private final java.util.Map<String, javafx.scene.image.Image> iconCache =
            new java.util.HashMap<>();

    private javafx.scene.image.Image iconFor(DatasetEntry e, double size) {
        String key = e.iconSeed() + "|" + darkMode;
        javafx.scene.image.Image img = iconCache.get(key);
        if (img == null) {
            img = DatasetIdenticon.generate(e.iconSeed(), size,
                    darkMode ? DatasetIdenticon.DARK : DatasetIdenticon.LIGHT);
            iconCache.put(key, img);
        }
        return img;
    }

    private void rebuildCards() {
        cardsBox.getChildren().clear();
        double iconSize = 72;
        // FilteredList preserves source order, so sorting happens here at render
        // time over a copy. Default is Modified-newest.
        List<DatasetEntry> ordered = new ArrayList<>(filtered);
        SortOption sort = sortChoice == null ? loadSortPref() : sortChoice.getValue();
        if (sort == null) sort = SortOption.MODIFIED_DESC;
        ordered.sort(sort.comparator);
        for (DatasetEntry e : ordered) {
            DatasetCard card = new DatasetCard(e, iconFor(e, iconSize), iconSize, darkMode);
            card.setSelected(e == selected);
            card.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2) openDataset(e);
                else selectEntry(e, card);
            });
            cardsBox.getChildren().add(card);
        }
    }

    private void regenerateAllIcons() {
        // Theme changed — drop cached icons so they regenerate in the new variant.
        iconCache.clear();
        rebuildCards();
    }

    /**
     * Open a dataset (double-clicked card). For archives we build the canonical
     * Mars URL and hand it to the open-archive callback; N5 datasets aren't opened
     * this way (no action for now).
     */
    /**
     * Build the openable URL/path for a dataset, for either source: an S3 URL via
     * MarsS3Browser.buildPath (cloud), or an absolute local file path (local). For
     * local, the entry path is relative to the indexed root, which — because a
     * local index is keyed by its path — is the source "bucket".
     */
    private String datasetUrl(DatasetEntry e) {
        if (localMode) {
            String base = sourceBucket(); // the indexed local root path
            File f = new File(base, e.getPath());
            return f.getAbsolutePath();
        }
        return MarsS3Browser.buildPath(endpointField.getText().trim(),
                currentBucket(), e.getPath());
    }

    private void openDataset(DatasetEntry e) {
        if (e == null || !e.isArchive()) {
            setStatus(e != null && e.isN5()
                    ? "N5 datasets can't be opened from here yet." : "");
            return;
        }
        String url = datasetUrl(e);
        if (onOpenArchive != null) {
            stampOpened(e);
            onOpenArchive.accept(url);
            setStatus("Opening " + e.getName() + " …");
        } else {
            setStatus("No open handler set for: " + url);
        }
    }

    /**
     * Record that a dataset was just opened for viewing: set its opened-at to now,
     * persist user data, and refresh ordering if the Opened sort or Recent filter
     * is active so the change is visible immediately.
     */
    private void stampOpened(DatasetEntry e) {
        if (e == null) return;
        e.setOpenedAtMillis(System.currentTimeMillis());
        persistUserData();
        boolean recentActive = recentToggle != null && recentToggle.isSelected();
        boolean openedSort = sortChoice != null
                && sortChoice.getValue() == SortOption.OPENED_DESC;
        if (recentActive) applyFilter(); // may newly include this entry
        else if (openedSort) rebuildCards();
    }

    /** Find a loaded entry by its resolved URL (for the script pane's open callback). */
    private DatasetEntry entryForUrl(String url) {
        if (url == null) return null;
        for (DatasetEntry e : allEntries) {
            if (e.isArchive() && url.equals(datasetUrl(e))) return e;
        }
        return null;
    }

    // Set by the command that launches the window, to actually open an archive URL
    // (e.g. delegating to the same path CloudArchiveOpenWindow's callback uses).
    private java.util.function.Consumer<String> onOpenArchive;

    public void setOnOpenArchive(java.util.function.Consumer<String> handler) {
        this.onOpenArchive = handler;
    }

    private void refreshCardTags(DatasetEntry e) {
        for (javafx.scene.Node n : cardsBox.getChildren()) {
            if (n instanceof DatasetCard && ((DatasetCard) n).getEntry() == e) {
                ((DatasetCard) n).rebuildTagChips();
            }
        }
    }

    private void selectEntry(DatasetEntry e, DatasetCard card) {
        selected = e;
        for (javafx.scene.Node n : cardsBox.getChildren()) {
            if (n instanceof DatasetCard) ((DatasetCard) n).setSelected(n == card);
        }
        showDetailsFor(e);
        loadCommentsFor(e);
    }

    private void loadCommentsFor(DatasetEntry e) {
        if (notesPane == null) return;
        // Unbind while swapping so the change callback doesn't attribute the
        // load to the previous dataset.
        notesBoundEntry = null;
        notesPane.setMarkdown(e == null ? "" : e.getCommentsMarkdown());
        notesPane.setDisable(e == null);
        if (notesHeader != null)
            notesHeader.setText(e == null ? "Notes — select a dataset"
                    : "Notes — " + e.getName());
        notesBoundEntry = e;
    }

    // -----------------------------------------------------------------
    // Persistence helpers
    // -----------------------------------------------------------------

    private void persistUserData() {
        if (store == null) return;
        try {
            store.writeUserData(sourceEndpoint(),
                    sourceBucket(), new ArrayList<>(allEntries));
        } catch (Exception ignore) {}
    }

    private void persistPrefs() {
        PREFS.put(PREF_ENDPOINT, endpointField.getText().trim());
        PREFS.put(PREF_BUCKET, currentBucket());
        PREFS.put(PREF_INDEX_FOLDER, indexFolderField.getText().trim());
        if (localPathField != null) PREFS.put(PREF_LOCAL_PATH, localPathField.getText().trim());
        PREFS.putBoolean(PREF_LOCAL_MODE, localMode);
        // Window size + divider positions, so the layout reopens the same next run.
        if (stage != null) {
            PREFS.putDouble(PREF_WIN_W, stage.getWidth());
            PREFS.putDouble(PREF_WIN_H, stage.getHeight());
        }
        // Capture the live divider positions if all three panes are present (the
        // user may have dragged since the last collapse/expand).
        if (split != null && split.getItems().contains(leftPane)
                && split.getItems().contains(rightPane)) {
            double[] pos = split.getDividerPositions();
            if (pos.length >= 2) { savedLeftDivider = pos[0]; savedRightDivider = pos[1]; }
        }
        PREFS.putDouble(PREF_DIV_LEFT, savedLeftDivider);
        PREFS.putDouble(PREF_DIV_RIGHT, savedRightDivider);
        // Theme state is owned by MarsThemeManager (its own pref), not stored here.
    }

    private void loadPrefsIntoFields() {
        // fields were already initialized from PREFS at construction; nothing else needed
    }

    // -----------------------------------------------------------------
    // Small utilities
    // -----------------------------------------------------------------

    private void chooseIndexFolder() {
        javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
        dc.setTitle("Choose index storage folder");
        java.io.File f = dc.showDialog(stage);
        if (f != null) {
            indexFolderField.setText(f.getAbsolutePath());
            persistPrefs();
        }
    }

    private void applyTheme(Scene scene) {
        if (scene == null) return;
        // Delegate to the app-wide manager, which clears and applies the single
        // master stylesheet (master-light.css / master-dark.css) that styles
        // everything — same mechanism CloudArchiveOpenWindow and the rest use.
        MarsThemeManager.applyTheme(scene, darkMode);
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    /**
     * Style a filter toggle so its pressed state is unmistakable: selected shows a
     * filled accent background with white text; unselected is a muted outlined
     * button. Resolves against the active theme's -fx-accent, so it works in both
     * light and dark.
     */
    private static void styleFilterToggle(javafx.scene.control.ToggleButton t) {
        if (t.isSelected()) {
            t.setStyle("-fx-background-color: -fx-accent; -fx-text-fill: white;"
                    + " -fx-font-weight: bold; -fx-background-radius: 4;"
                    + " -fx-border-color: -fx-accent; -fx-border-width: 1; -fx-border-radius: 4;");
        } else {
            t.setStyle("-fx-background-color: transparent;"
                    + " -fx-text-fill: -fx-text-inner-color; -fx-opacity: 0.75;"
                    + " -fx-background-radius: 4;"
                    + " -fx-border-color: derive(-fx-text-inner-color, 45%);"
                    + " -fx-border-width: 1; -fx-border-radius: 4;");
        }
    }

    private static VBox labeled(String label, javafx.scene.layout.Region field) {
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 11px; -fx-opacity: 0.8;");
        field.setMaxWidth(Double.MAX_VALUE);
        return new VBox(2, l, field);
    }

    private static String fmtMillis(Long millis) {
        if (millis == null) return "—";
        return DatasetCardDateFormat.FMT.format(Instant.ofEpochMilli(millis));
    }

    private static String humanBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        String[] units = {"KB", "MB", "GB", "TB"};
        double v = bytes;
        int i = -1;
        do { v /= 1024; i++; } while (v >= 1024 && i < units.length - 1);
        return String.format("%.1f %s", v, units[i]);
    }

    // Shared date format holder (avoids re-instantiating in a static context).
    private static final class DatasetCardDateFormat {
        static final java.time.format.DateTimeFormatter FMT =
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                        .withZone(ZoneId.systemDefault());
    }
}