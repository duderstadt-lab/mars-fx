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
	
	protected ErrorDataSetRenderer filledHistogramRenderer;
	protected ErrorDataSetRenderer outlineHistogramRenderer;
	
	protected ArrayList<Histogram> filledHistograms, outlineHistograms;
	
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
        
        filledHistogramRenderer = new ErrorDataSetRenderer();
        filledHistogramRenderer.setPolyLineStyle(LineStyle.HISTOGRAM_FILLED);
        //filledHistogramRenderer.setErrorType(ErrorStyle.NONE);
        //Make sure this is set to false. Otherwise second to last points seems to be lost :(...
        filledHistogramRenderer.pointReductionProperty().set(false);
        
        outlineHistogramRenderer = new ErrorDataSetRenderer();
        outlineHistogramRenderer.setPolyLineStyle(LineStyle.STAIR_CASE);
        outlineHistogramRenderer.setErrorType(ErrorStyle.NONE);
        //outlineHistogramRenderer.setErrorType(ErrorStyle.ERRORBARS);
        outlineHistogramRenderer.pointReductionProperty().set(false);
        
        filledHistograms = new ArrayList<Histogram>();
        outlineHistograms = new ArrayList<Histogram>();
        
        histChart.getRenderers().add(filledHistogramRenderer);
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
		
		for (String name : outputs.keySet())
			if (outputs.get(name) == null) 
				return;
		
		filledHistograms.clear();
		outlineHistograms.clear();
	
		String yLabel = (String) outputs.get("yLabel");
		String xLabel = (String) outputs.get("xLabel");
		String title = (String)outputs.get("title");
		Integer bins = (Integer)outputs.get("bins");
		Double min = (Double)outputs.get("min");
		Double max = (Double)outputs.get("max");
		
		Set<String> series = new HashSet<String>();
		for (String outputName : outputs.keySet()) {
			if(outputName.startsWith("series")) {
				int index = outputName.indexOf("_");
				series.add(outputName.substring(0, index));
			}
		}
		
		for (String seriesName : series) {
			Histogram hist = buildDataSet(outputs, seriesName, bins.intValue(), min.doubleValue(), max.doubleValue());
			
			Boolean fill = new Boolean(true);
			if (outputs.containsKey(seriesName + "_" + "fill"))
				fill = (Boolean)outputs.get(seriesName + "_" + "fill");
			
			if (fill.booleanValue())
				filledHistograms.add(hist);
			else {
				outlineHistograms.add(hist);
			}
		}
			
        Platform.runLater(new Runnable() {
			@Override
			public void run() {
				xAxis.setName(xLabel);
				yAxis.setName(yLabel);
				xAxis.setAutoRanging(false);
				xAxis.setMin(min);
				xAxis.setMax(max);
				
				histChart.setTitle(title);
			    //xAxis.setAutoRanging(true);

				filledHistogramRenderer.getDatasets().clear();
				filledHistogramRenderer.getDatasets().addAll(filledHistograms);
				
				outlineHistogramRenderer.getDatasets().clear();
				outlineHistogramRenderer.getDatasets().addAll(outlineHistograms);
			}
    	});
	}
	
	protected Histogram buildDataSet(Map<String, Object> outputs, String seriesName, int bins, double min, double max) {
		Histogram hist = new Histogram(seriesName, bins, min, max);
		
		if (outputs.containsKey(seriesName + "_" + "values")) {
			Double[] values = (Double[]) outputs.get(seriesName + "_" + "values");
			for (Double value : values)
				hist.fill(value.doubleValue());
		}
		
		Random r = new Random();
		
		for (int i=0; i<100; i++) {
			hist.fill(r.nextGaussian());
		}
		
		String styleString = "";
		if (outputs.containsKey(seriesName + "_" + "strokeColor"))
			styleString += "strokeColor=" + (String)outputs.get(seriesName + "_" + "strokeColor") + "; ";
		if (outputs.containsKey(seriesName + "_" + "strokeWidth"))
			styleString += "strokeWidth=" + ((Integer)outputs.get(seriesName + "_" + "strokeWidth")).intValue();
		
    	hist.setStyle(styleString);
    	
    	for (int i=0; i < bins;i++) {
    		System.out.println("X " + hist.getBinCenter(0, i) + " Y " + hist.getBinContent(i));
    	}
    	
    	return hist;
	}
	
	@Override
	public Node getIcon() {
		Region barchartIcon = new Region();
		barchartIcon.getStyleClass().add("barchartIcon");
		return barchartIcon;
	}
}
