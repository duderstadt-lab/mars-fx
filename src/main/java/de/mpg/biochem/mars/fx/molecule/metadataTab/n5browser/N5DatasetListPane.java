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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import de.mpg.biochem.mars.n5.DatasetEntry;
import de.mpg.biochem.mars.n5.MarsS3Browser;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import javafx.scene.text.Text;

/**
 * Lists the datasets inside a single .n5 container and lets the user pick one.
 * Used by both the cloud (MinIO) and local browse dialogs. All N5 I/O runs off
 * the JavaFX Application Thread.
 *
 * @author Karl Duderstadt
 */
public class N5DatasetListPane extends BorderPane {

    private final ListView<DatasetEntry> datasetList = new ListView<>();
    private final Label headerLabel = new Label();
    private final Label statusLabel = new Label();
    private final Text refresh;
    private final RotateTransition refreshSpin;

    private final StringProperty selectedDataset = new SimpleStringProperty(null);

    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            new ThreadFactory() {

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "N5DatasetList");
                    t.setDaemon(true);
                    return t;
                }
            });

    private Task<List<DatasetEntry>> currentTask;

    public N5DatasetListPane() {
        setPadding(new Insets(5));

        refresh = FontAwesomeIconFactory.get().createIcon(
                FontAwesomeIcon.REFRESH, "1.2em");
        refreshSpin = new RotateTransition(Duration.millis(800), refresh);
        refreshSpin.setByAngle(360);
        refreshSpin.setCycleCount(RotateTransition.INDEFINITE);
        refreshSpin.setInterpolator(Interpolator.LINEAR);

        VBox header = new VBox(2, headerLabel, statusLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 5, 0));
        headerLabel.setWrapText(true);
        headerLabel.setMaxWidth(Double.MAX_VALUE);
        setTop(header);

        datasetList.setCellFactory(lv -> new ListCell<DatasetEntry>() {

            @Override
            protected void updateItem(DatasetEntry item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getRowLabel());
            }
        });

        datasetList.getSelectionModel().selectedItemProperty().addListener((obs,
                                                                            old, sel) -> selectedDataset.set(sel == null ? null : sel.getName()));

        setCenter(datasetList);
    }

    /** Selected dataset name (e.g. "Pos0"), or null. Bindable. */
    public StringProperty selectedDatasetProperty() {
        return selectedDataset;
    }

    public String getSelectedDataset() {
        return selectedDataset.get();
    }

    /** Clears the list (e.g. when no .n5 is selected). */
    public void clear() {
        datasetList.getItems().clear();
        headerLabel.setText("");
        statusLabel.setText("");
        selectedDataset.set(null);
    }

    /**
     * Load and display the datasets inside the given .n5 root URL. {@code
     * displayName} is shown as the header (e.g. the .n5 file name). {@code
     * preselect} optionally selects a dataset by name once loaded.
     */
    public void load(final String n5RootUrl, final String displayName,
                     final String preselect)
    {
        if (currentTask != null && currentTask.isRunning()) currentTask.cancel();

        headerLabel.setText(displayName == null ? "" : displayName);
        datasetList.getItems().clear();
        selectedDataset.set(null);
        statusLabel.setGraphic(refresh);
        statusLabel.setText("");
        refreshSpin.playFromStart();

        final Task<List<DatasetEntry>> task =
                new Task<List<DatasetEntry>>()
                {

                    @Override
                    protected List<DatasetEntry> call() {
                        return MarsS3Browser.listDatasets(n5RootUrl);
                    }
                };

        task.setOnSucceeded(e -> {
            if (task != currentTask) return;
            stopSpin();
            final List<DatasetEntry> entries = task.getValue();
            datasetList.getItems().setAll(entries);
            if (entries.isEmpty()) statusLabel.setText("No datasets found");
            else if (preselect != null) {
                for (DatasetEntry de : entries)
                    if (preselect.equals(de.getName())) {
                        datasetList.getSelectionModel().select(de);
                        break;
                    }
            }
        });

        task.setOnFailed(e -> {
            if (task != currentTask) return;
            stopSpin();
            statusLabel.setText("Could not read datasets");
        });

        task.setOnCancelled(e -> {});

        currentTask = task;
        executor.submit(task);
    }

    private void stopSpin() {
        refreshSpin.stop();
        refresh.setRotate(0);
        statusLabel.setGraphic(null);
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
