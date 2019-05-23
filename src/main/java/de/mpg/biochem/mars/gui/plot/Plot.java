package de.mpg.biochem.mars.gui.plot;

import cern.extjfx.chart.AxisMode;
import cern.extjfx.chart.NumericAxis;
import cern.extjfx.chart.XYChartPane;
import cern.extjfx.chart.XYChartPlugin;
import cern.extjfx.chart.data.DataReducingObservableList;
import cern.extjfx.chart.plugins.CrosshairIndicator;
import cern.extjfx.chart.plugins.DataPointTooltip;
import cern.extjfx.chart.plugins.Panner;
import cern.extjfx.chart.plugins.Zoomer;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.chart.XYChart;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.scene.text.*;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

import de.mpg.biochem.mars.table.*;

import cern.extjfx.chart.plugins.*;
import javafx.scene.input.MouseEvent;

import java.util.HashMap;
import java.util.Set;
import java.util.function.Predicate;

import de.mpg.biochem.mars.gui.molecule.moleculesTab.DatasetOptionsPane;
import de.mpg.biochem.mars.gui.molecule.moleculesTab.MoleculeSubTab;
import de.mpg.biochem.mars.gui.options.MarkdownExtensionsPane;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import de.mpg.biochem.mars.gui.util.Action;
import de.mpg.biochem.mars.gui.util.ActionUtils;
import de.mpg.biochem.mars.molecule.Molecule;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

public class Plot extends BorderPane implements MoleculeSubTab {
	private DataReducingObservableList<Number, Number> data;
	
	public static final Predicate<MouseEvent> PAN_MOUSE_FILTER = event -> MouseEvents
		    .isOnlyPrimaryButtonDown(event) && MouseEvents.modifierKeysUp(event);
	
	private NumericAxis xAxis;
	private NumericAxis yAxis;
	private LineChart<Number, Number> lineChart;
	private XYChartPane<Number, Number> chartPane;
	
	private Molecule molecule;
	
	private Panner panner;
	
	private Node datasetOptions;
	private DatasetOptionsPane datasetOptionsPane;
	
	private HashMap<Integer, String> seriesColorMap = new HashMap<>();
	
	private BooleanProperty trackSelected = new SimpleBooleanProperty();
	private BooleanProperty zoomXYSelected = new SimpleBooleanProperty();
	private BooleanProperty zoomXSelected = new SimpleBooleanProperty();
	private BooleanProperty zoomYSelected = new SimpleBooleanProperty();
	private BooleanProperty panSelected = new SimpleBooleanProperty();
	//private BooleanProperty crosshairSelected = new SimpleBooleanProperty();

	public Plot() {
		Label titleLabel = new Label(getDescription());
        titleLabel.setStyle("-fx-border-color: grey");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        setTop(titleLabel);

        SplitPane centerPane = new SplitPane(createSamplePane());
        Node controlPane = createControlPane();
        if (controlPane != null) {
            centerPane.getItems().add(controlPane);
            centerPane.setDividerPositions(0.65);
        }
        setCenter(centerPane);
        setTop(createToolBar());

    	panner = new Panner();
    	panner.setMouseFilter(PAN_MOUSE_FILTER);
	}

	private Node createToolBar() { 
		Action trackCursor = new Action("Track", "Shortcut+T", CIRCLE_ALT, e -> addPlugin(new MARSDataPointTooltip<Number, Number>(), Cursor.DEFAULT),
				null, trackSelected);
		Action zoomXYCursor = new Action("select XY region", "Shortcut+S", ARROWS, e -> addPlugin(new Zoomer(true), Cursor.CROSSHAIR),
				null, zoomXYSelected);
		Action zoomXCursor = new Action("select X region", "Shortcut+X", ARROWS_H, e -> addPlugin(new Zoomer(AxisMode.X, true), Cursor.H_RESIZE),
				null, zoomXSelected);
		Action zoomYCursor = new Action("select Y region", "Shortcut+Y", ARROWS_V, e -> addPlugin(new Zoomer(AxisMode.Y, true), Cursor.V_RESIZE),
				null, zoomYSelected);
		Action panCursor = new Action("pan", "Shortcut+P", HAND_PAPER_ALT, e -> addPlugin(panner, Cursor.MOVE),
				null, panSelected);
		//Action crosshairCursor = new Action("crosshair Indicator", "Shortcut+C", PLUS, e -> addPlugin(new CrosshairIndicator<Number, Number>(), Cursor.CROSSHAIR),
		//		null, crosshairSelected);
		
		Node[] toolButtons = ActionUtils.createToolBarButtons(
				trackCursor,
				zoomXYCursor,
				zoomXCursor,
				zoomYCursor,
				panCursor);
		
		ToggleGroup toolGroup = new ToggleGroup();
		for (Node n : toolButtons)
			((ToggleButton)n).setToggleGroup(toolGroup);
		
		ToolBar toolBar = new ToolBar(toolButtons);
		toolBar.getItems().add(new Separator());
		/*
		 * Needs to be manually written.
		 * 
		Action resetXZoom = new Action("Reset Zoom", "Shortcut+X", ARROWS_H, e -> resetXZoom());
		toolBar.getItems().add(ActionUtils.createToolBarButton(resetXZoom));
		
		Action resetYZoom = new Action("Reset Zoom", "Shortcut+Y", ARROWS_V, e -> resetYZoom());
		toolBar.getItems().add(ActionUtils.createToolBarButton(resetYZoom));
		*/
		Action resetXYZoom = new Action("Reset Zoom", "Shortcut+R", EXPAND, e -> resetXYZoom());
		toolBar.getItems().add(ActionUtils.createToolBarButton(resetXYZoom));
		
		//settings
		Action properties = new Action("settings", "Shortcut+S", COG, e -> properties());
		toolBar.getItems().add(ActionUtils.createToolBarButton(properties));
		
		// horizontal spacer
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		toolBar.getItems().add(spacer);
		
		datasetOptionsPane = new DatasetOptionsPane(molecule, this);
		datasetOptions = ActionUtils.createToolBarButton(new Action("Dataset", "Shortcut+C", LINE_CHART, e -> {
			PopOver popOver = new PopOver();
			popOver.setTitle("Dataset");
			popOver.setHeaderAlwaysVisible(true);
			popOver.setArrowLocation(ArrowLocation.TOP_CENTER);
			popOver.setContentNode(datasetOptionsPane);
			popOver.show(datasetOptions);
		}));
		
		toolBar.getItems().add(datasetOptions);
		
		Action addPlot = new Action("Add Plot", "Shortcut+A", PLUS, e -> addChart());
		toolBar.getItems().add(ActionUtils.createToolBarButton(addPlot));
		
		return toolBar;
	}
	
	public String getName() {
		return "Data Set";
	}

	public String getDescription() {
		return "Plot";
	}

	public Node createSamplePane() {
		xAxis = new NumericAxis();
		xAxis.setAutoRangeRounding(false);
		xAxis.setForceZeroInRange(false);
		xAxis.setAutoRangePadding(0);
		
		yAxis = new NumericAxis();
		yAxis.setAutoRangeRounding(false);
		yAxis.setForceZeroInRange(false);
		yAxis.setAutoRangePadding(0);
		
		lineChart = new LineChart<>(xAxis, yAxis);
		lineChart.setCreateSymbols(false);
		lineChart.setAnimated(false);
		chartPane = new XYChartPane<>(lineChart);
		chartPane.setLegendVisible(false);
		
		return chartPane;
	}
	
	public void addChart() {
		
	}
	
	public void addLinePlot(MARSResultsTable table, String xColumn, String yColumn) {
		addLinePlot(table, xColumn, yColumn, Color.BLACK, -1);
	}
	
	public void addLinePlot(MARSResultsTable table, String xColumn, String yColumn, Color color, int index) {
		//data = new DataReducingObservableList<>(xAxis, RandomDataGenerator.generateData(0, 1, pointsCount));
		XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
		for (int row=0; row< table.getRowCount(); row++) {
			series.getData().add(new XYChart.Data<Number, Number>(table.getValue(xColumn, row), table.getValue(yColumn, row)));
		}

		series.setName(yColumn);
		
		lineChart.getData().add(series);
		final String colorString = String.format("rgba(%d, %d, %d, 1.0)", Math.round(color.getRed()*255), Math.round(color.getGreen()*255), Math.round(color.getBlue()*255));
		final String lineStyle = String.format("-fx-stroke-width: 1px; -fx-stroke: %s;", colorString);
		series.getNode().lookup(".chart-series-line").setStyle(lineStyle);
		
		seriesColorMap.put(index, colorString);
	}
	
	public void clear() {
		lineChart.getData().clear();
		seriesColorMap.clear();
	}
	
	public void setTitle(String name) {
		chartPane.setTitle(name);
	}

	public void addSegmentPlot(MARSResultsTable segmentTable) {
		LineChart<Number, Number> segmentChart = new LineChart<>(xAxis, yAxis);
		
		//for (int row=0; row< segmentTable.getRowCount(); row++) {
			XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
			
			//series.getData().add(new XYChart.Data<Number, Number>(segmentTable.getValue("x1", row), segmentTable.getValue("y1", row)));
			//series.getData().add(new XYChart.Data<Number, Number>(segmentTable.getValue("x2", row), segmentTable.getValue("y2", row)));
			
			series.getData().add(new XYChart.Data<Number, Number>(1, 5000));
			series.getData().add(new XYChart.Data<Number, Number>(1, 5000));
			
			
			segmentChart.getData().add(series);
		//}
		 
		segmentChart.setStyle("-fx-stroke-width: 1px;");
		segmentChart.setCreateSymbols(false);
		segmentChart.setAnimated(false);
		chartPane.getOverlayCharts().add(segmentChart);
	}
	
	/*
	public void updateLegend() {
		Platform.runLater(() -> {
			for (Node node: chartPane.lookupAll(".chart-legend-item-symbol")) {
	            for (String styleClass: node.getStyleClass()) {
	                if (styleClass.startsWith("series")) {
	                    final int i = Integer.parseInt(styleClass.substring(6));
	                    if (seriesColorMap.containsKey(i))
	                    	node.setStyle("-fx-background-color: " + seriesColorMap.get(i) + ", white;");
	                    //break;
	                }
	            }
			}
		});
	}
	*/
	private void addPlugin(XYChartPlugin<Number, Number> plugin, Cursor cursor) {
		removePlugins();
		if (toolSelected()) {
			chartPane.getPlugins().add(plugin);
			chartPane.setCursor(cursor);
		} else {
			chartPane.setCursor(Cursor.DEFAULT);
		}
	}
	
	private void removePlugins() {
		while (chartPane.getPlugins().size()>0)
			chartPane.getPlugins().remove(0);
	}
	
	private boolean toolSelected() {
		if (trackSelected.get() || zoomXYSelected.get() || zoomXSelected.get() || zoomYSelected.get() || panSelected.get())
			return true;
		else
			return false;
	}
	
	// Remaining Actions
	
	private void resetXYZoom() {
		xAxis.setAutoRanging(true);
		yAxis.setAutoRanging(true);
	}
	/*
	private void resetXZoom() {
		xAxis.setAutoRanging(true);
	}
	
	private void resetYZoom() {
		yAxis.setAutoRanging(true);
	}
	*/
	// Settings
	
	private void properties() {
		
	}
	
	public void setXLabel(String xAxisLabel) {
		xAxis.setLabel(xAxisLabel);
	}
	
	public void setYLabel(String yAxisLabel) {
		yAxis.setLabel(yAxisLabel);
	}
	
	protected Node createControlPane() {
        return null;
    }

	@Override
	public void setMolecule(Molecule molecule) {
		datasetOptionsPane.setMolecule(molecule);
	}
}
