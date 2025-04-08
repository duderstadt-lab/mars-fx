/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2025 Karl Duderstadt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package de.mpg.biochem.mars.fx.dashboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.imagej.ops.Initializable;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.AxisLabelOverlapPolicy;
import  io.fair_acc.chartfx.renderer.LineStyle;
import  io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import  io.fair_acc.dataset.spi.DefaultErrorDataSet;
import de.mpg.biochem.mars.fx.plot.tools.MarsCategoryAxis;
import de.mpg.biochem.mars.fx.plot.tools.MarsNumericAxis;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

public abstract class AbstractCategoryChartWidget extends
	AbstractScriptableWidget implements MarsDashboardWidget, Initializable
{

	protected XYChart barChart;
	protected MarsCategoryAxis xAxis;
	protected MarsNumericAxis yAxis;

	protected ArrayList<String> requiredGlobalFields = new ArrayList<String>(
		Arrays.asList("xlabel", "ylabel", "title", "xvalues", "yvalues"));

	@Override
	public void initialize() {
		super.initialize();

		if (lang.getLanguageName().equals("Python (PyImageJ)")) {
			setContent(getIcon(), new BorderPane());
		}
		else {
			xAxis = new MarsCategoryAxis("Categories");
			xAxis.setOverlapPolicy(AxisLabelOverlapPolicy.SHIFT_ALT);

			yAxis = new MarsNumericAxis();
			yAxis.setName("Frequency");
			yAxis.setMinorTickCount(0);
			yAxis.setForceZeroInRange(true);
			yAxis.setAutoRanging(true);
			yAxis.setAutoRangeRounding(false);
			// yAxis.setTickLabelFormatter(new MarsIntegerFormatter());

			barChart = new XYChart(xAxis, yAxis);
			barChart.setAnimated(false);
			barChart.getRenderers().clear();
			final ErrorDataSetRenderer renderer = new ErrorDataSetRenderer();
			renderer.setPolyLineStyle(LineStyle.NONE);
			renderer.setDrawBars(true);
			renderer.setBarWidthPercentage(70);
			renderer.setDrawMarker(false);

			// Make sure this is set to false. Otherwise second to last points seems
			// to be
			// lost :(...
			renderer.pointReductionProperty().set(false);
			barChart.getRenderers().add(renderer);
			barChart.setLegendVisible(false);
			//barChart.horizontalGridLinesVisibleProperty().set(false);
			//barChart.verticalGridLinesVisibleProperty().set(false);

			//barChart.setTriggerDistance(0);

			barChart.setPrefSize(100, 100);
			barChart.setPadding(new Insets(10, 20, 10, 10));
			setContent(getIcon(), barChart);
		}

		rootPane.setMinSize(250, 250);
		rootPane.setMaxSize(250, 250);
	}

	@Override
	public void run() {
		Map<String, Object> outputs = runScript();

		if (outputs == null) return;

		if (lang.getLanguageName().equals("Python (PyImageJ)")) {
			if (!outputs.containsKey("imgsrc")) {
				writeToLog("required output imgsrc is missing.");
				return;
			}

			imgsrc = (String) outputs.get("imgsrc");
			loadImage();
			return;
		}

		for (String field : requiredGlobalFields)
			if (!outputs.containsKey(field)) {
				writeToLog("required output " + field + " is missing.");
				return;
			}

		String xLabel = (String) outputs.get("xlabel");
		String yLabel = (String) outputs.get("ylabel");
		String title = (String) outputs.get("title");
		String[] xValues = (String[]) outputs.get("xvalues");
		Double[] yValues = (Double[]) outputs.get("yvalues");

		if (xValues.length != yValues.length) {
			writeToLog("The length of xvalues does not match that of yvalues.");
			return;
		}

		final DefaultErrorDataSet dataSet = new DefaultErrorDataSet("myData");

		if (outputs.containsKey("color")) dataSet.setStyle("fillColor:" +
			(String) outputs.get("color") + ";");

		List<String> categories = new ArrayList<String>();

		for (int row = 0; row < xValues.length; row++) {
			dataSet.add(row, yValues[row].doubleValue());
			categories.add(xValues[row]);
		}

		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				xAxis.setName(xLabel);
				yAxis.setName(yLabel);
				// Check if a y-range was provided
				if (outputs.containsKey("ymin") && outputs.containsKey("ymax")) {
					yAxis.setAutoRanging(false);
					yAxis.setMin((Double) outputs.get("ymin"));
					yAxis.setMax((Double) outputs.get("ymax"));
				}
				else if (outputs.containsKey("ymax")) {
					yAxis.setAutoRanging(false);
					yAxis.setMin(0.0);
					yAxis.setMax((Double) outputs.get("ymax"));
				}
				else if (outputs.containsKey("ymin")) {
					yAxis.setAutoRanging(true);
				}

				xAxis.setCategories(categories);
				barChart.setTitle(title);
				barChart.getDatasets().clear();
				barChart.getDatasets().add(dataSet);

				Platform.runLater(() -> barChart.layoutChildren());
			}
		});
	}

	@Override
	public Node getIcon() {
		Region categoryIcon = new Region();
		categoryIcon.getStyleClass().add("categoriesIcon");
		return categoryIcon;
	}
}
