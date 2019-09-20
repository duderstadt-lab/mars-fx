package de.mpg.biochem.mars.fx.plot;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LINE_CHART;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import com.jfoenix.controls.JFXBadge;

import cern.extjfx.chart.AxisMode;
import cern.extjfx.chart.NumericAxis;
import cern.extjfx.chart.XYChartPane;
import cern.extjfx.chart.XYChartPlugin;
import cern.extjfx.chart.plugins.AbstractValueIndicator;
import de.mpg.biochem.mars.fx.plot.tools.MarsDataPointTooltip;
import de.mpg.biochem.mars.fx.plot.tools.MarsRegionSelectionTool;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.table.MarsTable;
import javafx.collections.ObservableList;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.chart.XYChart.Data;

public abstract class AbstractSubPlot implements SubPlot {
	protected NumericAxis globalXAxis, globalYAxis;
	protected LineChart<Number, Number> dummyChart;
	protected XYChartPane<Number, Number> chartPane;
	
	protected double xMIN = 0;
	protected double xMAX = 100;
	
	protected double yMIN = 0;
	protected double yMAX = 100;
	
	protected JFXBadge datasetOptionsButton;
	protected DatasetOptionsPane datasetOptionsPane;
	
	protected PlotPane plotPane;
	
	public AbstractSubPlot(PlotPane plotPane, String plotTitle) {
		this.plotPane = plotPane;
		
		datasetOptionsPane = new DatasetOptionsPane(getDataTable(), this);
		datasetOptionsButton = new JFXBadge(ActionUtils.createToolBarButton(new Action("Dataset", "Shortcut+C", LINE_CHART, e -> {
			PopOver popOver = new PopOver();
			popOver.setTitle(plotTitle);
			popOver.setHeaderAlwaysVisible(true);
			popOver.setAutoHide(false);
			popOver.setArrowLocation(ArrowLocation.TOP_CENTER);
			popOver.setContentNode(datasetOptionsPane);
			popOver.show(datasetOptionsButton);
		})));
		
		globalXAxis = createAxis();
		globalYAxis = createAxis();
		
		dummyChart = new LineChart<Number, Number>(globalXAxis, globalYAxis);
		dummyChart.setCreateSymbols(false);
		dummyChart.setAnimated(false);
		
		chartPane = new XYChartPane<>(dummyChart);
		chartPane.setCommonYAxis(true);
		chartPane.setMaxHeight(Double.MAX_VALUE);
		chartPane.setMaxWidth(Double.MAX_VALUE);		
		//For the moment lets hide the legend
		chartPane.setLegendVisible(false);
	}
	
	protected NumericAxis createAxis() {
		NumericAxis axis = new NumericAxis();
		axis.setAutoRangeRounding(false);
		axis.setForceZeroInRange(false);
		axis.setAutoRangePadding(0);
		return axis;
	}
	
	public void addSeries(PlotSeries plotSeries) {
		getPlotSeriesList().add(plotSeries);
	}
	
	public void clear() {
		chartPane.getOverlayCharts().clear();
	}
	
	public void setTitle(String name) {
		chartPane.setTitle(name);
	}

	public ObservableList<PlotSeries> getPlotSeriesList() {
		return datasetOptionsPane.getPlotSeriesList();
	}
	
	public void update() {
		clear();

		for (int i=0;i<getPlotSeriesList().size();i++) {
			PlotSeries plotSeries = getPlotSeriesList().get(i);
			
			if (plotSeries.xColumnField().getSelectionModel().getSelectedIndex() != -1 
				&& plotSeries.yColumnField().getSelectionModel().getSelectedIndex() != -1) {
					if (plotSeries.getType().equals("Line")) {
						plotSeries.setChart(addLine(plotSeries));
					} else if (plotSeries.getType().equals("Scatter")) {
						plotSeries.setChart(addScatter(plotSeries));
					}
			}
		}
		if (!datasetOptionsPane.getTitle().equals(""))
			setTitle(datasetOptionsPane.getTitle());
		if (!datasetOptionsPane.getXAxisName().equals(""))
			setXLabel(datasetOptionsPane.getXAxisName());
		if (!datasetOptionsPane.getYAxisName().equals(""))
			setYLabel(datasetOptionsPane.getYAxisName());
		
		resetXYZoom();
	}
	
	@Override
	public void setTool(XYChartPlugin<Number, Number> plugin, Cursor cursor) {
		removeTools();
		if (plugin instanceof MarsPlotPlugin) {
			((MarsPlotPlugin) plugin).setDatasetOptionsPane(datasetOptionsPane);
		}
		chartPane.getPlugins().add(plugin);
		chartPane.setCursor(cursor);
	}
	
	@Override
	public void removeTools() {
		for (XYChartPlugin<Number, Number> plugin : chartPane.getPlugins())
			if (!(plugin instanceof AbstractValueIndicator))
				chartPane.getPlugins().remove(plugin);
		chartPane.setCursor(Cursor.DEFAULT);
	}
	
	public void setXLabel(String xAxisLabel) {
		dummyChart.getXAxis().setLabel(xAxisLabel);
	}
	
	public void setYLabel(String yAxisLabel) {
		dummyChart.getYAxis().setLabel(yAxisLabel);
	}
	
	public void resetXYZoom() {
		if (getPlotSeriesList().size() == 0)
			return;
		
		//Make sure the columns have been picked otherwise do nothing...
		for (int i=0; i < getPlotSeriesList().size(); i++) {
			if (getPlotSeriesList().get(i).getXColumn() == null || getPlotSeriesList().get(i).getYColumn() == null)
				return;
		}
		
		xMIN = Double.MAX_VALUE;
		xMAX = Double.MIN_VALUE;
		
		yMIN = Double.MAX_VALUE;
		yMAX = Double.MIN_VALUE;
		
		for (int i=0; i < getPlotSeriesList().size(); i++) {
			String xColumn = getPlotSeriesList().get(i).getXColumn();
			String yColumn = getPlotSeriesList().get(i).getYColumn();
			
			double xmin = getDataTable().min(xColumn);
			double xmax = getDataTable().max(xColumn);
			
			double ymin = getDataTable().min(yColumn);
			double ymax = getDataTable().max(yColumn);
			
			if (xmin < xMIN)
				xMIN = xmin;
			
			if (xmax > xMAX)
				xMAX = xmax;
		
			if (ymin < yMIN)
				yMIN = ymin;
			
			if (ymax > yMAX)
				yMAX = ymax;
		}
		
		Series<Number, Number> series = new Series<Number, Number>();
		series.getData().add(new Data<Number, Number>(xMIN,yMIN));
		series.getData().add(new Data<Number, Number>(xMAX,yMAX));
		
		dummyChart.getData().clear();
		dummyChart.getData().add(series);
		
		resetXAxis(globalXAxis);
		resetYAxis(globalYAxis);
		
		final String colorString = String.format("rgba(%d, %d, %d, %d)", 0, 0, 0, 0);
		final String lineStyle = String.format("-fx-stroke-width: %s; -fx-stroke: %s;", 0, colorString);
		series.getNode().lookup(".chart-series-line").setStyle(lineStyle);
	}
	
	protected void resetXAxis(NumericAxis xAxis) {
		if (xAxis.getLowerBound() > xMAX || xAxis.getLowerBound() > xMIN) {
			xAxis.setLowerBound(xMIN);
			xAxis.setUpperBound(xMAX);
		} else {
			xAxis.setUpperBound(xMAX);
			xAxis.setLowerBound(xMIN);
		}
	}
	
	protected void resetYAxis(NumericAxis yAxis) {
		if (yAxis.getLowerBound() > yMAX || yAxis.getLowerBound() > yMIN) {
			yAxis.setLowerBound(yMIN);
			yAxis.setUpperBound(yMAX);
		} else {
			yAxis.setUpperBound(yMAX);
			yAxis.setLowerBound(yMIN);
		}
	}
	
	public NumericAxis getXAxis() {
		return globalXAxis;
	}
	
	public NumericAxis getYAxis() {
		return globalYAxis;
	}
	
	@Override
	public Node getNode() {
		return chartPane;
	}
	
	public DatasetOptionsPane getDatasetOptionsPane() {
		return datasetOptionsPane;
	}
	
	public JFXBadge getDatasetOptionsButton() {
		return datasetOptionsButton;
	}
	
	protected abstract XYChart<Number, Number> addLine(PlotSeries plotSeries);
	
	protected abstract XYChart<Number, Number> addScatter(PlotSeries plotSeries);
	
	protected abstract MarsTable getDataTable();
}
