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
package de.mpg.biochem.mars.fx.molecule.metadataTab.n5browser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.prefs.Preferences;

import com.amazonaws.AmazonServiceException;

import de.mpg.biochem.mars.n5.MarsS3Browser;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

/**
 * JavaFX dialog for browsing N5 datasets stored in MinIO/S3 buckets. Lists
 * buckets (best effort), browses folders down to the .n5, then lists datasets
 * inside it. Returns the selected server/bucket/.n5-root/dataset.
 * <p>
 * When opened with an existing path, the dialog pre-navigates: it selects the
 * bucket, expands the folder tree down to the .n5, and preselects the dataset.
 * <p>
 * The last-used server endpoint is remembered across sessions via
 * {@link Preferences}; there is no hardcoded default, so the field is empty on
 * first use and prefilled thereafter.
 *
 * @author Karl Duderstadt
 */
public class N5MinioBrowserDialog extends
        Dialog<N5MinioBrowserDialog.BrowseResult>
{

    private static final String SERVER_PREF_KEY = "mars.n5.lastServer";

    public static final class BrowseResult {

        public final String server;
        public final String bucket;
        public final String n5Root;  // path within bucket, ends with .n5
        public final String dataset; // e.g. "Pos0"

        public BrowseResult(String server, String bucket, String n5Root,
                            String dataset)
        {
            this.server = server;
            this.bucket = bucket;
            this.n5Root = n5Root;
            this.dataset = dataset;
        }

        /** Canonical Mars N5 URL for the .n5 root. */
        public String getFullPath() {
            return MarsS3Browser.buildPath(server, bucket, n5Root);
        }
    }

    /** Node kinds in the folder tree. */
    private enum Kind {
        FOLDER, N5_ROOT
    }

    private static final class S3Node {

        final String name; // last path segment
        final String prefix; // full prefix within the bucket (no leading slash)
        final Kind kind;

        S3Node(String name, String prefix, Kind kind) {
            this.name = name;
            this.prefix = prefix;
            this.kind = kind;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final TextField serverField = new TextField();
    private final TextField bucketField = new TextField();
    private final ListView<String> bucketList = new ListView<>();
    private final TreeView<S3Node> folderTree = new TreeView<>();
    private final N5DatasetListPane datasetPane = new N5DatasetListPane();
    private final Label statusLabel = new Label();

    private MarsS3Browser browser;
    private String currentBucket;
    private String currentN5Root; // prefix of the selected .n5, or null

    // Pre-navigation target (parsed from an existing path), consumed step by step.
    private String pendingBucket;
    private List<String> pendingSegments; // path segments down to and incl. .n5
    private String pendingDataset;

    private final java.util.Set<String> loadingPrefixes = new java.util.HashSet<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            new ThreadFactory()
            {

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "N5MinioBrowser");
                    t.setDaemon(true);
                    return t;
                }
            });

    /**
     * @param initialPath an existing canonical Mars N5 URL to pre-navigate to,
     *          or null/empty.
     * @param initialDataset the dataset name stored alongside the path, or null.
     */
    public N5MinioBrowserDialog(final Window owner, final String initialPath,
                                final String initialDataset)
    {
        setTitle("Open N5 — MinIO");
        initOwner(owner);
        setResizable(true);

        serverField.setPromptText("https://server:port/");
        bucketField.setPromptText("bucket name");

        // --- top: server + bucket + connect ---
        GridPane top = new GridPane();
        top.setHgap(8);
        top.setVgap(6);
        top.setPadding(new Insets(10));

        Label serverLbl = new Label("Server");
        Label bucketLbl = new Label("Bucket");
        Button connectButton = new Button("Connect");

        top.add(serverLbl, 0, 0);
        top.add(serverField, 1, 0);
        top.add(connectButton, 2, 0);
        top.add(bucketLbl, 0, 1);
        top.add(bucketField, 1, 1);
        top.add(statusLabel, 1, 2);
        GridPane.setHgrow(serverField, Priority.ALWAYS);
        GridPane.setHgrow(bucketField, Priority.ALWAYS);

        connectButton.setOnAction(e -> connect());
        bucketField.setOnAction(e -> selectBucket(bucketField.getText().trim()));

        // --- middle: buckets | folder tree ---
        bucketList.getSelectionModel().selectedItemProperty().addListener((obs,
                                                                           old, sel) -> {
            if (sel != null) {
                bucketField.setText(sel);
                selectBucket(sel);
            }
        });

        bucketList.setCellFactory(lv -> new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String bucket, boolean empty) {
                super.updateItem(bucket, empty);
                if (empty || bucket == null) {
                    setText(null);
                    setGraphic(null);
                }
                else {
                    setText(bucket);
                    setGraphic(FontAwesomeIconFactory.get().createIcon(
                            FontAwesomeIcon.ARCHIVE, "1.0em"));
                }
            }
        });

        folderTree.setShowRoot(false);
        folderTree.getSelectionModel().selectedItemProperty().addListener((obs,
                                                                           old, sel) -> onFolderSelected(sel));

        SplitPane middle = new SplitPane(bucketList, folderTree);
        middle.setDividerPositions(0.3);

        // --- bottom: dataset list inside the selected .n5 ---
        SplitPane vertical = new SplitPane(middle, datasetPane);
        vertical.setOrientation(Orientation.VERTICAL);
        //vertical.setDividerPositions(0.72);

        BorderPane content = new BorderPane();
        content.setTop(top);
        content.setCenter(vertical);
        content.setPrefSize(800, 600);
        content.getStyleClass().add("bdv-source-options");   // <-- add here
        getDialogPane().setContent(content);

        ButtonType okType = new ButtonType("OK", ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        final Node okButton = getDialogPane().lookupButton(okType);
        okButton.setDisable(true);
        datasetPane.selectedDatasetProperty().addListener((obs, old, sel) -> okButton
                .setDisable(sel == null || currentN5Root == null));

        datasetPane.setMinHeight(0);

        setResultConverter(bt -> {
            if (bt == okType && currentN5Root != null && datasetPane
                    .getSelectedDataset() != null)
            {
                // Remember the server endpoint for next time.
                saveLastServer(endpoint());
                return new BrowseResult(endpoint(), currentBucket, currentN5Root,
                        datasetPane.getSelectedDataset());
            }
            return null;
        });

        setOnHidden(e -> {
            datasetPane.shutdown();
            executor.shutdownNow();
            if (browser != null) browser.close();
        });

        setOnShown(e -> Platform.runLater(() ->
                vertical.setDividerPositions(0.6)));

        // Decide the starting server: parse an existing path if present, else the
        // last-used server from preferences (empty on first ever use).
        final MarsS3Browser.ParsedPath parsed = MarsS3Browser.parsePath(
                initialPath);
        if (parsed != null) {
            serverField.setText(parsed.server);
            pendingBucket = parsed.bucket;
            pendingSegments = new ArrayList<>(Arrays.asList(parsed.n5Root.split(
                    "/")));
            pendingDataset = initialDataset;
        }
        else {
            serverField.setText(loadLastServer());
        }

        // Auto-connect on open only if we already have a server address.
        if (!serverField.getText().trim().isEmpty()) Platform.runLater(
                this::connect);

        Platform.runLater(() -> {
            serverField.deselect();
            serverField.end();
        });
    }

    private String endpoint() {
        return serverField.getText().trim();
    }

    private void connect() {
        final String server = endpoint();
        if (server.isEmpty()) {
            statusLabel.setText("Enter a server address");
            return;
        }

        statusLabel.setText("Connecting…");
        bucketList.getItems().clear();
        folderTree.setRoot(null);
        datasetPane.clear();
        currentN5Root = null;

        if (browser != null) browser.close();
        browser = new MarsS3Browser(server);

        final Task<List<String>> task = new Task<List<String>>()
        {

            @Override
            protected List<String> call() {
                return browser.listBuckets();
            }
        };

        task.setOnSucceeded(e -> {
            bucketList.getItems().setAll(task.getValue());
            statusLabel.setText(task.getValue().isEmpty() ? "No buckets" : "");

            if (pendingBucket != null) {
                bucketField.setText(pendingBucket);
                if (bucketList.getItems().contains(pendingBucket)) {
                    bucketList.scrollTo(pendingBucket);
                    bucketList.getSelectionModel().select(pendingBucket); // → selectBucket via listener
                }
                else {
                    // Bucket not in the (possibly denied/empty) list — browse it directly.
                    selectBucket(pendingBucket);
                }
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex instanceof AmazonServiceException && "AccessDenied".equals(
                    ((AmazonServiceException) ex).getErrorCode()))
            {
                statusLabel.setText("Bucket list unavailable — enter a bucket name");
                // Listing denied, but we can still pre-navigate a known bucket.
                if (pendingBucket != null) {
                    bucketField.setText(pendingBucket);
                    selectBucket(pendingBucket);
                }
            }
            else {
                statusLabel.setText("Could not reach server — check the address");
            }
        });

        executor.submit(task);
    }

    private void selectBucket(final String bucket) {
        if (bucket == null || bucket.isEmpty() || browser == null) return;
        currentBucket = bucket;
        currentN5Root = null;
        datasetPane.clear();
        statusLabel.setText("");

        final S3Node rootNode = new S3Node(bucket, "", Kind.FOLDER);
        final TreeItem<S3Node> root = new TreeItem<>(rootNode);
        folderTree.setRoot(root);
        root.setExpanded(true);
        loadChildren(root);
    }

    private void loadChildren(final TreeItem<S3Node> parent) {
        if (parent == null) return;
        final S3Node node = parent.getValue();
        if (node == null || node.kind == Kind.N5_ROOT) return; // stop at .n5

        // Only load once: a freshly-created folder has a single placeholder child
        // whose value is null. Anything else means we've already populated it.
        final boolean hasPlaceholder = parent.getChildren().size() == 1 && parent
                .getChildren().get(0).getValue() == null;
        if (!parent.getChildren().isEmpty() && !hasPlaceholder) return;

        // Don't start a second load for a prefix already being loaded.
        if (!loadingPrefixes.add(node.prefix)) return;

        final Task<List<String>> task = new Task<List<String>>()
        {

            @Override
            protected List<String> call() {
                return browser.listFolders(currentBucket, node.prefix);
            }
        };

        task.setOnSucceeded(e -> {
            loadingPrefixes.remove(node.prefix);
            final List<String> folders = task.getValue();
            parent.getChildren().clear();
            for (String folder : folders) {
                final String childPrefix = node.prefix.isEmpty() ? folder
                        : node.prefix + "/" + folder;
                final Kind kind = browser.isN5(folder) ? Kind.N5_ROOT : Kind.FOLDER;
                final TreeItem<S3Node> child = new TreeItem<>(new S3Node(folder,
                        childPrefix, kind));
                if (kind == Kind.FOLDER) {
                    child.getChildren().add(new TreeItem<>((S3Node) null));
                    child.expandedProperty().addListener((o, was, isNow) -> {
                        if (isNow) loadChildren(child);
                    });
                }
                parent.getChildren().add(child);
            }
            if (folders.isEmpty()) statusLabel.setText("Empty: " + node.prefix);

            // Continue any pending pre-navigation from this freshly-loaded level.
            advancePendingNavigation(parent);
        });

        task.setOnFailed(e -> {
            loadingPrefixes.remove(node.prefix);
            parent.getChildren().clear();
            Throwable ex = task.getException();
            statusLabel.setText("Could not list " + node.name + ": " + (ex == null
                    ? "error" : ex.getMessage()));
            pendingSegments = null;
        });

        executor.submit(task);
    }

    /**
     * Advances pre-navigation one level: given a just-loaded parent, finds the
     * next pending path segment among its children and either expands it (folder)
     * or selects it (.n5 root). Silently stops if the segment is missing.
     */
    private void advancePendingNavigation(final TreeItem<S3Node> justLoaded) {
        if (pendingSegments == null || pendingSegments.isEmpty()) return;
        if (justLoaded.getValue() == null) return;

        final String parentPrefix = justLoaded.getValue().prefix;
        final int consumed = parentPrefix.isEmpty() ? 0 : parentPrefix.split("/")
                .length;
        if (consumed >= pendingSegments.size()) return;

        final String nextName = pendingSegments.get(consumed);

        System.out.println("[N5Browser] advance: consumed=" + consumed +
                " nextName=" + nextName + " childrenKinds=" +
                justLoaded.getChildren().stream()
                        .filter(c -> c.getValue() != null)
                        .map(c -> c.getValue().name + ":" + c.getValue().kind)
                        .collect(java.util.stream.Collectors.toList()));

        for (TreeItem<S3Node> child : justLoaded.getChildren()) {
            if (child.getValue() == null) continue;
            if (nextName.equals(child.getValue().name)) {
                if (child.getValue().kind == Kind.N5_ROOT) {
                    final TreeItem<S3Node> n5Item = child;
                    pendingSegments = null;

                    TreeItem<S3Node> p = n5Item.getParent();
                    while (p != null) {
                        p.setExpanded(true);
                        p = p.getParent();
                    }

                    // Select now (drives dataset loading)...
                    folderTree.getSelectionModel().select(n5Item);

                    // ...and re-assert after layout + any competing walk settles, so the
                    // highlight sticks even though selecting triggered the dataset load.
                    Platform.runLater(() -> Platform.runLater(() -> {
                        folderTree.getSelectionModel().select(n5Item);
                        final int n5Row = folderTree.getRow(n5Item);
                        if (n5Row >= 0) folderTree.getFocusModel().focus(n5Row);

                        // Scroll so the containing folder is at the top, for context.
                        final TreeItem<S3Node> parentFolder = n5Item.getParent();
                        final int scrollRow = (parentFolder != null)
                                ? folderTree.getRow(parentFolder) : n5Row;
                        if (scrollRow >= 0) folderTree.scrollTo(Math.max(0, scrollRow - 1));
                    }));
                    return;
                }
                else {
                    folderTree.scrollTo(folderTree.getRow(child));
                    child.setExpanded(true); // triggers its own load + continuation
                }
                return;
            }
        }
        // Target segment not found at this level — abandon pre-navigation quietly.
        pendingSegments = null;
    }

    private void onFolderSelected(final TreeItem<S3Node> item) {
        if (item == null || item.getValue() == null) {
            currentN5Root = null;
            datasetPane.clear();
            return;
        }
        final S3Node node = item.getValue();
        if (node.kind == Kind.N5_ROOT) {
            currentN5Root = node.prefix;
            final String url = MarsS3Browser.buildPath(endpoint(), currentBucket,
                    node.prefix);
            final String preselect = pendingDataset; // null in the manual case
            pendingDataset = null;
            datasetPane.load(url, url, preselect);   // url as both root and header text
        }
        else {
            currentN5Root = null;
            datasetPane.clear();
        }
    }

    // ---- server preference persistence ----

    private static String loadLastServer() {
        final Preferences prefs = Preferences.userNodeForPackage(
                N5MinioBrowserDialog.class);
        return prefs.get(SERVER_PREF_KEY, "");
    }

    private static void saveLastServer(final String server) {
        if (server == null || server.isEmpty()) return;
        final Preferences prefs = Preferences.userNodeForPackage(
                N5MinioBrowserDialog.class);
        prefs.put(SERVER_PREF_KEY, server);
    }
}
