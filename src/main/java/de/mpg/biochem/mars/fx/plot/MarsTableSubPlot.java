package de.mpg.biochem.mars.fx.plot;

import java.util.ArrayList;
import java.util.List;

import cern.extjfx.chart.NumericAxis;
import cern.extjfx.chart.data.DataReducingObservableList;
import de.mpg.biochem.mars.table.MarsTable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;

public class MarsTableSubPlot extends AbstractSubPlot {
	protected MarsTable table;
	
	public MarsTableSubPlot(PlotPane plotPane, String plotTitle, MarsTable table) {
		super(plotPane, plotTitle);
		this.table = table;
		getDatasetOptionsPane().setTable(table);
		update();
	}
	
	protected void addLine(PlotSeries plotSeries) {
		String xColumn = plotSeries.getXColumn();
		String yColumn = plotSeries.getYColumn();
		
		NumericAxis xAxis = createAxis();
		NumericAxis yAxis = createAxis();
		
		resetXYZoom();
		
		resetXAxis(xAxis);
		resetYAxis(yAxis);
		
		LineChartWithSegments lineChart = new LineChartWithSegments(null, plotSeries, xAxis, yAxis);
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
	
	protected void addScatter(PlotSeries plotSeries) {
		String xColumn = plotSeries.getXColumn();
		String yColumn = plotSeries.getYColumn();

		NumericAxis xAxis = createAxis();
		NumericAxis yAxis = createAxis();
		
		resetXYZoom();
		
		resetXAxis(xAxis);
		resetYAxis(yAxis);

		ScatterChartWithSegments scatterChart = new ScatterChartWithSegments(null, plotSeries, xAxis, yAxis);
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

	@Override
	protected MarsTable getDataTable() {
		return table;
	}

	@Override
	public void fireEvent(Event event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeTools() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeIndicators() {
		// TODO Auto-generated method stub
		
	}
}
