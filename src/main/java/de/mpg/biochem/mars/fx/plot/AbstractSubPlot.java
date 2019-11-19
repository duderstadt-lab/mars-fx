package de.mpg.biochem.mars.fx.plot;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LINE_CHART;

import java.util.HashSet;
import java.util.Set;

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
import de.gsi.chart.ui.geometry.Side;
import de.gsi.dataset.spi.DefaultDataSet;
import de.gsi.dataset.spi.DoubleDataSet;
import de.gsi.chart.ui.css.StylishBooleanProperty;
import de.gsi.chart.ui.css.StylishObjectProperty;

import de.gsi.chart.ui.geometry.Side;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;

import javafx.beans.property.ObjectProperty;
import de.mpg.biochem.mars.fx.plot.tools.MarsNumericAxis;
import de.mpg.biochem.mars.fx.plot.tools.MarsZoomer;
import de.mpg.biochem.mars.fx.plot.tools.SegmentDataSetRenderer;
//import de.mpg.biochem.mars.fx.plot.tools.MarsDataPointTooltip;
//import de.mpg.biochem.mars.fx.plot.tools.MarsRegionSelectionTool;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.table.MarsTable;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.StyleableObjectProperty;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.util.StringConverter;

public abstract class AbstractSubPlot implements SubPlot {
	protected MarsNumericAxis xAxis, yAxis;
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
		
		//User style sheets !
		
		chartPane.getGridRenderer().getVerticalMajorGrid().setStyle("-fx-stroke: rgb(237, 14, 14);");
		chartPane.getGridRenderer().getHorizontalMajorGrid().setStyle("-fx-stroke: #ed0e0e;");
	}
	
	protected MarsNumericAxis createAxis() {
		MarsNumericAxis axis = new MarsNumericAxis();
		axis.setMinorTickCount(0);
		axis.setAutoRangeRounding(false);
		axis.setForceZeroInRange(false);
		axis.setAnimated(false);
		return axis;
	}
	
	public void addSeries(PlotSeries plotSeries) {
		getPlotSeriesList().add(plotSeries);
	}
	
	public void setTitle(String name) {
		chartPane.setTitle(name);
	}

	public ObservableList<PlotSeries> getPlotSeriesList() {
		return datasetOptionsPane.getPlotSeriesList();
	}
	
	public void update() {
		chartPane.getDatasets().clear();
		
		removeIndicators();
		
		Set<String> xAxisList = new HashSet<String>();
		Set<String> yAxisList = new HashSet<String>();

		for (int i=0;i<getPlotSeriesList().size();i++) {
			PlotSeries plotSeries = getPlotSeriesList().get(i);
			
			if (plotSeries.xColumnField().getSelectionModel().getSelectedIndex() != -1 
				&& plotSeries.yColumnField().getSelectionModel().getSelectedIndex() != -1) {
					addDataSet(plotSeries);
					xAxisList.add(plotSeries.getXColumn());
					yAxisList.add(plotSeries.getYColumn());
			}
		}
		if (!datasetOptionsPane.getTitle().equals(""))
			setTitle(datasetOptionsPane.getTitle());
		if (!datasetOptionsPane.getXAxisName().equals(""))
			setXLabel(datasetOptionsPane.getXAxisName());
		if (!datasetOptionsPane.getYAxisName().equals(""))
			setYLabel(datasetOptionsPane.getYAxisName());
		
		addIndicators(xAxisList, yAxisList);
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
			if (!(plugin instanceof AbstractValueIndicator)) {
				if (plugin instanceof MarsZoomer)
					((MarsZoomer) plugin).removeSliderListeners(chartPane);
				chartPane.getPlugins().remove(plugin);
			}
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
	
	public MarsNumericAxis getXAxis() {
		return xAxis;
	}
	
	public MarsNumericAxis getYAxis() {
		return yAxis;
	}
	
	@Override
	public Node getNode() {
		return chartPane;
	}
	
	public XYChart getChart() {
		return chartPane;
	}
	
	public PlotPane getPlotPane() {
		return plotPane;
	}
	
	public DatasetOptionsPane getDatasetOptionsPane() {
		return datasetOptionsPane;
	}
	
	public JFXBadge getDatasetOptionsButton() {
		return datasetOptionsButton;
	}
	
	public abstract void addDataSet(PlotSeries plotSeries);
	
	public abstract void addIndicators(Set<String> AxisList, Set<String> yAxisList);
	
	public abstract void removeIndicators();
	
	protected abstract MarsTable getDataTable();
}
