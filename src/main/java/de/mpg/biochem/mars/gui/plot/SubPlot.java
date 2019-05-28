package de.mpg.biochem.mars.gui.plot;

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
import de.mpg.biochem.mars.gui.molecule.moleculesTab.MoleculeSubTab;
import de.mpg.biochem.mars.gui.util.Action;
import de.mpg.biochem.mars.gui.util.ActionUtils;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.table.MARSResultsTable;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.ButtonBase;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;

public class SubPlot implements MoleculeSubTab {
	private NumericAxis xAxis;
	private NumericAxis yAxis;
	private LineChart<Number, Number> lineChart;
	private ScatterChart<Number, Number> scatterChart;
	private XYChartPane<Number, Number> chartPane;
	
	private Molecule molecule;
	
	private JFXBadge datasetOptionsButton;
	private DatasetOptionsPane datasetOptionsPane;
	
	private PlotPane plotPane;
	
	public SubPlot(PlotPane plotPane) {
		this.plotPane = plotPane;
		
		datasetOptionsPane = new DatasetOptionsPane(molecule, this);
		datasetOptionsButton = new JFXBadge(ActionUtils.createToolBarButton(new Action("Dataset", "Shortcut+C", LINE_CHART, e -> {
			PopOver popOver = new PopOver();
			popOver.setTitle("Dataset");
			popOver.setHeaderAlwaysVisible(true);
			popOver.setAutoHide(false);
			popOver.setArrowLocation(ArrowLocation.TOP_CENTER);
			popOver.setContentNode(datasetOptionsPane);
			popOver.show(datasetOptionsButton);
		})));

		xAxis = new NumericAxis();
		xAxis.setAutoRangeRounding(false);
		xAxis.setForceZeroInRange(false);
		xAxis.setAutoRangePadding(0);
		
		yAxis = new NumericAxis();
		yAxis.setAutoRangeRounding(false);
		yAxis.setForceZeroInRange(false);
		yAxis.setAutoRangePadding(0);
		
		lineChart = new LineChart<Number, Number>(xAxis, yAxis) {
            private List<Shape> shapes = new ArrayList<>();

            @Override
            public void layoutPlotChildren() {
                super.layoutPlotChildren();
                getPlotChildren().removeAll(shapes);
                shapes.clear();
                for (int i=0;i<getPlotSeriesList().size();i++) {
                	if (molecule.hasSegmentsTable(getPlotSeriesList().get(i).getXColumn(), getPlotSeriesList().get(i).getYColumn())) {
	                	MARSResultsTable segmentTable = molecule.getSegmentsTable(getPlotSeriesList().get(i).getXColumn(), getPlotSeriesList().get(i).getYColumn());
	                	for (int row=0;row<segmentTable.getRowCount();row++) {
	                		if (!Double.isNaN(segmentTable.getValue("x1", row)) && 
	                			!Double.isNaN(segmentTable.getValue("y1", row)) &&	
	                			!Double.isNaN(segmentTable.getValue("x2", row)) &&
	                			!Double.isNaN(segmentTable.getValue("y2", row))) {
				                    double x1 = xAxis.getDisplayPosition(segmentTable.getValue("x1", row));
				                    double y1 = yAxis.getDisplayPosition(segmentTable.getValue("y1", row));
				                    double x2 = xAxis.getDisplayPosition(segmentTable.getValue("x2", row));
				                    double y2 = yAxis.getDisplayPosition(segmentTable.getValue("y2", row));
				                    Line line = new Line(x1, y1, x2, y2);
				                    line.setStroke(getPlotSeriesList().get(i).getSegmentsColor());
				                    line.setStrokeWidth(1);
				                    shapes.add(line);
	                		}	
	                	}
                	}
                }
                getPlotChildren().addAll(shapes);
            }
        };
		lineChart.setCreateSymbols(false);
		lineChart.setAnimated(false);
		/*
		scatterChart = new ScatterChart<Number, Number>(xAxis, yAxis) {
            private List<Shape> shapes = new ArrayList<>();

            @Override
            public void layoutPlotChildren() {
                super.layoutPlotChildren();
                getPlotChildren().removeAll(shapes);
                shapes.clear();
                for (int i=0;i<getPlotSeriesList().size();i++) {
                	if (molecule.hasSegmentsTable(getPlotSeriesList().get(i).getXColumn(), getPlotSeriesList().get(i).getYColumn())) {
	                	MARSResultsTable segmentTable = molecule.getSegmentsTable(getPlotSeriesList().get(i).getXColumn(), getPlotSeriesList().get(i).getYColumn());
	                	for (int row=0;row<segmentTable.getRowCount();row++) {
	                		if (!Double.isNaN(segmentTable.getValue("x1", row)) && 
	                			!Double.isNaN(segmentTable.getValue("y1", row)) &&	
	                			!Double.isNaN(segmentTable.getValue("x2", row)) &&
	                			!Double.isNaN(segmentTable.getValue("y2", row))) {
				                    double x1 = xAxis.getDisplayPosition(segmentTable.getValue("x1", row));
				                    double y1 = yAxis.getDisplayPosition(segmentTable.getValue("y1", row));
				                    double x2 = xAxis.getDisplayPosition(segmentTable.getValue("x2", row));
				                    double y2 = yAxis.getDisplayPosition(segmentTable.getValue("y2", row));
				                    Line line = new Line(x1, y1, x2, y2);
				                    line.setStroke(getPlotSeriesList().get(i).getSegmentsColor());
				                    line.setStrokeWidth(1);
				                    shapes.add(line);
	                		}	
	                	}
                	}
                }
                getPlotChildren().addAll(shapes);
            }
        };
		scatterChart.setAnimated(false);
		*/
		chartPane = new XYChartPane<>(lineChart);
		chartPane.setMaxHeight(Double.MAX_VALUE);
		chartPane.setMaxWidth(Double.MAX_VALUE);
		//chartPane.getOverlayCharts().add(scatterChart);
		
		//For the moment lets hide the legend
		chartPane.setLegendVisible(false);
	}
	
	
	public void addSeries(PlotSeries plotSeries) {
		getPlotSeriesList().add(plotSeries);
	}
	
	public void clear() {
		lineChart.getData().clear();
		//scatterChart.getData().clear();
	}
	
	public void setTitle(String name) {
		chartPane.setTitle(name);
	}
	
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
	
	public ObservableList<PlotSeries> getPlotSeriesList() {
		return datasetOptionsPane.getPlotSeriesList();
	}
	
	public void update() {
		clear();

		for (int i=0;i<getPlotSeriesList().size();i++) {
			if (getPlotSeriesList().get(i).xColumnField().getSelectionModel().getSelectedIndex() != -1 
				&& getPlotSeriesList().get(i).yColumnField().getSelectionModel().getSelectedIndex() != -1)
					loadPlotSeries(getPlotSeriesList().get(i), lineChart);
			//NEED TO ADD SCATTER PLOT UPDATE HERE
		}
		if (!datasetOptionsPane.getTitle().equals(""))
			setTitle(datasetOptionsPane.getTitle());
		if (!datasetOptionsPane.getXAxisName().equals(""))
			setXLabel(datasetOptionsPane.getXAxisName());
		if (!datasetOptionsPane.getYAxisName().equals(""))
			setYLabel(datasetOptionsPane.getYAxisName());
		
		updateLegend();
	}
	
	private void loadPlotSeries(PlotSeries plotSeries, LineChart<Number, Number> currentLineChart) {
		MARSResultsTable table = molecule.getDataTable();
		String xColumn = plotSeries.getXColumn();
		String yColumn = plotSeries.getYColumn();
		String width = plotSeries.getWidth();
		Color color = plotSeries.getColor();
		
		DataReducingObservableList<Number, Number> reducedData = new DataReducingObservableList<>(xAxis);
		ArrayData<Number, Number> sourceData = ArrayData.of(table.getColumnAsDoubles(xColumn), table.getColumnAsDoubles(yColumn));
		reducedData.setData(sourceData);
		reducedData.maxPointsCountProperty().bind(plotPane.maxPointsCount());
		
		Series<Number, Number> series = new Series<>(plotSeries.getYColumn(), reducedData);
		currentLineChart.getData().add(series);
		final String colorString = String.format("rgba(%d, %d, %d, 1.0)", Math.round(color.getRed()*255), Math.round(color.getGreen()*255), Math.round(color.getBlue()*255));
		final String lineStyle = String.format("-fx-stroke-width: %s; -fx-stroke: %s;", width, colorString);
		series.getNode().lookup(".chart-series-line").setStyle(lineStyle);
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
		xAxis.setLabel(xAxisLabel);
	}
	
	public void setYLabel(String yAxisLabel) {
		yAxis.setLabel(yAxisLabel);
	}
	
	public void resetXYZoom() {
		double xMIN = Double.MAX_VALUE;
		double xMAX = Double.MIN_VALUE;
		
		double yMIN = Double.MAX_VALUE;
		double yMAX = Double.MIN_VALUE;
		
		for (int i=0; i < getPlotSeriesList().size(); i++) {
			String xColumn = getPlotSeriesList().get(i).getXColumn();
			String yColumn = getPlotSeriesList().get(i).getYColumn();
			
			double xmin = molecule.getDataTable().min(xColumn);
			double xmax = molecule.getDataTable().max(xColumn);
			
			double ymin = molecule.getDataTable().min(yColumn);
			double ymax = molecule.getDataTable().max(yColumn);
			
			if (xmin < xMIN)
				xMIN = xmin;
			
			if (xmax > xMAX)
				xMAX = xmax;
		
			if (ymin < yMIN)
				yMIN = ymin;
			
			if (ymax > yMAX)
				yMAX = ymax;
		}
		
		xAxis.setLowerBound(xMIN);
		xAxis.setUpperBound(xMAX);
		
		yAxis.setLowerBound(yMIN);
		yAxis.setUpperBound(yMAX);
	}
	
	public NumericAxis getXAxis() {
		return xAxis;
	}
	
	public NumericAxis getYAxis() {
		return yAxis;
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
	
	@Override
	public void setMolecule(Molecule molecule) {
		this.molecule = molecule;
		datasetOptionsPane.setMolecule(molecule);
		update();
	}
}
