package de.mpg.biochem.mars.fx.plot;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LINE_CHART;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import com.jfoenix.controls.JFXBadge;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.AxisLabelFormatter;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.AbstractValueIndicator;
import de.gsi.chart.plugins.ChartPlugin;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.datareduction.DefaultDataReducer;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.dataset.spi.DefaultDataSet;
import de.gsi.dataset.spi.DoubleDataSet;
import de.mpg.biochem.mars.fx.plot.tools.SegmentDataSetRenderer;
//import de.mpg.biochem.mars.fx.plot.tools.MarsDataPointTooltip;
//import de.mpg.biochem.mars.fx.plot.tools.MarsRegionSelectionTool;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.table.MarsTable;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.util.StringConverter;

public abstract class AbstractSubPlot implements SubPlot {
	protected DefaultNumericAxis xAxis, yAxis;
	protected XYChart chartPane;
	
	protected JFXBadge datasetOptionsButton;
	protected DatasetOptionsPane datasetOptionsPane;
	
	protected PlotPane plotPane;
	
	//protected  renderer;
	
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
		
		xAxis = createAxis();
		yAxis = createAxis();
		
		AxisLabelFormatter defaultConverter = yAxis.getAxisLabelFormatter();
		StringConverter<Number> modifiedConverter = new StringConverter<Number>() {
		    @Override
		    public String toString(Number object) {
		        // N.B. added spaces before/after as work-around
		        return " " + defaultConverter.toString(object) + " ";
		    }

		    @Override
		    public Number fromString(String string) {
		        return defaultConverter.fromString(string.trim());
		    }
		};
		yAxis.setTickLabelFormatter(modifiedConverter);
		
		chartPane = new XYChart(xAxis, yAxis);
		chartPane.setAnimated(false);
		
		chartPane.setMaxHeight(Double.MAX_VALUE);
		chartPane.setMaxWidth(Double.MAX_VALUE);		
		//For the moment lets hide the legend
		chartPane.setLegendVisible(false);
		
		SegmentDataSetRenderer renderer = new SegmentDataSetRenderer();
		
		final DefaultDataReducer reductionAlgorithm = (DefaultDataReducer) renderer.getRendererDataReducer();
		reductionAlgorithm.setMinPointPixelDistance(0);
		
		renderer.setDrawMarker(false);
		
		chartPane.getRenderers().setAll(renderer);
		
		//chartPane.setHorizontalGridLinesVisible(false);
		//chartPane.setVerticalGridLinesVisible(false);
		
		chartPane.getGridRenderer().getVerticalMajorGrid().setStyle("-fx-stroke: rgb(237, 14, 14);");
		chartPane.getGridRenderer().getHorizontalMajorGrid().setStyle("-fx-stroke: #ed0e0e;");
	}
	
	protected DefaultNumericAxis createAxis() {
		DefaultNumericAxis axis = new DefaultNumericAxis();
		axis.setMinorTickCount(0);
		axis.setAutoRangeRounding(false);
		axis.setForceZeroInRange(false);
		axis.setAnimated(false);
		//axis.setAutoRangePadding(0);
		return axis;
	}
	
	public void addSeries(PlotSeries plotSeries) {
		getPlotSeriesList().add(plotSeries);
	}
	
	public void clear() {
		chartPane.getDatasets().clear();
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
					addDataSet(plotSeries);
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
		chartPane.getXAxis().setName(xAxisLabel);
	}
	
	public void setYLabel(String yAxisLabel) {
		chartPane.getYAxis().setName(yAxisLabel);
	}
	
	public void resetXYZoom() {
		if (getPlotSeriesList().size() == 0)
			return;
		
		//Make sure the columns have been picked otherwise do nothing...
		for (int i=0; i < getPlotSeriesList().size(); i++) {
			if (getPlotSeriesList().get(i).getXColumn() == null || getPlotSeriesList().get(i).getYColumn() == null)
				return;
		}

		xAxis.setAutoRanging(true);
		yAxis.setAutoRanging(true);
	}
	
	public DefaultNumericAxis getXAxis() {
		return xAxis;
	}
	
	public DefaultNumericAxis getYAxis() {
		return yAxis;
	}
	
	@Override
	public Node getNode() {
		return chartPane;
	}
	
	public XYChart getChart() {
		return chartPane;
	}
	
	public DatasetOptionsPane getDatasetOptionsPane() {
		return datasetOptionsPane;
	}
	
	public JFXBadge getDatasetOptionsButton() {
		return datasetOptionsButton;
	}
	
	protected abstract void addDataSet(PlotSeries plotSeries);
	
	protected abstract MarsTable getDataTable();
}
