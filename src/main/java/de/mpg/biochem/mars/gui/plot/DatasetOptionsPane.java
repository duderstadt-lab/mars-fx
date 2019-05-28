package de.mpg.biochem.mars.gui.plot;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.COG;

import java.util.ArrayList;
import java.util.Arrays;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.gui.molecule.moleculesTab.MoleculeSubTab;
import de.mpg.biochem.mars.gui.options.Options;
import de.mpg.biochem.mars.molecule.Molecule;
import javafx.animation.RotateTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.scene.text.Text;

import org.tbee.javafx.scene.layout.fxml.MigPane;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXColorPicker;
import com.jfoenix.controls.JFXTextField;

public class DatasetOptionsPane extends MigPane implements MoleculeSubTab  {
	private TextField titleField, yNameField;
	private JFXButton removeButton, addButton, updateButton;
	private ComboBox<String> xColumnField;
	
	private TableView<PlotSeries> plotPropertiesTable;
	
	private ObservableList<PlotSeries> plotSeriesList = FXCollections.observableArrayList();
	
	private Molecule molecule;
	
	private SubPlot subPlot;

	public DatasetOptionsPane(Molecule molecule, SubPlot subPlot) {
		this.molecule = molecule;
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
        
        TableColumn<PlotSeries, ComboBox<String>> typeColumn = new TableColumn<>("Type");
		typeColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getTypeField()));
		typeColumn.setPrefWidth(70);
		typeColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(typeColumn);
        typeColumn.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<PlotSeries, ComboBox<String>> yValuesColumn = new TableColumn<>("Y Values");
        yValuesColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().yColumnField()));
        yValuesColumn.setStyle("-fx-alignment: CENTER;");

        yValuesColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(yValuesColumn);
        
        TableColumn<PlotSeries, JFXColorPicker> colorColumn = new TableColumn<>("Color");
        colorColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getColorField()));
        //colorColumn.setPrefWidth(70);
        colorColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(colorColumn);
        colorColumn.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<PlotSeries, JFXTextField> strokeColumn = new TableColumn<>("Stroke");
        strokeColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getWidthField()));
        strokeColumn.setPrefWidth(40);
        strokeColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(strokeColumn);
        strokeColumn.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<PlotSeries, JFXCheckBox> drawSegmentsColumn = new TableColumn<>("Segments");
        drawSegmentsColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getDrawSegmentsField()));
        drawSegmentsColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(drawSegmentsColumn);
        drawSegmentsColumn.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<PlotSeries, JFXColorPicker> segmentsColorColumn = new TableColumn<>("Segment Color");
        segmentsColorColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getSegmentsColorField()));
        segmentsColorColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(segmentsColorColumn);
        segmentsColorColumn.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<PlotSeries, JFXTextField> segmentsStrokeColumn = new TableColumn<>("Segment Stroke");
        segmentsStrokeColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getSegmentsWidthField()));
        segmentsStrokeColumn.setPrefWidth(40);
        segmentsStrokeColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(segmentsStrokeColumn);
        segmentsStrokeColumn.setStyle("-fx-alignment: CENTER;");
        
        plotPropertiesTable.setItems(plotSeriesList);
		
		removeButton = new JFXButton();
		removeButton.setGraphic(FontAwesomeIconFactory.get().createIcon(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.MINUS_CIRCLE, "1.3em"));
		removeButton.setOnAction(e -> {
			if(plotPropertiesTable.getSelectionModel().getSelectedIndex() != -1)
				plotSeriesList.remove(plotPropertiesTable.getSelectionModel().getSelectedIndex());
		});
		
		addButton = new JFXButton();
		addButton.setGraphic(FontAwesomeIconFactory.get().createIcon(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLUS_CIRCLE, "1.3em"));
		addButton.setOnAction(e -> {
			PlotSeries defaultPlotSeries = new PlotSeries(molecule.getDataTable(), xColumnField);
			plotSeriesList.add(defaultPlotSeries);
		});
		
		updateButton = new JFXButton();
		Text syncIcon = OctIconFactory.get().createIcon(de.jensd.fx.glyphs.octicons.OctIcon.SYNC, "1.3em");
		updateButton.setGraphic(syncIcon);
		updateButton.setOnAction(e -> {
			 RotateTransition rt = new RotateTransition(Duration.millis(500), syncIcon);
		     rt.setByAngle(360);
		     rt.setCycleCount(4);
		     rt.play();
		     subPlot.update();
		     rt.stop();
		});
	}
	
	@Override
	public void setMolecule(Molecule molecule) {
		this.molecule = molecule;
		
		String xSelection = xColumnField.getSelectionModel().getSelectedItem();
		xColumnField.getItems().clear();
		xColumnField.getItems().addAll(molecule.getDataTable().getColumnHeadings());
		xColumnField.getSelectionModel().select(xSelection);
		
		for (PlotSeries propertiesRow : plotSeriesList) {
			String ySelection = propertiesRow.yColumnField().getSelectionModel().getSelectedItem();
			propertiesRow.yColumnField().getItems().clear();
			propertiesRow.yColumnField().getItems().addAll(molecule.getDataTable().getColumnHeadings());
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

