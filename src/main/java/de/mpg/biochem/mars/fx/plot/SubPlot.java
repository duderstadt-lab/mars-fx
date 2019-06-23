package de.mpg.biochem.mars.fx.plot;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LINE_CHART;

import java.util.ArrayList;
import java.util.List;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import com.jfoenix.controls.JFXBadge;

import cern.extjfx.chart.NumericAxis;
import cern.extjfx.chart.XYChartPane;
import cern.extjfx.chart.XYChartPlugin;
import cern.extjfx.chart.data.*;
import cern.extjfx.chart.data.DataReducingObservableList;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubTab;
import de.mpg.biochem.mars.fx.table.TableSubTab;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.table.MARSResultsTable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public class SubPlot implements MoleculeSubTab, TableSubTab {
	private NumericAxis globalXAxis, globalYAxis;
	private LineChart<Number, Number> dummyChart;
	private XYChartPane<Number, Number> chartPane;
	
	private double xMIN = 0;
	private double xMAX = 100;
	
	private double yMIN = 0;
	private double yMAX = 100;
	
	private Molecule molecule;
	private MARSResultsTable table;
	
	private JFXBadge datasetOptionsButton;
	private DatasetOptionsPane datasetOptionsPane;
	
	private PlotPane plotPane;
	
	public SubPlot(PlotPane plotPane, String plotTitle) {
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
	
	private NumericAxis createAxis() {
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
	
	/*
	public void updateLegend() {
		Platform.runLater(() -> {
			for (Node node: chartPane.lookupAll(".chart-legend-item-symbol")) {
	            for (String styleClass: node.getStyleClass()) {
	                if (styleClass.startsWith("series")) {
	                    final int i = Integer.parseInt(styleClass.substring(6));
	                    if (getPlotSeriesList().size() > i) {
	                    	Color color = getPlotSeriesList().get(i).getColor();
	                    	String colorString = String.format("rgba(%d, %d, %d, 1.0)", Math.round(color.getRed()*255), Math.round(color.getGreen()*255), Math.round(color.getBlue()*255));
	                    	node.setStyle("-fx-background-color: " + colorString + ", " + colorString + ";");
	                    }
	                }
	            }
			}
		});
	}
	*/
	public ObservableList<PlotSeries> getPlotSeriesList() {
		return datasetOptionsPane.getPlotSeriesList();
	}
	
	public void update() {
		clear();

		for (int i=0;i<getPlotSeriesList().size();i++) {
			PlotSeries plotSeries = getPlotSeriesList().get(i);
			
			if (plotSeries.xColumnField().getSelectionModel().getSelectedIndex() != -1 
				&& plotSeries.yColumnField().getSelectionModel().getSelectedIndex() != -1) {
					if (plotSeries.getType().equals("Line"))
						addLine(plotSeries);
					else if (plotSeries.getType().equals("Scatter"))
						addScatter(plotSeries);
			}
		}
		if (!datasetOptionsPane.getTitle().equals(""))
			setTitle(datasetOptionsPane.getTitle());
		if (!datasetOptionsPane.getXAxisName().equals(""))
			setXLabel(datasetOptionsPane.getXAxisName());
		if (!datasetOptionsPane.getYAxisName().equals(""))
			setYLabel(datasetOptionsPane.getYAxisName());
		
		//updateLegend();
		resetXYZoom();
	}
	
	private void addLine(PlotSeries plotSeries) {
		String xColumn = plotSeries.getXColumn();
		String yColumn = plotSeries.getYColumn();
		
		NumericAxis xAxis = createAxis();
		NumericAxis yAxis = createAxis();
		
		resetXYZoom();
		
		resetXAxis(xAxis);
		resetYAxis(yAxis);
		
		MARSResultsTable segmentsTable = null;
		if (molecule.hasSegmentsTable(xColumn, yColumn))
			segmentsTable = molecule.getSegmentsTable(xColumn, yColumn);
		
		LineChartWithSegments lineChart = new LineChartWithSegments(segmentsTable, plotSeries, xAxis, yAxis);
		lineChart.setCreateSymbols(false);
		lineChart.setAnimated(false);
		
		List<Data<Number, Number>> data = new ArrayList<>();
		for (int row=0;row<getDataTable().getRowCount();row++) {
			double x = getDataTable().getValue(xColumn, row);
			double y = getDataTable().getValue(yColumn, row);
			
			if (!Double.isNaN(x) && !Double.isNaN(y))
				data.add(new Data<>(x, y));
		}
		
		//If the columns are entirely NaN values. Don't add he plot
		if (data.size() == 0)
			return;
		
		ObservableList<Data<Number, Number>> sourceData = FXCollections.observableArrayList(data);
		
		DataReducingObservableList<Number, Number> reducedData = new DataReducingObservableList<>(xAxis, sourceData);
		reducedData.maxPointsCountProperty().bind(plotPane.maxPointsCount());
		
		Series<Number, Number> series = new Series<>(plotSeries.getYColumn(), reducedData);
		lineChart.getData().add(series);
			
		lineChart.updateStyle(plotPane.getStyleSheetUpdater());
		
		chartPane.getOverlayCharts().add(lineChart);
	}
	
	private void addScatter(PlotSeries plotSeries) {
		String xColumn = plotSeries.getXColumn();
		String yColumn = plotSeries.getYColumn();

		NumericAxis xAxis = createAxis();
		NumericAxis yAxis = createAxis();
		
		resetXYZoom();
		
		resetXAxis(xAxis);
		resetYAxis(yAxis);
		
		MARSResultsTable segmentsTable = null;
		if (molecule.hasSegmentsTable(xColumn, yColumn))
			segmentsTable = molecule.getSegmentsTable(xColumn, yColumn);

		ScatterChartWithSegments scatterChart = new ScatterChartWithSegments(segmentsTable, plotSeries, xAxis, yAxis);
		scatterChart.setAnimated(false);
		
		List<Data<Number, Number>> data = new ArrayList<>();
		for (int row=0;row<getDataTable().getRowCount();row++) {
			double x = getDataTable().getValue(xColumn, row);
			double y = getDataTable().getValue(yColumn, row);
			
			if (!Double.isNaN(x) && !Double.isNaN(y))
				data.add(new Data<>(x, y));
		}
		
		//If the columns are entirely NaN values. Don't add he plot
		if (data.size() == 0)
			return;
		
		ObservableList<Data<Number, Number>> sourceData = FXCollections.observableArrayList(data);
		
		DataReducingObservableList<Number, Number> reducedData = new DataReducingObservableList<>(xAxis, sourceData);
		reducedData.maxPointsCountProperty().bind(plotPane.maxPointsCount());
		
		Series<Number, Number> series = new Series<>(plotSeries.getYColumn(), reducedData);
		scatterChart.getData().add(series);
		
		scatterChart.updateStyle(plotPane.getStyleSheetUpdater());
		
		chartPane.getOverlayCharts().add(scatterChart);
	}
	
	public void addPlugin(XYChartPlugin<Number, Number> plugin, Cursor cursor) {
		removePlugins();

		chartPane.getPlugins().add(plugin);
		chartPane.setCursor(cursor);
	}
	
	public void removePlugins() {
		while (chartPane.getPlugins().size()>0)
			chartPane.getPlugins().remove(0);
		
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
	
	private void resetXAxis(NumericAxis xAxis) {
		if (xAxis.getLowerBound() > xMAX || xAxis.getLowerBound() > xMIN) {
			xAxis.setLowerBound(xMIN);
			xAxis.setUpperBound(xMAX);
		} else {
			xAxis.setUpperBound(xMAX);
			xAxis.setLowerBound(xMIN);
		}
	}
	
	private void resetYAxis(NumericAxis yAxis) {
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
	
	public Node getNode() {
		return chartPane;
	}
	
	public DatasetOptionsPane getDataOptionsPane() {
		return datasetOptionsPane;
	}
	
	public JFXBadge getDatasetOptionsButton() {
		return datasetOptionsButton;
	}
	
	private MARSResultsTable getDataTable() {
		if (molecule != null) {
			return molecule.getDataTable();
		} else if (table != null) {
			return this.table;
		}
		return null;
	}
	
	@Override
	public void setMolecule(Molecule molecule) {
		this.molecule = molecule;
		datasetOptionsPane.setTable(getDataTable());
		update();
	}

	@Override
	public void setTable(MARSResultsTable table) {
		this.table = table;
		datasetOptionsPane.setTable(table);
		update();
	}
}
