package de.mpg.biochem.mars.gui;

import cern.extjfx.chart.NumericAxis;
import cern.extjfx.chart.XYChartPane;
import cern.extjfx.chart.data.DataReducingObservableList;
import cern.extjfx.chart.plugins.CrosshairIndicator;
import cern.extjfx.chart.plugins.DataPointTooltip;
import cern.extjfx.chart.plugins.Panner;
import cern.extjfx.chart.plugins.Zoomer;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;

import javafx.scene.chart.XYChart;
import de.mpg.biochem.mars.table.*;
import org.scijava.table.*;

public class Plot extends AbstractSamplePane {
	DataReducingObservableList<Number, Number> data;

	@Override
	public String getName() {
		return "Data Set";
	}

	@Override
	public String getDescription() {
		return "The DataReducingObservableList reduces the number of points to be visualized in the given X range. ";
	}

	@Override
	public Node createSamplePane() {
		NumericAxis xAxis = new NumericAxis();
		//xAxis.setAnimated(false);

		NumericAxis yAxis = new NumericAxis();
		//yAxis.setAnimated(false);
		yAxis.setAutoRangePadding(0.1);

		final int pointsCount = 100_000;
		LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
		lineChart.setTitle("Series with " + pointsCount + " points");
		//lineChart.setAnimated(false);
		//lineChart.setCreateSymbols(false);

		//data = new DataReducingObservableList<>(xAxis, RandomDataGenerator.generateData(0, 1, pointsCount));
		MARSResultsTable table = new MARSResultsTable("My Table");
		
		DoubleColumn xCol = new DoubleColumn("X");
		DoubleColumn yCol = new DoubleColumn("Y");
		
		for (int row=0;row<100;row++) {
			xCol.add((double)row*0.25);
			yCol.add((double)row);
		}
		
		table.add(xCol);
		table.add(yCol);
		
		XYChart.Series series = new XYChart.Series();
		for (int row=0; row< table.getRowCount(); row++) {
			series.getData().add(new XYChart.Data(table.getValue(0, row), table.getValue(1, row)));
		}
		
		lineChart.getData().add(series);
		lineChart.setStyle("-fx-stroke-width: 1px;");

		XYChartPane<Number, Number> chartPane = new XYChartPane<>(lineChart);
		chartPane.getPlugins().addAll(new Zoomer(), new Panner(), new DataPointTooltip(), new CrosshairIndicator<>());
		
		return chartPane;
	}
}
