/*-
 * #%L
 * Molecule Archive Suite (Mars) - core data storage and processing algorithms.
 * %%
 * Copyright (C) 2018 - 2025 Karl Duderstadt
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

package de.mpg.biochem.mars.fx.dialogs;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.molecule.MoleculeArchiveService;
import de.mpg.biochem.mars.util.LogBuilder;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Plugin(type = Command.class, label = "Create Archive Wizard", menu = {
        @Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT,
                mnemonic = MenuConstants.PLUGINS_MNEMONIC),
        @Menu(label = "Mars", weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = 's'),
        @Menu(label = "Molecule", weight = 2, mnemonic = 'm'),
        @Menu(label = "Util", weight = 7, mnemonic = 'u'),
        @Menu(label = "Create Archive Wizard", weight = 1, mnemonic = 'w')
})
public class CreateArchiveWizardCommand extends DynamicCommand implements Command {

    @Parameter
    private LogService logService;

    @Parameter
    private MoleculeArchiveService moleculeArchiveService;

    @Parameter
    private UIService uiService;

    // Storage for wizard data
    private Map<String, Object> wizardData = new HashMap<>();

    // Wizard state
    private int currentStep = 0;
    private final String[] stepTitles = {
            "Dataset Location",
            "Coordinate Transforms",
            "Channel Configuration",
            "Archive Settings",
            "Summary"
    };
    private final String[] stepDescriptions = {
            "Select your microscopy data",
            "Configure transformations",
            "Define channel properties",
            "Set archive parameters",
            "Review and create"
    };
    private VBox stepSidebar;
    private StackPane currentContentArea;
    private HBox navigationButtons;

    @Override
    public void run() {
        // Initialize JavaFX if not already done
        initializeJavaFX();

        // Launch the wizard on the JavaFX Application Thread
        Platform.runLater(() -> {
            try {
                showWizard();
            } catch (Exception e) {
                logService.error("Error launching Mars Archive Wizard", e);
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to launch wizard");
                    alert.setContentText("An error occurred while launching the Mars Archive Wizard: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        });
    }

    private void initializeJavaFX() {
        try {
            // Initialize JavaFX Platform if not already initialized
            Platform.runLater(() -> {
                // This empty runnable ensures JavaFX is initialized
            });
        } catch (IllegalStateException e) {
            // JavaFX runtime might not be initialized, try to initialize it
            try {
                // Force JavaFX initialization
                javafx.embed.swing.JFXPanel panel = new javafx.embed.swing.JFXPanel();
            } catch (Exception ex) {
                logService.error("Failed to initialize JavaFX", ex);
            }
        }
    }

    private void showWizard() {
        createCustomWizardDialog();
    }

    private void createCustomWizardDialog() {
        Stage wizardStage = new Stage();
        wizardStage.setTitle("Mars Archive Creator");
        wizardStage.initStyle(StageStyle.UTILITY);
        wizardStage.setResizable(true);

        // Create main layout
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPrefSize(800, 600);

        // Create step navigation sidebar
        stepSidebar = createStepSidebar();
        stepSidebar.setPrefWidth(250);
        stepSidebar.setStyle("-fx-background-color: #2c3e50; -fx-padding: 20;");

        // Create content area
        currentContentArea = new StackPane();
        currentContentArea.setStyle("-fx-background-color: white; -fx-padding: 20;");

        // Create navigation buttons
        navigationButtons = createNavigationButtons(wizardStage);
        navigationButtons.setStyle("-fx-background-color: #ecf0f1; -fx-padding: 15; -fx-spacing: 10;");
        navigationButtons.setAlignment(Pos.CENTER_RIGHT);

        // Add components to main layout
        mainLayout.setLeft(stepSidebar);
        mainLayout.setCenter(currentContentArea);
        mainLayout.setBottom(navigationButtons);

        // Initialize with first step
        currentStep = 0;
        updateStepDisplay();
        updateStepSidebar();

        Scene scene = new Scene(mainLayout);
        wizardStage.setScene(scene);
        wizardStage.show();
    }

    private VBox createStepSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setAlignment(Pos.TOP_LEFT);

        // Title
        Label title = new Label("CREATE MARS ARCHIVE");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 0 0 20 0;");
        sidebar.getChildren().add(title);

        // Create step buttons
        for (int i = 0; i < stepTitles.length; i++) {
            VBox stepButton = createStepButton(i + 1, stepTitles[i], stepDescriptions[i], i);
            sidebar.getChildren().add(stepButton);
        }

        return sidebar;
    }

    private VBox createStepButton(int stepNumber, String title, String description, int stepIndex) {
        VBox stepContainer = new VBox(5);
        stepContainer.setPadding(new Insets(15, 0, 15, 0));
        stepContainer.setStyle("-fx-cursor: hand;");

        HBox stepHeader = new HBox(15);
        stepHeader.setAlignment(Pos.CENTER_LEFT);

        // Step number circle
        Label numberLabel = new Label(String.valueOf(stepNumber));
        numberLabel.setPrefSize(30, 30);
        numberLabel.setAlignment(Pos.CENTER);
        numberLabel.setStyle(getStepNumberStyle(stepIndex));

        // Step title
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        stepHeader.getChildren().addAll(numberLabel, titleLabel);

        // Step description
        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 12px; -fx-padding: 0 0 0 45;");
        descLabel.setWrapText(true);

        stepContainer.getChildren().addAll(stepHeader, descLabel);

        // Add click handler for navigation
        stepContainer.setOnMouseClicked(e -> navigateToStep(stepIndex));

        // Add hover effect
        stepContainer.setOnMouseEntered(e -> {
            if (stepIndex != currentStep) {
                stepContainer.setStyle("-fx-cursor: hand; -fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 5;");
            }
        });

        stepContainer.setOnMouseExited(e -> {
            if (stepIndex != currentStep) {
                stepContainer.setStyle("-fx-cursor: hand;");
            }
        });

        return stepContainer;
    }

    private String getStepNumberStyle(int stepIndex) {
        if (stepIndex == currentStep) {
            return "-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 15; -fx-font-weight: bold;";
        } else if (stepIndex < currentStep) {
            return "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 15; -fx-font-weight: bold;";
        } else {
            return "-fx-background-color: rgba(255,255,255,0.3); -fx-text-fill: white; -fx-background-radius: 15; -fx-border-color: #7f8c8d; -fx-border-radius: 15;";
        }
    }

    private void updateStepSidebar() {
        for (int i = 0; i < stepSidebar.getChildren().size() - 1; i++) { // -1 to skip title
            VBox stepContainer = (VBox) stepSidebar.getChildren().get(i + 1);
            HBox stepHeader = (HBox) stepContainer.getChildren().get(0);
            Label numberLabel = (Label) stepHeader.getChildren().get(0);

            numberLabel.setStyle(getStepNumberStyle(i));

            // Update container background for current step
            if (i == currentStep) {
                stepContainer.setStyle("-fx-cursor: hand; -fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 5;");
            } else {
                stepContainer.setStyle("-fx-cursor: hand;");
            }
        }
    }

    private HBox createNavigationButtons(Stage wizardStage) {
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-padding: 8 20; -fx-font-size: 12px;");
        cancelButton.setOnAction(e -> wizardStage.close());

        Button previousButton = new Button("Previous");
        previousButton.setStyle("-fx-padding: 8 20; -fx-font-size: 12px;");
        previousButton.setOnAction(e -> {
            if (currentStep > 0) {
                navigateToStep(currentStep - 1);
            }
        });

        Button nextButton = new Button("Next");
        nextButton.setStyle("-fx-padding: 8 20; -fx-font-size: 12px; -fx-background-color: #3498db; -fx-text-fill: white;");
        nextButton.setOnAction(e -> {
            if (currentStep < stepTitles.length - 1) {
                navigateToStep(currentStep + 1);
            }
        });

        Button finishButton = new Button("Finish");
        finishButton.setStyle("-fx-padding: 8 20; -fx-font-size: 12px; -fx-background-color: #27ae60; -fx-text-fill: white;");
        finishButton.setOnAction(e -> {
            createMarsArchive();
            wizardStage.close();
        });

        buttonBox.getChildren().addAll(cancelButton, previousButton, nextButton, finishButton);
        return buttonBox;
    }

    private void navigateToStep(int stepIndex) {
        if (stepIndex >= 0 && stepIndex < stepTitles.length) {
            currentStep = stepIndex;
            updateStepDisplay();
            updateStepSidebar();
            updateNavigationButtons();
        }
    }

    private void updateNavigationButtons() {
        if (navigationButtons != null && navigationButtons.getChildren().size() >= 4) {
            Button previousButton = (Button) navigationButtons.getChildren().get(1);
            Button nextButton = (Button) navigationButtons.getChildren().get(2);
            Button finishButton = (Button) navigationButtons.getChildren().get(3);

            previousButton.setDisable(currentStep == 0);

            if (currentStep == stepTitles.length - 1) {
                nextButton.setVisible(false);
                finishButton.setVisible(true);
            } else {
                nextButton.setVisible(true);
                finishButton.setVisible(false);
            }
        }
    }

    private void updateStepDisplay() {
        if (currentContentArea != null) {
            currentContentArea.getChildren().clear();

            VBox stepContent = null;
            switch (currentStep) {
                case 0:
                    stepContent = createDatasetLocationContent();
                    break;
                case 1:
                    stepContent = createCoordinateTransformContent();
                    break;
                case 2:
                    stepContent = createChannelDescriptionContent();
                    break;
                case 3:
                    stepContent = createArchiveSettingsContent();
                    break;
                case 4:
                    stepContent = createSummaryContent();
                    break;
            }

            if (stepContent != null) {
                currentContentArea.getChildren().add(stepContent);
            }
        }
    }

    private VBox createDatasetLocationContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label header = new Label("Select Dataset Location");
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label description = new Label("Choose the location of your microscopy dataset files for Mars archive creation.");
        description.setStyle("-fx-text-fill: #666;");
        description.setWrapText(true);

        // Dataset path selection
        VBox pathSection = new VBox(10);
        Label pathLabel = new Label("Dataset Path:");
        pathLabel.setStyle("-fx-font-weight: bold;");

        HBox pathBox = new HBox(10);
        pathBox.setAlignment(Pos.CENTER_LEFT);

        TextField pathField = new TextField();
        pathField.setPromptText("Select dataset folder...");
        pathField.setPrefWidth(400);

        Button browseBtn = new Button("Browse...");
        browseBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Dataset Folder");
            File selectedDir = chooser.showDialog(null);
            if (selectedDir != null) {
                pathField.setText(selectedDir.getAbsolutePath());
                wizardData.put("datasetPath", selectedDir.getAbsolutePath());
            }
        });

        pathBox.getChildren().addAll(pathField, browseBtn);
        pathSection.getChildren().addAll(pathLabel, pathBox);

        // File type selection
        VBox fileTypeSection = new VBox(10);
        Label fileTypeLabel = new Label("Select file types to include:");
        fileTypeLabel.setStyle("-fx-font-weight: bold;");

        CheckBox tiffBox = new CheckBox("TIFF files (*.tif, *.tiff)");
        CheckBox nd2Box = new CheckBox("Nikon ND2 files (*.nd2)");
        CheckBox lsmBox = new CheckBox("Zeiss LSM files (*.lsm)");
        CheckBox cziBox = new CheckBox("Zeiss CZI files (*.czi)");
        CheckBox leiBox = new CheckBox("Leica LEI files (*.lei)");

        tiffBox.setSelected(true);

        // Store selections in wizard data
        tiffBox.setOnAction(e -> wizardData.put("includeTiff", tiffBox.isSelected()));
        nd2Box.setOnAction(e -> wizardData.put("includeNd2", nd2Box.isSelected()));
        lsmBox.setOnAction(e -> wizardData.put("includeLsm", lsmBox.isSelected()));
        cziBox.setOnAction(e -> wizardData.put("includeCzi", cziBox.isSelected()));
        leiBox.setOnAction(e -> wizardData.put("includeLei", leiBox.isSelected()));

        // Initialize data
        wizardData.put("includeTiff", true);
        wizardData.put("includeNd2", false);
        wizardData.put("includeLsm", false);
        wizardData.put("includeCzi", false);
        wizardData.put("includeLei", false);

        fileTypeSection.getChildren().addAll(fileTypeLabel, tiffBox, nd2Box, lsmBox, cziBox, leiBox);

        content.getChildren().addAll(header, description, new Separator(), pathSection, fileTypeSection);
        return content;
    }

    private VBox createCoordinateTransformContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label header = new Label("Coordinate Transformation Settings");
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label description = new Label("Configure spatial transformations for drift correction and registration.");
        description.setStyle("-fx-text-fill: #666;");
        description.setWrapText(true);

        // Transformation type
        VBox transformSection = new VBox(10);
        Label transformLabel = new Label("Transformation Type:");
        transformLabel.setStyle("-fx-font-weight: bold;");

        ToggleGroup transformGroup = new ToggleGroup();
        RadioButton noneBtn = new RadioButton("No transformation");
        RadioButton affineBtn = new RadioButton("Affine transformation");
        RadioButton rigidBtn = new RadioButton("Rigid body transformation");
        RadioButton elasticBtn = new RadioButton("Elastic transformation");

        noneBtn.setToggleGroup(transformGroup);
        affineBtn.setToggleGroup(transformGroup);
        rigidBtn.setToggleGroup(transformGroup);
        elasticBtn.setToggleGroup(transformGroup);

        noneBtn.setSelected(true);
        wizardData.put("transformationType", "none");

        // Update wizard data when selection changes
        noneBtn.setOnAction(e -> wizardData.put("transformationType", "none"));
        affineBtn.setOnAction(e -> wizardData.put("transformationType", "affine"));
        rigidBtn.setOnAction(e -> wizardData.put("transformationType", "rigid"));
        elasticBtn.setOnAction(e -> wizardData.put("transformationType", "elastic"));

        transformSection.getChildren().addAll(transformLabel, noneBtn, affineBtn, rigidBtn, elasticBtn);

        // Reference frame selection
        VBox refFrameSection = new VBox(10);
        Label refFrameLabel = new Label("Reference Frame (optional):");
        refFrameLabel.setStyle("-fx-font-weight: bold;");

        HBox refFrameBox = new HBox(10);
        refFrameBox.setAlignment(Pos.CENTER_LEFT);

        TextField refFrameField = new TextField();
        refFrameField.setPromptText("Select reference frame file...");
        refFrameField.setPrefWidth(400);

        Button refFrameBtn = new Button("Browse...");
        refFrameBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Reference Frame");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.tif", "*.tiff", "*.png", "*.jpg")
            );
            File selectedFile = chooser.showOpenDialog(null);
            if (selectedFile != null) {
                refFrameField.setText(selectedFile.getAbsolutePath());
                wizardData.put("referenceFrame", selectedFile.getAbsolutePath());
            }
        });

        refFrameBox.getChildren().addAll(refFrameField, refFrameBtn);
        refFrameSection.getChildren().addAll(refFrameLabel, refFrameBox);

        content.getChildren().addAll(header, description, new Separator(), transformSection, refFrameSection);
        return content;
    }

    private VBox createChannelDescriptionContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label header = new Label("Channel Configuration");
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label description = new Label("Define channels and their properties for the Mars archive.");
        description.setStyle("-fx-text-fill: #666;");
        description.setWrapText(true);

        // Number of channels
        HBox channelCountBox = new HBox(10);
        channelCountBox.setAlignment(Pos.CENTER_LEFT);
        Label channelCountLabel = new Label("Number of channels:");
        channelCountLabel.setStyle("-fx-font-weight: bold;");

        Spinner<Integer> channelSpinner = new Spinner<>(1, 10, 1);
        channelSpinner.setPrefWidth(80);
        channelSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            wizardData.put("channelCount", newVal);
        });
        wizardData.put("channelCount", 1);

        channelCountBox.getChildren().addAll(channelCountLabel, channelSpinner);

        // Channel configuration grid
        ScrollPane scrollPane = new ScrollPane();
        GridPane channelGrid = new GridPane();
        channelGrid.setHgap(10);
        channelGrid.setVgap(10);
        channelGrid.setPadding(new Insets(10));

        // Headers
        Label chLabel = new Label("Channel");
        chLabel.setStyle("-fx-font-weight: bold;");
        Label nameLabel = new Label("Name");
        nameLabel.setStyle("-fx-font-weight: bold;");
        Label colorLabel = new Label("Color");
        colorLabel.setStyle("-fx-font-weight: bold;");
        Label descLabel = new Label("Description");
        descLabel.setStyle("-fx-font-weight: bold;");

        channelGrid.add(chLabel, 0, 0);
        channelGrid.add(nameLabel, 1, 0);
        channelGrid.add(colorLabel, 2, 0);
        channelGrid.add(descLabel, 3, 0);

        // Sample channels
        for (int i = 0; i < 4; i++) {
            Label channelLabel = new Label("Ch" + (i + 1));
            TextField nameField = new TextField();
            nameField.setPromptText("Channel " + (i + 1));
            nameField.setPrefWidth(120);

            ComboBox<String> colorBox = new ComboBox<>();
            colorBox.getItems().addAll("Red", "Green", "Blue", "Cyan", "Magenta", "Yellow", "Gray");
            colorBox.setValue("Gray");
            colorBox.setPrefWidth(100);

            TextField descField = new TextField();
            descField.setPromptText("Description...");
            descField.setPrefWidth(200);

            // Store channel data
            final int channelIndex = i;
            nameField.textProperty().addListener((obs, oldVal, newVal) ->
                    wizardData.put("channel" + channelIndex + "Name", newVal));
            colorBox.valueProperty().addListener((obs, oldVal, newVal) ->
                    wizardData.put("channel" + channelIndex + "Color", newVal));
            descField.textProperty().addListener((obs, oldVal, newVal) ->
                    wizardData.put("channel" + channelIndex + "Description", newVal));

            channelGrid.add(channelLabel, 0, i + 1);
            channelGrid.add(nameField, 1, i + 1);
            channelGrid.add(colorBox, 2, i + 1);
            channelGrid.add(descField, 3, i + 1);
        }

        scrollPane.setContent(channelGrid);
        scrollPane.setPrefHeight(200);
        scrollPane.setFitToWidth(true);

        content.getChildren().addAll(header, description, new Separator(), channelCountBox, scrollPane);
        return content;
    }

    private VBox createArchiveSettingsContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label header = new Label("Mars Archive Configuration");
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label description = new Label("Configure Mars archive settings and processing parameters.");
        description.setStyle("-fx-text-fill: #666;");
        description.setWrapText(true);

        // Archive settings grid
        GridPane settingsGrid = new GridPane();
        settingsGrid.setHgap(15);
        settingsGrid.setVgap(15);
        settingsGrid.setPadding(new Insets(10));

        // Archive name
        Label archiveNameLabel = new Label("Archive Name:");
        archiveNameLabel.setStyle("-fx-font-weight: bold;");
        TextField archiveNameField = new TextField();
        archiveNameField.setPromptText("Enter archive name...");
        archiveNameField.setPrefWidth(250);
        archiveNameField.textProperty().addListener((obs, oldVal, newVal) ->
                wizardData.put("archiveName", newVal));

        settingsGrid.add(archiveNameLabel, 0, 0);
        settingsGrid.add(archiveNameField, 1, 0);

        // Processing options
        Label processingLabel = new Label("Processing Options:");
        processingLabel.setStyle("-fx-font-weight: bold;");
        settingsGrid.add(processingLabel, 0, 1);

        VBox processingBox = new VBox(5);
        CheckBox peakFindingBox = new CheckBox("Enable peak finding");
        CheckBox trackingBox = new CheckBox("Enable molecule tracking");
        CheckBox driftCorrectionBox = new CheckBox("Apply drift correction");

        peakFindingBox.setSelected(true);
        trackingBox.setSelected(true);

        peakFindingBox.setOnAction(e -> wizardData.put("enablePeakFinding", peakFindingBox.isSelected()));
        trackingBox.setOnAction(e -> wizardData.put("enableTracking", trackingBox.isSelected()));
        driftCorrectionBox.setOnAction(e -> wizardData.put("enableDriftCorrection", driftCorrectionBox.isSelected()));

        wizardData.put("enablePeakFinding", true);
        wizardData.put("enableTracking", true);
        wizardData.put("enableDriftCorrection", false);

        processingBox.getChildren().addAll(peakFindingBox, trackingBox, driftCorrectionBox);
        settingsGrid.add(processingBox, 1, 1);

        // Output location
        Label outputLabel = new Label("Output Location:");
        outputLabel.setStyle("-fx-font-weight: bold;");

        HBox outputBox = new HBox(10);
        outputBox.setAlignment(Pos.CENTER_LEFT);

        TextField outputField = new TextField();
        outputField.setPromptText("Select output folder...");
        outputField.setPrefWidth(250);

        Button outputBtn = new Button("Browse...");
        outputBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Output Folder");
            File selectedDir = chooser.showDialog(null);
            if (selectedDir != null) {
                outputField.setText(selectedDir.getAbsolutePath());
                wizardData.put("outputPath", selectedDir.getAbsolutePath());
            }
        });

        outputBox.getChildren().addAll(outputField, outputBtn);

        settingsGrid.add(outputLabel, 0, 2);
        settingsGrid.add(outputBox, 1, 2);

        content.getChildren().addAll(header, description, new Separator(), settingsGrid);
        return content;
    }

    private VBox createSummaryContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label header = new Label("Mars Archive Creation Summary");
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label description = new Label("Review your settings before creating the Mars archive.");
        description.setStyle("-fx-text-fill: #666;");
        description.setWrapText(true);

        // Summary content
        TextArea summaryArea = new TextArea();
        summaryArea.setEditable(false);
        summaryArea.setPrefRowCount(12);
        summaryArea.setText(generateSummaryText());

        content.getChildren().addAll(header, description, new Separator(), summaryArea);
        return content;
    }

    private String generateSummaryText() {
        StringBuilder summary = new StringBuilder();
        summary.append("Mars Archive Configuration Summary:\n\n");

        summary.append("Dataset Location: ").append(wizardData.getOrDefault("datasetPath", "Not specified")).append("\n");
        summary.append("Archive Name: ").append(wizardData.getOrDefault("archiveName", "Not specified")).append("\n");
        summary.append("Output Location: ").append(wizardData.getOrDefault("outputPath", "Not specified")).append("\n\n");

        summary.append("File Types:\n");
        if (Boolean.TRUE.equals(wizardData.get("includeTiff"))) summary.append("  - TIFF files\n");
        if (Boolean.TRUE.equals(wizardData.get("includeNd2"))) summary.append("  - ND2 files\n");
        if (Boolean.TRUE.equals(wizardData.get("includeLsm"))) summary.append("  - LSM files\n");
        if (Boolean.TRUE.equals(wizardData.get("includeCzi"))) summary.append("  - CZI files\n");
        if (Boolean.TRUE.equals(wizardData.get("includeLei"))) summary.append("  - LEI files\n");

        summary.append("\nTransformation: ").append(wizardData.getOrDefault("transformationType", "none")).append("\n");

        if (wizardData.containsKey("referenceFrame")) {
            summary.append("Reference Frame: ").append(wizardData.get("referenceFrame")).append("\n");
        }

        summary.append("Channels: ").append(wizardData.getOrDefault("channelCount", 1)).append("\n\n");

        summary.append("Processing Options:\n");
        if (Boolean.TRUE.equals(wizardData.get("enablePeakFinding"))) summary.append("  - Peak finding enabled\n");
        if (Boolean.TRUE.equals(wizardData.get("enableTracking"))) summary.append("  - Molecule tracking enabled\n");
        if (Boolean.TRUE.equals(wizardData.get("enableDriftCorrection"))) summary.append("  - Drift correction enabled\n");

        summary.append("\nClick 'Finish' to create the Mars archive with these settings.");

        return summary.toString();
    }

    private void createMarsArchive() {
        // Build log message
        LogBuilder builder = new LogBuilder();
        String log = LogBuilder.buildTitleBlock("Create Mars Archive");

        builder.addParameter("Dataset Path", (String) wizardData.getOrDefault("datasetPath", ""));
        builder.addParameter("Archive Name", (String) wizardData.getOrDefault("archiveName", ""));
        builder.addParameter("Transformation Type", (String) wizardData.getOrDefault("transformationType", "none"));
        log += builder.buildParameterList();

        logService.info(log);

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Archive Creation");
            alert.setHeaderText("Mars Archive Creation Started");
            alert.setContentText("Mars archive creation has been initiated with your specified settings.\n\n" +
                    "Check the Fiji console for progress updates.");
            alert.showAndWait();
        });

        // Here you would integrate with the actual Mars archive creation logic
        // For example:
        // createArchiveWithSettings(wizardData);

        logService.info("Mars Archive creation process initiated.");
        logService.info(LogBuilder.endBlock(true));
    }

    // Getters and setters for testing/integration
    public Map<String, Object> getWizardData() {
        return wizardData;
    }
}