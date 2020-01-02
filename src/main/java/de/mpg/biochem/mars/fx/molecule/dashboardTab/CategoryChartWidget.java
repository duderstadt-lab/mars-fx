package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

import static de.jensd.fx.glyphs.octicons.OctIcon.CODE;

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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.AnchorPane;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.scijava.Context;
import org.scijava.module.ModuleException;
import org.scijava.script.ScriptLanguage;
import org.scijava.ui.swing.script.EditorPane;
import javafx.embed.swing.SwingNode;
import javafx.application.Application;
import javafx.application.Platform;

import javax.swing.SwingUtilities;
import javafx.scene.control.ScrollPane;

import org.scijava.script.ScriptHeaderService;
import org.scijava.script.ScriptInfo;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;

import org.scijava.module.ModuleService;
import javafx.scene.control.TextArea;

public class CategoryChartWidget extends AbstractDashboardWidget {

	protected ScriptService scriptService;
	protected ModuleService moduleService;
	protected Context context;
	protected ScriptLanguage lang;
	protected TextArea textarea;
	
	protected final XYChart barChart;
	protected final MarsCategoryAxis xAxis;
	protected final MarsNumericAxis yAxis;
	protected final LanguageSettableEditorPane editorpane;

	public CategoryChartWidget(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive,
			DashboardTab parent) {
		super(archive, parent);
		
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

        barChart.getPlugins().add(new EditAxis());
        final Zoomer zoomer = new Zoomer();
        barChart.getPlugins().add(zoomer);

		StackPane stack = new StackPane();
		stack.setPadding(new Insets(10, 10, 10, 10));
		stack.getChildren().add(barChart);
		stack.setPrefSize(250, 250);
		
		Tab chartTab = new Tab();
		chartTab.setGraphic(OctIconFactory.get().createIcon(getIcon(), "1.0em"));
		
		getTabPane().getTabs().add(chartTab);
		
        BorderPane chartPane = new BorderPane();
        chartPane.setCenter(stack);
        chartTab.setContent(chartPane);
        
        rootPane.setMinSize(250, 250);
        rootPane.setMaxSize(250, 250);
        
        //Script Pane
        Tab scriptTab = new Tab();
        scriptTab.setGraphic(OctIconFactory.get().createIcon(CODE, "1.0em"));
        editorpane = new LanguageSettableEditorPane();
        context = archive.getMoleculeArchiveService().getContext();
        scriptService = context.getService(ScriptService.class);
		lang = scriptService.getLanguageByName("Groovy");
		moduleService = context.getService(ModuleService.class);
		
		context.inject(editorpane);
        
        editorpane.setLanguage(lang);
        final SwingNode swingNode = new SwingNode();
        createSwingContent(swingNode);
        
        scriptTab.setContent(swingNode);
        
        getTabPane().getTabs().add(scriptTab);
        
        textarea = new TextArea();
        textarea.setEditable(false);
        textarea.setWrapText(true);
  
        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(textarea);
        borderPane.setPrefSize(250, 250);
        
        Tab logTab = new Tab();
        logTab.setContent(borderPane);
        logTab.setGraphic(OctIconFactory.get().createIcon(BOOK, "1.0em"));
        getTabPane().getTabs().add(logTab);
	}
	
	private void createSwingContent(final SwingNode swingNode) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                swingNode.setContent(editorpane);
            }
        });
    }

	@Override
	protected void build() {
		Reader reader = new StringReader(editorpane.getText());
		
		ScriptInfo scriptInfo = new ScriptInfo(context, "script.groovy", reader);
		scriptInfo.setLanguage(lang);
			
		ScriptModule module = null;
		try {
			module = scriptInfo.createModule();
			context.inject(module);
		} catch (ModuleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Console console = new Console(textarea);
        PrintStream ps = new PrintStream(console, true);
        
        Writer writer;
		try {
			writer = new OutputStreamWriter(ps,"UTF-8");
			
			module.setOutputWriter(writer);
			module.setErrorWriter(writer);
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		module.setInput("archive", archive);
		
		try {
			moduleService.run(module, true).get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String[] categoriesArray = (String[]) module.getOutput("categories");
		Double[] yValues = (Double[]) module.getOutput("yValues");
		
		System.out.println(categoriesArray[0] + " " + yValues[0]);
		
        final DefaultErrorDataSet dataSet = new DefaultErrorDataSet("myData");
        dataSet.setStyle("Bar");
        
        List<String> categories = new ArrayList<String>();
        
        for (int index=0;index < yValues.length; index++) {
        	dataSet.add(index, yValues[index].doubleValue());
        	categories.add(categoriesArray[index]);
        	index++;
        }
   
        Platform.runLater(new Runnable() {
			@Override
			public void run() {
				xAxis.setCategories(categories);
			    barChart.getDatasets().clear();
			    barChart.getDatasets().add(dataSet);
			}
    	});
	}

	@Override
	public String getName() {
		return "bar graph";
	}

	@Override
	protected void createIOMaps() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public GlyphIcons getIcon() {
		return BAR_CHART;
	}
	
	public static class Console extends OutputStream {

        private TextArea output;

        public Console(TextArea ta) {
            this.output = ta;
        }

        @Override
        public void write(int i) throws IOException {
            output.appendText(String.valueOf((char) i));
        }
    }
}
