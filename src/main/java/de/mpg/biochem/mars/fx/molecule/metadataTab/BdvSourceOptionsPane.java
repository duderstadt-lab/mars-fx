/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2021 Karl Duderstadt
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
package de.mpg.biochem.mars.fx.molecule.metadataTab;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import org.controlsfx.control.ToggleSwitch;
import org.janelia.saalfeldlab.n5.ij.N5Importer.N5BasePathFun;
import org.janelia.saalfeldlab.n5.ij.N5Importer.N5ViewerReaderFun;
import org.janelia.saalfeldlab.n5.metadata.DefaultMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import org.janelia.saalfeldlab.n5.ui.DatasetSelectorDialog;
import org.janelia.saalfeldlab.n5.ui.N5DatasetTreeCellRenderer;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.metadata.MarsBdvSource;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.GridPane;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class BdvSourceOptionsPane extends VBox {
	private TextField m00, m01, m02, m10, m11, m12, cField, pathField;
	private Label datasetInfo, n5Dataset;
	private ToggleSwitch driftCorrectSwitch;
	private BooleanProperty driftCorrect = new SimpleBooleanProperty();
	private Button pathButton;
	//private Label pathValidation;
	private MarsBdvSource marsBdvSource;
	
	private HBox n5OptionsHBox;
	
	public BdvSourceOptionsPane() {
		setPadding(new Insets(15, 20, 15, 20));
		setSpacing(5);
		setFillWidth(true);
		
		setStyle("-fx-border-color: lightgray");
		
		GridPane gridpane1 = new GridPane();
		
		//For reference...
		//Insets(double top, double right, double bottom, double left)
		
		Label m00Label = new Label("m00");
		gridpane1.add(m00Label, 0, 0);
		GridPane.setMargin(m00Label, new Insets(0, 5, 10, 5));
		
		m00 = new TextField();
		m00.textProperty().addListener((observable, oldValue, newValue) -> setAffineTransform3D(m00.getText(), 0, 0));
		m00.setPrefWidth(80);
		m00.setMaxWidth(80);
		gridpane1.add(m00, 1, 0);
		GridPane.setMargin(m00, new Insets(0, 5, 10, 5));
		
		Label m01Label = new Label("m01");
		gridpane1.add(m01Label, 2, 0);
		GridPane.setMargin(m01Label, new Insets(0, 5, 10, 5));
		
		m01 = new TextField();
		m01.textProperty().addListener((observable, oldValue, newValue) -> setAffineTransform3D(m01.getText(), 0, 1));
		m01.setPrefWidth(80);
		m01.setMaxWidth(80);
		gridpane1.add(m01, 3, 0);
		GridPane.setMargin(m01, new Insets(0, 5, 10, 5));
		
		Label m02Label = new Label("m02");
		gridpane1.add(m02Label, 4, 0);
		GridPane.setMargin(m02Label, new Insets(0, 5, 10, 5));
		
		m02 = new TextField();
		m02.textProperty().addListener((observable, oldValue, newValue) -> setAffineTransform3D(m02.getText(), 0, 3));
		m02.setPrefWidth(80);
		m02.setMaxWidth(80);
		gridpane1.add(m02, 5, 0);
		GridPane.setMargin(m02, new Insets(0, 5, 10, 5));
		
		getChildren().add(gridpane1);
		
		//Option to lock Y-range
		GridPane gridpane2 = new GridPane();
		
		Label m10Label = new Label("m10");
		gridpane2.add(m10Label, 0, 0);
		GridPane.setMargin(m10Label, new Insets(0, 5, 10, 5));
		
		m10 = new TextField();
		m10.textProperty().addListener((observable, oldValue, newValue) -> setAffineTransform3D(m10.getText(), 1, 0));
		m10.setPrefWidth(80);
		m10.setMaxWidth(80);
		gridpane2.add(m10, 1, 0);
		GridPane.setMargin(m10, new Insets(0, 5, 10, 5));
		
		Label m11Label = new Label("m11");
		gridpane2.add(m11Label, 2, 0);
		GridPane.setMargin(m11Label, new Insets(0, 5, 10, 5));
		
		m11 = new TextField();
		m11.textProperty().addListener((observable, oldValue, newValue) -> setAffineTransform3D(m11.getText(), 1, 1));
		m11.setPrefWidth(80);
		m11.setMaxWidth(80);
		gridpane2.add(m11, 3, 0);
		GridPane.setMargin(m11, new Insets(0, 5, 10, 5));
		
		Label m12Label = new Label("m12");
		gridpane2.add(m12Label, 4, 0);
		GridPane.setMargin(m12Label, new Insets(0, 5, 10, 5));
		
		m12 = new TextField();
		m12.textProperty().addListener((observable, oldValue, newValue) -> setAffineTransform3D(m12.getText(), 1, 3));
		m12.setPrefWidth(80);
		m12.setMaxWidth(80);
		gridpane2.add(m12, 5, 0);
		GridPane.setMargin(m12, new Insets(0, 5, 10, 5));
		
		getChildren().add(gridpane2);
		
		GridPane gridpane3 = new GridPane();
		
		driftCorrectSwitch = new ToggleSwitch();
		driftCorrect.setValue(false);
		driftCorrectSwitch.selectedProperty().bindBidirectional(driftCorrect);
		driftCorrect.addListener((observable, oldValue, newValue) -> {
			if (marsBdvSource != null)
				marsBdvSource.setCorrectDrift(newValue);
		});
		
		Label driftCorrectLabel = new Label("Drift Correct");
		gridpane3.add(driftCorrectLabel, 0, 0);
		GridPane.setMargin(driftCorrectLabel, new Insets(0, 5, 10, 5));

		gridpane3.add(driftCorrectSwitch, 1, 0);
		GridPane.setMargin(driftCorrectSwitch, new Insets(0, 5, 10, 5));
		
		getChildren().add(gridpane3);

		HBox pathBox = new HBox();
		pathBox.setAlignment(Pos.CENTER_LEFT);
		
		Label filePathLabel = new Label("Path");
		filePathLabel.setPrefWidth(30);
		filePathLabel.setMaxWidth(30);
		HBox.setMargin(filePathLabel, new Insets(0, 5, 10, 5));
		pathBox.getChildren().add(filePathLabel);
		
		pathField = new TextField();
		HBox.setMargin(pathField, new Insets(0, 5, 10, 5));
		HBox.setHgrow(pathField, Priority.ALWAYS);
		pathBox.getChildren().add(pathField);
		
		Text times = FontAwesomeIconFactory.get().createIcon(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TIMES, "1.5em");
		times.setStyle(times.getStyle() + "-fx-fill: red;");
		
		Text check = FontAwesomeIconFactory.get().createIcon(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CHECK, "1.5em");
		check.setStyle(check.getStyle() + "-fx-fill: green;");
		
		Label  pathValidation = new Label("");
		pathValidation.setGraphic(times);
		HBox.setMargin(pathValidation, new Insets(0, 5, 10, 5));
		pathBox.getChildren().add(pathValidation);
		
		pathField.textProperty().addListener((observable, oldValue, newValue) -> {
			File file = new File(pathField.getText());
			if (file.exists())
				pathValidation.setGraphic(check);
			else
				pathValidation.setGraphic(times);	
			
			if (marsBdvSource != null)
				marsBdvSource.setPath(pathField.getText());
		});
		
		pathButton = new Button("Browse");
		pathButton.setPrefWidth(70);
		pathButton.setMaxWidth(70);
		pathButton.setOnAction(e -> {
			final File path = (pathField.getText().trim().equals("")) ? new File(System.getProperty("user.home")) : new File(pathField.getText().trim());
			if (marsBdvSource.isN5()) {
				SwingUtilities.invokeLater(new Runnable() {
		            @Override
		            public void run() {
		            	DatasetSelectorDialog selectionDialog = new DatasetSelectorDialog(
							new N5ViewerReaderFun(),
							new N5BasePathFun(),
							path.getAbsolutePath(),
							null, // no group parsers
							new N5MetadataParser[]{
								new DefaultMetadata( "", -1 )
							});
		            	
	            			selectionDialog.setVirtualOption( false );
		            		selectionDialog.setCropOption( false );
				
		            		selectionDialog.setTreeRenderer( new N5DatasetTreeCellRenderer( true ) );
		            		
		            		//Prevents NullPointerException
		            		selectionDialog.setContainerPathUpdateCallback( x -> { });
		            		
		            		final Consumer< DataSelection > callback = (DataSelection dataSelection) -> {
		            			Platform.runLater(new Runnable() {
		            				@Override
		            				public void run() {
		            					pathField.setText(selectionDialog.getN5RootPath());
				            			n5Dataset.setText(dataSelection.metadata.get(0).getPath());
		            				}
		            	    	});
		            		};
		            		
		            		selectionDialog.run( callback );
		            }
		        });
			} else {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Select xml");
				File startingDirectory = new File(path.getParent());
				if (startingDirectory.exists())
					fileChooser.setInitialDirectory(startingDirectory);
				fileChooser.getExtensionFilters().add(new ExtensionFilter("xml file", "*.xml"));
				File file = fileChooser.showOpenDialog(getScene().getWindow());
				
				if (file != null) {
					marsBdvSource.setPath(file.getAbsolutePath());
					pathField.setText(file.getAbsolutePath());
				}
			}
		});
		HBox.setMargin(pathButton, new Insets(0, 5, 10, 5));
		pathBox.getChildren().add(pathButton);

		getChildren().add(pathBox);
		
		n5OptionsHBox = new HBox();
		n5OptionsHBox.setAlignment(Pos.CENTER_LEFT);
		
		Label n5Label = new Label("Dataset");
		HBox.setMargin(n5Label, new Insets(0, 5, 10, 5));
		n5OptionsHBox.getChildren().add(n5Label);
		
		n5Dataset = new Label("");
		HBox.setMargin(n5Dataset, new Insets(0, 5, 10, 5));
		n5OptionsHBox.getChildren().add(n5Dataset);
		
		Text times2 = FontAwesomeIconFactory.get().createIcon(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TIMES, "1.5em");
		times2.setStyle(times.getStyle() + "-fx-fill: red;");
		
		Text check2 = FontAwesomeIconFactory.get().createIcon(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CHECK, "1.5em");
		check2.setStyle(check.getStyle() + "-fx-fill: green;");
		
		Label datasetValidation = new Label("");
		datasetValidation.setGraphic(times2);
		HBox.setMargin(datasetValidation, new Insets(0, 5, 10, 5));
		n5OptionsHBox.getChildren().add(datasetValidation);
		
		pathField.textProperty().addListener((observable, oldValue, newValue) -> {
			if (marsBdvSource == null)
				return;
			
			File file = new File(pathField.getText() + "/" + marsBdvSource.getN5Dataset());
			if (file.exists())
				datasetValidation.setGraphic(check2);
			else
				datasetValidation.setGraphic(times2);	
		});
		
		Label cLabel = new Label("C");
		HBox.setMargin(cLabel, new Insets(0, 5, 10, 5));
		n5OptionsHBox.getChildren().add(cLabel);
		
		cField = new TextField();
		cField.textProperty().addListener((observable, oldValue, newValue) -> {
			if (marsBdvSource != null) {
				try {
					marsBdvSource.setChannel(Integer.valueOf(cField.getText()));
				} catch (NumberFormatException e) {
					marsBdvSource.setChannel(0);
				}
			}
		});
		cField.setPrefWidth(50);
		cField.setMaxWidth(50);
		HBox.setMargin(cField, new Insets(0, 5, 10, 5));
		n5OptionsHBox.getChildren().add(cField);
		
		getChildren().add(n5OptionsHBox);
		
		GridPane infoGridpane = new GridPane();
		
		datasetInfo = new Label();
		infoGridpane.add(datasetInfo, 0, 0);
		GridPane.setMargin(datasetInfo, new Insets(0, 5, 10, 5));
		
		getChildren().add(infoGridpane);
	}
	
	private void setAffineTransform3D(String value, int m, int n) {
		if (marsBdvSource == null)
			return;
		
		double number;
		try {
			number = Double.valueOf(value);
		} catch (NumberFormatException e) {
			return;
		}
		
		marsBdvSource.getAffineTransform3D().set(number, m, n);
	}
	
	public void setMarsBdvSource(MarsBdvSource marsBdvSource) {
		if (marsBdvSource == null) {
			this.marsBdvSource = null;
			
			m00.setText("1.0");
			m01.setText("0.0"); 
			m02.setText("0.0"); 
			m10.setText("0.0"); 
			m11.setText("1.0"); 
			m12.setText("0.0"); 
			driftCorrect.set(false);
			pathField.setText("");
			
			cField.setText("0");
			n5Dataset.setText("");
			datasetInfo.setText("");
			
			for (Node node : getChildren())
				node.setDisable(true);
		} else {
			for (Node node : getChildren())
				node.setDisable(false);
			
			this.marsBdvSource = marsBdvSource;
			m00.setText(String.valueOf(marsBdvSource.getAffineTransform3D().get(0, 0)));
			m01.setText(String.valueOf(marsBdvSource.getAffineTransform3D().get(0, 1))); 
			m02.setText(String.valueOf(marsBdvSource.getAffineTransform3D().get(0, 3))); 
			m10.setText(String.valueOf(marsBdvSource.getAffineTransform3D().get(1, 0))); 
			m11.setText(String.valueOf(marsBdvSource.getAffineTransform3D().get(1, 1))); 
			m12.setText(String.valueOf(marsBdvSource.getAffineTransform3D().get(1, 3))); 
			driftCorrect.set(marsBdvSource.getCorrectDrift());
			pathField.setText(marsBdvSource.getPath());
			
			if (marsBdvSource.isN5()) {
				if (!getChildren().contains(n5OptionsHBox)) getChildren().add(getChildren().size() - 1, n5OptionsHBox);
				cField.setText(String.valueOf(marsBdvSource.getChannel()));
				n5Dataset.setText(marsBdvSource.getN5Dataset());
				
				//Dataset information
				if (marsBdvSource.getProperties().containsKey("info"))
					datasetInfo.setText("Dimensions " + marsBdvSource.getProperties().get("info"));
			} else {
				getChildren().remove(n5OptionsHBox);
				datasetInfo.setText("");
			}
		}
	}
}
