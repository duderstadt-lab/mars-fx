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
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;

import com.amazonaws.AmazonServiceException;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.util.IJStage;
import de.mpg.biochem.mars.fx.util.MarsThemeManager;
import de.mpg.biochem.mars.n5.MarsS3Browser;
import ij.WindowManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Modeless Stage-based window for opening Mars archives (.yama) from S3/MinIO.
 * Built as a Stage wrapped in an {@link IJStage} shadow frame (like the archive
 * windows) so AWT focus moves onto its proxy when focused, preventing Cmd/Ctrl
 * shortcuts from leaking to ImageJ. The chosen archive URL is delivered via a
 * callback (the window is modeless, so there is no blocking showAndWait).
 *
 * @author Karl Duderstadt
 */
public class CloudArchiveOpenWindow {

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

    private final Button openButton = new Button("Open");

    private String pendingBucket;
    private List<String> pendingSegments;

    private MarsS3Browser browser;
    private String currentBucket;
    private String selectedUrl;

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

    private Stage stage;
    private IJStage ijStage;
    private final Consumer<String> resultCallback;

    private CloudArchiveOpenWindow(final Consumer<String> resultCallback) {
        this.resultCallback = resultCallback;
    }

    /**
     * Builds and shows the window. The callback is invoked once with the chosen
     * archive URL (Open) or null (Cancel / closed). Must be called on the FX
     * thread.
     */
    private static CloudArchiveOpenWindow openInstance;

    public static void show(final Consumer<String> resultCallback) {
        // Only one Open Archive window at a time — focus the existing one.
        if (openInstance != null && openInstance.stage != null) {
            openInstance.stage.toFront();
            openInstance.stage.requestFocus();
            return;
        }
        final CloudArchiveOpenWindow win = new CloudArchiveOpenWindow(
                resultCallback);
        openInstance = win;
        win.build();
    }

    private void build() {
        stage = new Stage();
        stage.setTitle("Open Archive — S3");

        ijStage = new IJStage(stage);
        ijStage.buildShadowFrame();
        SwingUtilities.invokeLater(() -> WindowManager.addWindow(ijStage));

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
                                                                           old, sel) -> onTreeSelected(sel));
        SplitPane middle = new SplitPane(bucketList, folderTree);
        middle.setDividerPositions(0.3);

        // --- bottom: paste field + recents ---
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
                updateOpen();
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

        // --- button bar ---
        Button cancelButton = new Button("Cancel");
        openButton.setDisable(true);
        cancelButton.setOnAction(e -> finish(null));
        openButton.setOnAction(e -> finish(selectedUrl));
        HBox buttonBar = new HBox(8, cancelButton, openButton);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(8, 10, 10, 10));

        BorderPane content = new BorderPane();
        content.setTop(top);
        content.setCenter(vertical);
        content.setBottom(buttonBar);
        content.getStyleClass().add("bdv-source-options");

        Scene scene = new Scene(content, 800, 640);
        MarsThemeManager.applyTheme(scene);
        stage.setScene(scene);

        stage.setOnCloseRequest(e -> {
            if (!resultDelivered) {
                resultDelivered = true;
                if (resultCallback != null) resultCallback.accept(null);
            }
            cleanupExecutor();
            SwingUtilities.invokeLater(() -> {
                WindowManager.removeWindow(ijStage);
                ijStage.cleanup();
            });
        });

        stage.setOnHidden(e -> openInstance = null);

        stage.setOnShown(e -> vertical.setDividerPositions(0.7));
        stage.show();

        serverField.setText(loadLastServer());
        if (!serverField.getText().trim().isEmpty()) Platform.runLater(
                this::connect);
        Platform.runLater(() -> {
            serverField.deselect();
            serverField.end();
        });
    }

    private boolean resultDelivered = false;

    private void finish(final String url) {
        if (resultDelivered) return;
        resultDelivered = true;

        if (url != null) {
            saveLastServer(endpoint());
            addRecent(url);
        }
        if (resultCallback != null) resultCallback.accept(url);

        cleanupExecutor();
        stage.close();
        SwingUtilities.invokeLater(() -> {
            WindowManager.removeWindow(ijStage);
            ijStage.cleanup();
        });
        openInstance = null;
    }

    private void cleanupExecutor() {
        final MarsS3Browser b = browser;
        new Thread(() -> {
            executor.shutdownNow();
            if (b != null) b.close();
        }, "CloudArchiveOpen-cleanup").start();
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
        selectedUrl = url;
        updateOpen();
        serverField.setText(parsed.server);
        pendingBucket = parsed.bucket;
        pendingSegments = new ArrayList<>(Arrays.asList(parsed.n5Root.split("/")));
        connect();
    }

    private void updateOpen() {
        openButton.setDisable(selectedUrl == null);
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
        updateOpen();

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
                return new FolderContents(browser.listFolders(currentBucket,
                        node.prefix), browser.listFiles(currentBucket, node.prefix));
            }
        };
        task.setOnSucceeded(e -> {
            loadingPrefixes.remove(node.prefix);
            parent.getChildren().clear();
            final FolderContents contents = task.getValue();
            for (String folder : contents.folders) {
                final String childPrefix = node.prefix.isEmpty() ? folder
                        : node.prefix + "/" + folder;
                if (browser.isArchive(folder)) parent.getChildren().add(
                        new TreeItem<>(new S3Node(folder, childPrefix, Kind.ARCHIVE)));
                else if (folder.endsWith(".n5")) parent.getChildren().add(
                        new TreeItem<>(new S3Node(folder, childPrefix, Kind.N5)));
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
                        final TreeItem<S3Node> pf = archiveItem.getParent();
                        final int scrollRow = (pf != null) ? folderTree.getRow(pf)
                                : row;
                        if (scrollRow >= 0) folderTree.scrollTo(scrollRow);
                    }));
                }
                else {
                    folderTree.scrollTo(folderTree.getRow(child));
                    child.setExpanded(true);
                }
                return;
            }
        }
        pendingSegments = null;
    }

    private void onTreeSelected(final TreeItem<S3Node> item) {
        if (item == null || item.getValue() == null) return;
        final S3Node node = item.getValue();
        if (node.kind == Kind.ARCHIVE) {
            selectedUrl = MarsS3Browser.buildPath(endpoint(), currentBucket,
                    node.prefix);
            recentList.getSelectionModel().clearSelection();
            updateOpen();
        }
        else if (recentList.getSelectionModel().getSelectedItem() == null) {
            selectedUrl = null;
            updateOpen();
        }
    }

    private List<String> loadRecents() {
        final String stored = Preferences.userNodeForPackage(
                CloudArchiveOpenWindow.class).get(RECENTS_PREF_KEY, "");
        final List<String> recents = new ArrayList<>();
        if (!stored.isEmpty()) for (String line : stored.split("\n"))
            if (!line.trim().isEmpty()) recents.add(line);
        return recents;
    }

    private void addRecent(final String url) {
        final List<String> recents = loadRecents();
        recents.remove(url);
        recents.add(0, url);
        while (recents.size() > MAX_RECENTS)
            recents.remove(recents.size() - 1);
        Preferences.userNodeForPackage(CloudArchiveOpenWindow.class).put(
                RECENTS_PREF_KEY, String.join("\n", recents));
    }

    private static String loadLastServer() {
        return Preferences.userNodeForPackage(CloudArchiveOpenWindow.class).get(
                SERVER_PREF_KEY, "");
    }

    private static void saveLastServer(final String server) {
        if (server == null || server.isEmpty()) return;
        Preferences.userNodeForPackage(CloudArchiveOpenWindow.class).put(
                SERVER_PREF_KEY, server);
    }
}
