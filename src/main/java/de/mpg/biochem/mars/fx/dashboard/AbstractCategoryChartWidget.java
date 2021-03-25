/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2021 Karl Duderstadt
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

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static de.jensd.fx.glyphs.octicons.OctIcon.BEAKER;
import static de.jensd.fx.glyphs.octicons.OctIcon.CODE;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;

import de.jensd.fx.glyphs.GlyphIcons;
import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.fx.plot.tools.MarsCategoryAxis;
import de.mpg.biochem.mars.fx.plot.tools.MarsNumericAxis;
import de.mpg.biochem.mars.fx.plot.tools.MarsZoomer;
import de.mpg.biochem.mars.fx.plot.tools.SegmentDataSetRenderer;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.table.MarsTable;
import de.gsi.chart.XYChart;
import de.gsi.chart.axes.AxisLabelOverlapPolicy;
import de.gsi.chart.axes.spi.CategoryAxis;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.LineStyle;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.dataset.spi.DefaultErrorDataSet;
import de.gsi.dataset.testdata.spi.RandomDataGenerator;
import javafx.geometry.Insets;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import net.imagej.ops.Initializable;
import javafx.scene.layout.AnchorPane;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.module.ModuleException;
import org.scijava.script.ScriptLanguage;
import javafx.embed.swing.SwingNode;
import javafx.application.Application;
import javafx.application.Platform;

import javax.swing.SwingUtilities;
import javafx.scene.control.ScrollPane;

import javax.swing.JScrollPane;

import org.scijava.script.ScriptHeaderService;
import org.scijava.script.ScriptInfo;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;

import org.scijava.module.ModuleService;
import javafx.scene.control.TextArea;
import javafx.scene.Node;

import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import org.apache.commons.io.IOUtils;
import org.scijava.Cancelable;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;

public abstract class AbstractCategoryChartWidget extends AbstractScriptableWidget
		implements MarsDashboardWidget, Initializable {

	protected XYChart barChart;
	protected MarsCategoryAxis xAxis;
	protected MarsNumericAxis yAxis;

	protected ArrayList<String> requiredGlobalFields = new ArrayList<String>(
			Arrays.asList("xlabel", "ylabel", "title", "xvalues", "yvalues"));

	@Override
	public void initialize() {
		super.initialize();

		xAxis = new MarsCategoryAxis("Categories");
		xAxis.setOverlapPolicy(AxisLabelOverlapPolicy.SHIFT_ALT);

		yAxis = new MarsNumericAxis();
		yAxis.setName("Frequency");
		yAxis.setMinorTickVisible(false);
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

		// Make sure this is set to false. Otherwise second to last points seems to be
		// lost :(...
		renderer.pointReductionProperty().set(false);
		barChart.getRenderers().add(renderer);
		barChart.setLegend(null);
		barChart.horizontalGridLinesVisibleProperty().set(false);
		barChart.verticalGridLinesVisibleProperty().set(false);
		
		barChart.setTriggerDistance(0);
		
		barChart.setPrefSize(100, 100);
		barChart.setPadding(new Insets(10, 20, 10, 10));
		setContent(getIcon(), barChart);

		rootPane.setMinSize(250, 250);
		rootPane.setMaxSize(250, 250);
	}

	@Override
	public void run() {
		Map<String, Object> outputs = runScript();

		if (outputs == null)
			return;

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

		if (outputs.containsKey("color"))
			dataSet.setStyle("fillColor:" + (String) outputs.get("color") + ";");

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
				} else if (outputs.containsKey("ymax")) {
					yAxis.setAutoRanging(false);
					yAxis.setMin(0.0);
					yAxis.setMax((Double) outputs.get("ymax"));
				} else if (outputs.containsKey("ymin")) {
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
