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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import org.kordamp.ikonli.fontawesome.FontAwesome;

import de.mpg.biochem.mars.fx.dialogs.RoverConfirmationDialog;
import de.mpg.biochem.mars.fx.molecule.metadataTab.BdvSourceOptionsPane;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.fx.util.MarsN5ImageMetadata;
import de.mpg.biochem.mars.fx.util.MarsThemeManager;
import de.mpg.biochem.mars.metadata.MarsBdvSource;
import de.mpg.biochem.mars.metadata.MarsOMEMetadata;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.n5.MarsS3Browser;
import de.mpg.biochem.mars.util.LogBuilder;
import de.mpg.biochem.mars.util.MarsMath;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.StringConverter;

/**
 * Step-by-step wizard, opened from the Dataset Explorer's plus button, that
 * builds a new Molecule Archive from the N5/Zarr containers currently shown by
 * the explorer's filter. Every dataset inside a container (typically one per
 * stage position, Pos0…PosN, each a multi-channel time series) becomes one
 * {@link MarsOMEMetadata} record, carrying the {@link MarsBdvSource}s defined
 * for it — usually one per channel.
 *
 * <p>Layout:
 * <pre>
 *  BorderPane
 *  ├── LEFT:   step sidebar (numbered circles joined by connector lines)
 *  └── CENTER: the current step's page
 *  Buttons:    Cancel · Back · Next (Next becomes Create on the last step)
 * </pre>
 *
 * <p>Steps: 1. Datasets (a checkable container → dataset tree; checking a
 * container checks all its datasets), 2. BDV Sources (sources per dataset,
 * with per-channel autofill and copying to other datasets), 3. Archive Type,
 * 4. Destination (folder + name, defaulting to the explorer's current bucket
 * or local folder, or to the containers' shared subfolder).
 *
 * <p>Theme: the sidebar, step and tile styles live in the light/dark theme
 * sheets (classes prefixed {@code wizard-} and {@code archive-type-}), applied
 * through {@link MarsThemeManager} like every other Mars dialog.
 */
public class NewArchiveWizard extends Dialog<Void> {

    /** One N5/Zarr container offered by the explorer. */
    public static final class Container {

        final String name;      // display name, e.g. "lane1.n5"
        final String path;      // path relative to the bucket or local root
        final String url;       // openable URL (S3) or absolute local path
        final long sizeBytes;   // -1 if unknown

        public Container(String name, String path, String url, long sizeBytes) {
            this.name = name;
            this.path = path;
            this.url = url;
            this.sizeBytes = sizeBytes;
        }
    }

    /**
     * The archive types offered on the type step, each with the icon its
     * molecules tab uses in the archive window. Classes are resolved by name
     * so optional types (TransverseFlowArchive lives in mars-transverseflow,
     * which depends on mars-fx rather than the other way round) only show up
     * enabled when they're on the classpath.
     */
    enum ArchiveType {
        SINGLE_MOLECULE("Single Molecule", "SingleMoleculeArchive",
                "de.mpg.biochem.mars.molecule.SingleMoleculeArchive",
                "Spots tracked over time, e.g. TIRF single-molecule imaging.",
                "moleculeIcon", null),
        DNA_MOLECULE("DNA Molecule", "DnaMoleculeArchive",
                "de.mpg.biochem.mars.molecule.DnaMoleculeArchive",
                "Stretched DNA molecules with positions along each strand.",
                "dnaIcon", null),
        OBJECT("Object", "ObjectArchive",
                "de.mpg.biochem.mars.object.ObjectArchive",
                "Segmented objects with shapes tracked over time.",
                "objectIcon", null),
        TRANSVERSE_FLOW("Transverse Flow", "TransverseFlowArchive",
                "de.mpg.biochem.mars.transverseflow.TransverseFlowArchive",
                "Replication forks imaged with transverse flow.",
                "transverseFlowIcon",
                "de/mpg/biochem/mars/transverseflow/TransverseFlowFrame.css");

        final String title;
        final String archiveClassName;
        final String className;
        final String description;
        final String iconStyleClass;
        final String iconStylesheet; // extra sheet defining the icon, or null

        ArchiveType(String title, String archiveClassName, String className,
                    String description, String iconStyleClass, String iconStylesheet) {
            this.title = title;
            this.archiveClassName = archiveClassName;
            this.className = className;
            this.description = description;
            this.iconStyleClass = iconStyleClass;
            this.iconStylesheet = iconStylesheet;
        }

        /** The type with this archive class name, or null if it isn't one of these. */
        static ArchiveType forClassName(String className) {
            for (ArchiveType t : values()) if (t.className.equals(className)) return t;
            return null;
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

        /** The molecules-tab icon for this type, sized for a tile. */
        Region icon() {
            Region icon = new Region();
            icon.getStyleClass().addAll(iconStyleClass, "archive-type-icon");
            // Inline, since icons.css (loaded after the theme sheets) sizes these at 20.
            icon.setStyle("-fx-min-width: 40px; -fx-min-height: 40px;"
                    + " -fx-max-width: 40px; -fx-max-height: 40px;");
            if (iconStylesheet != null) {
                Class<?> c = archiveClass();
                URL sheet = c == null ? null : c.getClassLoader().getResource(iconStylesheet);
                if (sheet != null) icon.getStylesheets().add(sheet.toExternalForm());
                else icon.getStyleClass().setAll("moleculeIcon", "archive-type-icon");
            }
            return icon;
        }
    }

    /** One container on the dataset tree, with its datasets once listed. */
    private static final class ContainerNode {

        final Container container;
        final CheckBoxTreeItem<Object> treeItem;
        final List<DatasetRecord> datasets = new ArrayList<>();
        boolean listed;

        ContainerNode(Container container) {
            this.container = container;
            this.treeItem = new CheckBoxTreeItem<>(this, null, true);
            this.treeItem.setExpanded(true);
        }
    }

    /**
     * One dataset inside a container — the unit that becomes a metadata record,
     * holding the sources defined for it.
     */
    private static final class DatasetRecord {

        final ContainerNode container;
        final de.mpg.biochem.mars.n5.DatasetEntry entry;
        final CheckBoxTreeItem<Object> treeItem;
        final String uid = MarsMath.getUUID58().substring(0, 10);
        final ObservableList<MarsBdvSource> sources = FXCollections.observableArrayList();

        DatasetRecord(ContainerNode container, de.mpg.biochem.mars.n5.DatasetEntry entry) {
            this.container = container;
            this.entry = entry;
            this.treeItem = new CheckBoxTreeItem<>(this);
        }

        String url() { return container.container.url; }

        /** Dataset path inside the container ("/" when the root is the dataset). */
        String datasetName() { return entry.getName(); }

        /** Readable name: the dataset, or the container when the root is the dataset. */
        String displayName() {
            return MarsS3Browser.ROOT_DATASET.equals(entry.getName())
                    ? container.container.name : entry.getName();
        }

        boolean included() { return treeItem.isSelected(); }

        /**
         * Channels in the dataset. Mars writes XY[Z]CT, and the BDV loader slices
         * channels from the second-to-last axis, so that's the channel count for
         * 4+ dimensions; XYT (or smaller) has a single channel.
         */
        int channels() {
            long[] d = entry.getDimensions();
            return d.length >= 4 ? (int) d[d.length - 2] : 1;
        }

        /** Timepoints (OME SizeT): the last axis for 3+ dimensions. */
        long timepoints() {
            long[] d = entry.getDimensions();
            return d.length >= 3 ? d[d.length - 1] : 1;
        }

        /** E.g. "3 channels · 200 timepoints · 512×512 · uint16". */
        String summary() {
            long[] d = entry.getDimensions();
            String xy = d.length >= 2 ? d[0] + "×" + d[1] : entry.getDimensionString();
            int c = channels();
            long t = timepoints();
            return c + (c == 1 ? " channel" : " channels") + " · " + t
                    + (t == 1 ? " timepoint" : " timepoints") + " · " + xy + " · "
                    + entry.getDataType();
        }
    }

    private static final String FIELD_STYLE = ArchiveDestinationPane.FIELD_STYLE;

    // Divider positions of the sources step, remembered between sessions.
    private static final Preferences PREFS =
            Preferences.userNodeForPackage(NewArchiveWizard.class).node("newArchiveWizard");
    private static final String PREF_DIV_DATASETS = "dividerDatasets";
    private static final String PREF_DIV_SOURCES = "dividerSources";

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
    private final List<ContainerNode> containers = new ArrayList<>();
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
    private TreeView<Object> datasetTree;
    private Label datasetCountLabel;
    private int pendingListings = 0;

    // Step 2
    private TreeView<Object> recordTree;
    private ListView<MarsBdvSource> sourceList;
    private TextField sourceNameField;
    private Label recordSummary;
    private Button addChannelsButton;
    private MenuButton applyButton;
    private BdvSourceOptionsPane optionsPane;
    private SplitPane bodySplit;
    private SplitPane sourcesSplit;

    // Step 3
    private Label typeDescription;

    // Step 4
    private ArchiveDestinationPane destinationPane;
    private Label summaryLabel;
    private CheckBox openAfterCreate;
    private ProgressIndicator createProgress;
    private Label createStatus;

    private Consumer<ArchiveDestinationPane.Created> onCreated;

    // Lists the datasets inside each container, a few containers at a time.
    private final ExecutorService executor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "NewArchiveWizard");
        t.setDaemon(true);
        return t;
    });

    /**
     * @param owner the explorer window
     * @param containers N5/Zarr containers from the explorer's current filter,
     *          in card order
     * @param skippedArchives number of archives in the filter (not offered)
     * @param localSource true if the explorer is showing a local folder
     * @param endpoint S3 server (cloud source); ignored for local
     * @param bucketOrRoot current bucket (cloud) or indexed root folder (local)
     * @param darkMode current theme
     */
    public NewArchiveWizard(Window owner, List<Container> containers,
                            int skippedArchives, boolean localSource,
                            String endpoint, String bucketOrRoot, boolean darkMode)
    {
        this.localSource = localSource;
        this.endpoint = endpoint == null ? "" : endpoint;
        this.bucketOrRoot = bucketOrRoot == null ? "" : bucketOrRoot;
        this.skippedArchives = skippedArchives;
        for (Container c : containers) this.containers.add(new ContainerNode(c));

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
        content.setPrefSize(1240, 700);
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
            PREFS.putDouble(PREF_DIV_DATASETS, bodySplit.getDividerPositions()[0]);
            PREFS.putDouble(PREF_DIV_SOURCES, sourcesSplit.getDividerPositions()[0]);
            executor.shutdownNow();
            // Unbind first so a pending debounced field check finds no source
            // and skips, rather than submitting to the stopped executor.
            optionsPane.setMarsBdvSource(null);
            optionsPane.shutdown();
        });

        // Theme the pane, not the scene — the dialog has no scene until it shows.
        MarsThemeManager.applyTheme(getDialogPane(), darkMode);

        goTo(0);
        for (ContainerNode c : this.containers) listContainer(c);
    }

    /** Called on the FX thread once the archive has been saved. */
    public void setOnCreated(Consumer<ArchiveDestinationPane.Created> handler) {
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
        if (step == 3) onDestinationPageShown();
        updateButtons();
    }

    private boolean canAdvance() {
        switch (step) {
            case 0: return pendingListings == 0 && !includedRecords().isEmpty();
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

    /** Hand cursor on sidebar steps that can be jumped to. */
    private void updateStepCursors() {
        for (int i = 0; i < stepRows.size(); i++)
            stepRows.get(i).setCursor(!busy && i <= furthestStep && i != step
                    ? javafx.scene.Cursor.HAND : javafx.scene.Cursor.DEFAULT);
    }

    /** Checked datasets, in container then dataset order. */
    private List<DatasetRecord> includedRecords() {
        List<DatasetRecord> list = new ArrayList<>();
        for (ContainerNode c : containers)
            for (DatasetRecord r : c.datasets)
                if (r.included()) list.add(r);
        return list;
    }

    // -----------------------------------------------------------------
    // Step 1 — Datasets
    // -----------------------------------------------------------------

    private Region buildDatasetsPage() {
        CheckBoxTreeItem<Object> root = new CheckBoxTreeItem<>("root");
        for (ContainerNode c : containers) root.getChildren().add(c.treeItem);

        TreeView<Object> tree = new TreeView<>(root);
        datasetTree = tree;
        tree.setShowRoot(false);
        tree.setCellFactory(tv -> new DatasetTreeCell());
        VBox.setVgrow(tree, Priority.ALWAYS);
        if (containers.isEmpty()) {
            tree.setVisible(false);
            tree.setManaged(false);
        }
        Label empty = new Label(
                "No N5/Zarr containers match the explorer's current filter.");
        empty.setVisible(containers.isEmpty());
        empty.setManaged(containers.isEmpty());

        Button all = new Button("Select all");
        all.setOnAction(e -> containers.forEach(c -> c.treeItem.setSelected(true)));
        Button none = new Button("Select none");
        none.setOnAction(e -> containers.forEach(c -> c.treeItem.setSelected(false)));
        Button expand = new Button("Expand all");
        expand.setOnAction(e -> containers.forEach(c -> c.treeItem.setExpanded(true)));
        Button collapse = new Button("Collapse all");
        collapse.setOnAction(e -> containers.forEach(c -> c.treeItem.setExpanded(false)));
        datasetCountLabel = new Label();
        datasetCountLabel.getStyleClass().add("wizard-hint");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(6, datasetCountLabel, spacer, expand, collapse, all, none);
        footer.setAlignment(Pos.CENTER_LEFT);
        updateDatasetCount();

        return page("Datasets",
                "Every dataset inside a container (e.g. each stage position Pos0, "
                        + "Pos1, …) becomes one metadata record. Checking a container "
                        + "selects all of its datasets. The containers follow the "
                        + "Dataset Explorer's current search and filters.",
                tree, empty, footer);
    }

    /** List a container's datasets off the FX thread and add them to the tree. */
    private void listContainer(ContainerNode c) {
        pendingListings++;
        updateDatasetCount();
        Task<List<de.mpg.biochem.mars.n5.DatasetEntry>> task =
                new Task<List<de.mpg.biochem.mars.n5.DatasetEntry>>() {
            @Override
            protected List<de.mpg.biochem.mars.n5.DatasetEntry> call() {
                return MarsS3Browser.listDatasets(c.container.url);
            }
        };
        task.setOnSucceeded(e -> containerListed(c, task.getValue()));
        task.setOnFailed(e -> containerListed(c, new ArrayList<>()));
        executor.submit(task);
    }

    private void containerListed(ContainerNode c, List<de.mpg.biochem.mars.n5.DatasetEntry> entries) {
        pendingListings--;
        c.listed = true;
        // New children take the container's state so a checked container stays
        // fully checked (CheckBoxTreeItem recomputes the parent from children).
        boolean selected = c.treeItem.isSelected();
        for (de.mpg.biochem.mars.n5.DatasetEntry entry : entries) {
            DatasetRecord r = new DatasetRecord(c, entry);
            r.treeItem.setSelected(selected);
            r.treeItem.selectedProperty().addListener((o, was, now) -> {
                updateDatasetCount();
                updateButtons();
            });
            c.datasets.add(r);
            c.treeItem.getChildren().add(r.treeItem);
        }
        if (entries.isEmpty()) c.treeItem.setSelected(false);
        datasetTree.refresh(); // container row: "Reading …" → dataset count
        updateDatasetCount();
        updateButtons();
    }

    private void updateDatasetCount() {
        if (datasetCountLabel == null) return;
        List<DatasetRecord> included = includedRecords();
        int containerCount = 0;
        for (ContainerNode c : containers)
            for (DatasetRecord r : c.datasets)
                if (r.included()) { containerCount++; break; }
        String text = included.size() + " dataset(s) selected in " + containerCount
                + " container(s)";
        if (pendingListings > 0) text = "Reading " + pendingListings + " container(s) … · " + text;
        if (skippedArchives > 0)
            text += " · " + skippedArchives + " archive(s) in the filter skipped";
        datasetCountLabel.setText(text);
    }

    /** Container or dataset row with a checkbox tied to its CheckBoxTreeItem. */
    private static final class DatasetTreeCell extends TreeCell<Object> {

        private final CheckBox check = new CheckBox();
        private final Label name = new Label();
        private final Label info = new Label();
        private final Label size = new Label();
        private final StackPane iconHolder = new StackPane();
        private final HBox root;
        private CheckBoxTreeItem<Object> bound;

        DatasetTreeCell() {
            info.getStyleClass().add("wizard-hint");
            size.getStyleClass().add("wizard-hint");
            VBox text = new VBox(1, name, info);
            HBox.setHgrow(text, Priority.ALWAYS);
            iconHolder.setMinWidth(22);
            root = new HBox(8, check, iconHolder, text, size);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(2, 4, 2, 0));
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (bound != null) {
                check.selectedProperty().unbindBidirectional(bound.selectedProperty());
                check.indeterminateProperty().unbindBidirectional(bound.indeterminateProperty());
                bound = null;
            }
            if (empty || item == null || !(getTreeItem() instanceof CheckBoxTreeItem)) {
                setGraphic(null);
                setText(null);
                return;
            }
            bound = (CheckBoxTreeItem<Object>) getTreeItem();
            check.selectedProperty().bindBidirectional(bound.selectedProperty());
            check.indeterminateProperty().bindBidirectional(bound.indeterminateProperty());

            if (item instanceof ContainerNode) {
                ContainerNode c = (ContainerNode) item;
                name.setText(c.container.name);
                name.setStyle("-fx-font-weight: bold;");
                String state = !c.listed ? "Reading datasets …"
                        : c.datasets.isEmpty() ? "No datasets found"
                        : c.datasets.size() + " dataset(s)";
                info.setText(c.container.path + " · " + state);
                size.setText(c.container.sizeBytes >= 0 ? humanBytes(c.container.sizeBytes) : "");
                iconHolder.getChildren().setAll(ActionUtils.icon(FontAwesome.DATABASE, "1.3em"));
                check.setDisable(c.listed && c.datasets.isEmpty());
            }
            else {
                DatasetRecord r = (DatasetRecord) item;
                name.setText(r.displayName());
                name.setStyle("");
                info.setText(r.summary());
                size.setText(humanBytes(r.entry.getNumBytes()));
                iconHolder.getChildren().setAll(ActionUtils.icon(FontAwesome.IMAGE, "1.0em"));
                check.setDisable(false);
            }
            setText(null);
            setGraphic(root);
        }
    }

    // -----------------------------------------------------------------
    // Step 2 — BDV sources
    // -----------------------------------------------------------------

    private Region buildSourcesPage() {
        // --- Datasets (records), grouped under their containers ---
        recordTree = new TreeView<>();
        recordTree.setShowRoot(false);
        recordTree.setCellFactory(tv -> new TreeCell<Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                setTooltip(null);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }
                if (item instanceof ContainerNode) {
                    setText(((ContainerNode) item).container.name);
                    setTooltip(new Tooltip(((ContainerNode) item).container.path));
                    setGraphic(ActionUtils.icon(FontAwesome.DATABASE, "1.0em"));
                    setStyle("-fx-font-weight: bold;");
                }
                else {
                    DatasetRecord r = (DatasetRecord) item;
                    int n = r.sources.size();
                    setText(r.displayName() + "   " + r.channels() + " ch · " + n
                            + (n == 1 ? " source" : " sources"));
                    setGraphic(ActionUtils.icon(FontAwesome.IMAGE, "1.0em"));
                    setStyle("");
                }
            }
        });
        recordTree.getSelectionModel().selectedItemProperty().addListener((o, was, now) -> {
            Object v = now == null ? null : now.getValue();
            if (v instanceof ContainerNode && !now.getChildren().isEmpty()) {
                // A container header isn't a record — step into its first dataset.
                TreeItem<Object> first = now.getChildren().get(0);
                javafx.application.Platform.runLater(() ->
                        recordTree.getSelectionModel().select(first));
                return;
            }
            showRecord(v instanceof DatasetRecord ? (DatasetRecord) v : null);
        });

        // --- Selected dataset's summary, with the channel count up front ---
        recordSummary = new Label();
        recordSummary.getStyleClass().add("wizard-record-summary");
        recordSummary.setWrapText(true);
        recordSummary.setMaxWidth(Double.MAX_VALUE);

        // --- Sources of the selected dataset ---
        sourceNameField = new TextField();
        sourceNameField.setPromptText("Source name");
        sourceNameField.setStyle(FIELD_STYLE);
        HBox.setHgrow(sourceNameField, Priority.ALWAYS);
        sourceNameField.setOnAction(e -> {
            e.consume();
            addSourceFromField();
        });

        Button add = new Button();
        add.setGraphic(ActionUtils.icon(FontAwesome.PLUS, "1.0em"));
        add.setTooltip(new Tooltip("Add a source for the next unused channel"));
        add.setOnAction(e -> addSourceFromField());
        HBox addRow = new HBox(4, sourceNameField, add);
        addRow.setAlignment(Pos.CENTER_LEFT);

        addChannelsButton = new Button("Add a source per channel");
        addChannelsButton.setGraphic(ActionUtils.icon(FontAwesome.TH_LIST, "1.0em"));
        addChannelsButton.setMaxWidth(Double.MAX_VALUE);
        addChannelsButton.setOnAction(e -> addSourcePerChannel());

        sourceList = new ListView<>();
        sourceList.setEditable(true);
        // Double-click renames; the commit renames the source in place.
        sourceList.setCellFactory(lv -> {
            TextFieldListCell<MarsBdvSource> cell = new TextFieldListCell<MarsBdvSource>(
                    new StringConverter<MarsBdvSource>() {
                @Override
                public String toString(MarsBdvSource s) {
                    return s == null ? "" : s.getName();
                }
                @Override
                public MarsBdvSource fromString(String name) {
                    return new MarsBdvSource(name);
                }
            }) {
                @Override
                public void updateItem(MarsBdvSource item, boolean empty) {
                    super.updateItem(item, empty);
                    if (!empty && item != null && !isEditing()) {
                        setText(item.getName() + "   (C" + item.getChannel() + ")");
                        setGraphic(ActionUtils.icon(FontAwesome.IMAGE, "1.0em"));
                    }
                }
            };
            return cell;
        });
        sourceList.setOnEditCommit(e -> {
            MarsBdvSource s = sourceList.getItems().get(e.getIndex());
            String name = e.getNewValue().getName().trim();
            boolean taken = false;
            for (MarsBdvSource other : sourceList.getItems())
                if (other != s && other.getName().equals(name)) taken = true;
            if (!name.isEmpty() && !taken) s.setName(name);
            sourceList.refresh();
        });
        sourceList.setPlaceholder(new Label("No sources yet"));
        sourceList.setTooltip(new Tooltip("Double-click a source to rename it"));
        VBox.setVgrow(sourceList, Priority.ALWAYS);
        sourceList.getSelectionModel().selectedItemProperty().addListener(
                (o, was, now) -> optionsPane.setMarsBdvSource(now));

        Button remove = new Button("Remove");
        remove.setGraphic(ActionUtils.icon(FontAwesome.MINUS, "1.0em"));
        remove.disableProperty().bind(sourceList.getSelectionModel()
                .selectedItemProperty().isNull());
        remove.setOnAction(e -> {
            DatasetRecord r = selectedRecord();
            MarsBdvSource s = sourceList.getSelectionModel().getSelectedItem();
            if (r != null && s != null) {
                r.sources.remove(s);
                recordTree.refresh();
            }
        });

        MenuItem toContainer = new MenuItem("Datasets in this container");
        toContainer.setOnAction(e -> applySources(false));
        MenuItem toAll = new MenuItem("All datasets");
        toAll.setOnAction(e -> applySources(true));
        applyButton = new MenuButton("Apply to", ActionUtils.icon(FontAwesome.CLONE, "1.0em"),
                toContainer, toAll);
        applyButton.setTooltip(new Tooltip(
                "Copy these sources and their settings to other datasets, pointing "
                        + "each copy at that dataset."));

        HBox sourceButtons = new HBox(6, remove, applyButton);

        // Datasets get their own full-height column so many positions fit.
        VBox left = new VBox(6, sectionLabel("Datasets"), recordTree);
        VBox.setVgrow(recordTree, Priority.ALWAYS);
        left.setMinWidth(160);

        // Sources of the selected dataset, then the selected source's settings
        // (the metadata tab's own pane).
        optionsPane = new BdvSourceOptionsPane();
        optionsPane.setMarsBdvSource(null);
        HBox.setHgrow(addRow, Priority.ALWAYS);
        addChannelsButton.setMaxWidth(Region.USE_PREF_SIZE);
        HBox addLine = new HBox(6, addRow, addChannelsButton);
        addLine.setAlignment(Pos.CENTER_LEFT);
        sourceList.setMinHeight(60);
        VBox sourcesBox = new VBox(6,
                sectionLabel("Sources"), recordSummary, addLine, sourceList, sourceButtons);
        sourcesBox.setPadding(new Insets(0, 0, 8, 0));
        sourcesBox.setMinHeight(150);

        // The settings scroll when the divider is dragged up past their height.
        ScrollPane optionsScroll = new ScrollPane(optionsPane);
        optionsScroll.setFitToWidth(true);
        optionsScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;"
                + " -fx-padding: 0; -fx-background-insets: 0;");
        VBox settingsBox = new VBox(6, sectionLabel("Source settings"), optionsScroll);
        VBox.setVgrow(optionsScroll, Priority.ALWAYS);
        settingsBox.setPadding(new Insets(8, 0, 0, 0));
        settingsBox.setMinHeight(80);

        // Sources over settings, divider draggable. Settings keep their height as
        // the dialog grows, so extra room goes to the source list.
        sourcesSplit = new SplitPane(sourcesBox, settingsBox);
        sourcesSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        SplitPane.setResizableWithParent(settingsBox, false);
        sourcesSplit.setDividerPositions(PREFS.getDouble(PREF_DIV_SOURCES, 0.36));

        // Datasets beside sources, divider draggable; the dataset column keeps
        // its width as the dialog grows.
        bodySplit = new SplitPane(left, sourcesSplit);
        SplitPane.setResizableWithParent(left, false);
        bodySplit.setDividerPositions(PREFS.getDouble(PREF_DIV_DATASETS, 0.27));
        VBox.setVgrow(bodySplit, Priority.ALWAYS);

        return page("BDV Sources",
                "Select a dataset and add its BigDataViewer sources, usually one per "
                        + "channel. Sources point at the dataset with the next unused "
                        + "channel; use Apply to copy them to the other datasets.",
                bodySplit);
    }

    private DatasetRecord selectedRecord() {
        TreeItem<Object> item = recordTree.getSelectionModel().getSelectedItem();
        return item != null && item.getValue() instanceof DatasetRecord
                ? (DatasetRecord) item.getValue() : null;
    }

    /** Rebuild the dataset tree from the checked datasets, keeping the selection. */
    private void onSourcesPageShown() {
        DatasetRecord selected = selectedRecord();
        TreeItem<Object> root = new TreeItem<>("root");
        TreeItem<Object> toSelect = null;
        for (ContainerNode c : containers) {
            TreeItem<Object> ci = null;
            for (DatasetRecord r : c.datasets) {
                if (!r.included()) continue;
                if (ci == null) {
                    ci = new TreeItem<>(c);
                    ci.setExpanded(true);
                    root.getChildren().add(ci);
                }
                TreeItem<Object> ri = new TreeItem<>(r);
                ci.getChildren().add(ri);
                if (r == selected || (toSelect == null && selected == null)) toSelect = ri;
            }
        }
        recordTree.setRoot(root);
        if (toSelect == null && !root.getChildren().isEmpty())
            toSelect = root.getChildren().get(0).getChildren().get(0);
        if (toSelect != null) recordTree.getSelectionModel().select(toSelect);
        else showRecord(null);
    }

    private void showRecord(DatasetRecord r) {
        boolean has = r != null;
        sourceNameField.setDisable(!has);
        addChannelsButton.setDisable(!has);
        applyButton.setDisable(!has || includedRecords().size() < 2);
        if (!has) {
            sourceList.setItems(FXCollections.observableArrayList());
            recordSummary.setText("Select a dataset.");
            optionsPane.setMarsBdvSource(null);
            return;
        }
        recordSummary.setText(r.displayName() + " — " + r.summary());
        sourceList.setItems(r.sources);
        if (!r.sources.isEmpty()) sourceList.getSelectionModel().select(0);
        else optionsPane.setMarsBdvSource(null);
    }

    private void addSourceFromField() {
        DatasetRecord r = selectedRecord();
        if (r == null) return;
        String name = sourceNameField.getText() == null ? "" : sourceNameField.getText().trim();
        if (name.isEmpty()) name = uniqueName(r, "Channel " + nextChannel(r));
        for (MarsBdvSource existing : r.sources) {
            if (existing.getName().equals(name)) {
                sourceList.getSelectionModel().select(existing);
                return; // names are unique within a record
            }
        }
        MarsBdvSource s = newSource(r, name, nextChannel(r));
        r.sources.add(s);
        sourceNameField.clear();
        sourceList.getSelectionModel().select(s);
        recordTree.refresh();
    }

    /** One source per channel not yet covered, named "Channel n". */
    private void addSourcePerChannel() {
        DatasetRecord r = selectedRecord();
        if (r == null) return;
        MarsBdvSource last = null;
        for (int c = 0; c < r.channels(); c++) {
            if (channelUsed(r, c)) continue;
            last = newSource(r, uniqueName(r, "Channel " + c), c);
            r.sources.add(last);
        }
        if (last != null) sourceList.getSelectionModel().select(last);
        recordTree.refresh();
    }

    /** A source pointing at the dataset, at the given channel, identity transform. */
    private static MarsBdvSource newSource(DatasetRecord r, String name, int channel) {
        MarsBdvSource s = new MarsBdvSource(name);
        s.setN5(true);
        s.setPath(r.url());
        s.setN5Dataset(r.datasetName());
        s.setChannel(channel);
        return s;
    }

    private static boolean channelUsed(DatasetRecord r, int channel) {
        for (MarsBdvSource s : r.sources) if (s.getChannel() == channel) return true;
        return false;
    }

    /** Lowest channel with no source yet (0 once every channel is covered). */
    private static int nextChannel(DatasetRecord r) {
        for (int c = 0; c < r.channels(); c++) if (!channelUsed(r, c)) return c;
        return 0;
    }

    private static String uniqueName(DatasetRecord r, String base) {
        String name = base;
        int i = 2;
        while (true) {
            boolean taken = false;
            for (MarsBdvSource s : r.sources) if (s.getName().equals(name)) taken = true;
            if (!taken) return name;
            name = base + " (" + i++ + ")";
        }
    }

    /**
     * Copy the selected dataset's sources to the other checked datasets — in the
     * same container, or everywhere — each copy pointing at its own dataset.
     */
    private void applySources(boolean allContainers) {
        DatasetRecord from = selectedRecord();
        if (from == null) return;
        List<DatasetRecord> targets = new ArrayList<>();
        for (DatasetRecord r : includedRecords())
            if (r != from && (allContainers || r.container == from.container)) targets.add(r);
        if (targets.isEmpty()) return;

        int withSources = 0;
        int tooFewChannels = 0;
        int maxChannel = -1;
        for (MarsBdvSource s : from.sources) maxChannel = Math.max(maxChannel, s.getChannel());
        for (DatasetRecord r : targets) {
            if (!r.sources.isEmpty()) withSources++;
            if (maxChannel >= r.channels()) tooFewChannels++;
        }
        StringBuilder msg = new StringBuilder("Copy the " + from.sources.size()
                + " source(s) of " + from.displayName() + " to " + targets.size()
                + " other dataset(s)?");
        if (withSources > 0) msg.append("\n\n").append(withSources)
                .append(" of them already have sources, which will be replaced.");
        if (tooFewChannels > 0) msg.append("\n\n").append(tooFewChannels)
                .append(" of them have fewer channels than these sources use.");
        RoverConfirmationDialog a = new RoverConfirmationDialog(
                getDialogPane().getScene().getWindow(), msg.toString(),
                ButtonType.YES, ButtonType.NO);
        a.setTitle("Apply sources");
        MarsThemeManager.applyTheme(a.getDialogPane(), MarsThemeManager.isDarkTheme());
        if (a.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        for (DatasetRecord r : targets) {
            r.sources.clear();
            for (MarsBdvSource s : from.sources) r.sources.add(copyFor(s, r));
        }
        recordTree.refresh();
    }

    /** Copy a source's settings, pointing the copy at another dataset. */
    private static MarsBdvSource copyFor(MarsBdvSource s, DatasetRecord target) {
        MarsBdvSource c = new MarsBdvSource(s.getName());
        c.setN5(s.isN5());
        c.setPath(target.url());
        c.setN5Dataset(target.datasetName());
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
        grid.setHgap(14);
        grid.setVgap(14);

        ArchiveType[] types = ArchiveType.values();
        for (int i = 0; i < types.length; i++) {
            ArchiveType type = types[i];
            boolean available = type.archiveClass() != null;

            Region icon = type.icon();
            StackPane iconHolder = new StackPane(icon);
            iconHolder.setMinHeight(48);
            Label title = new Label(type.title);
            title.getStyleClass().add("archive-type-title");
            Label cls = new Label(type.archiveClassName);
            cls.getStyleClass().add("wizard-hint");
            VBox inner = new VBox(6, iconHolder, title, cls);
            inner.setAlignment(Pos.CENTER);

            ToggleButton tile = new ToggleButton();
            tile.setGraphic(inner);
            tile.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            tile.getStyleClass().add("archive-type-tile");
            tile.setUserData(type);
            tile.setToggleGroup(group);
            tile.setDisable(!available);
            tile.setPrefSize(190, 130);
            tile.setMinSize(190, 130);
            tile.setMaxSize(190, 130);
            if (!available) tile.setTooltip(new Tooltip(type.archiveClassName
                    + " is not installed — enable the update site that provides it."));
            // Double-click picks the type and moves on.
            tile.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && tile.isSelected()) goTo(3);
            });
            grid.add(tile, i % 2, i / 2);
        }

        typeDescription = new Label("Select an archive type.");
        typeDescription.getStyleClass().add("wizard-page-subtitle");
        typeDescription.setWrapText(true);

        group.selectedToggleProperty().addListener((o, was, now) -> {
            // Keep one tile selected — clicking the selected tile doesn't clear it.
            if (now == null && was != null) {
                was.setSelected(true);
                return;
            }
            archiveType = now == null ? null : (ArchiveType) now.getUserData();
            typeDescription.setText(archiveType == null ? "Select an archive type."
                    : archiveType.title + ": " + archiveType.description);
            updateButtons();
        });

        return page("Archive Type",
                "Choose the kind of Molecule Archive to create.", grid, typeDescription);
    }

    // -----------------------------------------------------------------
    // Step 4 — Destination
    // -----------------------------------------------------------------

    private Region buildDestinationPage() {
        destinationPane = new ArchiveDestinationPane(localSource, endpoint, bucketOrRoot);
        destinationPane.setName(defaultName());
        destinationPane.setOnChange(this::refreshDestinationSummary);

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
                destinationPane,
                new Separator(),
                sectionLabel("Summary"), summaryLabel,
                openAfterCreate,
                statusRow);
        form.setFillWidth(true);

        return page("Destination",
                "Choose where the new archive is saved. It defaults to the bucket "
                        + "or folder open in the explorer, inside the containers' "
                        + "shared subfolder when they all have one.",
                form);
    }

    /** Re-suggest the folder for the current selection unless it was edited. */
    private void onDestinationPageShown() {
        List<String> paths = new ArrayList<>();
        for (ContainerNode c : usedContainers()) paths.add(c.container.path);
        destinationPane.suggestFolder(paths);
        refreshDestinationSummary();
    }

    /** Containers with at least one checked dataset (all of them before listing). */
    private List<ContainerNode> usedContainers() {
        List<ContainerNode> used = new ArrayList<>();
        for (ContainerNode c : containers) {
            boolean any = !c.listed;
            for (DatasetRecord r : c.datasets) if (r.included()) any = true;
            if (any) used.add(c);
        }
        return used;
    }

    private String defaultName() {
        if (containers.size() == 1) {
            String n = containers.get(0).container.name.replaceAll("/+$", "");
            return n.replaceAll("(?i)\\.(n5|zarr)$", "");
        }
        return "archive";
    }

    private boolean canCreate() {
        return !busy && archiveType != null && destinationPane.destination() != null
                && !includedRecords().isEmpty();
    }

    private void refreshDestinationSummary() {
        if (summaryLabel == null) return;
        List<DatasetRecord> records = includedRecords();
        int sourceCount = 0;
        for (DatasetRecord r : records) sourceCount += r.sources.size();
        int withoutSources = 0;
        for (DatasetRecord r : records) if (r.sources.isEmpty()) withoutSources++;
        String text = "Creates a "
                + (archiveType == null ? "Molecule Archive" : archiveType.archiveClassName)
                + " with " + records.size() + " metadata record(s), one per dataset, and "
                + sourceCount + " BDV source(s).";
        if (withoutSources > 0) text += " " + withoutSources + " record(s) have no sources.";
        summaryLabel.setText(text);
        updateButtons();
    }

    // -----------------------------------------------------------------
    // Create
    // -----------------------------------------------------------------

    private void create() {
        if (!canCreate()) return;
        final String dest = destinationPane.destination();
        final String name = destinationPane.fileName();
        final ArchiveType type = archiveType;
        final boolean store = destinationPane.isStoreEnding();
        final boolean open = openAfterCreate.isSelected();
        final List<DatasetRecord> records = includedRecords();

        setBusy(true, "Creating " + name + " …");

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                if (ArchiveDestinationPane.exists(dest)) throw new IOException(dest
                        + " already exists. Choose another name or folder.");

                MoleculeArchive<?, MarsOMEMetadata, ?, ?> archive = type.create(name);
                for (DatasetRecord r : records) {
                    MarsOMEMetadata metadata = new MarsOMEMetadata(r.uid);
                    metadata.setSourceDirectory(r.url());
                    // Sizes and planes from the dataset, so tracking can use the record.
                    metadata.setImage(MarsN5ImageMetadata.createImage(
                            r.entry.getDimensions(), r.displayName()), 0);
                    for (MarsBdvSource s : r.sources) metadata.putBdvSource(s);
                    metadata.logln("Created in the Dataset Explorer from dataset "
                            + r.datasetName() + " of " + r.url());
                    archive.putMetadata(metadata);
                }

                LogBuilder builder = new LogBuilder();
                String log = LogBuilder.buildTitleBlock("New Archive (Dataset Explorer)");
                builder.addParameter("Archive type", type.archiveClassName);
                builder.addParameter("Metadata records", records.size());
                builder.addParameter("Destination", dest);
                log += builder.buildParameterList();
                log += "\n" + LogBuilder.endBlock(true);
                archive.logln(log);

                if (ArchiveDestinationPane.isCloud(dest)) {
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
            if (onCreated != null) onCreated.accept(new ArchiveDestinationPane.Created(
                    dest, name, destinationPane.relativeToSource(dest), open));
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

    private void setBusy(boolean on, String message) {
        busy = on;
        createProgress.setVisible(on);
        createStatus.setText(message);
        destinationPane.setEditable(!on);
        updateButtons();
        updateStepCursors();
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
