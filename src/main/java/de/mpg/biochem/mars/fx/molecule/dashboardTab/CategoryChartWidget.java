package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

import static de.jensd.fx.glyphs.octicons.OctIcon.CODE;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;

import de.jensd.fx.glyphs.GlyphIcons;
import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.fx.plot.tools.MarsCategoryAxis;
import de.mpg.biochem.mars.fx.plot.tools.MarsNumericAxis;
import de.mpg.biochem.mars.fx.plot.tools.MarsZoomer;
import de.mpg.biochem.mars.fx.plot.tools.SegmentDataSetRenderer;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
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
import org.scijava.ui.swing.script.EditorPane;
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

@Plugin( type = CategoryChartWidget.class, name = "CategoryChartWidget" )
public class CategoryChartWidget extends AbstractScriptableWidget implements MarsDashboardWidget, SciJavaPlugin {
	
	protected XYChart barChart;
	protected MarsCategoryAxis xAxis;
	protected MarsNumericAxis yAxis;

	@Override
	public void initialize() {
		super.initialize();
		
		try {
			loadScript("categorychart.groovy");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
        xAxis = new MarsCategoryAxis("Tag");
        xAxis.setOverlapPolicy(AxisLabelOverlapPolicy.SHIFT_ALT);
        yAxis = new MarsNumericAxis();
        yAxis.setName("Molecules");
        yAxis.setMinorTickVisible(false);
        yAxis.setForceZeroInRange(true);
        yAxis.setAutoRanging(true);
        yAxis.setAutoRangeRounding(false);
        //yAxis.setTickLabelFormatter(new MarsIntegerFormatter());

        barChart = new XYChart(xAxis, yAxis);
        barChart.setAnimated(false);
        barChart.getRenderers().clear();
        final ErrorDataSetRenderer renderer = new ErrorDataSetRenderer();
        renderer.setPolyLineStyle(LineStyle.NONE);
        renderer.setDrawBars(true);
        renderer.setBarWidthPercentage(70);
        renderer.setDrawMarker(false);
        
        //Make sure this is set to false. Otherwise second to last points seems to be lost :(...
        renderer.pointReductionProperty().set(false);
        barChart.getRenderers().add(renderer);
        barChart.legendVisibleProperty().set(false);
        barChart.horizontalGridLinesVisibleProperty().set(false);
        barChart.verticalGridLinesVisibleProperty().set(false);

		StackPane stack = new StackPane();
		stack.setPadding(new Insets(10, 10, 10, 10));
		stack.getChildren().add(barChart);
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
		
		System.out.println("number of outputs " + outputs.size());
		
		for (String name : outputs.keySet())
			if (outputs.get(name) == null) {
				System.out.println("found a null");
				return;
			}
		
		//switch statement on keySet ???
		
		String[] xValues = (String[]) outputs.get("xValues");
		Double[] yValues = (Double[]) outputs.get("yValues");
		String yLabel = (String) outputs.get("yLabel");
		String xLabel = (String) outputs.get("xLabel");
		String fillColor = (String)outputs.get("fillColor");
		String title = (String)outputs.get("title");
		
        final DefaultErrorDataSet dataSet = new DefaultErrorDataSet("myData");
        dataSet.setStyle("fillColor:" + fillColor + ";");
        
        List<String> categories = new ArrayList<String>();
        
        for (int row=0;row < xValues.length; row++) {
        	dataSet.add(row, yValues[row].doubleValue());
        	categories.add(xValues[row]);
        }
   
        Platform.runLater(new Runnable() {
			@Override
			public void run() {
				xAxis.setName(xLabel);
				yAxis.setName(yLabel);
				
				xAxis.setCategories(categories);
				barChart.setTitle(title);
			    barChart.getDatasets().clear();
			    barChart.getDatasets().add(dataSet);
			    
			    xAxis.setAutoRanging(true);
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
