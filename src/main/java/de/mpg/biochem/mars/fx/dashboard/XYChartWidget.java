/*******************************************************************************
 * Copyright (C) 2019, Duderstadt Lab
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package de.mpg.biochem.mars.fx.dashboard;

import de.gsi.chart.XYChart;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.LineStyle;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.dataset.spi.DefaultErrorDataSet;
import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.fx.plot.tools.MarsDataPointTracker;
import de.mpg.biochem.mars.fx.plot.tools.MarsNumericAxis;
import de.mpg.biochem.mars.molecule.MarsMetadata;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.scijava.Cancelable;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;

import net.imagej.ops.Initializable;

@Plugin( type = XYChartWidget.class, name = "XYChartWidget" )
public class XYChartWidget extends AbstractScriptableWidget implements MarsDashboardWidget, SciJavaPlugin, Initializable {

	protected XYChart xyChart;
	protected MarsNumericAxis xAxis, yAxis;
	
	protected ErrorDataSetRenderer renderer;
	
	protected ArrayList<DefaultErrorDataSet> datasets;
	
	protected ArrayList<String> requiredGlobalFields = new ArrayList<String>(Arrays.asList("xlabel", "ylabel", "title"));
	
	@Override
	public void initialize() {
		super.initialize();
		
		try {
			loadScript("xychart");
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

        xyChart = new XYChart(xAxis, yAxis);
        xyChart.getPlugins().add(new MarsDataPointTracker());
        xyChart.setAnimated(false);
        xyChart.getRenderers().clear();
        
        xyChart.getPlugins().add(new MarsDataPointTracker());
        //Zoomer zoom = new Zoomer();
        //xyChart.getPlugins().add(zoom);
        
        datasets = new ArrayList<DefaultErrorDataSet>();
        
        renderer = new ErrorDataSetRenderer();
        renderer.setMarkerSize(5);
        renderer.setPolyLineStyle(LineStyle.NORMAL);
        renderer.setErrorType(ErrorStyle.ERRORBARS);
        renderer.setDrawMarker(false);
        renderer.setAssumeSortedData(false);
        
        xyChart.getRenderers().add(renderer);
        xyChart.legendVisibleProperty().set(false);
        xyChart.horizontalGridLinesVisibleProperty().set(false);
        xyChart.verticalGridLinesVisibleProperty().set(false);

		StackPane stack = new StackPane();
		stack.setPadding(new Insets(10, 10, 10, 10));
		stack.getChildren().add(xyChart);
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
		
		if (outputs == null) {
			writeToLog("No outputs were generated by this script.");
			return;
		}
		
		for (String field : requiredGlobalFields)
			if (!outputs.containsKey(field)) {
				writeToLog("required output " + field + " is missing.");
				return;
			}
		
		datasets.clear();
		
		String xlabel = (String) outputs.get("xlabel");
		String ylabel = (String) outputs.get("ylabel");
		String title = (String)outputs.get("title");
		
		//Check if an x-range was provided
		final boolean autoXRanging; 
		if (outputs.containsKey("xmin") && outputs.containsKey("xmax")) {
			autoXRanging = false;
		} else if (outputs.containsKey("xmin")) {
			writeToLog("required output xmax is missing.");
			return;
		} else if (outputs.containsKey("xmax")) {
			writeToLog("required output xmin is missing.");
			return;
		} else {
			autoXRanging = true;
		}

		//Check if a y-range was provided
		final boolean autoYRanging; 
		if (outputs.containsKey("ymin") && outputs.containsKey("ymax")) {
			autoYRanging = false;
		} else if (outputs.containsKey("ymin")) {
			writeToLog("required output xmax is missing.");
			return;
		} else if (outputs.containsKey("ymax")) {
			writeToLog("required output xmin is missing.");
			return;
		} else {
			autoYRanging = true;
		}
		
		LinkedHashSet<String> seriesSet = new LinkedHashSet<String>();
		for (String outputName : outputs.keySet()) {
			if(outputName.startsWith("series")) {
				int index = outputName.indexOf("_");
				seriesSet.add(outputName.substring(0, index));
			}
		}
		
		List<String> series = new ArrayList<String>(seriesSet);
		Collections.sort(series);
		
		for (String seriesName : series) {
			DefaultErrorDataSet dataset = buildDataSet(outputs, seriesName);
			if (dataset != null)
				datasets.add(dataset);
			else {
				return;
			}
		}
			
        Platform.runLater(new Runnable() {
			@Override
			public void run() {
				xAxis.setName(xlabel);
				if (autoXRanging) {
					xAxis.setAutoRanging(true);
				} else {
					xAxis.setAutoRanging(false);
					xAxis.setMin((Double) outputs.get("xmin"));
					xAxis.setMax((Double) outputs.get("xmax"));
				}
				
				yAxis.setName(ylabel);
				if (autoYRanging) {
					yAxis.setAutoRanging(true);
				} else {
					yAxis.setAutoRanging(false);
					yAxis.setMin((Double) outputs.get("ymin"));
					yAxis.setMax((Double) outputs.get("ymax"));
				}
				
				xyChart.setTitle(title);
				
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
			
			Double[] error = null;
			
			if (xvalues.length == 0) {
				writeToLog(seriesName + "_xvalues has zero values.");
				return null;
			}
			
			dataPointCount = xvalues.length;
			
			if (xvalues.length != yvalues.length) {
				writeToLog(seriesName + "_xvalues and " + seriesName + "_yvalues do not have the same dimensions.");
				return null;
			}
			
			if (outputs.containsKey(seriesName + "_error")) {
				error = (Double[]) outputs.get(seriesName + "_error");
				
				if (error.length == 0) {
					writeToLog(seriesName + "_error has zero values.");
					return null;
				}
				
				if (error.length != dataPointCount) {
					writeToLog(seriesName + "_yvalues and " + seriesName + "_error do not have the same dimensions.");
					return null;
				}
			}
			
			if (error != null) {
				for (int index=0;index<xvalues.length;index++)
					dataset.add(xvalues[index], yvalues[index], error[index], error[index]);
			} else {
				for (int index=0;index<xvalues.length;index++)
					dataset.add(xvalues[index], yvalues[index]);
			}
			
		} else if (outputs.containsKey(seriesName + "_xvalues")) {
			writeToLog("required output " + seriesName + "_yvalues"  + " is missing");
			return null;
		} else if (outputs.containsKey(seriesName + "_yvalues")) {
			writeToLog("required output " + seriesName + "_xvalues"  + " is missing");
			return null;
		}
		
		String styleString = "";
		
		if (outputs.containsKey(seriesName + "_strokeColor"))
				styleString += "strokeColor=" + (String)outputs.get(seriesName + "_strokeColor") + "; ";
		
		if (outputs.containsKey(seriesName + "_fillColor"))
				styleString += "fillColor=" + (String)outputs.get(seriesName + "_fillColor") + "; ";
		
		if (outputs.containsKey(seriesName + "_strokeWidth"))
			styleString += "strokeWidth=" + ((Integer)outputs.get(seriesName + "_strokeWidth")).intValue();
		
		dataset.setStyle(styleString);
    	
    	return dataset;
	}

	@Override
	public Node getIcon() {
		Region xychartIcon = new Region();
		xychartIcon.getStyleClass().add("xychartIcon");
		return xychartIcon;
	}

	@Override
	public String getName() {
		return "XYChartWidget";
	}
}