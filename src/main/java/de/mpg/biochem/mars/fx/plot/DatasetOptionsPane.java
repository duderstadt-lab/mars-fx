package de.mpg.biochem.mars.fx.plot;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.COG;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.MINUS;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLUS;

import java.util.ArrayList;
import java.util.Arrays;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubTab;
import de.mpg.biochem.mars.fx.options.Options;
import de.mpg.biochem.mars.fx.table.TableSubTab;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.table.MARSResultsTable;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import org.tbee.javafx.scene.layout.fxml.MigPane;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXColorPicker;
import com.jfoenix.controls.JFXTextField;

public class DatasetOptionsPane extends MigPane implements TableSubTab {
	private TextField titleField, yNameField;
	private Button removeButton, addButton, updateButton;
	private ComboBox<String> xColumnField;
	
	private TableView<PlotSeries> plotPropertiesTable;
	
	private ObservableList<PlotSeries> plotSeriesList = FXCollections.observableArrayList();
	
	private MARSResultsTable table;
	
	private SubPlot subPlot;

	public DatasetOptionsPane(MARSResultsTable table, SubPlot subPlot) {
		this.table = table;
		this.subPlot = subPlot;
		
		setLayout("insets dialog");
		
		initComponents();
		
		add(new Label("Title "), "");
		add(titleField, "wrap");
		add(new Label("Y Axis "), "");
		add(yNameField, "wrap");
		add(new Label("X Axis Column"), "");
		add(xColumnField, "wrap");
		
		add(plotPropertiesTable, "span, wrap");
		
		add(addButton, "split 2");
		add(removeButton, "");
		add(updateButton, "right");
	}
	
	private void initComponents() {
		titleField = new TextField();
		yNameField = new TextField();
		xColumnField = new ComboBox<>();
		
		plotPropertiesTable = new TableView<PlotSeries>();
		
		plotPropertiesTable.prefHeightProperty().set(200);
		plotPropertiesTable.prefWidthProperty().set(400);
        
        TableColumn<PlotSeries, ComboBox<String>> typeColumn = new TableColumn<>("Type");
		typeColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getTypeField()));
		typeColumn.setMinWidth(100);
		typeColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(typeColumn);
        typeColumn.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<PlotSeries, ComboBox<String>> yValuesColumn = new TableColumn<>("Y Values");
        yValuesColumn.setMinWidth(100);
        yValuesColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().yColumnField()));
        yValuesColumn.setStyle("-fx-alignment: CENTER;");

        yValuesColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(yValuesColumn);
        
        TableColumn<PlotSeries, JFXColorPicker> colorColumn = new TableColumn<>("Color");
        colorColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getColorField()));
        colorColumn.setMinWidth(50);
        colorColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(colorColumn);
        colorColumn.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<PlotSeries, JFXTextField> strokeColumn = new TableColumn<>("Stroke");
        strokeColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getWidthField()));
        strokeColumn.setMinWidth(100);
        strokeColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(strokeColumn);
        strokeColumn.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<PlotSeries, JFXCheckBox> drawSegmentsColumn = new TableColumn<>("Segments");
        drawSegmentsColumn.setMinWidth(50);
        drawSegmentsColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getDrawSegmentsField()));
        drawSegmentsColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(drawSegmentsColumn);
        drawSegmentsColumn.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<PlotSeries, JFXColorPicker> segmentsColorColumn = new TableColumn<>("Segment Color");
        drawSegmentsColumn.setMinWidth(100);
        segmentsColorColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getSegmentsColorField()));
        segmentsColorColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(segmentsColorColumn);
        segmentsColorColumn.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<PlotSeries, JFXTextField> segmentsStrokeColumn = new TableColumn<>("Segment Stroke");
        segmentsStrokeColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getSegmentsWidthField()));
        segmentsStrokeColumn.setMinWidth(100);
        segmentsStrokeColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(segmentsStrokeColumn);
        segmentsStrokeColumn.setStyle("-fx-alignment: CENTER;");
        
        plotPropertiesTable.setItems(plotSeriesList);
		
		removeButton = new Button();
		removeButton.setGraphic(FontAwesomeIconFactory.get().createIcon(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.MINUS, "1.0em"));
		removeButton.setCenterShape(true);
		removeButton.setStyle(
                "-fx-background-radius: 5em; " +
                "-fx-min-width: 20px; " +
                "-fx-min-height: 20px; " +
                "-fx-max-width: 20px; " +
                "-fx-max-height: 20px;"
        );
		removeButton.setOnAction(e -> {
			if(plotPropertiesTable.getSelectionModel().getSelectedIndex() != -1)
				plotSeriesList.remove(plotPropertiesTable.getSelectionModel().getSelectedIndex());
		});
		
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
			PlotSeries defaultPlotSeries = new PlotSeries(table, xColumnField);
			plotSeriesList.add(defaultPlotSeries);
		});

		Text syncIcon = OctIconFactory.get().createIcon(de.jensd.fx.glyphs.octicons.OctIcon.SYNC, "1.0em");
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
		     subPlot.update();
		});
		
		/*
		RotateTransition rt = new RotateTransition(Duration.millis(500), updateLabel);
		rt.setInterpolator(Interpolator.LINEAR);
		rt.setByAngle(0);
		rt.setByAngle(360);
	    rt.setCycleCount(Animation.INDEFINITE);
	     
		updateLabel.setOnMouseClicked(e -> {
		     //rt.play();
		     Task<Void> spin = new Task<Void>() {
	            @Override
	            protected Void call() throws Exception {
	    		     rt.setByAngle(360);
	    		     rt.setCycleCount(10000);
	    		     rt.play();
	                return null;
	            }
	         };
	         spin.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
	            @Override
	            public void handle(WorkerStateEvent event) {
	            	
	            }
	         });
	          new Thread(spin).start();
	          
	        
		     subPlot.update();
	         //rt.stop();
		});
		*/
	}
	
	@Override
	public void setTable(MARSResultsTable table) {
		this.table = table;
		
		String xSelection = xColumnField.getSelectionModel().getSelectedItem();
		xColumnField.getItems().clear();
		xColumnField.getItems().addAll(table.getColumnHeadings());
		xColumnField.getSelectionModel().select(xSelection);
		
		for (PlotSeries propertiesRow : plotSeriesList) {
			String ySelection = propertiesRow.yColumnField().getSelectionModel().getSelectedItem();
			propertiesRow.yColumnField().getItems().clear();
			propertiesRow.yColumnField().getItems().addAll(table.getColumnHeadings());
			if (ySelection != null)
				propertiesRow.yColumnField().getSelectionModel().select(ySelection);
		}
	}
	
	public ObservableList<PlotSeries> getPlotSeriesList() {
		return plotSeriesList;
	}
	
	public String getTitle() {
		return titleField.getText();
	}
	
	public String getYAxisName() {
		return yNameField.getText();
	}
	
	public ComboBox<String> xColumnField() {
		return xColumnField;
	}
	
	public String getXAxisName() {
		if (xColumnField.getSelectionModel().getSelectedIndex() != -1)
			return xColumnField.getSelectionModel().getSelectedItem();
		else
			return "";
	}
	
	public void setSubPlot(SubPlot subPlot) {
		this.subPlot = subPlot;
	}
}

