package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import org.scijava.plugin.Plugin;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.AxisLabelOverlapPolicy;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.renderer.LineStyle;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.dataset.spi.DefaultErrorDataSet;
import de.jensd.fx.glyphs.GlyphIcons;
import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.fx.plot.tools.MarsCategoryAxis;
import de.mpg.biochem.mars.fx.plot.tools.MarsNumericAxis;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import de.gsi.chart.renderer.ErrorStyle;

import de.gsi.dataset.spi.Histogram;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.scijava.Cancelable;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;

import net.imagej.ops.Initializable;
import org.apache.commons.math3.stat.Frequency;

@Plugin( type = HistogramWidget.class, name = "HistogramWidget" )
public class HistogramWidget extends AbstractScriptableWidget implements MarsDashboardWidget, SciJavaPlugin, Initializable {

	protected XYChart histChart;
	protected MarsNumericAxis xAxis, yAxis;
	
	protected ErrorDataSetRenderer outlineHistogramRenderer;
	
	protected ArrayList<Histogram> outlineHistograms;
	
	protected ArrayList<String> requiredGlobalFields = new ArrayList<String>(Arrays.asList("xlabel", 
			"ylabel", "title", "bins", "xmin", "xmax"));
	
	@Override
	public void initialize() {
		super.initialize();
		
		try {
			loadScript("histogramchart.groovy");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		xAxis = new MarsNumericAxis("");
        //xAxis.setOverlapPolicy(AxisLabelOverlapPolicy.SHIFT_ALT);
        xAxis.minorTickVisibleProperty().set(false);
        xAxis.setAutoRangeRounding(false);
        //xAxis.setAutoRanging(true);
        yAxis = new MarsNumericAxis("");
        yAxis.setMinorTickVisible(false);
        yAxis.setForceZeroInRange(true);
        yAxis.setAutoRanging(true);
        yAxis.setAutoRangeRounding(false);

        histChart = new XYChart(xAxis, yAxis);
        histChart.setAnimated(false);
        histChart.getRenderers().clear();
        
        outlineHistogramRenderer = new ErrorDataSetRenderer();
        outlineHistogramRenderer.setPolyLineStyle(LineStyle.HISTOGRAM);
        outlineHistogramRenderer.setErrorType(ErrorStyle.NONE);
        outlineHistogramRenderer.pointReductionProperty().set(false);
        
        outlineHistograms = new ArrayList<Histogram>();
        
        histChart.getRenderers().add(outlineHistogramRenderer);
        histChart.legendVisibleProperty().set(false);
        histChart.horizontalGridLinesVisibleProperty().set(false);
        histChart.verticalGridLinesVisibleProperty().set(false);

		StackPane stack = new StackPane();
		stack.setPadding(new Insets(10, 10, 10, 10));
		stack.getChildren().add(histChart);
		stack.setPrefSize(250, 250);

        BorderPane chartPane = new BorderPane();
        chartPane.setCenter(stack);
        setContent(getIcon(), chartPane);
        
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
		
		outlineHistograms.clear();
	
		String ylabel = (String) outputs.get("ylabel");
		String xlabel = (String) outputs.get("xlabel");
		String title = (String)outputs.get("title");
		Integer bins = (Integer)outputs.get("bins");
		Double xmin = (Double)outputs.get("xmin");
		Double xmax = (Double)outputs.get("xmax");
		
		double[] xBins = new double[bins.intValue() + 1];
		xBins[0] = xmin.doubleValue();
		double binWidth = (xmax.doubleValue() - xmin.doubleValue())/bins.doubleValue();
		for (int bin=0;bin<bins.intValue();bin++)
			xBins[bin + 1] = xBins[0] + (bin + 1)*binWidth;
		
		Set<String> series = new HashSet<String>();
		for (String outputName : outputs.keySet()) {
			if(outputName.startsWith("series")) {
				int index = outputName.indexOf("_");
				series.add(outputName.substring(0, index));
			}
		}
		
		for (String seriesName : series) {
			Histogram dataset = buildDataSet(outputs, seriesName, xBins);
			if (dataset != null)
				outlineHistograms.add(dataset);
			else {
				return;
			}
		}
			
        Platform.runLater(new Runnable() {
			@Override
			public void run() {
				xAxis.setName(xlabel);
				yAxis.setName(ylabel);
				xAxis.setAutoRanging(false);
				xAxis.setMin(xmin);
				xAxis.setMax(xmax);
				
				histChart.setTitle(title);
				
				outlineHistogramRenderer.getDatasets().clear();
				outlineHistogramRenderer.getDatasets().addAll(outlineHistograms);
			}
    	});
	}
	
	protected Histogram buildDataSet(Map<String, Object> outputs, String seriesName, double[] xBins) {
		Histogram hist = new Histogram(seriesName, xBins);
		
		if (outputs.containsKey(seriesName + "_" + "values")) {
			Double[] values = (Double[]) outputs.get(seriesName + "_" + "values");
			for (Double value : values)
				hist.fill(value.doubleValue());
		} else {
			writeToLog("Required field " + seriesName + "_values is missing.");
			return null;
		}
		
		String styleString = "";
		if (outputs.containsKey(seriesName + "_" + "strokeColor"))
			styleString += "strokeColor=" + (String)outputs.get(seriesName + "_" + "strokeColor") + "; ";
		if (outputs.containsKey(seriesName + "_" + "strokeWidth"))
			styleString += "strokeWidth=" + ((Integer)outputs.get(seriesName + "_" + "strokeWidth")).intValue();
		
    	hist.setStyle(styleString);
    	
    	return hist;
	}
	
	@Override
	public Node getIcon() {
		Region barchartIcon = new Region();
		barchartIcon.getStyleClass().add("barchartIcon");
		return barchartIcon;
	}
}
