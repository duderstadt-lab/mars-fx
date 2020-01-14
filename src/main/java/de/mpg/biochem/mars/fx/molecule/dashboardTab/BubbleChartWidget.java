package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import de.gsi.chart.XYChart;
import de.gsi.chart.marker.DefaultMarker;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.LineStyle;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.dataset.spi.DefaultErrorDataSet;
import de.gsi.dataset.spi.Histogram;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.fx.plot.tools.MarsDataPointTracker;
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

import de.gsi.chart.plugins.DataPointTooltip;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.scijava.Cancelable;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;

import net.imagej.ops.Initializable;

import org.apache.commons.lang3.ArrayUtils;

@Plugin( type = BubbleChartWidget.class, name = "BubbleChartWidget" )
public class BubbleChartWidget extends AbstractScriptableWidget implements MarsDashboardWidget, SciJavaPlugin, Initializable {

	protected XYChart bubbleChart;
	protected MarsNumericAxis xAxis, yAxis;
	
	protected ErrorDataSetRenderer renderer;
	
	protected ArrayList<DefaultErrorDataSet> datasets;
	
	@Override
	public void initialize() {
		super.initialize();
		
		try {
			loadScript("bubblechart.groovy");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		xAxis = new MarsNumericAxis("");
        xAxis.minorTickVisibleProperty().set(false);
        xAxis.setAutoRanging(true);
        xAxis.setAutoRangeRounding(false);
        
        yAxis = new MarsNumericAxis("");
        yAxis.setMinorTickVisible(false);
        yAxis.setAutoRanging(true);
        yAxis.setAutoRangeRounding(false);

        bubbleChart = new XYChart(xAxis, yAxis);
        //bubbleChart.getPlugins().add(new DataPointTooltip());
        bubbleChart.getPlugins().add(new MarsDataPointTracker());
        bubbleChart.setAnimated(false);
        bubbleChart.getRenderers().clear();
        
        bubbleChart.getPlugins().add(new MarsDataPointTracker());
        
        datasets = new ArrayList<DefaultErrorDataSet>();
        
        renderer = new ErrorDataSetRenderer();
        renderer.setMarkerSize(5);
        renderer.setPolyLineStyle(LineStyle.NONE);
        renderer.setErrorType(ErrorStyle.NONE);
        renderer.setDrawMarker(true);
        renderer.setAssumeSortedData(false);
        
        bubbleChart.getRenderers().add(renderer);
        bubbleChart.legendVisibleProperty().set(false);
        bubbleChart.horizontalGridLinesVisibleProperty().set(false);
        bubbleChart.verticalGridLinesVisibleProperty().set(false);

		StackPane stack = new StackPane();
		stack.setPadding(new Insets(10, 10, 10, 10));
		stack.getChildren().add(bubbleChart);
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
		
		datasets.clear();
	
		String ylabel = (String) outputs.get("ylabel");
		String xlabel = (String) outputs.get("xlabel");
		String title = (String)outputs.get("title");
		
		Double xmin = (Double) outputs.get("xmin");
		Double xmax = (Double) outputs.get("xmax");
		Double ymin = (Double) outputs.get("ymin");
		Double ymax = (Double) outputs.get("ymax");
		
		
		Set<String> series = new HashSet<String>();
		for (String outputName : outputs.keySet()) {
			if(outputName.startsWith("series")) {
				int index = outputName.indexOf("_");
				series.add(outputName.substring(0, index));
			}
		}
		
		for (String seriesName : series) 
			datasets.add(buildDataSet(outputs, seriesName));
			
        Platform.runLater(new Runnable() {
			@Override
			public void run() {
				xAxis.setName(xlabel);
				xAxis.setAutoRanging(false);
				xAxis.setMin(xmin);
				xAxis.setMax(xmax);
				
				yAxis.setName(ylabel);
				yAxis.setAutoRanging(false);
				yAxis.setMin(ymin);
				yAxis.setMax(ymax);
				
				bubbleChart.setTitle(title);
				
				renderer.getDatasets().clear();
				renderer.getDatasets().addAll(datasets);
			}
    	});
	}
	
	protected DefaultErrorDataSet buildDataSet(Map<String, Object> outputs, String seriesName) {
		DefaultErrorDataSet dataset = new DefaultErrorDataSet(seriesName);
		
		int dataPointCount = 0;
		
		if (outputs.containsKey(seriesName + "_xvalues") 
				&& outputs.containsKey(seriesName + "_yvalues")) {
			Double[] xvalues = (Double[]) outputs.get(seriesName + "_xvalues");
			Double[] yvalues = (Double[]) outputs.get(seriesName + "_yvalues");
			
			dataPointCount = xvalues.length;
			
			for (int index=0;index<xvalues.length;index++)
				dataset.add(xvalues[index], yvalues[index]);
		}
		
		if (outputs.containsKey(seriesName + "_label")) {
			String[] label = (String[]) outputs.get(seriesName + "_label");
			for (int index=0;index<label.length;index++)
				dataset.addDataLabel(index, label[index]);
		}
		
		String[] styleString = new String[dataPointCount];
		for (int index=0;index<dataPointCount;index++)
			styleString[index] = "";
		
		if (outputs.containsKey(seriesName + "_color")) {
			String[] colors = (String[]) outputs.get(seriesName + "_color");
			for (int index=0;index<dataPointCount;index++)
				styleString[index] += "markerColor=" + colors[index] + "; ";
		} else if (outputs.containsKey(seriesName + "_markerColor")) {
			for (int index=0;index<dataPointCount;index++)
				styleString[index] += "markerColor=" + (String)outputs.get(seriesName + "_markerColor") + "; ";
		}
		
		if (outputs.containsKey(seriesName + "_size")) {
			Double[] size = (Double[]) outputs.get(seriesName + "_size");
			for (int index=0;index<dataPointCount;index++)
				styleString[index] += "markerSize=" + size[index] + "; ";
		}
		
		for (int index=0;index<dataPointCount;index++)
			dataset.addDataStyle(index, styleString[index] + "markerType=circle;");
    	
    	return dataset;
	}

	@Override
	public Node getIcon() {
		Region xychartIcon = new Region();
		xychartIcon.getStyleClass().add("bubblechartIcon");
		return xychartIcon;
	}
}
