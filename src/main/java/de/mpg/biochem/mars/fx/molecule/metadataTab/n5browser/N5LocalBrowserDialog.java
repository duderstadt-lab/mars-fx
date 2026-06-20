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

import java.io.File;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import de.mpg.biochem.mars.fx.util.MarsThemeManager;
import javafx.stage.Window;

/**
 * Reduced JavaFX dialog for browsing N5 datasets in a local .n5 directory.
 * Uses a DirectoryChooser to locate the .n5, then lists datasets inside it.
 *
 * @author Karl Duderstadt
 */
public class N5LocalBrowserDialog extends Dialog<N5LocalBrowserDialog.LocalResult> {

    public static final class LocalResult {

        public final String n5Path;  // absolute path to the .n5 directory
        public final String dataset; // e.g. "Pos0"

        public LocalResult(String n5Path, String dataset) {
            this.n5Path = n5Path;
            this.dataset = dataset;
        }
    }

    private final TextField pathField = new TextField();
    private final N5DatasetListPane datasetPane = new N5DatasetListPane();
    private final Label statusLabel = new Label();

    public N5LocalBrowserDialog(final Window owner, final String initialPath) {
        setTitle("Open N5 — Local");
        initOwner(owner);
        setResizable(true);

        if (initialPath != null) pathField.setText(initialPath);
        pathField.setPromptText("/path/to/dataset.n5");

        Label pathLbl = new Label("Path");
        Button browseButton = new Button("Browse");
        browseButton.setOnAction(e -> chooseDirectory());
        pathField.setOnAction(e -> loadPath(pathField.getText().trim()));

        HBox top = new HBox(8, pathLbl, pathField, browseButton);
        top.setPadding(new Insets(10));
        HBox.setHgrow(pathField, Priority.ALWAYS);

        BorderPane content = new BorderPane();
        content.setTop(top);
        content.setCenter(datasetPane);
        content.setBottom(statusLabel);
        BorderPane.setMargin(statusLabel, new Insets(0, 10, 8, 10));
        content.setPrefSize(640, 480);
        content.getStyleClass().add("bdv-source-options");   // <-- add here
        getDialogPane().setContent(content);

        ButtonType okType = new ButtonType("OK", ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        final javafx.scene.Node okButton = getDialogPane().lookupButton(okType);
        okButton.setDisable(true);
        datasetPane.selectedDatasetProperty().addListener((obs, old, sel) -> okButton
                .setDisable(sel == null));

        setResultConverter(bt -> {
            if (bt == okType && datasetPane.getSelectedDataset() != null) {
                return new LocalResult(pathField.getText().trim(), datasetPane
                        .getSelectedDataset());
            }
            return null;
        });

        setOnHidden(e -> datasetPane.shutdown());

        // If we were given a path that looks like an .n5, load it immediately.
        if (initialPath != null && initialPath.endsWith(".n5")) loadPath(
                initialPath);
    }

    private void chooseDirectory() {
        final DirectoryChooser chooser = new DirectoryChooser();
        final File current = new File(pathField.getText().trim());
        if (current.getParentFile() != null && current.getParentFile().exists())
            chooser.setInitialDirectory(current.getParentFile());

        final File dir = chooser.showDialog(getDialogPane().getScene().getWindow());
        if (dir == null) return;

        if (!dir.getName().endsWith(".n5")) {
            statusLabel.setText("Selected folder is not an .n5 directory");
            return;
        }
        pathField.setText(dir.getAbsolutePath());
        loadPath(dir.getAbsolutePath());
    }

    private void loadPath(final String path) {
        if (path == null || path.isEmpty()) return;
        statusLabel.setText("");
        final String name = new File(path).getName();
        datasetPane.load(path, name, null);
    }
}
