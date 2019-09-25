package de.mpg.biochem.mars.fx.plot;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LINE_CHART;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import com.jfoenix.controls.JFXBadge;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.AbstractValueIndicator;
import de.gsi.chart.plugins.ChartPlugin;
import de.gsi.dataset.spi.DefaultDataSet;
import de.gsi.dataset.spi.DoubleDataSet;
//import de.mpg.biochem.mars.fx.plot.tools.MarsDataPointTooltip;
//import de.mpg.biochem.mars.fx.plot.tools.MarsRegionSelectionTool;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.table.MarsTable;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;

public abstract class AbstractSubPlot implements SubPlot {
	protected DefaultNumericAxis globalXAxis, globalYAxis;
	protected DoubleDataSet dummyDataset;
	protected XYChart chartPane;
	
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

		
		dummyDataset = new DoubleDataSet("dummy dataset");
		
		chartPane = new XYChart(globalXAxis, globalYAxis);
		chartPane.getDatasets().add(dummyDataset);
		chartPane.setAnimated(false);
		
		//chartPane.setCommonYAxis(true);
		chartPane.setMaxHeight(Double.MAX_VALUE);
		chartPane.setMaxWidth(Double.MAX_VALUE);		
		//For the moment lets hide the legend
		chartPane.setLegendVisible(false);
	}
	
	protected DefaultNumericAxis createAxis() {
		DefaultNumericAxis axis = new DefaultNumericAxis();
		axis.setAutoRangeRounding(false);
		axis.setForceZeroInRange(false);
		axis.setAnimated(false);
		axis.setAutoRangePadding(0);
		return axis;
	}
	
	public void addSeries(PlotSeries plotSeries) {
		getPlotSeriesList().add(plotSeries);
	}
	
	public void clear() {
		chartPane.getDatasets().clear();
		chartPane.getDatasets().add(dummyDataset);
	}
	
	public void setTitle(String name) {
		chartPane.setTitle(name);
	}

	public ObservableList<PlotSeries> getPlotSeriesList() {
		return datasetOptionsPane.getPlotSeriesList();
	}
	
	public void update() {
		clear();

		chartPane.getDatasets().add(dummyDataset);
		chartPane.setAnimated(false);
		
		//chartPaneane.setCommonYAxis(true);
		chartPane.setMaxHeight(Double.MAX_VALUE);
		chartPane.setMaxWidth(Double.MAX_VALUE);		
		//For the moment lets hide the legend
		chartPane.setLegendVisible(false);

		for (int i=0;i<getPlotSeriesList().size();i++) {
			PlotSeries plotSeries = getPlotSeriesList().get(i);
			
			if (plotSeries.xColumnField().getSelectionModel().getSelectedIndex() != -1 
				&& plotSeries.yColumnField().getSelectionModel().getSelectedIndex() != -1) {
					if (plotSeries.getType().equals("Line")) {
						chartPane.getDatasets().add(addLine(plotSeries));
					} else if (plotSeries.getType().equals("Scatter")) {
						chartPane.getDatasets().add(addScatter(plotSeries));
					}
			}
		}
		if (!datasetOptionsPane.getTitle().equals(""))
			setTitle(datasetOptionsPane.getTitle());
		if (!datasetOptionsPane.getXAxisName().equals(""))
			setXLabel(datasetOptionsPane.getXAxisName());
		if (!datasetOptionsPane.getYAxisName().equals(""))
			setYLabel(datasetOptionsPane.getYAxisName());
	}
	
	@Override
	public void setTool(ChartPlugin plugin, Cursor cursor) {
		removeTools();
		if (plugin instanceof MarsPlotPlugin) {
			((MarsPlotPlugin) plugin).setDatasetOptionsPane(datasetOptionsPane);
		}
		chartPane.getPlugins().add(plugin);
		chartPane.setCursor(cursor);
	}
	
	@Override
	public void removeTools() {
		for (ChartPlugin plugin : chartPane.getPlugins())
			if (!(plugin instanceof AbstractValueIndicator))
				chartPane.getPlugins().remove(plugin);
		chartPane.setCursor(Cursor.DEFAULT);
	}
	
	public void setXLabel(String xAxisLabel) {
		chartPane.getXAxis().setLabel(xAxisLabel);
	}
	
	public void setYLabel(String yAxisLabel) {
		chartPane.getYAxis().setLabel(yAxisLabel);
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

		dummyDataset.clearData();
		final double[] xValues = new double[2];
        final double[] yValues = new double[2];
        xValues[0] = xMIN;
        yValues[0] = yMIN;
        xValues[1] = xMAX;
        yValues[1] = yMAX;
		dummyDataset.set(xValues, yValues);
		
		resetXAxis(globalXAxis);
		resetYAxis(globalYAxis);
		
		//final String colorString = String.format("rgba(%d, %d, %d, %d)", 0, 0, 0, 0);
		//final String lineStyle = String.format("-fx-stroke-width: %s; -fx-stroke: %s;", 0, colorString);
		//series.getNode().lookup(".chart-series-line").setStyle(lineStyle);
	}
	
	protected void resetXAxis(DefaultNumericAxis xAxis) {
		if (xAxis.getLowerBound() > xMAX || xAxis.getLowerBound() > xMIN) {
			xAxis.setLowerBound(xMIN);
			xAxis.setUpperBound(xMAX);
		} else {
			xAxis.setUpperBound(xMAX);
			xAxis.setLowerBound(xMIN);
		}
	}
	
	protected void resetYAxis(DefaultNumericAxis yAxis) {
		if (yAxis.getLowerBound() > yMAX || yAxis.getLowerBound() > yMIN) {
			yAxis.setLowerBound(yMIN);
			yAxis.setUpperBound(yMAX);
		} else {
			yAxis.setUpperBound(yMAX);
			yAxis.setLowerBound(yMIN);
		}
	}
	
	public DefaultNumericAxis getXAxis() {
		return globalXAxis;
	}
	
	public DefaultNumericAxis getYAxis() {
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
	
	protected abstract DoubleDataSet addLine(PlotSeries plotSeries);
	protected abstract DoubleDataSet addScatter(PlotSeries plotSeries);
	
	protected abstract MarsTable getDataTable();
}
