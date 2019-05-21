package de.mpg.biochem.mars.gui.molecule.moleculesTab;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.COG;

import java.util.ArrayList;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.gui.options.Options;
import de.mpg.biochem.mars.gui.plot.Plot;
import de.mpg.biochem.mars.gui.plot.PlotProperties;
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

public class DatasetOptionsPane extends MigPane implements MoleculeSubTab  {
	
	private TextField titleField, xNameField, yNameField;
	private JFXButton removeButton, addButton, updateButton;
	private ComboBox<String> xColumnField;
	
	private TableView<PlotPropertiesRow> plotPropertiesTable;
	
	private ObservableList<PlotPropertiesRow> plotPropertiesRowList = FXCollections.observableArrayList();
	
	protected static Color[] colors = {Color.BLACK, Color.BLUE, Color.CYAN, Color.GRAY, Color.GREEN, Color.MAGENTA, Color.ORANGE, Color.PINK, Color.RED, Color.YELLOW};
	
	protected static String[] types = {"Line","Scatter","Bar"};
	
	private Molecule molecule;
	private Plot plot;

	public DatasetOptionsPane(Molecule molecule, Plot plot) {
		this.molecule = molecule;
		this.plot = plot;
		
		plotPropertiesRowList.add(new PlotPropertiesRow(new PlotProperties("slice", "x", Color.BLACK, "Line", Color.RED)));
		
		setLayout("insets dialog");
		
		initComponents();
		
		add(new Label("Title "), "");
		add(titleField, "wrap");
		add(new Label("X Axis "), "");
		add(xNameField, "wrap");
		add(new Label("Y Axis "), "");
		add(yNameField, "wrap");
		add(new Label("X Axis Column"), "");
		add(xColumnField, "wrap");
		
		add(plotPropertiesTable, "span, wrap");
		
		add(addButton, "left");
		add(removeButton, "left");
		add(updateButton, "right");
	}
	
	private void initComponents() {
		titleField = new TextField();
		xNameField = new TextField();
		yNameField = new TextField();
		xColumnField = new ComboBox<>();
		
		plotPropertiesTable = new TableView<PlotPropertiesRow>();
        
        TableColumn<PlotPropertiesRow, ComboBox<String>> typeColumn = new TableColumn<>("Type");
		typeColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getTypeField()));
		typeColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(typeColumn);
        typeColumn.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<PlotPropertiesRow, ComboBox<String>> yValuesColumn = new TableColumn<>("Y Values");
        yValuesColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().yColumnField()));
        yValuesColumn.setStyle("-fx-alignment: CENTER;");

        yValuesColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(yValuesColumn);
        
        TableColumn<PlotPropertiesRow, ComboBox<Color>> colorColumn = new TableColumn<>("Color");
        colorColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getColorField()));
        //colorColumn.setPrefWidth(70);
        colorColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(colorColumn);
        colorColumn.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<PlotPropertiesRow, JFXCheckBox> drawSegmentsColumn = new TableColumn<>("Segments");
        drawSegmentsColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getDrawSegmentsField()));
        //colorColumn.setPrefWidth(50);
        drawSegmentsColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(drawSegmentsColumn);
        drawSegmentsColumn.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<PlotPropertiesRow, ComboBox<Color>> segmentsColorColumn = new TableColumn<>("Segment Color");
        segmentsColorColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getSegmentsColor()));
        segmentsColorColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(segmentsColorColumn);
        segmentsColorColumn.setStyle("-fx-alignment: CENTER;");
        
        plotPropertiesTable.setItems(plotPropertiesRowList);
		
		removeButton = new JFXButton();
		removeButton.setGraphic(FontAwesomeIconFactory.get().createIcon(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.MINUS_CIRCLE, "1.3em"));
		removeButton.setOnAction(e -> {
			if(plotPropertiesTable.getSelectionModel().getSelectedIndex() != -1)
				plotPropertiesRowList.remove(plotPropertiesTable.getSelectionModel().getSelectedIndex());
		});
		
		addButton = new JFXButton();
		addButton.setGraphic(FontAwesomeIconFactory.get().createIcon(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLUS_CIRCLE, "1.3em"));
		addButton.setOnAction(e -> {
			plotPropertiesRowList.add(new PlotPropertiesRow(new PlotProperties("slice", "x", Color.BLACK, "Line", Color.RED)));
		});
		
		updateButton = new JFXButton();
		Text syncIcon = OctIconFactory.get().createIcon(de.jensd.fx.glyphs.octicons.OctIcon.SYNC, "1.3em");
		updateButton.setGraphic(syncIcon);
		updateButton.setOnAction(e -> {
			RotateTransition rt = new RotateTransition(Duration.millis(500), syncIcon);
		     rt.setByAngle(360);
		     rt.setCycleCount(1);
		     rt.play();
		     updatePlot();
		     //rt.stop();
		});
	}
	
	public void updatePlot() {
		plot.clear();
		for (PlotPropertiesRow propertiesRow : plotPropertiesRowList) {
			if (xColumnField.getSelectionModel().getSelectedIndex() != -1 
				&& propertiesRow.yColumnField().getSelectionModel().getSelectedIndex() != -1)
					plot.addLinePlot(molecule.getDataTable(), 
						xColumnField.getSelectionModel().getSelectedItem(), 
						propertiesRow.getYColumn(),
						propertiesRow.getColor());
		}
	}
	
	@Override
	public void setMolecule(Molecule molecule) {
		this.molecule = molecule;
		
		String xSelection = xColumnField.getSelectionModel().getSelectedItem();
		xColumnField.getItems().clear();
		xColumnField.getItems().addAll(molecule.getDataTable().getColumnHeadings());
		xColumnField.getSelectionModel().select(xSelection);
		
		for (PlotPropertiesRow propertiesRow : plotPropertiesRowList) {
			String ySelection = propertiesRow.yColumnField().getSelectionModel().getSelectedItem();
			propertiesRow.yColumnField().getItems().clear();
			propertiesRow.yColumnField().getItems().addAll(molecule.getDataTable().getColumnHeadings());
			if (ySelection != null)
				propertiesRow.yColumnField().getSelectionModel().select(ySelection);
		}
		updatePlot();
	}
	
	public void setPlot(Plot plot) {
		this.plot = plot;
	}
	
	private class PlotPropertiesRow {
		private ComboBox<String> yColumnField, typeField;
		private ComboBox<Color> colorField, segmentColorField;
		private JFXCheckBox drawSegmentsColumn;
		private PlotProperties properties;
		
		PlotPropertiesRow(PlotProperties properties) {
			this.properties = properties;
			initComponents();
			load();
		}
		
		void initComponents() {
			typeField = new ComboBox<>();
			yColumnField = new ComboBox<>();
			colorField = new ComboBox<>();

			drawSegmentsColumn = new JFXCheckBox();
			segmentColorField = new ComboBox<>();
			
			Callback<ListView<Color>, ListCell<Color>> factory = new Callback<ListView<Color>, ListCell<Color>>() {
		        @Override
		        public ListCell<Color> call(ListView<Color> list) {
		            return new ColorRectCell();
		        }
		    };

		    colorField.setCellFactory(factory);
		    colorField.setButtonCell(factory.call(null));
			
		    segmentColorField.setCellFactory(factory);
		    segmentColorField.setButtonCell(factory.call(null));
		}
		
		void load() {
			typeField.getItems().addAll(types);
			typeField.getSelectionModel().select("Line");
			
			yColumnField.getSelectionModel().select(properties.yColumnName());
			if (molecule != null)
				yColumnField.getItems().addAll(molecule.getDataTable().getColumnHeadings());
			
			colorField.getItems().addAll(colors);
			colorField.getSelectionModel().select(properties.getColor());
			colorField.setPrefWidth(50);
			
			segmentColorField.getItems().addAll(colors);
			segmentColorField.getSelectionModel().select(properties.getSegmentsColor());
			segmentColorField.setPrefWidth(50);
		}
		
		public ComboBox<String> getTypeField() {
			return typeField;
		}
		
		public ComboBox<Color> getColorField() {
			return colorField;
		}
		
		public ComboBox<Color> getSegmentsColor() {
			return segmentColorField;
		}
		
		public ComboBox<String> yColumnField() {
			return yColumnField;
		}
		
		public JFXCheckBox getDrawSegmentsField() {
			return drawSegmentsColumn;
		}
		
		public String getType() {
			return properties.getType();
		}
		
		public boolean drawSegments() {
			return drawSegmentsColumn.isSelected();
		}
		
		public String getYColumn() {
			return yColumnField.getSelectionModel().getSelectedItem();
		}
		
		public Color getColor() {
			return colorField.getSelectionModel().getSelectedItem();
		}
	}
	
	static class ColorRectCell extends ListCell<Color>{
	      @Override
	      public void updateItem(Color item, boolean empty){
	          super.updateItem(item, empty);
	          Rectangle rect = new Rectangle(15,15);
	          if(item != null){
	              rect.setFill(item);
	              setGraphic(rect);
	          }
	      }
	}
}

