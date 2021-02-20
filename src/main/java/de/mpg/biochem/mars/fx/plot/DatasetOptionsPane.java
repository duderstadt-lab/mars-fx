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
package de.mpg.biochem.mars.fx.plot;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import org.apache.batik.bridge.ViewBox;
import org.controlsfx.control.ToggleSwitch;

import static java.util.stream.Collectors.toList;

import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
import de.mpg.biochem.mars.fx.options.Options;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.table.MarsTable;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import de.gsi.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;

import javafx.scene.Node;
import javafx.scene.layout.Priority;

//import org.tbee.javafx.scene.layout.fxml.MigPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXColorPicker;
import com.jfoenix.controls.JFXTextField;

public class DatasetOptionsPane extends VBox {
	private TextField titleField, xNameField, yNameField, yMinField, yMaxField;
	private Button addButton, updateButton;
	private RadioButton radioButtonMolecules, radioButtonMetadata, radioButtonNone;
	private ToggleGroup indicatorSourceGroup;
	
	private ToggleSwitch fixYBoundsSwitch;
	private BooleanProperty fixYBounds = new SimpleBooleanProperty();
	
	private TableView<PlotSeries> plotPropertiesTable;
	
	private ObservableList<PlotSeries> plotSeriesList = FXCollections.observableArrayList();
	
	private ToggleGroup trackingGroup;
	
	private SubPlot subPlot;
	
	private ArrayList<String> columns;
	private boolean marsTableDisplay = false;
	
	public DatasetOptionsPane(Set<String> columns, SubPlot subPlot, boolean marsTableDisplay) {
		this.subPlot = subPlot;
		this.marsTableDisplay = marsTableDisplay;
		this.columns = (ArrayList<String>)columns.stream().sorted().collect(toList());
		
		buildOptionsPane();
	}

	public DatasetOptionsPane(Set<String> columns, SubPlot subPlot) {
		this.subPlot = subPlot;
		this.columns = (ArrayList<String>)columns.stream().sorted().collect(toList());
		this.marsTableDisplay = false;
		
		buildOptionsPane();
	}
	
	private void buildOptionsPane() {
		trackingGroup = new ToggleGroup();
		
		setPadding(new Insets(15, 20, 15, 20));
		setSpacing(5);
		
		initComponents();
		
		GridPane gridpane1 = new GridPane();
		
		//For reference...
		//Insets(double top, double right, double bottom, double left)
		
		Label title = new Label("Title");
		gridpane1.add(title, 0, 0);
		GridPane.setMargin(title, new Insets(0, 5, 10, 5));
		
		gridpane1.add(titleField, 1, 0);
		GridPane.setMargin(titleField, new Insets(0, 5, 10, 5));
		
		Label xAxisLabel = new Label("X Axis");
		gridpane1.add(xAxisLabel, 2, 0);
		GridPane.setMargin(xAxisLabel, new Insets(0, 5, 10, 5));
		
		gridpane1.add(xNameField, 3, 0);
		GridPane.setMargin(xNameField, new Insets(0, 5, 10, 5));
		
		Label yAxisLabel = new Label("Y Axis");
		gridpane1.add(yAxisLabel, 4, 0);
		GridPane.setMargin(yAxisLabel, new Insets(0, 5, 10, 5));
		
		gridpane1.add(yNameField, 5, 0);
		GridPane.setMargin(yNameField, new Insets(0, 5, 10, 5));
		
		getChildren().add(gridpane1);
		
		//Option to lock Y-range
		GridPane gridpane2 = new GridPane();
		
		Label fixYBoundsLabel = new Label("Fix Y Bounds");
		gridpane2.add(fixYBoundsLabel, 0, 0);
		GridPane.setMargin(fixYBoundsLabel, new Insets(0, 5, 10, 5));

		gridpane2.add(fixYBoundsSwitch, 1, 0);
		GridPane.setMargin(fixYBoundsSwitch, new Insets(0, 5, 10, 5));
		
		Label yMinLabel = new Label("Y Min");
		gridpane2.add(yMinLabel, 2, 0);
		GridPane.setMargin(yMinLabel, new Insets(0, 5, 10, 5));
		
		gridpane2.add(yMinField, 3, 0);
		GridPane.setMargin(yMinField, new Insets(0, 5, 10, 5));
		
		Label yMaxLabel = new Label("Y Max");
		gridpane2.add(yMaxLabel, 4, 0);
		GridPane.setMargin(yMaxLabel, new Insets(0, 5, 10, 5));
		
		gridpane2.add(yMaxField, 5, 0);
		GridPane.setMargin(yMaxField, new Insets(0, 5, 10, 5));
		
		getChildren().add(gridpane2);
		
		EventHandler<KeyEvent> handleYFieldEnter = new EventHandler<KeyEvent>() {
	        @Override
	        public void handle(KeyEvent ke) {
	            if (ke.getCode().equals(KeyCode.ENTER)) {
	            	if (!fixYBounds.get())
	            		fixYBounds.set(true);
	            }
	        }
		};
		
		yMinField.setOnKeyPressed(handleYFieldEnter);
		yMaxField.setOnKeyPressed(handleYFieldEnter);
		
		//add(Node child, int columnIndex, int rowIndex, int colspan, int rowspan)
		GridPane gridpane3 = new GridPane();
		
		Label indicatorLabel = new Label("Indicators");
		gridpane3.add(indicatorLabel, 0, 1);
		GridPane.setMargin(indicatorLabel, new Insets(0, 5, 10, 5));
		
		HBox radioButtons = new HBox(radioButtonMolecules, radioButtonMetadata, radioButtonNone);
		gridpane3.add(radioButtons, 1, 1);
		GridPane.setMargin(radioButtons, new Insets(0, 5, 10, 5));
		
		if (!marsTableDisplay)
			getChildren().add(gridpane3);
		
		VBox.setVgrow(plotPropertiesTable, Priority.ALWAYS);
		getChildren().add(plotPropertiesTable);
		
		BorderPane bottomButtons = new BorderPane();
		bottomButtons.setLeft(addButton);
		bottomButtons.setRight(updateButton);
		
		getChildren().add(bottomButtons);
	}
	
	private void initComponents() {
		titleField = new TextField();
		yNameField = new TextField();
		xNameField = new TextField();
		
		fixYBoundsSwitch = new ToggleSwitch();
		fixYBounds.setValue(false);
		fixYBoundsSwitch.selectedProperty().bindBidirectional(fixYBounds);
		
		yMinField = new TextField();
		yMaxField = new TextField();
		
        radioButtonMolecules = new RadioButton("Molecules");
        radioButtonMetadata = new RadioButton("Metadata");
        radioButtonNone = new RadioButton("None");

        indicatorSourceGroup = new ToggleGroup();

        radioButtonMolecules.setToggleGroup(indicatorSourceGroup);
        radioButtonMetadata.setToggleGroup(indicatorSourceGroup);
        radioButtonNone.setToggleGroup(indicatorSourceGroup);
        
        //Default Indicator setting
        radioButtonMolecules.setSelected(true);
		
		plotPropertiesTable = new TableView<PlotSeries>();
		
		TableColumn<PlotSeries, PlotSeries> deleteColumn = new TableColumn<>();
    	deleteColumn.setPrefWidth(30);
    	deleteColumn.setMinWidth(30);
    	deleteColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
    	deleteColumn.setCellFactory(param -> new TableCell<PlotSeries, PlotSeries>() {
            private final Button removeButton = new Button();

            @Override
            protected void updateItem(PlotSeries plotSeries, boolean empty) {
                super.updateItem(plotSeries, empty);

                if (plotSeries == null) {
                    setGraphic(null);
                    return;
                }
                
                removeButton.setGraphic(FontAwesomeIconFactory.get().createIcon(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.MINUS, "1.0em"));
        		removeButton.setCenterShape(true);
        		removeButton.setStyle(
                        "-fx-background-radius: 5em; " +
                        "-fx-min-width: 18px; " +
                        "-fx-min-height: 18px; " +
                        "-fx-max-width: 18px; " +
                        "-fx-max-height: 18px;"
                );
        		
                setGraphic(removeButton);
                removeButton.setOnAction(e -> {
        			plotSeriesList.remove(plotSeries);
        		});
            }
        });
    	deleteColumn.setStyle( "-fx-alignment: CENTER;");
    	deleteColumn.setSortable(false);
    	plotPropertiesTable.getColumns().add(deleteColumn);
    	
    	TableColumn<PlotSeries, RadioButton> trackColumn = new TableColumn<>("Track");
    	trackColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getTrackingButton()));
    	trackColumn.setMinWidth(30);
    	trackColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(trackColumn);
        trackColumn.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<PlotSeries, ComboBox<String>> typeColumn = new TableColumn<>("Type");
		typeColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getTypeField()));
		typeColumn.setMinWidth(100);
		typeColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(typeColumn);
        typeColumn.setStyle("-fx-alignment: CENTER;");
		
		TableColumn<PlotSeries, ComboBox<String>> xValuesColumn = new TableColumn<>("X Values");
        xValuesColumn.setMinWidth(150);
        xValuesColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().xColumnField()));
        xValuesColumn.setStyle("-fx-alignment: CENTER;");
        
        xValuesColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(xValuesColumn);
        
        TableColumn<PlotSeries, ComboBox<String>> yValuesColumn = new TableColumn<>("Y Values");
        yValuesColumn.setMinWidth(150);
        yValuesColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().yColumnField()));
        yValuesColumn.setStyle("-fx-alignment: CENTER;");

        yValuesColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(yValuesColumn);
        
        TableColumn<PlotSeries, ComboBox<String>> lineStyleColumn = new TableColumn<>("Style");
        lineStyleColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().lineStyle()));
        lineStyleColumn.setMinWidth(100);
        lineStyleColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(lineStyleColumn);
        lineStyleColumn.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<PlotSeries, JFXColorPicker> colorColumn = new TableColumn<>("Color");
        colorColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getColorField()));
        colorColumn.setMinWidth(100);
        colorColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(colorColumn);
        colorColumn.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<PlotSeries, JFXTextField> strokeColumn = new TableColumn<>("Stroke");
        strokeColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getWidthField()));
        strokeColumn.setMinWidth(30);
        strokeColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(strokeColumn);
        strokeColumn.setStyle("-fx-alignment: CENTER;");
        
        if (!marsTableDisplay) {
	        TableColumn<PlotSeries, JFXCheckBox> drawSegmentsColumn = new TableColumn<>("Segments");
	        drawSegmentsColumn.setMinWidth(40);
	        drawSegmentsColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getDrawSegmentsField()));
	        drawSegmentsColumn.setSortable(false);
	        plotPropertiesTable.getColumns().add(drawSegmentsColumn);
	        drawSegmentsColumn.setStyle("-fx-alignment: CENTER;");
	        
	        TableColumn<PlotSeries, JFXColorPicker> segmentsColorColumn = new TableColumn<>("Segment Color");
	        segmentsColorColumn.setMinWidth(110);
	        segmentsColorColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getSegmentsColorField()));
	        segmentsColorColumn.setSortable(false);
	        plotPropertiesTable.getColumns().add(segmentsColorColumn);
	        segmentsColorColumn.setStyle("-fx-alignment: CENTER;");
	        
	        TableColumn<PlotSeries, JFXTextField> segmentsStrokeColumn = new TableColumn<>("Segment Stroke");
	        segmentsStrokeColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getSegmentsWidthField()));
	        segmentsStrokeColumn.setMinWidth(110);
	        segmentsStrokeColumn.setSortable(false);
	        plotPropertiesTable.getColumns().add(segmentsStrokeColumn);
	        segmentsStrokeColumn.setStyle("-fx-alignment: CENTER;");
        }
        
        plotPropertiesTable.setItems(plotSeriesList);
		
		addButton = new Button();
		addButton.setGraphic(FontAwesomeIconFactory.get().createIcon(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLUS, "1.0em"));
		addButton.setCenterShape(true);
		addButton.setStyle(
                "-fx-background-radius: 5em; " +
                "-fx-min-width: 20px; " +
                "-fx-min-height: 20px; " +
                "-fx-max-width: 20px; " +
                "-fx-max-height: 20px;"
        );
		addButton.setOnAction(e -> {
			if (columns != null) {
				PlotSeries defaultPlotSeries = new PlotSeries(columns);
				defaultPlotSeries.getTrackingButton().setToggleGroup(trackingGroup);
				if (plotSeriesList.size() < 1)
					defaultPlotSeries.getTrackingButton().selectedProperty().set(true);
				plotSeriesList.add(defaultPlotSeries);
			}
		});

		Text syncIcon = OctIconFactory.get().createIcon(REFRESH, "1.0em");
		updateButton = new Button();
		updateButton.setGraphic(syncIcon);
		updateButton.setCenterShape(true);
		updateButton.setStyle(
                "-fx-background-radius: 5em; " +
                "-fx-min-width: 20px; " +
                "-fx-min-height: 20px; " +
                "-fx-max-width: 20px; " +
                "-fx-max-height: 20px;"
        );
		updateButton.setOnMouseClicked(e -> {
			if (plotSeriesList.size() > 0) {
				PlotSeries plotSeries = plotSeriesList.get(0);
				
				if (xNameField.getText().equals(""))
					xNameField.setText(plotSeries.getXColumn());
				if (yNameField.getText().equals(""))
					yNameField.setText(plotSeries.getYColumn());
			}
		    subPlot.update();
		});
	}
	
	public void setColumns(Set<String> columns) {
		this.columns = (ArrayList<String>)columns.stream().sorted().collect(toList());
		
		for (PlotSeries propertiesRow : plotSeriesList) {
			String xSelection = propertiesRow.xColumnField().getSelectionModel().getSelectedItem();
			propertiesRow.xColumnField().getItems().clear();
			propertiesRow.xColumnField().getItems().addAll(columns);
			if (xSelection != null)
				propertiesRow.xColumnField().getSelectionModel().select(xSelection);
			
			String ySelection = propertiesRow.yColumnField().getSelectionModel().getSelectedItem();
			propertiesRow.yColumnField().getItems().clear();
			propertiesRow.yColumnField().getItems().addAll(columns);
			if (ySelection != null)
				propertiesRow.yColumnField().getSelectionModel().select(ySelection);
		}
	}
	
	public XYChart getTrackingChart() {
		for (PlotSeries series : getPlotSeriesList()) {
			if (series.track())
				return series.getChart();
		}
		
		return null;
	}
	
	public PlotSeries getTrackingSeries() {
		for (PlotSeries series : getPlotSeriesList())
			if (series.track())
				return series;

		return null;
	}
	
	public ObservableList<PlotSeries> getPlotSeriesList() {
		return plotSeriesList;
	}
	
	public String getTitle() {
		return titleField.getText();
	}
	
	public void setTitle(String title) {
		titleField.setText(title);
	}
	
	public String getXAxisName() {
		return xNameField.getText();
	}
	
	public void setXAxisName(String name) {
		xNameField.setText(name);
	}

	public void setYAxisName(String name) {
		yNameField.setText(name);
	}
	
	public String getYAxisName() {
		return yNameField.getText();
	}
	
	BooleanProperty fixYBounds() {
		return fixYBounds;
	}
	
	void setYMin(double yMin) {
		yMinField.setText(String.valueOf(yMin));
	}
	
	double getYMin() {
		return Double.valueOf(yMinField.getText());
	}
	
	void setYMax(double yMax) {
		yMaxField.setText(String.valueOf(yMax));
	}
	
	double getYMax() {
		return Double.valueOf(yMaxField.getText());
	}
	
	public void setSubPlot(SubPlot subPlot) {
		this.subPlot = subPlot;
		
		subPlot.getYAxis().minProperty().addListener((ob, o, n) -> yMinField.setText(String.valueOf(n.doubleValue())));
		subPlot.getYAxis().maxProperty().addListener((ob, o, n) -> yMaxField.setText(String.valueOf(n.doubleValue())));
	}
	
	public String getSelectedIndicator() {
		if (isMoleculeIndicators())
			return "Molecules";
		else if (isMetadataIndicators())
			return "Metadata";
		else if (isNoneIndicators())
			return "None";
		
		return "";
	}
	
	public void setSelectedIndicator(String name) {
		if (name.equals("Molecules"))
			indicatorSourceGroup.selectToggle(radioButtonMolecules);
		else if (name.equals("Metadata"))
			indicatorSourceGroup.selectToggle(radioButtonMetadata);
		else if (name.equals("None"))
			indicatorSourceGroup.selectToggle(radioButtonNone);
	}
	
	public boolean isMoleculeIndicators() {
		return indicatorSourceGroup.getSelectedToggle().equals(radioButtonMolecules);
	}
	
	public boolean isMetadataIndicators() {
		return indicatorSourceGroup.getSelectedToggle().equals(radioButtonMetadata);
	}
	
	public boolean isNoneIndicators() {
		return indicatorSourceGroup.getSelectedToggle().equals(radioButtonNone);
	}
}

