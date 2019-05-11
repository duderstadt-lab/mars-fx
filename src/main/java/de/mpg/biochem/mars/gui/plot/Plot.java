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

import java.util.function.Predicate;

//import org.controlsfx.control.PopOver;
//import org.controlsfx.control.PopOver.ArrowLocation;
import de.mpg.biochem.mars.gui.util.Action;
import de.mpg.biochem.mars.gui.util.ActionUtils;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

public class Plot extends BorderPane {
	DataReducingObservableList<Number, Number> data;
	
	public static final Predicate<MouseEvent> PAN_MOUSE_FILTER = event -> MouseEvents
		    .isOnlyPrimaryButtonDown(event) && MouseEvents.modifierKeysUp(event);
	
	NumericAxis xAxis;
	NumericAxis yAxis;
	LineChart<Number, Number> lineChart;
	XYChartPane<Number, Number> chartPane;
	
	Panner panner;
	
	BooleanProperty trackSelected = new SimpleBooleanProperty();
	BooleanProperty zoomXYSelected = new SimpleBooleanProperty();
	BooleanProperty zoomXSelected = new SimpleBooleanProperty();
	BooleanProperty zoomYSelected = new SimpleBooleanProperty();
	BooleanProperty panSelected = new SimpleBooleanProperty();
	BooleanProperty crosshairSelected = new SimpleBooleanProperty();

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
		Action crosshairCursor = new Action("crosshair Indicator", "Shortcut+C", PLUS, e -> addPlugin(new CrosshairIndicator<Number, Number>(), Cursor.CROSSHAIR),
				null, crosshairSelected);
		
		Node[] toolButtons = ActionUtils.createToolBarButtons(
				trackCursor,
				zoomXYCursor,
				zoomXCursor,
				zoomYCursor,
				panCursor,
				crosshairCursor);
		
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
		
		// horizontal spacer
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		toolBar.getItems().add(spacer);
		
		//settings
		Action properties = new Action("settings", "Shortcut+S", COG, e -> properties());
		toolBar.getItems().add(ActionUtils.createToolBarButton(properties));
		
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
		chartPane = new XYChartPane<>(lineChart);
		
		return chartPane;
	}
	
	public void addLinePlot(MARSResultsTable table, String xColumn, String yColumn) {
		//data = new DataReducingObservableList<>(xAxis, RandomDataGenerator.generateData(0, 1, pointsCount));
		XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
		for (int row=0; row< table.getRowCount(); row++) {
			series.getData().add(new XYChart.Data<Number, Number>(table.getValue(xColumn, row), table.getValue(yColumn, row)));
		}

		lineChart.setStyle("-fx-stroke-width: 1px;");
		lineChart.setCreateSymbols(false);
		lineChart.setAnimated(false);
		lineChart.getData().add(series);
	}
	/*
	 * TODO Finishe segment plot as overlay ???
	public void addSegmentPlot(MARSResultsTable segmentTable) {
		//data = new DataReducingObservableList<>(xAxis, RandomDataGenerator.generateData(0, 1, pointsCount));
		XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
		for (int row=0; row< table.getRowCount(); row++) {
			series.getData().add(new XYChart.Data<Number, Number>(table.getValue(xColumn, row), table.getValue(yColumn, row)));
		}
		
		lineChart.getData().add(series);
		lineChart.setStyle("-fx-stroke-width: 1px;");
		lineChart.setCreateSymbols(false);
		lineChart.setAnimated(false);
	}
	*/
	// Cursor Util functions
	
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
		if (trackSelected.get() || zoomXYSelected.get() || zoomXSelected.get() || zoomYSelected.get() || panSelected.get() || crosshairSelected.get())
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
}
