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
import java.util.List;

import org.kordamp.ikonli.fontawesome.FontAwesome;

import de.mpg.biochem.mars.fx.dialogs.s3.CloudArchiveSaveDialog;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.fx.util.MarsThemeManager;
import de.mpg.biochem.mars.n5.MarsS3Browser;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

/**
 * Where a new archive is written — folder, name and ending (.yama or
 * .yama.store) — shared by the Dataset Explorer's new-archive wizard and its
 * merge dialog. The folder may be a local path or an S3 URL; Browse offers
 * both. The suggested folder is the explorer's current bucket or local folder,
 * narrowed to the inputs' shared subfolder when they all have one, and is only
 * re-suggested while the user hasn't edited it.
 */
public class ArchiveDestinationPane extends VBox {

    /** Outcome of a successful create or merge, handed back to the explorer. */
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

        public Created(String location, String name, String relativePath, boolean open) {
            this.location = location;
            this.name = name;
            this.relativePath = relativePath;
            this.open = open;
        }
    }

    public static final String ENDING_FILE = ".yama";
    public static final String ENDING_STORE = ".yama.store";

    // Explicit border so fields stay visible in light mode — same treatment as
    // the explorer's own text fields.
    static final String FIELD_STYLE =
            "-fx-border-color: derive(-fx-text-inner-color, 55%);"
                    + " -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;";

    private final boolean localSource;
    private final String endpoint;
    private final String bucketOrRoot;

    private final TextField folderField = new TextField();
    private final TextField nameField = new TextField();
    private final Button endingButton = new Button(ENDING_FILE);
    private final Label preview = new Label();
    private boolean storeEnding = false;
    private String lastSuggestedFolder = "";
    private Runnable onChange;

    /**
     * @param localSource true if the explorer is showing a local folder
     * @param endpoint S3 server (cloud source); ignored for local
     * @param bucketOrRoot current bucket (cloud) or indexed root folder (local)
     */
    public ArchiveDestinationPane(boolean localSource, String endpoint,
                                  String bucketOrRoot)
    {
        super(6);
        this.localSource = localSource;
        this.endpoint = endpoint == null ? "" : endpoint;
        this.bucketOrRoot = bucketOrRoot == null ? "" : bucketOrRoot;

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

        nameField.setStyle(FIELD_STYLE);
        nameField.setPromptText("archive name");
        HBox.setHgrow(nameField, Priority.ALWAYS);
        endingButton.setTooltip(new Tooltip(
                "Click to switch between a single .yama file and a .yama.store "
                        + "virtual store"));
        endingButton.setMinWidth(90);
        endingButton.setOnAction(e -> setStoreEnding(!storeEnding));
        HBox nameRow = new HBox(6, nameField, endingButton);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        folderField.textProperty().addListener((o, a, b) -> changed());
        nameField.textProperty().addListener((o, a, b) -> changed());

        preview.getStyleClass().add("wizard-hint");
        preview.setWrapText(true);

        getChildren().addAll(sectionLabel("Folder"), folderRow,
                sectionLabel("Name"), nameRow, preview);
        setFillWidth(true);
        changed();
    }

    /** Called on every change to the folder, name or ending. */
    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    private void changed() {
        String dest = destination();
        preview.setText(dest == null ? "Enter a folder and a name." : "Saves to " + dest);
        if (onChange != null) onChange.run();
    }

    /**
     * Suggest a folder for inputs at these paths (relative to the bucket or
     * local root). Ignored once the user has typed or browsed to their own.
     */
    public void suggestFolder(List<String> relativePaths) {
        String current = folderField.getText() == null ? "" : folderField.getText().trim();
        if (!current.isEmpty() && !current.equals(lastSuggestedFolder)) return;
        lastSuggestedFolder = defaultFolder(relativePaths);
        folderField.setText(lastSuggestedFolder);
    }

    public void setName(String name) {
        nameField.setText(name);
    }

    public void setStoreEnding(boolean store) {
        storeEnding = store;
        endingButton.setText(store ? ENDING_STORE : ENDING_FILE);
        changed();
    }

    public boolean isStoreEnding() {
        return storeEnding;
    }

    /** Lock the fields while a save is running. */
    public void setEditable(boolean editable) {
        setDisable(!editable);
    }

    /** File name including its ending, or "" when no name is entered. */
    public String fileName() {
        String n = nameField.getText() == null ? "" : nameField.getText().trim();
        // Drop an ending typed into the name, the button carries it.
        n = n.replaceAll("(?i)\\.yama(\\.store)?$", "");
        return n.isEmpty() ? "" : n + (storeEnding ? ENDING_STORE : ENDING_FILE);
    }

    /** Full destination (S3 URL or absolute path), or null if incomplete. */
    public String destination() {
        String folder = folderField.getText() == null ? "" : folderField.getText().trim();
        String name = fileName();
        if (folder.isEmpty() || name.isEmpty()) return null;
        if (isCloud(folder)) return folder.endsWith("/") ? folder + name : folder + "/" + name;
        return new File(folder, name).getAbsolutePath();
    }

    /**
     * Suggested folder: the explorer's current bucket or local folder, narrowed
     * to the inputs' parent subfolder when they all share the same one.
     */
    private String defaultFolder(List<String> relativePaths) {
        if (bucketOrRoot.isEmpty()) return "";
        String common = null;
        boolean same = true;
        for (String path : relativePaths) {
            String parent = parentOf(path);
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

    /** Parent folder of a relative path ("" at the top level). */
    private static String parentOf(String path) {
        if (path == null) return "";
        String p = path.replace('\\', '/');
        while (p.endsWith("/")) p = p.substring(0, p.length() - 1);
        int i = p.lastIndexOf('/');
        return i < 0 ? "" : p.substring(0, i);
    }

    private void browseS3() {
        String name = fileName();
        CloudArchiveSaveDialog dialog = new CloudArchiveSaveDialog(
                getScene().getWindow(), name.isEmpty() ? "archive" : name);
        MarsThemeManager.applyTheme(dialog.getDialogPane(), MarsThemeManager.isDarkTheme());
        dialog.showAndWait().ifPresent(url -> {
            // Split the chosen URL back into folder + name + ending.
            int slash = url.lastIndexOf('/');
            String file = url.substring(slash + 1);
            setStoreEnding(file.endsWith(ENDING_STORE));
            folderField.setText(url.substring(0, slash + 1));
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
        File dir = chooser.showDialog(getScene().getWindow());
        if (dir != null) folderField.setText(dir.getAbsolutePath());
    }

    /**
     * The destination's path relative to the explorer's current bucket or local
     * root, or null when it was saved somewhere else.
     */
    public String relativeToSource(String dest) {
        if (bucketOrRoot.isEmpty() || dest == null) return null;
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

    public static boolean isCloud(String location) {
        String f = location.trim().toLowerCase();
        return f.startsWith("http://") || f.startsWith("https://") || f.startsWith("s3://");
    }

    /** True if something is already saved at the destination (local or S3). */
    public static boolean exists(String dest) {
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

    static Label sectionLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("wizard-section-label");
        return l;
    }
}
