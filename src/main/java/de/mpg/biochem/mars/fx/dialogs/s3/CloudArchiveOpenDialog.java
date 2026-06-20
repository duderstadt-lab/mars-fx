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
package de.mpg.biochem.mars.fx.dialogs.s3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.prefs.Preferences;

import com.amazonaws.AmazonServiceException;

import de.mpg.biochem.mars.fx.util.MarsThemeManager;
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
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * JavaFX dialog for opening Mars archives (.yama) stored in S3/MinIO buckets.
 * Browses buckets and folders, treating .yama files and .yama.store
 * directories as selectable leaves, .n5 directories as non-enterable leaves,
 * and shows a recently-opened list plus a paste-URL field at the bottom.
 * Returns the full archive URL.
 *
 * @author Karl Duderstadt
 */
public class CloudArchiveOpenDialog extends Dialog<String> {

    private static final String SERVER_PREF_KEY = "mars.s3.lastServer";
    private static final String RECENTS_PREF_KEY = "recentOpenURLs";
    private static final int MAX_RECENTS = 15;

    private enum Kind {
        FOLDER, ARCHIVE, N5
    }

    private static final class S3Node {

        final String name;
        final String prefix;
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

    private static final class FolderContents {

        final List<String> folders;
        final List<String> files;

        FolderContents(List<String> folders, List<String> files) {
            this.folders = folders;
            this.files = files;
        }
    }

    private final TextField pasteField = new TextField();
    private final TextField serverField = new TextField();
    private final TextField bucketField = new TextField();
    private final ListView<String> bucketList = new ListView<>();
    private final TreeView<S3Node> folderTree = new TreeView<>();
    private final ListView<String> recentList = new ListView<>();
    private final Label statusLabel = new Label();

    // Pre-navigation target (parsed from a pasted/recent URL), consumed stepwise.
    private String pendingBucket;
    private List<String> pendingSegments;

    private MarsS3Browser browser;
    private String currentBucket;
    private String selectedUrl; // full .yama url of current selection

    private final java.util.Set<String> loadingPrefixes =
            new java.util.HashSet<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            new ThreadFactory()
            {

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "CloudArchiveOpen");
                    t.setDaemon(true);
                    return t;
                }
            });

    public CloudArchiveOpenDialog(final Window owner) {
        setTitle("Open Archive — S3");
        initOwner(owner);
        setResizable(true);

        serverField.setPromptText("https://server:port/");
        bucketField.setPromptText("bucket name");

        // --- top: server + bucket + connect ---
        GridPane top = new GridPane();
        top.setHgap(8);
        top.setVgap(6);
        top.setPadding(new Insets(10));

        Button connectButton = new Button("Connect");
        top.add(new Label("Server"), 0, 0);
        top.add(serverField, 1, 0);
        top.add(connectButton, 2, 0);
        top.add(new Label("Bucket"), 0, 1);
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

        folderTree.setShowRoot(false);
        folderTree.getSelectionModel().selectedItemProperty().addListener((obs,
                                                                           old, sel) -> onTreeSelected(sel));
        SplitPane middle = new SplitPane(bucketList, folderTree);
        middle.setDividerPositions(0.3);

        // --- bottom: paste field + recently opened archives ---
        VBox recentBox = new VBox(2);
        recentBox.setPadding(new Insets(5, 0, 0, 0));

        pasteField.setPromptText(
                "Or paste a full archive URL (…/file.yama) and press Enter");
        pasteField.setOnAction(e -> navigateToUrl(pasteField.getText().trim()));

        Label recentLabel = new Label("Recent");
        recentList.getItems().setAll(loadRecents());
        recentList.getSelectionModel().selectedItemProperty().addListener((obs,
                                                                           old, sel) -> {
            if (sel != null) {
                selectedUrl = sel;
                updateOk();
                folderTree.getSelectionModel().clearSelection();
            }
        });
        recentList.setCellFactory(lv -> new ListCell<String>()
        {

            @Override
            protected void updateItem(String url, boolean empty) {
                super.updateItem(url, empty);
                setText((empty || url == null) ? null : url);
            }
        });
        recentList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                final String url = recentList.getSelectionModel().getSelectedItem();
                if (url != null) navigateToUrl(url);
            }
        });
        VBox.setVgrow(recentList, Priority.ALWAYS);

        recentBox.getChildren().addAll(pasteField, recentLabel, recentList);

        SplitPane vertical = new SplitPane(middle, recentBox);
        vertical.setOrientation(Orientation.VERTICAL);

        BorderPane content = new BorderPane();
        content.setTop(top);
        content.setCenter(vertical);
        content.setPrefSize(800, 600);
        content.getStyleClass().add("bdv-source-options");
        getDialogPane().setContent(content);
        MarsThemeManager.applyTheme(getDialogPane());

        ButtonType okType = new ButtonType("Open", ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        final Node okButton = getDialogPane().lookupButton(okType);
        okButton.setDisable(true);

        setResultConverter(bt -> {
            if (bt == okType && selectedUrl != null) {
                saveLastServer(endpoint());
                addRecent(selectedUrl);
                return selectedUrl;
            }
            return null;
        });

        setOnShown(e -> vertical.setDividerPositions(0.7));
        setOnHidden(e -> {
            executor.shutdownNow();
            if (browser != null) browser.close();
        });

        serverField.setText(loadLastServer());
        if (!serverField.getText().trim().isEmpty()) Platform.runLater(
                this::connect);
        Platform.runLater(() -> {
            serverField.deselect();
            serverField.end();
        });
    }

    private void navigateToUrl(final String url) {
        if (url == null || !url.endsWith(".yama")) {
            statusLabel.setText("Enter a full URL ending in .yama");
            return;
        }
        final MarsS3Browser.ParsedPath parsed = MarsS3Browser.parsePath(url);
        if (parsed == null) {
            statusLabel.setText("Could not parse that URL");
            return;
        }
        // Make it immediately selectable even before the tree catches up.
        selectedUrl = url;
        updateOk();

        serverField.setText(parsed.server);
        pendingBucket = parsed.bucket;
        pendingSegments = new ArrayList<>(Arrays.asList(parsed.n5Root.split("/")));
        // Reconnect to the (possibly new) server, then pre-navigate.
        connect();
    }

    private void updateOk() {
        final Node okButton = getDialogPane().lookupButton(getDialogPane()
                .getButtonTypes().get(0));
        if (okButton != null) okButton.setDisable(selectedUrl == null);
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
        updateOk();

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
                    bucketList.getSelectionModel().select(pendingBucket);
                }
                else {
                    selectBucket(pendingBucket);
                }
            }
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex instanceof AmazonServiceException && "AccessDenied".equals(
                    ((AmazonServiceException) ex).getErrorCode())) statusLabel.setText(
                    "Bucket list unavailable — enter a bucket name");
            else statusLabel.setText("Could not reach server — check the address");

            if (pendingBucket != null) {
                bucketField.setText(pendingBucket);
                selectBucket(pendingBucket);
            }
        });
        executor.submit(task);
    }

    private void selectBucket(final String bucket) {
        if (bucket == null || bucket.isEmpty() || browser == null) return;
        currentBucket = bucket;
        statusLabel.setText("");
        final TreeItem<S3Node> root = new TreeItem<>(new S3Node(bucket, "",
                Kind.FOLDER));
        folderTree.setRoot(root);
        root.setExpanded(true);
        loadChildren(root);
    }

    private void loadChildren(final TreeItem<S3Node> parent) {
        if (parent == null) return;
        final S3Node node = parent.getValue();
        if (node == null || node.kind == Kind.ARCHIVE || node.kind == Kind.N5)
            return;

        final boolean hasPlaceholder = parent.getChildren().size() == 1 && parent
                .getChildren().get(0).getValue() == null;
        if (!parent.getChildren().isEmpty() && !hasPlaceholder) return;
        if (!loadingPrefixes.add(node.prefix)) return;

        final Task<FolderContents> task = new Task<FolderContents>()
        {

            @Override
            protected FolderContents call() {
                final List<String> folders = browser.listFolders(currentBucket,
                        node.prefix);
                final List<String> files = browser.listFiles(currentBucket,
                        node.prefix);
                return new FolderContents(folders, files);
            }
        };

        task.setOnSucceeded(e -> {
            loadingPrefixes.remove(node.prefix);
            parent.getChildren().clear();

            final FolderContents contents = task.getValue();

            for (String folder : contents.folders) {
                final String childPrefix = node.prefix.isEmpty() ? folder
                        : node.prefix + "/" + folder;
                if (browser.isArchive(folder)) {
                    parent.getChildren().add(new TreeItem<>(new S3Node(folder,
                            childPrefix, Kind.ARCHIVE)));
                }
                else if (folder.endsWith(".n5")) {
                    // .n5 shown as a dead-end leaf — visible for context, not enterable.
                    parent.getChildren().add(new TreeItem<>(new S3Node(folder,
                            childPrefix, Kind.N5)));
                }
                else {
                    final TreeItem<S3Node> child = new TreeItem<>(new S3Node(folder,
                            childPrefix, Kind.FOLDER));
                    child.getChildren().add(new TreeItem<>((S3Node) null));
                    child.expandedProperty().addListener((o, was, isNow) -> {
                        if (isNow) loadChildren(child);
                    });
                    parent.getChildren().add(child);
                }
            }

            for (String file : contents.files) {
                if (browser.isArchive(file)) {
                    final String childPrefix = node.prefix.isEmpty() ? file
                            : node.prefix + "/" + file;
                    parent.getChildren().add(new TreeItem<>(new S3Node(file,
                            childPrefix, Kind.ARCHIVE)));
                }
            }

            if (parent.getChildren().isEmpty()) statusLabel.setText("Empty: " +
                    node.prefix);

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

    private void advancePendingNavigation(final TreeItem<S3Node> justLoaded) {
        if (pendingSegments == null || pendingSegments.isEmpty()) return;
        if (justLoaded.getValue() == null) return;

        final String parentPrefix = justLoaded.getValue().prefix;
        final int consumed = parentPrefix.isEmpty() ? 0 : parentPrefix.split("/")
                .length;
        if (consumed >= pendingSegments.size()) return;

        final String nextName = pendingSegments.get(consumed);

        for (TreeItem<S3Node> child : justLoaded.getChildren()) {
            if (child.getValue() == null) continue;
            if (nextName.equals(child.getValue().name)) {
                if (child.getValue().kind == Kind.ARCHIVE) {
                    final TreeItem<S3Node> archiveItem = child;
                    pendingSegments = null;

                    TreeItem<S3Node> p = archiveItem.getParent();
                    while (p != null) {
                        p.setExpanded(true);
                        p = p.getParent();
                    }
                    folderTree.getSelectionModel().select(archiveItem);

                    Platform.runLater(() -> Platform.runLater(() -> {
                        folderTree.getSelectionModel().select(archiveItem);
                        final int row = folderTree.getRow(archiveItem);
                        if (row >= 0) folderTree.getFocusModel().focus(row);
                        final TreeItem<S3Node> parentFolder = archiveItem.getParent();
                        final int scrollRow = (parentFolder != null) ? folderTree
                                .getRow(parentFolder) : row;
                        if (scrollRow >= 0) folderTree.scrollTo(scrollRow);
                    }));
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

    private void onTreeSelected(final TreeItem<S3Node> item) {
        if (item == null || item.getValue() == null) return;
        final S3Node node = item.getValue();
        if (node.kind == Kind.ARCHIVE) {
            selectedUrl = MarsS3Browser.buildPath(endpoint(), currentBucket,
                    node.prefix);
            recentList.getSelectionModel().clearSelection();
            updateOk();
        }
        else {
            if (recentList.getSelectionModel().getSelectedItem() == null) {
                selectedUrl = null;
                updateOk();
            }
        }
    }

    // ---- recents ----

    private List<String> loadRecents() {
        final String stored = Preferences.userNodeForPackage(
                CloudArchiveOpenDialog.class).get(RECENTS_PREF_KEY, "");
        final List<String> recents = new ArrayList<>();
        if (!stored.isEmpty()) {
            for (String line : stored.split("\n"))
                if (!line.trim().isEmpty()) recents.add(line);
        }
        return recents;
    }

    private void addRecent(final String url) {
        final List<String> recents = loadRecents();
        recents.remove(url);
        recents.add(0, url);
        while (recents.size() > MAX_RECENTS)
            recents.remove(recents.size() - 1);
        Preferences.userNodeForPackage(CloudArchiveOpenDialog.class).put(
                RECENTS_PREF_KEY, String.join("\n", recents));
    }

    // ---- server endpoint persistence ----

    private static String loadLastServer() {
        return Preferences.userNodeForPackage(CloudArchiveOpenDialog.class).get(
                SERVER_PREF_KEY, "");
    }

    private static void saveLastServer(final String server) {
        if (server == null || server.isEmpty()) return;
        Preferences.userNodeForPackage(CloudArchiveOpenDialog.class).put(
                SERVER_PREF_KEY, server);
    }
}