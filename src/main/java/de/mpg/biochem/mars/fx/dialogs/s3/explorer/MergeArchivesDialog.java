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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.kordamp.ikonli.fontawesome.FontAwesome;

import de.mpg.biochem.mars.fx.dialogs.RoverErrorDialog;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.fx.util.MarsThemeManager;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

/**
 * Dialog, opened from the Dataset Explorer's merge button, that merges the
 * Molecule Archives (.yama / .yama.store) shown by the explorer's current
 * filter into a new archive. Archives are listed with their type (read in the
 * background) and can be unchecked; below the list is the same destination
 * section as the new-archive wizard. The merge itself is done by
 * {@link ArchiveMerger}.
 */
public class MergeArchivesDialog extends Dialog<Void> {

    /** One archive offered by the explorer. */
    public static final class Archive {

        final String name;      // e.g. "lane1.yama"
        final String path;      // path relative to the bucket or local root
        final String url;       // openable URL (S3) or absolute local path
        final long sizeBytes;   // -1 if unknown

        public Archive(String name, String path, String url, long sizeBytes) {
            this.name = name;
            this.path = path;
            this.url = url;
            this.sizeBytes = sizeBytes;
        }

        boolean isStore() {
            return name.endsWith(ArchiveDestinationPane.ENDING_STORE);
        }
    }

    private static final class Row {

        final Archive archive;
        final BooleanProperty include = new SimpleBooleanProperty(true);
        String type;   // archive class name; null until read
        String error;  // set if the type couldn't be read

        Row(Archive archive) { this.archive = archive; }
    }

    private static final String WARNING_STYLE = "-fx-text-fill: #e5534b;";

    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private final ListView<Row> list = new ListView<>(rows);
    private final Label countLabel = new Label();
    private final Label typeLabel = new Label();
    private final ArchiveDestinationPane destinationPane;
    private final Label summaryLabel = new Label();
    private final CheckBox openAfterMerge = new CheckBox("Open the archive when it's merged");
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label statusLabel = new Label();
    private final ButtonType mergeType = new ButtonType("Merge", ButtonData.OK_DONE);
    private final Button mergeButton;
    private final Button cancelButton;
    private boolean busy = false;
    private int pendingTypes = 0;
    private Consumer<ArchiveDestinationPane.Created> onMerged;

    // Reads archive types in the background, a few archives at a time.
    private final ExecutorService executor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "MergeArchivesDialog");
        t.setDaemon(true);
        return t;
    });

    /**
     * @param owner the explorer window
     * @param archives archives from the explorer's current filter, in card order
     * @param skippedContainers number of N5/Zarr containers in the filter (not offered)
     * @param localSource true if the explorer is showing a local folder
     * @param endpoint S3 server (cloud source); ignored for local
     * @param bucketOrRoot current bucket (cloud) or indexed root folder (local)
     * @param darkMode current theme
     */
    public MergeArchivesDialog(Window owner, List<Archive> archives, int skippedContainers,
                               boolean localSource, String endpoint, String bucketOrRoot,
                               boolean darkMode)
    {
        for (Archive a : archives) rows.add(new Row(a));

        setTitle("Merge Archives");
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        setResizable(true);

        // --- Archive list ---
        list.setCellFactory(lv -> new ArchiveCell());
        list.setPlaceholder(new Label("No archives match the explorer's current filter."));
        list.setMinHeight(120);
        VBox.setVgrow(list, Priority.ALWAYS);
        for (Row r : rows) r.include.addListener((o, was, now) -> selectionChanged());

        Button all = new Button("Select all");
        all.setOnAction(e -> rows.forEach(r -> r.include.set(true)));
        Button none = new Button("Select none");
        none.setOnAction(e -> rows.forEach(r -> r.include.set(false)));
        countLabel.getStyleClass().add("wizard-hint");
        typeLabel.setWrapText(true);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox listFooter = new HBox(6, countLabel, spacer, all, none);
        listFooter.setAlignment(Pos.CENTER_LEFT);
        if (skippedContainers > 0) countLabel.setUserData(skippedContainers);

        // --- Destination ---
        destinationPane = new ArchiveDestinationPane(localSource, endpoint, bucketOrRoot);
        destinationPane.setName("merged");
        boolean allStores = !archives.isEmpty();
        for (Archive a : archives) if (!a.isStore()) allStores = false;
        destinationPane.setStoreEnding(allStores);
        destinationPane.setOnChange(this::updateState);

        // --- Summary + progress ---
        summaryLabel.setWrapText(true);
        openAfterMerge.setSelected(true);
        progressBar.setPrefWidth(260);
        progressBar.setVisible(false);
        statusLabel.setWrapText(true);
        HBox statusRow = new HBox(10, progressBar, statusLabel);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Merge Archives");
        title.getStyleClass().add("wizard-page-title");
        Label subtitle = new Label("Merge the checked archives into one new archive. The "
                + "list follows the Dataset Explorer's current search and filters; all "
                + "archives must be the same type.");
        subtitle.getStyleClass().add("wizard-page-subtitle");
        subtitle.setWrapText(true);
        subtitle.setMinHeight(Region.USE_PREF_SIZE);

        VBox content = new VBox(8, title, subtitle, list, listFooter, typeLabel,
                new Separator(), destinationPane, new Separator(),
                ArchiveDestinationPane.sectionLabel("Summary"), summaryLabel,
                openAfterMerge, statusRow);
        content.setPadding(new Insets(20, 24, 12, 24));
        content.setPrefSize(860, 700);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(mergeType, ButtonType.CANCEL);
        Node bar = getDialogPane().lookup(".button-bar");
        if (bar instanceof ButtonBar) ((ButtonBar) bar).setButtonOrder("+C_O");
        mergeButton = (Button) getDialogPane().lookupButton(mergeType);
        cancelButton = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        mergeButton.setDefaultButton(false);
        mergeButton.addEventFilter(ActionEvent.ACTION, e -> {
            e.consume(); // the dialog closes itself once the merge succeeds
            merge();
        });

        // No closing mid-merge — the archive is being written.
        setOnCloseRequest(e -> {
            if (busy) e.consume();
        });
        setResultConverter(bt -> null);
        setOnHidden(e -> executor.shutdownNow());

        MarsThemeManager.applyTheme(getDialogPane(), darkMode);

        for (Row r : rows) readType(r);
        selectionChanged();
    }

    /** Called on the FX thread once the merged archive has been saved. */
    public void setOnMerged(Consumer<ArchiveDestinationPane.Created> handler) {
        this.onMerged = handler;
    }

    private List<Row> selectedRows() {
        List<Row> selected = new ArrayList<>();
        for (Row r : rows) if (r.include.get()) selected.add(r);
        return selected;
    }

    private void readType(Row r) {
        pendingTypes++;
        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return ArchiveMerger.archiveType(r.archive.url);
            }
        };
        task.setOnSucceeded(e -> typeRead(r, task.getValue(), null));
        task.setOnFailed(e -> typeRead(r, null, task.getException() == null ? "error"
                : task.getException().getMessage()));
        executor.submit(task);
    }

    private void typeRead(Row r, String type, String error) {
        pendingTypes--;
        r.type = type;
        r.error = error;
        list.refresh();
        updateState();
    }

    private void selectionChanged() {
        List<String> paths = new ArrayList<>();
        for (Row r : selectedRows()) paths.add(r.archive.path);
        destinationPane.suggestFolder(paths);
        updateState();
    }

    /**
     * The most common type among the selected archives — the one the others are
     * flagged against — or null if none has been read yet.
     */
    private String majorityType() {
        Map<String, Integer> counts = typeCounts();
        String best = null;
        for (Map.Entry<String, Integer> e : counts.entrySet())
            if (best == null || e.getValue() > counts.get(best)) best = e.getKey();
        return best;
    }

    private Map<String, Integer> typeCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Row r : selectedRows())
            if (r.type != null) counts.merge(r.type, 1, Integer::sum);
        return counts;
    }

    private void updateState() {
        List<Row> selected = selectedRows();
        String count = selected.size() + " of " + rows.size() + " archive(s) selected";
        Object skipped = countLabel.getUserData();
        if (skipped != null) count += " · " + skipped + " N5/Zarr container(s) in the filter skipped";
        countLabel.setText(count);

        Map<String, Integer> counts = typeCounts();
        int unreadable = 0;
        for (Row r : selected) if (r.error != null) unreadable++;
        if (pendingTypes > 0) {
            typeLabel.setText("Reading archive types …");
            typeLabel.setStyle("");
        }
        else if (unreadable > 0) {
            typeLabel.setText(unreadable + " selected archive(s) could not be read.");
            typeLabel.setStyle(WARNING_STYLE);
        }
        else if (counts.size() > 1) {
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, Integer> e : counts.entrySet())
                parts.add(e.getValue() + " " + ArchiveMerger.simpleName(e.getKey()));
            typeLabel.setText("The selected archives are different types ("
                    + String.join(", ", parts) + "). Only archives of the same type can "
                    + "be merged.");
            typeLabel.setStyle(WARNING_STYLE);
        }
        else {
            typeLabel.setText(counts.isEmpty() ? ""
                    : "All selected archives are " + ArchiveMerger.simpleName(
                            counts.keySet().iterator().next()) + ".");
            typeLabel.setStyle("");
        }

        String dest = destinationPane.destination();
        summaryLabel.setText(selected.size() < 2 ? "Select at least two archives to merge."
                : "Merges " + selected.size() + " archives into "
                        + (dest == null ? "the destination above" : destinationPane.fileName())
                        + ". The selected archives are not changed.");

        mergeButton.setDisable(busy || pendingTypes > 0 || selected.size() < 2
                || dest == null);
        cancelButton.setDisable(busy);
    }

    private void merge() {
        List<Row> selected = selectedRows();
        // Same-type and readability checks up front, with the details.
        List<String> inputs = new ArrayList<>();
        List<String> types = new ArrayList<>();
        for (Row r : selected) {
            if (r.error != null) {
                showError("Could not read " + r.archive.name + ":\n" + r.error);
                return;
            }
            inputs.add(r.archive.url);
            types.add(r.type);
        }
        try {
            ArchiveMerger.checkSameType(inputs, types);
        }
        catch (ArchiveMerger.MergeException e) {
            showError(e.getMessage());
            return;
        }

        final String dest = destinationPane.destination();
        final String name = destinationPane.fileName();
        final boolean store = destinationPane.isStoreEnding();
        final boolean open = openAfterMerge.isSelected();
        setBusy(true);

        Task<ArchiveMerger.Result> task = new Task<ArchiveMerger.Result>() {
            @Override
            protected ArchiveMerger.Result call() throws Exception {
                return ArchiveMerger.merge(inputs, dest, store, (message, fraction) ->
                        Platform.runLater(() -> {
                            statusLabel.setText(message);
                            progressBar.setProgress(fraction < 0
                                    ? ProgressBar.INDETERMINATE_PROGRESS : fraction);
                        }));
            }
        };
        task.setOnSucceeded(e -> {
            setBusy(false);
            if (onMerged != null) onMerged.accept(new ArchiveDestinationPane.Created(
                    dest, name, destinationPane.relativeToSource(dest), open));
            close();
        });
        task.setOnFailed(e -> {
            setBusy(false);
            Throwable ex = task.getException();
            String message = ex instanceof ArchiveMerger.MergeException ? ex.getMessage()
                    : "The merge failed: " + (ex == null ? "error" : ex);
            statusLabel.setText("Merge failed.");
            showError(message);
        });

        Thread t = new Thread(task, "MergeArchivesDialog-merge");
        t.setDaemon(true);
        t.start();
    }

    private void setBusy(boolean on) {
        busy = on;
        progressBar.setVisible(on);
        if (on) {
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            statusLabel.setText("Starting …");
        }
        list.setDisable(on);
        destinationPane.setEditable(!on);
        updateState();
    }

    private void showError(String message) {
        RoverErrorDialog alert = new RoverErrorDialog(getDialogPane().getScene().getWindow(),
                message);
        MarsThemeManager.applyTheme(alert.getDialogPane(), MarsThemeManager.isDarkTheme());
        alert.showAndWait();
    }

    /** Archive row: checkbox, type icon, name, path · type, size. */
    private final class ArchiveCell extends ListCell<Row> {

        private final CheckBox check = new CheckBox();
        private final StackPane iconHolder = new StackPane();
        private final Label name = new Label();
        private final Label info = new Label();
        private final Label size = new Label();
        private final HBox root;
        private Row bound;

        ArchiveCell() {
            name.setStyle("-fx-font-weight: bold;");
            info.getStyleClass().add("wizard-hint");
            size.getStyleClass().add("wizard-hint");
            iconHolder.setMinSize(24, 24);
            VBox text = new VBox(2, name, info);
            HBox.setHgrow(text, Priority.ALWAYS);
            root = new HBox(10, check, iconHolder, text, size);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(4));
        }

        @Override
        protected void updateItem(Row item, boolean empty) {
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
            name.setText(item.archive.name);
            size.setText(item.archive.sizeBytes >= 0 ? humanBytes(item.archive.sizeBytes) : "");

            String majority = majorityType();
            String typeText;
            boolean warn = false;
            if (item.error != null) {
                typeText = "Could not read: " + item.error;
                warn = true;
            }
            else if (item.type == null) typeText = "Reading type …";
            else {
                typeText = ArchiveMerger.simpleName(item.type);
                warn = item.include.get() && majority != null && !majority.equals(item.type);
                if (warn) typeText += " — different type";
            }
            info.setText(item.archive.path + " · " + typeText);
            info.setStyle(warn ? WARNING_STYLE : "");

            NewArchiveWizard.ArchiveType type = NewArchiveWizard.ArchiveType.forClassName(item.type);
            Node icon;
            if (type != null) {
                Region r = type.icon();
                r.setStyle("-fx-min-width: 20px; -fx-min-height: 20px;"
                        + " -fx-max-width: 20px; -fx-max-height: 20px;");
                icon = r;
            }
            else icon = ActionUtils.icon(FontAwesome.ARCHIVE, "1.3em");
            iconHolder.getChildren().setAll(icon);

            setText(null);
            setGraphic(root);
        }
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
