package de.mpg.biochem.mars.fx.molecule.metadataTab;

import java.io.File;

import org.controlsfx.control.ToggleSwitch;
import de.mpg.biochem.mars.metadata.MarsBdvSource;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class BdvSourceOptionsPane extends VBox {
	private TextField m00, m01, m02, m10, m11, m12, cField, n5Field, pathField;
	private ToggleSwitch driftCorrectSwitch;
	private BooleanProperty driftCorrect = new SimpleBooleanProperty();
	private Button pathButton;
	private MarsBdvSource marsBdvSource;
	
	public BdvSourceOptionsPane() {
		setPadding(new Insets(15, 20, 15, 20));
		setSpacing(5);
		
		GridPane gridpane1 = new GridPane();
		
		//For reference...
		//Insets(double top, double right, double bottom, double left)
		
		Label m00Label = new Label("m00");
		gridpane1.add(m00Label, 0, 0);
		GridPane.setMargin(m00Label, new Insets(0, 5, 10, 5));
		
		m00 = new TextField();
		m00.textProperty().addListener((observable, oldValue, newValue) -> {
			marsBdvSource.getAffineTransform3D().set(Double.valueOf(m00.getText()), 0, 0);
		});
		m00.setPrefWidth(80);
		m00.setMaxWidth(80);
		gridpane1.add(m00, 1, 0);
		GridPane.setMargin(m00, new Insets(0, 5, 10, 5));
		
		Label m01Label = new Label("m01");
		gridpane1.add(m01Label, 2, 0);
		GridPane.setMargin(m01Label, new Insets(0, 5, 10, 5));
		
		m01 = new TextField();
		m01.textProperty().addListener((observable, oldValue, newValue) -> {
			marsBdvSource.getAffineTransform3D().set(Double.valueOf(m01.getText()), 0, 1);
		});
		m01.setPrefWidth(80);
		m01.setMaxWidth(80);
		gridpane1.add(m01, 3, 0);
		GridPane.setMargin(m01, new Insets(0, 5, 10, 5));
		
		Label m02Label = new Label("m02");
		gridpane1.add(m02Label, 4, 0);
		GridPane.setMargin(m02Label, new Insets(0, 5, 10, 5));
		
		m02 = new TextField();
		m02.textProperty().addListener((observable, oldValue, newValue) -> {
			marsBdvSource.getAffineTransform3D().set(Double.valueOf(m02.getText()), 0, 3);
		});
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
		m10.textProperty().addListener((observable, oldValue, newValue) -> {
			marsBdvSource.getAffineTransform3D().set(Double.valueOf(m10.getText()), 1, 0);
		});
		m10.setPrefWidth(80);
		m10.setMaxWidth(80);
		gridpane2.add(m10, 1, 0);
		GridPane.setMargin(m10, new Insets(0, 5, 10, 5));
		
		Label m11Label = new Label("m11");
		gridpane2.add(m11Label, 2, 0);
		GridPane.setMargin(m11Label, new Insets(0, 5, 10, 5));
		
		m11 = new TextField();
		m11.textProperty().addListener((observable, oldValue, newValue) -> {
			marsBdvSource.getAffineTransform3D().set(Double.valueOf(m11.getText()), 1, 1);
		});
		m11.setPrefWidth(80);
		m11.setMaxWidth(80);
		gridpane2.add(m11, 3, 0);
		GridPane.setMargin(m11, new Insets(0, 5, 10, 5));
		
		Label m12Label = new Label("m12");
		gridpane2.add(m12Label, 4, 0);
		GridPane.setMargin(m12Label, new Insets(0, 5, 10, 5));
		
		m12 = new TextField();
		m12.textProperty().addListener((observable, oldValue, newValue) -> {
			marsBdvSource.getAffineTransform3D().set(Double.valueOf(m12.getText()), 1, 3);
		});
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
			marsBdvSource.setCorrectDrift(newValue);
		});
		
		Label driftCorrectLabel = new Label("Drift Correct");
		gridpane3.add(driftCorrectLabel, 0, 0);
		GridPane.setMargin(driftCorrectLabel, new Insets(0, 5, 10, 5));

		gridpane3.add(driftCorrectSwitch, 1, 0);
		GridPane.setMargin(driftCorrectSwitch, new Insets(0, 5, 10, 5));
		
		getChildren().add(gridpane3);

		GridPane gridpane4 = new GridPane();
		
		Label filePathLabel = new Label("Path");
		gridpane4.add(filePathLabel, 0, 0);
		GridPane.setMargin(filePathLabel, new Insets(0, 5, 10, 5));
		
		pathField = new TextField();
		pathField.setPrefWidth(300);
		pathField.setMaxWidth(300);
		gridpane4.add(pathField, 1, 0);
		GridPane.setMargin(pathField, new Insets(0, 5, 10, 5));
		
		pathButton = new Button("Browse");
		pathButton.setOnAction(e -> {
			File path;
			if (marsBdvSource.isN5()) {
				DirectoryChooser directoryChooser = new DirectoryChooser();
				directoryChooser.setTitle("Select N5 directory");
				directoryChooser.setInitialDirectory(new File(pathField.getText()));
				path = directoryChooser.showDialog(getScene().getWindow());
			} else {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Select xml");
				fileChooser.setInitialDirectory(new File(pathField.getText()));
				fileChooser.getExtensionFilters().add(new ExtensionFilter("xml file", "*.xml"));
				path = fileChooser.showOpenDialog(getScene().getWindow());
			}

			if (path != null) {
				marsBdvSource.setPath(path.getAbsolutePath());
				pathField.setText(path.getAbsolutePath());
			}
		});
		gridpane4.add(pathButton, 2, 0);
		GridPane.setMargin(pathButton, new Insets(0, 5, 10, 5));
		
		getChildren().add(gridpane4);
		
		GridPane gridpane5 = new GridPane();
		
		Label n5Label = new Label("N5 Dataset");
		gridpane5.add(n5Label, 4, 0);
		GridPane.setMargin(n5Label, new Insets(0, 5, 10, 5));
		
		n5Field = new TextField();
		n5Field.textProperty().addListener((observable, oldValue, newValue) -> {
			marsBdvSource.setN5Dataset(n5Field.getText());
		});
		gridpane5.add(n5Field, 5, 0);
		GridPane.setMargin(n5Field, new Insets(0, 5, 10, 5));
		
		Label cLabel = new Label("C");
		gridpane5.add(cLabel, 2, 0);
		GridPane.setMargin(cLabel, new Insets(0, 5, 10, 5));
		
		cField = new TextField();
		cField.textProperty().addListener((observable, oldValue, newValue) -> {
			marsBdvSource.setChannel(Integer.valueOf(cField.getText()));
		});
		cField.setPrefWidth(50);
		cField.setMaxWidth(50);
		gridpane5.add(cField, 3, 0);
		GridPane.setMargin(cField, new Insets(0, 5, 10, 5));
		
		getChildren().add(gridpane5);
	}
	
	public void setMarsBdvSource(MarsBdvSource marsBdvSource) {
		if (marsBdvSource == null)
			return;
		
		this.marsBdvSource = marsBdvSource;
		m00.setText(String.valueOf(marsBdvSource.getAffineTransform3D().get(0, 0)));
		m01.setText(String.valueOf(marsBdvSource.getAffineTransform3D().get(0, 1))); 
		m02.setText(String.valueOf(marsBdvSource.getAffineTransform3D().get(0, 3))); 
		m10.setText(String.valueOf(marsBdvSource.getAffineTransform3D().get(1, 0))); 
		m11.setText(String.valueOf(marsBdvSource.getAffineTransform3D().get(1, 1))); 
		m12.setText(String.valueOf(marsBdvSource.getAffineTransform3D().get(1, 3))); 
		n5Field.setText(marsBdvSource.getN5Dataset());
		driftCorrect.set(marsBdvSource.getCorrectDrift());
		pathField.setText(marsBdvSource.getPath());
	}
}
