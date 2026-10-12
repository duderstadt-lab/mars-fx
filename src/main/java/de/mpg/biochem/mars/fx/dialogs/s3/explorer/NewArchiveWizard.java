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
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.kordamp.ikonli.fontawesome.FontAwesome;

import de.mpg.biochem.mars.fx.dialogs.s3.CloudArchiveSaveDialog;
import de.mpg.biochem.mars.fx.molecule.metadataTab.BdvSourceOptionsPane;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.fx.util.MarsThemeManager;
import de.mpg.biochem.mars.metadata.MarsBdvSource;
import de.mpg.biochem.mars.metadata.MarsOMEMetadata;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.n5.MarsS3Browser;
import de.mpg.biochem.mars.util.LogBuilder;
import de.mpg.biochem.mars.util.MarsMath;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.SVGPath;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Window;

/**
 * Step-by-step wizard, opened from the Dataset Explorer's plus button, that
 * builds a new Molecule Archive from the N5/Zarr datasets currently shown by
 * the explorer's filter. Each dataset becomes one {@link MarsOMEMetadata}
 * record, and each record carries the {@link MarsBdvSource}s defined for it.
 *
 * <p>Layout:
 * <pre>
 *  BorderPane
 *  ├── LEFT:   step sidebar (numbered circles joined by connector lines)
 *  └── CENTER: the current step's page
 *  Buttons:    Cancel · Back · Next (Next becomes Create on the last step)
 * </pre>
 *
 * <p>Steps: 1. Datasets (which datasets become records), 2. BDV Sources
 * (sources per record, autofilled from each container), 3. Archive Type,
 * 4. Destination (folder + name, defaulting to the explorer's current bucket
 * or local folder, or to the datasets' shared subfolder).
 *
 * <p>Theme: the sidebar, step and tile styles live in the light/dark theme
 * sheets (classes prefixed {@code wizard-} and {@code archive-type-}), applied
 * through {@link MarsThemeManager} like every other Mars dialog.
 */
public class NewArchiveWizard extends Dialog<Void> {

    /** One N5/Zarr dataset offered by the explorer. */
    public static final class SourceDataset {

        final String name;      // display name, e.g. "lane1.n5"
        final String path;      // path relative to the bucket or local root
        final String url;       // openable URL (S3) or absolute local path
        final long sizeBytes;   // -1 if unknown

        public SourceDataset(String name, String path, String url, long sizeBytes) {
            this.name = name;
            this.path = path;
            this.url = url;
            this.sizeBytes = sizeBytes;
        }
    }

    /** Outcome of a successful create, handed to {@link #setOnCreated}. */
    public static final class Created {

        /** Saved location: S3 URL or absolute local path. */
        public final String location;
        /** File name of the archive, including its ending. */
        public final String name;
        /**
         * Path relative to the explorer's current bucket or local root, or null
         * when the archive was saved outside it.
         */
        public final String relativePath;
        /** True if the user asked for the archive to be opened. */
        public final boolean open;

        Created(String location, String name, String relativePath, boolean open) {
            this.location = location;
            this.name = name;
            this.relativePath = relativePath;
            this.open = open;
        }
    }

    /**
     * The archive types offered on the type step. Classes are resolved by name
     * so optional types (TransverseFlowArchive lives in mars-transverseflow,
     * which depends on mars-fx rather than the other way round) only show up
     * enabled when they're on the classpath.
     */
    private enum ArchiveType {
        SINGLE_MOLECULE("Single Molecule", "SingleMoleculeArchive",
                "de.mpg.biochem.mars.molecule.SingleMoleculeArchive",
                "Spots tracked over time, e.g. TIRF single-molecule imaging."),
        DNA_MOLECULE("DNA Molecule", "DnaMoleculeArchive",
                "de.mpg.biochem.mars.molecule.DnaMoleculeArchive",
                "Stretched DNA molecules with positions along each strand."),
        OBJECT("Object", "ObjectArchive",
                "de.mpg.biochem.mars.object.ObjectArchive",
                "Segmented objects with shapes tracked over time."),
        TRANSVERSE_FLOW("Transverse Flow", "TransverseFlowArchive",
                "de.mpg.biochem.mars.transverseflow.TransverseFlowArchive",
                "Replication forks imaged with transverse flow.");

        final String title;
        final String archiveClassName;
        final String className;
        final String description;

        ArchiveType(String title, String archiveClassName, String className,
                    String description) {
            this.title = title;
            this.archiveClassName = archiveClassName;
            this.className = className;
            this.description = description;
        }

        Class<?> archiveClass() {
            for (ClassLoader cl : new ClassLoader[] {
                    Thread.currentThread().getContextClassLoader(),
                    NewArchiveWizard.class.getClassLoader() }) {
                if (cl == null) continue;
                try {
                    return Class.forName(className, true, cl);
                }
                catch (ClassNotFoundException | LinkageError ignore) {}
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        MoleculeArchive<?, MarsOMEMetadata, ?, ?> create(String name) throws Exception {
            Class<?> c = archiveClass();
            if (c == null) throw new IllegalStateException(archiveClassName
                    + " is not available.");
            return (MoleculeArchive<?, MarsOMEMetadata, ?, ?>) c.getConstructor(
                    String.class).newInstance(name);
        }
    }

    /** One dataset row on the first step, with its include checkbox. */
    private static final class DatasetRow {

        final SourceDataset dataset;
        final BooleanProperty include = new SimpleBooleanProperty(true);

        DatasetRow(SourceDataset dataset) { this.dataset = dataset; }
    }

    /** The metadata record being assembled for one dataset. */
    private static final class RecordDraft {

        final SourceDataset dataset;
        final String uid = MarsMath.getUUID58().substring(0, 10);
        final ObservableList<MarsBdvSource> sources = FXCollections.observableArrayList();
        // Datasets inside the container; null until the background listing returns.
        List<de.mpg.biochem.mars.n5.DatasetEntry> containerDatasets;
        boolean listing;

        RecordDraft(SourceDataset dataset) { this.dataset = dataset; }
    }

    private static final String ENDING_FILE = ".yama";
    private static final String ENDING_STORE = ".yama.store";

    // Explicit border so fields stay visible in light mode — same treatment as
    // the explorer's own text fields.
    private static final String FIELD_STYLE =
            "-fx-border-color: derive(-fx-text-inner-color, 55%);"
                    + " -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;";

    private static final PseudoClass CURRENT = PseudoClass.getPseudoClass("current");
    private static final PseudoClass DONE = PseudoClass.getPseudoClass("done");

    private static final String[] STEP_NAMES = {
            "Datasets", "BDV Sources", "Archive Type", "Destination" };

    // Explorer context
    private final boolean localSource;
    private final String endpoint;
    private final String bucketOrRoot;
    private final int skippedArchives;

    // Model
    private final ObservableList<DatasetRow> datasetRows = FXCollections.observableArrayList();
    private final Map<String, RecordDraft> drafts = new LinkedHashMap<>(); // keyed by url
    private final ObservableList<RecordDraft> records = FXCollections.observableArrayList();
    private ArchiveType archiveType;

    // Navigation
    private int step = 0;
    private int furthestStep = 0;
    private final List<HBox> stepRows = new ArrayList<>();
    private final StackPane pageHolder = new StackPane();
    private final Region[] pages = new Region[STEP_NAMES.length];
    private final ButtonType backType = new ButtonType("Back", ButtonData.BACK_PREVIOUS);
    private final ButtonType nextType = new ButtonType("Next", ButtonData.NEXT_FORWARD);
    private Button backButton;
    private Button nextButton;
    private Button cancelButton;
    private boolean busy = false;

    // Step 1
    private Label datasetCountLabel;

    // Step 2
    private ListView<RecordDraft> recordList;
    private ListView<MarsBdvSource> sourceList;
    private ComboBox<String> sourceNameBox;
    private Label containerLabel;
    private BdvSourceOptionsPane optionsPane;

    // Step 4
    private TextField folderField;
    private TextField nameField;
    private Button endingButton;
    private boolean storeEnding = false;
    private Label destinationPreview;
    private Label summaryLabel;
    private CheckBox openAfterCreate;
    private ProgressIndicator createProgress;
    private Label createStatus;

    private Consumer<Created> onCreated;

    // Lists container datasets for source autofill, one container at a time.
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NewArchiveWizard");
        t.setDaemon(true);
        return t;
    });

    /**
     * @param owner the explorer window
     * @param datasets N5/Zarr datasets from the explorer's current filter, in
     *          card order
     * @param skippedArchives number of archives in the filter (not offered)
     * @param localSource true if the explorer is showing a local folder
     * @param endpoint S3 server (cloud source); ignored for local
     * @param bucketOrRoot current bucket (cloud) or indexed root folder (local)
     * @param darkMode current theme
     */
    public NewArchiveWizard(Window owner, List<SourceDataset> datasets,
                            int skippedArchives, boolean localSource,
                            String endpoint, String bucketOrRoot, boolean darkMode)
    {
        this.localSource = localSource;
        this.endpoint = endpoint == null ? "" : endpoint;
        this.bucketOrRoot = bucketOrRoot == null ? "" : bucketOrRoot;
        this.skippedArchives = skippedArchives;
        for (SourceDataset d : datasets) datasetRows.add(new DatasetRow(d));

        setTitle("New Molecule Archive");
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        setResizable(true);

        pages[0] = buildDatasetsPage();
        pages[1] = buildSourcesPage();
        pages[2] = buildTypePage();
        pages[3] = buildDestinationPage();

        BorderPane content = new BorderPane();
        content.setLeft(buildSidebar());
        pageHolder.setPadding(new Insets(20, 24, 12, 24));
        content.setCenter(pageHolder);
        content.setPrefSize(1240, 680);
        // Edge-to-edge sidebar: drop the dialog's default content padding.
        content.setStyle("-fx-padding: 0;");

        getDialogPane().setContent(content);
        getDialogPane().setPadding(Insets.EMPTY);
        getDialogPane().getStyleClass().add("new-archive-wizard");
        getDialogPane().getButtonTypes().addAll(backType, nextType, ButtonType.CANCEL);

        // Cancel on the left of the right-hand group, then Back and Next, on
        // every platform (the default order varies by OS).
        Node bar = getDialogPane().lookup(".button-bar");
        if (bar instanceof ButtonBar) ((ButtonBar) bar).setButtonOrder("+C_BX");

        backButton = (Button) getDialogPane().lookupButton(backType);
        nextButton = (Button) getDialogPane().lookupButton(nextType);
        cancelButton = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        nextButton.setDefaultButton(false);

        // Back/Next move between pages; consume so the dialog stays open.
        backButton.addEventFilter(ActionEvent.ACTION, e -> {
            e.consume();
            if (step > 0) goTo(step - 1);
        });
        nextButton.addEventFilter(ActionEvent.ACTION, e -> {
            e.consume();
            if (step == pages.length - 1) create();
            else if (canAdvance()) goTo(step + 1);
        });

        // No closing mid-save — the archive is being written.
        setOnCloseRequest(e -> {
            if (busy) e.consume();
        });

        setResultConverter(bt -> null);
        setOnHidden(e -> {
            executor.shutdownNow();
            // Unbind first so a pending debounced field check finds no source
            // and skips, rather than submitting to the stopped executor.
            optionsPane.setMarsBdvSource(null);
            optionsPane.shutdown();
        });

        // Theme the pane, not the scene — the dialog has no scene until it shows.
        MarsThemeManager.applyTheme(getDialogPane(), darkMode);

        goTo(0);
    }

    /** Called on the FX thread once the archive has been saved. */
    public void setOnCreated(Consumer<Created> handler) {
        this.onCreated = handler;
    }

    // -----------------------------------------------------------------
    // Sidebar + navigation
    // -----------------------------------------------------------------

    private Region buildSidebar() {
        Label title = new Label("New Archive");
        title.getStyleClass().add("wizard-sidebar-title");
        Label subtitle = new Label("Molecule Archive setup");
        subtitle.getStyleClass().add("wizard-sidebar-subtitle");

        VBox steps = new VBox();
        steps.setPadding(new Insets(28, 0, 0, 0));
        for (int i = 0; i < STEP_NAMES.length; i++) {
            Circle circle = new Circle(16);
            circle.getStyleClass().add("wizard-step-circle");
            Label number = new Label(String.valueOf(i + 1));
            number.getStyleClass().add("wizard-step-number");
            StackPane badge = new StackPane(circle, number);
            badge.setMinSize(32, 32);

            Label name = new Label(STEP_NAMES[i]);
            name.getStyleClass().add("wizard-step-label");

            HBox row = new HBox(14, badge, name);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("wizard-step");
            final int target = i;
            // Steps already reached can be revisited directly.
            row.setOnMouseClicked(e -> {
                if (!busy && target <= furthestStep && target != step) goTo(target);
            });
            stepRows.add(row);
            steps.getChildren().add(row);

            if (i < STEP_NAMES.length - 1) {
                Line connector = new Line(0, 0, 0, 34);
                connector.getStyleClass().add("wizard-step-connector");
                HBox holder = new HBox(connector);
                // Center the line under the 32 px badge.
                holder.setPadding(new Insets(4, 0, 4, 15.5));
                steps.getChildren().add(holder);
            }
        }

        VBox sidebar = new VBox(4, title, subtitle, steps);
        sidebar.getStyleClass().add("wizard-sidebar");
        sidebar.setPadding(new Insets(24, 24, 24, 24));
        sidebar.setMinWidth(220);
        sidebar.setPrefWidth(220);
        return sidebar;
    }

    private void goTo(int target) {
        if (step == 0 && target > 0) syncRecords();
        step = target;
        furthestStep = Math.max(furthestStep, step);
        pageHolder.getChildren().setAll(pages[step]);

        for (int i = 0; i < stepRows.size(); i++) {
            HBox row = stepRows.get(i);
            row.pseudoClassStateChanged(CURRENT, i == step);
            row.pseudoClassStateChanged(DONE, i < step);
        }
        updateStepCursors();

        if (step == 1) onSourcesPageShown();
        if (step == 3) refreshDestinationPreview();
        updateButtons();
    }

    private boolean canAdvance() {
        switch (step) {
            case 0: return includedCount() > 0;
            case 2: return archiveType != null;
            default: return true;
        }
    }

    private void updateButtons() {
        backButton.setDisable(busy || step == 0);
        cancelButton.setDisable(busy);
        boolean last = step == pages.length - 1;
        nextButton.setText(last ? "Create" : "Next");
        nextButton.setDisable(busy || (last ? !canCreate() : !canAdvance()));
    }

    // -----------------------------------------------------------------
    // Step 1 — Datasets
    // -----------------------------------------------------------------

    private Region buildDatasetsPage() {
        ListView<DatasetRow> list = new ListView<>(datasetRows);
        list.setCellFactory(lv -> new DatasetRowCell());
        list.setPlaceholder(new Label(
                "No N5/Zarr datasets match the explorer's current filter."));
        VBox.setVgrow(list, Priority.ALWAYS);

        for (DatasetRow r : datasetRows)
            r.include.addListener((o, was, now) -> {
                updateDatasetCount();
                updateButtons();
            });

        Button all = new Button("Select all");
        all.setOnAction(e -> datasetRows.forEach(r -> r.include.set(true)));
        Button none = new Button("Select none");
        none.setOnAction(e -> datasetRows.forEach(r -> r.include.set(false)));
        datasetCountLabel = new Label();
        datasetCountLabel.getStyleClass().add("wizard-hint");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(6, datasetCountLabel, spacer, all, none);
        footer.setAlignment(Pos.CENTER_LEFT);
        updateDatasetCount();

        return page("Datasets",
                "Each N5/Zarr dataset below becomes one metadata record. The list "
                        + "follows the Dataset Explorer's current search and filters.",
                list, footer);
    }

    private int includedCount() {
        int n = 0;
        for (DatasetRow r : datasetRows) if (r.include.get()) n++;
        return n;
    }

    private void updateDatasetCount() {
        if (datasetCountLabel == null) return;
        String text = includedCount() + " of " + datasetRows.size()
                + " dataset(s) selected";
        if (skippedArchives > 0)
            text += " · " + skippedArchives + " archive(s) in the filter skipped";
        datasetCountLabel.setText(text);
    }

    private static final class DatasetRowCell extends ListCell<DatasetRow> {

        private final CheckBox check = new CheckBox();
        private final Label name = new Label();
        private final Label path = new Label();
        private final Label size = new Label();
        private final HBox root;
        private DatasetRow bound;

        DatasetRowCell() {
            name.setStyle("-fx-font-weight: bold;");
            path.getStyleClass().add("wizard-hint");
            size.getStyleClass().add("wizard-hint");
            VBox text = new VBox(2, name, path);
            HBox.setHgrow(text, Priority.ALWAYS);
            root = new HBox(10, check, ActionUtils.icon(FontAwesome.DATABASE, "1.4em"),
                    text, size);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(4, 4, 4, 4));
        }

        @Override
        protected void updateItem(DatasetRow item, boolean empty) {
            super.updateItem(item, empty);
            if (bound != null) check.selectedProperty().unbindBidirectional(bound.include);
            bound = null;
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            bound = item;
            check.selectedProperty().bindBidirectional(item.include);
            name.setText(item.dataset.name);
            path.setText(item.dataset.path);
            size.setText(item.dataset.sizeBytes >= 0 ? humanBytes(item.dataset.sizeBytes) : "");
            setText(null);
            setGraphic(root);
        }
    }

    /**
     * Rebuild the record list from the checked datasets, keeping any sources
     * already defined for datasets that stay selected.
     */
    private void syncRecords() {
        List<RecordDraft> next = new ArrayList<>();
        for (DatasetRow r : datasetRows) {
            if (!r.include.get()) continue;
            next.add(drafts.computeIfAbsent(r.dataset.url, k -> new RecordDraft(r.dataset)));
        }
        RecordDraft selected = recordList.getSelectionModel().getSelectedItem();
        records.setAll(next);
        if (selected != null && records.contains(selected))
            recordList.getSelectionModel().select(selected);
    }

    // -----------------------------------------------------------------
    // Step 2 — BDV sources
    // -----------------------------------------------------------------

    private Region buildSourcesPage() {
        // --- Records (one per dataset) ---
        recordList = new ListView<>(records);
        recordList.setCellFactory(lv -> new ListCell<RecordDraft>() {
            @Override
            protected void updateItem(RecordDraft item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(item.dataset.name + "  (" + item.sources.size() + ")");
                setGraphic(ActionUtils.icon(FontAwesome.DATABASE, "1.0em"));
            }
        });
        recordList.setPrefHeight(170);
        recordList.getSelectionModel().selectedItemProperty().addListener(
                (o, was, now) -> showRecord(now));

        // --- Sources of the selected record ---
        sourceNameBox = new ComboBox<>();
        sourceNameBox.setEditable(true);
        sourceNameBox.setStyle(FIELD_STYLE);
        sourceNameBox.setPromptText("Source name");
        sourceNameBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(sourceNameBox, Priority.ALWAYS);
        sourceNameBox.setTooltip(new Tooltip(
                "Type a name, or pick one of the container's datasets to use it "
                        + "as both the name and the dataset."));
        sourceNameBox.getEditor().setOnAction(e -> {
            e.consume();
            addSourceFromField();
        });

        Button add = new Button();
        add.setGraphic(ActionUtils.icon(FontAwesome.PLUS, "1.0em"));
        add.setTooltip(new Tooltip("Add source"));
        add.setOnAction(e -> addSourceFromField());
        HBox addRow = new HBox(4, sourceNameBox, add);
        addRow.setAlignment(Pos.CENTER_LEFT);

        sourceList = new ListView<>();
        sourceList.setCellFactory(lv -> new ListCell<MarsBdvSource>() {
            @Override
            protected void updateItem(MarsBdvSource item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(item.getName());
                setGraphic(ActionUtils.icon(FontAwesome.IMAGE, "1.0em"));
            }
        });
        sourceList.setPlaceholder(new Label("No sources yet"));
        VBox.setVgrow(sourceList, Priority.ALWAYS);
        sourceList.getSelectionModel().selectedItemProperty().addListener(
                (o, was, now) -> optionsPane.setMarsBdvSource(now));

        Button remove = new Button("Remove");
        remove.setGraphic(ActionUtils.icon(FontAwesome.MINUS, "1.0em"));
        remove.disableProperty().bind(sourceList.getSelectionModel()
                .selectedItemProperty().isNull());
        remove.setOnAction(e -> {
            RecordDraft r = recordList.getSelectionModel().getSelectedItem();
            MarsBdvSource s = sourceList.getSelectionModel().getSelectedItem();
            if (r != null && s != null) {
                r.sources.remove(s);
                recordList.refresh();
            }
        });

        Button applyAll = new Button("Apply to all");
        applyAll.setGraphic(ActionUtils.icon(FontAwesome.CLONE, "1.0em"));
        applyAll.setTooltip(new Tooltip(
                "Copy these sources to every other record, pointing each copy "
                        + "at that record's own dataset."));
        applyAll.setOnAction(e -> applySourcesToAll());
        applyAll.disableProperty().bind(javafx.beans.binding.Bindings.size(records)
                .lessThan(2));

        HBox sourceButtons = new HBox(6, remove, applyAll);

        containerLabel = new Label();
        containerLabel.getStyleClass().add("wizard-hint");
        containerLabel.setWrapText(true);

        VBox left = new VBox(6,
                sectionLabel("Records"), recordList,
                sectionLabel("Sources"), addRow, sourceList, sourceButtons,
                containerLabel);
        left.setMinWidth(240);
        left.setPrefWidth(250);
        left.setMaxWidth(300);

        // --- Settings for the selected source (the metadata tab's own pane) ---
        optionsPane = new BdvSourceOptionsPane();
        optionsPane.setMarsBdvSource(null);
        VBox right = new VBox(6, sectionLabel("Source settings"), optionsPane);
        HBox.setHgrow(right, Priority.ALWAYS);

        HBox body = new HBox(16, left, right);
        VBox.setVgrow(body, Priority.ALWAYS);

        return page("BDV Sources",
                "Add the BigDataViewer sources for each record. New sources point "
                        + "at the record's dataset, with the container dataset and "
                        + "channel filled in where they can be guessed.",
                body);
    }

    private void onSourcesPageShown() {
        if (recordList.getSelectionModel().getSelectedItem() == null && !records.isEmpty())
            recordList.getSelectionModel().select(0);
        else showRecord(recordList.getSelectionModel().getSelectedItem());
    }

    private void showRecord(RecordDraft r) {
        if (r == null) {
            sourceList.setItems(FXCollections.observableArrayList());
            sourceNameBox.getItems().clear();
            containerLabel.setText("");
            optionsPane.setMarsBdvSource(null);
            return;
        }
        sourceList.setItems(r.sources);
        if (!r.sources.isEmpty()) sourceList.getSelectionModel().select(0);
        else optionsPane.setMarsBdvSource(null);
        updateContainerInfo(r);
        listContainer(r);
    }

    private void updateContainerInfo(RecordDraft r) {
        if (r.containerDatasets == null) {
            sourceNameBox.getItems().clear();
            containerLabel.setText(r.listing ? "Reading " + r.dataset.name + " …" : "");
            return;
        }
        List<String> names = new ArrayList<>();
        for (de.mpg.biochem.mars.n5.DatasetEntry e : r.containerDatasets)
            names.add(e.getName());
        sourceNameBox.getItems().setAll(names);
        containerLabel.setText(names.isEmpty() ? "No datasets found in " + r.dataset.name
                : "Container datasets: " + String.join(", ", names));
    }

    /** List the datasets inside the record's container (once), off the FX thread. */
    private void listContainer(RecordDraft r) {
        if (r.containerDatasets != null || r.listing) return;
        r.listing = true;
        updateContainerInfo(r);
        Task<List<de.mpg.biochem.mars.n5.DatasetEntry>> task =
                new Task<List<de.mpg.biochem.mars.n5.DatasetEntry>>() {
            @Override
            protected List<de.mpg.biochem.mars.n5.DatasetEntry> call() {
                return MarsS3Browser.listDatasets(r.dataset.url);
            }
        };
        task.setOnSucceeded(e -> containerListed(r, task.getValue()));
        task.setOnFailed(e -> containerListed(r, new ArrayList<>()));
        executor.submit(task);
    }

    private void containerListed(RecordDraft r, List<de.mpg.biochem.mars.n5.DatasetEntry> list) {
        r.listing = false;
        r.containerDatasets = list;
        // Fill in the dataset for any source added before the listing arrived.
        for (MarsBdvSource s : r.sources)
            if (s.getN5Dataset() == null || s.getN5Dataset().isEmpty()) autofill(r, s);
        if (r == recordList.getSelectionModel().getSelectedItem()) {
            updateContainerInfo(r);
            optionsPane.setMarsBdvSource(sourceList.getSelectionModel().getSelectedItem());
        }
    }

    private void addSourceFromField() {
        RecordDraft r = recordList.getSelectionModel().getSelectedItem();
        if (r == null) return;
        String name = sourceNameBox.getEditor().getText();
        name = name == null ? "" : name.trim();
        if (name.isEmpty()) return;
        for (MarsBdvSource existing : r.sources) {
            if (existing.getName().equals(name)) {
                sourceList.getSelectionModel().select(existing);
                return; // names are unique within a record
            }
        }
        MarsBdvSource s = new MarsBdvSource(name);
        s.setN5(true);
        s.setPath(r.dataset.url);
        r.sources.add(s);
        autofill(r, s);
        sourceNameBox.getEditor().clear();
        sourceNameBox.setValue(null);
        sourceList.getSelectionModel().select(s);
        recordList.refresh();
    }

    /**
     * Guess the container dataset and channel for a source: a dataset whose name
     * matches the source name (exactly, then by containment) is used at channel
     * 0; otherwise the first dataset is used, with the source's position in the
     * list as its channel, so successive sources step through the channels.
     */
    private static void autofill(RecordDraft r, MarsBdvSource s) {
        if (r.containerDatasets == null || r.containerDatasets.isEmpty()) return;
        String name = s.getName().toLowerCase();
        String match = null;
        for (de.mpg.biochem.mars.n5.DatasetEntry e : r.containerDatasets)
            if (e.getName().equalsIgnoreCase(name)) { match = e.getName(); break; }
        if (match == null)
            for (de.mpg.biochem.mars.n5.DatasetEntry e : r.containerDatasets) {
                String n = e.getName().toLowerCase();
                if (!name.isEmpty() && (n.contains(name) || name.contains(n))
                        && !n.equals(MarsS3Browser.ROOT_DATASET)) {
                    match = e.getName();
                    break;
                }
            }
        if (match != null) {
            s.setN5Dataset(match);
            s.setChannel(0);
        }
        else {
            s.setN5Dataset(r.containerDatasets.get(0).getName());
            s.setChannel(Math.max(0, r.sources.indexOf(s)));
        }
    }

    private void applySourcesToAll() {
        RecordDraft from = recordList.getSelectionModel().getSelectedItem();
        if (from == null) return;
        boolean othersHaveSources = false;
        for (RecordDraft r : records)
            if (r != from && !r.sources.isEmpty()) othersHaveSources = true;
        if (othersHaveSources) {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                    "Replace the sources of every other record with copies of the "
                            + "sources of " + from.dataset.name + "?",
                    ButtonType.YES, ButtonType.NO);
            a.initOwner(getDialogPane().getScene().getWindow());
            a.setHeaderText(null);
            MarsThemeManager.applyTheme(a.getDialogPane(), MarsThemeManager.isDarkTheme());
            if (a.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        }
        for (RecordDraft r : records) {
            if (r == from) continue;
            r.sources.clear();
            for (MarsBdvSource s : from.sources) r.sources.add(copyFor(s, r.dataset.url));
        }
        recordList.refresh();
    }

    /** Copy a source's settings, pointing the copy at a different dataset. */
    private static MarsBdvSource copyFor(MarsBdvSource s, String path) {
        MarsBdvSource c = new MarsBdvSource(s.getName());
        c.setN5(s.isN5());
        c.setPath(path);
        c.setN5Dataset(s.getN5Dataset());
        c.setChannel(s.getChannel());
        c.setCorrectDrift(s.getCorrectDrift());
        c.setSingleTimePointMode(s.getSingleTimePointMode());
        c.setSingleTimePoint(s.getSingleTimePoint());
        net.imglib2.realtransform.AffineTransform3D a = s.getAffineTransform3D();
        c.setAffineTransform2D(a.get(0, 0), a.get(0, 1), a.get(0, 3),
                a.get(1, 0), a.get(1, 1), a.get(1, 3));
        return c;
    }

    // -----------------------------------------------------------------
    // Step 3 — Archive type
    // -----------------------------------------------------------------

    private Region buildTypePage() {
        ToggleGroup group = new ToggleGroup();
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);

        ArchiveType[] types = ArchiveType.values();
        for (int i = 0; i < types.length; i++) {
            ArchiveType type = types[i];
            boolean available = type.archiveClass() != null;

            Label title = new Label(type.title);
            title.getStyleClass().add("archive-type-title");
            Label cls = new Label(type.archiveClassName);
            cls.getStyleClass().add("wizard-hint");
            Label desc = new Label(available ? type.description
                    : "Not installed — enable the update site that provides it.");
            desc.getStyleClass().add("archive-type-desc");
            desc.setWrapText(true);
            desc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            StackPane glyph = new StackPane(glyphFor(type));
            glyph.setMinHeight(64);
            VBox inner = new VBox(8, glyph, title, cls, desc);
            inner.setAlignment(Pos.TOP_CENTER);

            ToggleButton tile = new ToggleButton();
            tile.setGraphic(inner);
            tile.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            tile.getStyleClass().add("archive-type-tile");
            tile.setUserData(type);
            tile.setToggleGroup(group);
            tile.setDisable(!available);
            tile.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            tile.setPrefSize(280, 190);
            GridPane.setHgrow(tile, Priority.ALWAYS);
            GridPane.setVgrow(tile, Priority.ALWAYS);
            // Double-click picks the type and moves on.
            tile.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && tile.isSelected()) goTo(3);
            });
            grid.add(tile, i % 2, i / 2);
        }

        group.selectedToggleProperty().addListener((o, was, now) -> {
            // Keep one tile selected — clicking the selected tile doesn't clear it.
            if (now == null && was != null) {
                was.setSelected(true);
                return;
            }
            archiveType = now == null ? null : (ArchiveType) now.getUserData();
            updateButtons();
        });
        VBox.setVgrow(grid, Priority.ALWAYS);

        return page("Archive Type",
                "Choose the kind of Molecule Archive to create.", grid);
    }

    /** Small line-art glyph for each archive type, colored by the theme sheet. */
    private static Node glyphFor(ArchiveType type) {
        Group g = new Group();
        switch (type) {
            case SINGLE_MOLECULE: {
                // A bright spot with a faint halo, plus a short track.
                Circle halo = new Circle(32, 32, 20);
                halo.getStyleClass().add("archive-type-glyph");
                Circle spot = new Circle(32, 32, 8);
                spot.getStyleClass().add("archive-type-glyph-fill");
                SVGPath track = svg("M 58 50 C 66 44, 70 54, 78 46 S 88 40, 92 30");
                Circle end = new Circle(92, 30, 4);
                end.getStyleClass().add("archive-type-glyph-fill");
                g.getChildren().addAll(halo, spot, track, end);
                break;
            }
            case DNA_MOLECULE: {
                // Two intertwined strands tethered at both ends.
                SVGPath a = svg("M 4 32 C 19 8, 34 8, 49 32 S 79 56, 94 32");
                SVGPath b = svg("M 4 32 C 19 56, 34 56, 49 32 S 79 8, 94 32");
                Circle left = new Circle(4, 32, 4);
                left.getStyleClass().add("archive-type-glyph-fill");
                Circle right = new Circle(94, 32, 4);
                right.getStyleClass().add("archive-type-glyph-fill");
                g.getChildren().addAll(a, b, left, right);
                break;
            }
            case OBJECT: {
                // An irregular cell outline with a nucleus.
                SVGPath outline = svg("M 28 10 C 48 0, 74 6, 82 24 S 76 58, 54 60 "
                        + "S 14 58, 12 38 S 14 16, 28 10 Z");
                Circle nucleus = new Circle(46, 32, 9);
                nucleus.getStyleClass().add("archive-type-glyph-fill");
                g.getChildren().addAll(outline, nucleus);
                break;
            }
            case TRANSVERSE_FLOW: {
                // A replication fork: parental duplex splitting into two arms.
                SVGPath parental = svg("M 4 32 L 44 32");
                SVGPath leading = svg("M 44 32 C 58 30, 70 18, 92 10");
                SVGPath lagging = svg("M 44 32 C 58 34, 70 46, 92 54");
                Circle fork = new Circle(44, 32, 5);
                fork.getStyleClass().add("archive-type-glyph-fill");
                g.getChildren().addAll(parental, leading, lagging, fork);
                break;
            }
        }
        return g;
    }

    private static SVGPath svg(String content) {
        SVGPath p = new SVGPath();
        p.setContent(content);
        p.getStyleClass().add("archive-type-glyph");
        return p;
    }

    // -----------------------------------------------------------------
    // Step 4 — Destination
    // -----------------------------------------------------------------

    private Region buildDestinationPage() {
        folderField = new TextField(defaultFolder());
        folderField.setStyle(FIELD_STYLE);
        folderField.setPromptText(localSource ? "/path/to/folder"
                : "https://bucket.s3.server:9000/folder/");
        HBox.setHgrow(folderField, Priority.ALWAYS);

        MenuItem s3Item = new MenuItem("S3 bucket…", ActionUtils.icon(FontAwesome.CLOUD, "1.0em"));
        s3Item.setOnAction(e -> browseS3());
        MenuItem localItem = new MenuItem("Local folder…", ActionUtils.icon(FontAwesome.FOLDER, "1.0em"));
        localItem.setOnAction(e -> browseLocal());
        MenuButton browse = new MenuButton("Browse", null, s3Item, localItem);
        HBox folderRow = new HBox(6, folderField, browse);
        folderRow.setAlignment(Pos.CENTER_LEFT);

        nameField = new TextField(defaultName());
        nameField.setStyle(FIELD_STYLE);
        nameField.setPromptText("archive name");
        HBox.setHgrow(nameField, Priority.ALWAYS);
        endingButton = new Button(ENDING_FILE);
        endingButton.setTooltip(new Tooltip(
                "Click to switch between a single .yama file and a .yama.store "
                        + "virtual store"));
        endingButton.setMinWidth(90);
        endingButton.setOnAction(e -> {
            storeEnding = !storeEnding;
            endingButton.setText(storeEnding ? ENDING_STORE : ENDING_FILE);
            refreshDestinationPreview();
        });
        HBox nameRow = new HBox(6, nameField, endingButton);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        folderField.textProperty().addListener((o, a, b) -> refreshDestinationPreview());
        nameField.textProperty().addListener((o, a, b) -> refreshDestinationPreview());

        destinationPreview = new Label();
        destinationPreview.getStyleClass().add("wizard-hint");
        destinationPreview.setWrapText(true);

        summaryLabel = new Label();
        summaryLabel.setWrapText(true);

        openAfterCreate = new CheckBox("Open the archive when it's created");
        openAfterCreate.setSelected(true);

        createProgress = new ProgressIndicator();
        createProgress.setPrefSize(18, 18);
        createProgress.setVisible(false);
        createStatus = new Label();
        createStatus.setWrapText(true);
        HBox statusRow = new HBox(8, createProgress, createStatus);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        VBox form = new VBox(6,
                sectionLabel("Folder"), folderRow,
                sectionLabel("Name"), nameRow,
                destinationPreview,
                new Separator(),
                sectionLabel("Summary"), summaryLabel,
                openAfterCreate,
                statusRow);
        form.setFillWidth(true);

        return page("Destination",
                "Choose where the new archive is saved. It defaults to the bucket "
                        + "or folder open in the explorer, inside the datasets' "
                        + "shared subfolder when they all have one.",
                form);
    }

    /**
     * Suggested folder: the explorer's current bucket or local folder, narrowed
     * to the datasets' parent subfolder when they all share the same one.
     */
    private String defaultFolder() {
        if (bucketOrRoot.isEmpty()) return "";
        String common = null;
        boolean same = true;
        for (DatasetRow r : datasetRows) {
            String parent = parentOf(r.dataset.path);
            if (common == null) common = parent;
            else if (!common.equals(parent)) { same = false; break; }
        }
        String sub = same && common != null ? common : "";
        if (localSource) {
            return sub.isEmpty() ? bucketOrRoot : new File(bucketOrRoot, sub).getAbsolutePath();
        }
        if (endpoint.isEmpty()) return "";
        String url = MarsS3Browser.buildPath(endpoint, bucketOrRoot, sub);
        return url.endsWith("/") ? url : url + "/";
    }

    /** Parent folder of a relative dataset path ("" at the top level). */
    private String parentOf(String path) {
        if (path == null) return "";
        String p = path.replace('\\', '/');
        while (p.endsWith("/")) p = p.substring(0, p.length() - 1);
        int i = p.lastIndexOf('/');
        return i < 0 ? "" : p.substring(0, i);
    }

    private String defaultName() {
        if (datasetRows.size() == 1) {
            String n = datasetRows.get(0).dataset.name.replaceAll("/+$", "");
            return n.replaceAll("(?i)\\.(n5|zarr)$", "");
        }
        return "archive";
    }

    private static boolean isCloud(String folder) {
        String f = folder.trim().toLowerCase();
        return f.startsWith("http://") || f.startsWith("https://") || f.startsWith("s3://");
    }

    private String fileName() {
        String n = nameField.getText() == null ? "" : nameField.getText().trim();
        // Drop an ending typed into the name, the button carries it.
        n = n.replaceAll("(?i)\\.yama(\\.store)?$", "");
        return n.isEmpty() ? "" : n + (storeEnding ? ENDING_STORE : ENDING_FILE);
    }

    /** Full destination (S3 URL or absolute path), or null if incomplete. */
    private String destination() {
        String folder = folderField.getText() == null ? "" : folderField.getText().trim();
        String name = fileName();
        if (folder.isEmpty() || name.isEmpty()) return null;
        if (isCloud(folder)) return folder.endsWith("/") ? folder + name : folder + "/" + name;
        return new File(folder, name).getAbsolutePath();
    }

    private boolean canCreate() {
        return !busy && archiveType != null && destination() != null;
    }

    private void refreshDestinationPreview() {
        if (destinationPreview == null) return;
        String dest = destination();
        destinationPreview.setText(dest == null ? "Enter a folder and a name."
                : "Saves to " + dest);

        int sourceCount = 0;
        for (RecordDraft r : records) sourceCount += r.sources.size();
        summaryLabel.setText("Creates a "
                + (archiveType == null ? "Molecule Archive" : archiveType.archiveClassName)
                + " with " + records.size() + " metadata record(s) and "
                + sourceCount + " BDV source(s).");
        updateButtons();
    }

    private void browseS3() {
        CloudArchiveSaveDialog dialog = new CloudArchiveSaveDialog(
                getDialogPane().getScene().getWindow(), fileName().isEmpty()
                        ? defaultName() : fileName());
        MarsThemeManager.applyTheme(dialog.getDialogPane(), MarsThemeManager.isDarkTheme());
        dialog.showAndWait().ifPresent(url -> {
            // Split the chosen URL back into folder + name + ending.
            int slash = url.lastIndexOf('/');
            String folder = url.substring(0, slash + 1);
            String file = url.substring(slash + 1);
            storeEnding = file.endsWith(ENDING_STORE);
            endingButton.setText(storeEnding ? ENDING_STORE : ENDING_FILE);
            folderField.setText(folder);
            nameField.setText(file.replaceAll("\\.yama(\\.store)?$", ""));
        });
    }

    private void browseLocal() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose folder for the new archive");
        String current = folderField.getText() == null ? "" : folderField.getText().trim();
        File start = !current.isEmpty() && !isCloud(current) ? new File(current)
                : (localSource && !bucketOrRoot.isEmpty() ? new File(bucketOrRoot) : null);
        if (start != null && start.isDirectory()) chooser.setInitialDirectory(start);
        File dir = chooser.showDialog(getDialogPane().getScene().getWindow());
        if (dir != null) folderField.setText(dir.getAbsolutePath());
    }

    // -----------------------------------------------------------------
    // Create
    // -----------------------------------------------------------------

    private void create() {
        if (!canCreate()) return;
        final String dest = destination();
        final String name = fileName();
        final ArchiveType type = archiveType;
        final boolean store = storeEnding;
        final boolean open = openAfterCreate.isSelected();
        final List<RecordDraft> recordSnapshot = new ArrayList<>(records);

        setBusy(true, "Creating " + name + " …");

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                if (exists(dest)) throw new IOException(dest + " already exists. "
                        + "Choose another name or folder.");

                MoleculeArchive<?, MarsOMEMetadata, ?, ?> archive = type.create(name);
                for (RecordDraft r : recordSnapshot) {
                    MarsOMEMetadata metadata = new MarsOMEMetadata(r.uid);
                    metadata.setSourceDirectory(r.dataset.url);
                    for (MarsBdvSource s : r.sources) metadata.putBdvSource(s);
                    metadata.logln("Created in the Dataset Explorer from " + r.dataset.url);
                    archive.putMetadata(metadata);
                }

                LogBuilder builder = new LogBuilder();
                String log = LogBuilder.buildTitleBlock("New Archive (Dataset Explorer)");
                builder.addParameter("Archive type", type.archiveClassName);
                builder.addParameter("Metadata records", recordSnapshot.size());
                builder.addParameter("Destination", dest);
                log += builder.buildParameterList();
                log += "\n" + LogBuilder.endBlock(true);
                archive.logln(log);

                if (isCloud(dest)) {
                    if (store) archive.saveAsVirtualStore(dest);
                    else archive.saveAs(dest);
                }
                else {
                    File f = new File(dest);
                    if (f.getParentFile() != null) f.getParentFile().mkdirs();
                    if (store) archive.saveAsVirtualStore(f);
                    else archive.saveAs(f);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            setBusy(false, "");
            if (onCreated != null)
                onCreated.accept(new Created(dest, name, relativeToSource(dest), open));
            close();
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            setBusy(false, "Could not create the archive: "
                    + (ex == null ? "error" : ex.getMessage()));
        });

        Thread t = new Thread(task, "NewArchiveWizard-create");
        t.setDaemon(true);
        t.start();
    }

    /** True if something is already saved at the destination (local or S3). */
    private static boolean exists(String dest) {
        if (!isCloud(dest)) return new File(dest).exists();
        MarsS3Browser.ParsedPath p = MarsS3Browser.parsePath(dest);
        if (p == null) return false;
        try (MarsS3Browser browser = new MarsS3Browser(p.server)) {
            return browser.exists(p.bucket, p.n5Root);
        }
        catch (Exception ex) {
            return false; // can't tell — let the save itself report problems
        }
    }

    /**
     * The destination's path relative to the explorer's current bucket or local
     * root, or null when it was saved somewhere else.
     */
    private String relativeToSource(String dest) {
        if (bucketOrRoot.isEmpty()) return null;
        if (localSource) {
            if (isCloud(dest)) return null;
            String root = new File(bucketOrRoot).getAbsolutePath();
            String abs = new File(dest).getAbsolutePath();
            if (!abs.startsWith(root + File.separator)) return null;
            return abs.substring(root.length() + 1);
        }
        MarsS3Browser.ParsedPath p = MarsS3Browser.parsePath(dest);
        if (p == null || !bucketOrRoot.equals(p.bucket)) return null;
        return p.n5Root;
    }

    private void setBusy(boolean on, String message) {
        busy = on;
        createProgress.setVisible(on);
        createStatus.setText(message);
        folderField.setDisable(on);
        nameField.setDisable(on);
        endingButton.setDisable(on);
        updateButtons();
        updateStepCursors();
    }

    /** Hand cursor on sidebar steps that can be jumped to. */
    private void updateStepCursors() {
        for (int i = 0; i < stepRows.size(); i++)
            stepRows.get(i).setCursor(!busy && i <= furthestStep && i != step
                    ? javafx.scene.Cursor.HAND : javafx.scene.Cursor.DEFAULT);
    }

    // -----------------------------------------------------------------
    // Shared helpers
    // -----------------------------------------------------------------

    /** A page: bold title, a short explanation, then the body nodes. */
    private static Region page(String title, String subtitle, Node... body) {
        Label t = new Label(title);
        t.getStyleClass().add("wizard-page-title");
        Label s = new Label(subtitle);
        s.getStyleClass().add("wizard-page-subtitle");
        s.setWrapText(true);
        s.setMinHeight(Region.USE_PREF_SIZE);
        VBox box = new VBox(10);
        box.getChildren().addAll(t, s);
        box.getChildren().addAll(body);
        VBox.setMargin(s, new Insets(0, 0, 6, 0));
        return box;
    }

    private static Label sectionLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("wizard-section-label");
        return l;
    }

    private static String humanBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        String[] units = { "KB", "MB", "GB", "TB" };
        double v = bytes;
        int i = -1;
        do { v /= 1024; i++; } while (v >= 1024 && i < units.length - 1);
        return String.format("%.1f %s", v, units[i]);
    }
}
