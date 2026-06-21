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
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Modal dialog for saving a Mars archive (.yama / .yama.store) to S3/MinIO.
 * Launched from an open archive window (its owner), so it inherits theming and
 * focus routing. The destination is an editable folder Path plus a base Name; a
 * toggle button after the Name shows and cycles the ending (.yama /
 * .yama.store). Tree selections fill the Path (folder) or Path+Name+ending
 * (existing archive), but the fields are freely editable so new/empty paths
 * work too — S3 materializes the folder when the object is written. Returns the
 * full destination URL via showAndWait().
 *
 * @author Karl Duderstadt
 */
public class CloudArchiveSaveDialog extends Dialog<String> {

    private static final String SERVER_PREF_KEY = "mars.s3.lastServer";

    private static final String ENDING_FILE = ".yama";
    private static final String ENDING_STORE = ".yama.store";

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

    private final TextField serverField = new TextField();
    private final TextField bucketField = new TextField();
    private final javafx.scene.control.ListView<String> bucketList =
            new javafx.scene.control.ListView<>();
    private final TreeView<S3Node> folderTree = new TreeView<>();
    private final TextField pathField = new TextField();
    private final TextField nameField = new TextField();
    private final Button endingButton = new Button(ENDING_FILE);
    private final Label statusLabel = new Label();

    private MarsS3Browser browser;
    private String currentBucket;

    private boolean storeEnding = false; // false = .yama, true = .yama.store

    private final java.util.Set<String> loadingPrefixes =
            new java.util.HashSet<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            new ThreadFactory()
            {

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "CloudArchiveSave");
                    t.setDaemon(true);
                    return t;
                }
            });

    public CloudArchiveSaveDialog(final Window owner,
                                  final String initialArchiveName)
    {
        setTitle("Save Archive — S3");
        initOwner(owner);
        initModality(javafx.stage.Modality.APPLICATION_MODAL);
        setResizable(true);

        serverField.setPromptText("https://server:port/");
        bucketField.setPromptText("bucket name");
        if (initialArchiveName != null) nameField.setText(stripEnding(
                initialArchiveName));

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

        // --- bottom: Path row, then Name row with the ending toggle ---
        pathField.setPromptText("folder path (e.g. 2026/experiment1) — blank = bucket root");
        HBox pathRow = new HBox(8, new Label("Path"), pathField);
        pathRow.setAlignment(Pos.CENTER_LEFT);
        pathRow.setPadding(new Insets(8, 10, 0, 10));
        HBox.setHgrow(pathField, Priority.ALWAYS);

        nameField.setPromptText("archive name");
        endingButton.setCenterShape(true);
        endingButton.setStyle("-fx-background-radius: 2em; -fx-border-radius: 2em; " +
                "-fx-min-width: 90px; -fx-min-height: 30px; -fx-max-height: 30px;");
        endingButton.setFocusTraversable(false);
        endingButton.setOnAction(e -> {
            storeEnding = !storeEnding;
            endingButton.setText(currentEnding());
        });
        HBox nameRow = new HBox(8, new Label("Name"), nameField, endingButton);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        nameRow.setPadding(new Insets(6, 10, 8, 10));
        HBox.setHgrow(nameField, Priority.ALWAYS);

        // Align the two labels by giving them the same min width.
        ((Label) pathRow.getChildren().get(0)).setMinWidth(40);
        ((Label) nameRow.getChildren().get(0)).setMinWidth(40);

        VBox bottom = new VBox(pathRow, nameRow);

        BorderPane content = new BorderPane();
        content.setTop(top);
        content.setCenter(middle);
        content.setBottom(bottom);
        content.setPrefSize(760, 560);
        content.getStyleClass().add("bdv-source-options");
        getDialogPane().setContent(content);

        ButtonType saveType = new ButtonType("Save", ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        // Intercept Save to do the existence/overwrite check before closing.
        final Node saveButton = getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            ev.consume(); // always consume; we close manually after the check
            final String url = buildDestinationUrl();
            if (url == null) {
                statusLabel.setText("Connect to a bucket and enter a name");
                return;
            }
            checkOverwriteThenFinish(url);
        });

        setResultConverter(bt -> (bt == saveType) ? pendingResultUrl : null);

        setOnHidden(e -> {
            final MarsS3Browser b = browser;
            new Thread(() -> {
                executor.shutdownNow();
                if (b != null) b.close();
            }, "CloudArchiveSave-cleanup").start();
        });

        serverField.setText(loadLastServer());
        if (!serverField.getText().trim().isEmpty()) Platform.runLater(
                this::connect);
    }

    private String pendingResultUrl;

    private void checkOverwriteThenFinish(final String url) {
        statusLabel.setText("Checking…");
        final String key = destinationKey();
        final Task<Boolean> task = new Task<Boolean>()
        {

            @Override
            protected Boolean call() {
                return browser.exists(currentBucket, key);
            }
        };
        task.setOnSucceeded(e -> {
            statusLabel.setText("");
            if (task.getValue()) {
                Alert a = new Alert(AlertType.CONFIRMATION,
                        "An archive already exists at this location. Overwrite it?",
                        ButtonType.YES, ButtonType.NO);
                a.initOwner(getOwner());
                a.setHeaderText(null);
                a.showAndWait().ifPresent(bt -> {
                    if (bt == ButtonType.YES) finishWith(url);
                });
            }
            else {
                finishWith(url);
            }
        });
        task.setOnFailed(e -> statusLabel.setText("Could not verify destination"));
        executor.submit(task);
    }

    private void finishWith(final String url) {
        pendingResultUrl = url;
        saveLastServer(endpoint());
        setResult(url);
        close();
    }

    // ---- destination assembly ----

    private String currentEnding() {
        return storeEnding ? ENDING_STORE : ENDING_FILE;
    }

    private static String stripEnding(final String name) {
        if (name == null) return "";
        String n = name;
        if (n.endsWith(ENDING_STORE)) n = n.substring(0, n.length() -
                ENDING_STORE.length());
        else if (n.endsWith(ENDING_FILE)) n = n.substring(0, n.length() -
                ENDING_FILE.length());
        return n;
    }

    /** Normalized folder prefix from the Path field: no leading/trailing slash. */
    private String folderPrefix() {
        String folder = pathField.getText().trim();
        while (folder.startsWith("/"))
            folder = folder.substring(1);
        while (folder.endsWith("/"))
            folder = folder.substring(0, folder.length() - 1);
        return folder;
    }

    /** key within the bucket: folder/name+ending (no leading slash), or null. */
    private String destinationKey() {
        final String base = stripEnding(nameField.getText().trim());
        if (currentBucket == null || base.isEmpty()) return null;
        final String folder = folderPrefix();
        final String prefix = folder.isEmpty() ? "" : folder + "/";
        return prefix + base + currentEnding();
    }

    private String buildDestinationUrl() {
        final String key = destinationKey();
        if (key == null) return null;
        return MarsS3Browser.buildPath(endpoint(), currentBucket, key);
    }

    private void onTreeSelected(final TreeItem<S3Node> item) {
        if (item == null || item.getValue() == null) return;
        final S3Node node = item.getValue();
        if (node.kind == Kind.FOLDER) {
            // Fill the Path with the folder; leave Name as-is.
            pathField.setText(node.prefix);
        }
        else if (node.kind == Kind.ARCHIVE) {
            // Existing archive: Path = parent folder, Name = its base name,
            // ending toggle set to match its type (overwrite setup).
            final TreeItem<S3Node> parent = item.getParent();
            final String parentPrefix = (parent != null && parent.getValue() != null)
                    ? parent.getValue().prefix : "";
            pathField.setText(parentPrefix);
            nameField.setText(stripEnding(node.name));
            storeEnding = node.name.endsWith(ENDING_STORE);
            endingButton.setText(currentEnding());
        }
        // N5 nodes are inert.
    }

    // ---- browse ----

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
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex instanceof AmazonServiceException && "AccessDenied".equals(
                    ((AmazonServiceException) ex).getErrorCode())) statusLabel.setText(
                    "Bucket list unavailable — enter a bucket name");
            else statusLabel.setText("Could not reach server — check the address");
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
        });
        task.setOnFailed(e -> {
            loadingPrefixes.remove(node.prefix);
            parent.getChildren().clear();
            Throwable ex = task.getException();
            statusLabel.setText("Could not list " + node.name + ": " + (ex == null
                    ? "error" : ex.getMessage()));
        });
        executor.submit(task);
    }

    private static String loadLastServer() {
        return Preferences.userNodeForPackage(CloudArchiveSaveDialog.class).get(
                SERVER_PREF_KEY, "");
    }

    private static void saveLastServer(final String server) {
        if (server == null || server.isEmpty()) return;
        Preferences.userNodeForPackage(CloudArchiveSaveDialog.class).put(
                SERVER_PREF_KEY, server);
    }
}