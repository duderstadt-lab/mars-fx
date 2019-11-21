package de.mpg.biochem.mars.fx.plot;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.COG;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.MINUS;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLUS;

import java.util.ArrayList;
import java.util.Arrays;

import javafx.scene.layout.HBox;

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
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.text.Text;

import org.tbee.javafx.scene.layout.fxml.MigPane;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXColorPicker;
import com.jfoenix.controls.JFXTextField;

public class DatasetOptionsPane extends MigPane {
	private TextField titleField, xNameField, yNameField;
	private Button addButton, updateButton;
	private RadioButton radioButtonMolecules, radioButtonMetadata, radioButtonNone;
	private ToggleGroup indicatorSourceGroup;
	
	private TableView<PlotSeries> plotPropertiesTable;
	
	private ObservableList<PlotSeries> plotSeriesList = FXCollections.observableArrayList();
	
	private MarsTable table;
	private final ToggleGroup trackingGroup;
	
	private SubPlot subPlot;

	public DatasetOptionsPane(MarsTable table, SubPlot subPlot) {
		this.table = table;
		this.subPlot = subPlot;
		
		trackingGroup = new ToggleGroup();
		
		setLayout("insets dialog");
		
		initComponents();
		
		add(new Label("Title "), "");
		add(titleField, "wrap");
		add(new Label("X Axis "), "");
		add(xNameField, "wrap");
		add(new Label("Y Axis "), "");
		add(yNameField, "wrap");
				
		add(new Label("Indicators"), "wrap");
		add(new HBox(radioButtonMolecules, radioButtonMetadata, radioButtonNone), "wrap");
		
		add(plotPropertiesTable, "span, wrap");
		
		add(addButton, "");
		add(updateButton, "right");
	}
	
	private void initComponents() {
		titleField = new TextField();
		yNameField = new TextField();
		xNameField = new TextField();
		
        radioButtonMolecules = new RadioButton("Molecules ");
        radioButtonMetadata = new RadioButton("Metadata ");
        radioButtonNone = new RadioButton("None ");

        indicatorSourceGroup = new ToggleGroup();

        radioButtonMolecules.setToggleGroup(indicatorSourceGroup);
        radioButtonMetadata.setToggleGroup(indicatorSourceGroup);
        radioButtonNone.setToggleGroup(indicatorSourceGroup);
        
        //Default Indicator setting
        radioButtonMolecules.setSelected(true);
		
		plotPropertiesTable = new TableView<PlotSeries>();
		
		plotPropertiesTable.prefHeightProperty().set(200);
		plotPropertiesTable.prefWidthProperty().set(500);
		
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
    	trackColumn.setMinWidth(50);
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
        xValuesColumn.setMinWidth(100);
        xValuesColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().xColumnField()));
        xValuesColumn.setStyle("-fx-alignment: CENTER;");
        
        xValuesColumn.setSortable(false);
        plotPropertiesTable.getColumns().add(xValuesColumn);
        
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
		
		addButton = new Button();
		addButton.setGraphic(FontAwesomeIconFactory.get().createIcon(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLUS, "0.9em"));
		addButton.setCenterShape(true);
		addButton.setStyle(
                "-fx-background-radius: 5em; " +
                "-fx-min-width: 20px; " +
                "-fx-min-height: 20px; " +
                "-fx-max-width: 20px; " +
                "-fx-max-height: 20px;"
        );
		addButton.setOnAction(e -> {
			if (table != null) {
				PlotSeries defaultPlotSeries = new PlotSeries(table);
				defaultPlotSeries.getTrackingButton().setToggleGroup(trackingGroup);
				if (plotSeriesList.size() < 1)
					defaultPlotSeries.getTrackingButton().selectedProperty().set(true);
				plotSeriesList.add(defaultPlotSeries);
			}
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
	
	public void setTable(MarsTable table) {
		this.table = table;
		
		for (PlotSeries propertiesRow : plotSeriesList) {
			String xSelection = propertiesRow.xColumnField().getSelectionModel().getSelectedItem();
			propertiesRow.xColumnField().getItems().clear();
			propertiesRow.xColumnField().getItems().addAll(table.getColumnHeadings());
			if (xSelection != null)
				propertiesRow.xColumnField().getSelectionModel().select(xSelection);
			
			String ySelection = propertiesRow.yColumnField().getSelectionModel().getSelectedItem();
			propertiesRow.yColumnField().getItems().clear();
			propertiesRow.yColumnField().getItems().addAll(table.getColumnHeadings());
			if (ySelection != null)
				propertiesRow.yColumnField().getSelectionModel().select(ySelection);
		}
	}
	
	public XYChart getTrackingChart() {
		for (PlotSeries series : getPlotSeriesList()) {
			if (series.trackChart())
				return series.getChart();
		}
		
		return null;
	}
	
	public PlotSeries getTrackingSeries() {
		for (PlotSeries series : getPlotSeriesList())
			if (series.trackChart())
				return series;

		return null;
	}
	
	public ObservableList<PlotSeries> getPlotSeriesList() {
		return plotSeriesList;
	}
	
	public String getTitle() {
		return titleField.getText();
	}
	
	public String getXAxisName() {
		return xNameField.getText();
	}
	
	public String getYAxisName() {
		return yNameField.getText();
	}
	
	public void setSubPlot(SubPlot subPlot) {
		this.subPlot = subPlot;
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

