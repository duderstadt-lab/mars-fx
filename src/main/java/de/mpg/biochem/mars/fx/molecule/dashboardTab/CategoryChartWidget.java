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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.scijava.Context;
import org.scijava.script.ScriptLanguage;
import org.scijava.ui.swing.script.EditorPane;
import javafx.embed.swing.SwingNode;
import javafx.application.Application;
import javax.swing.SwingUtilities;

import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptService;

public class CategoryChartWidget extends AbstractDashboardWidget {
	
	protected final XYChart barChart;
	protected final MarsCategoryAxis xAxis;
	protected final MarsNumericAxis yAxis;
	protected final LanguageSettableEditorPane editorpane;

	public CategoryChartWidget(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive,
			DashboardTab parent) {
		super(archive, parent);
		
		final StackPane root = new StackPane();
        xAxis = new MarsCategoryAxis("Tag");
        xAxis.setOverlapPolicy(AxisLabelOverlapPolicy.SHIFT_ALT);
        yAxis = new MarsNumericAxis();
        yAxis.setName("Molecules");
        yAxis.setMinorTickVisible(false);
        yAxis.setForceZeroInRange(true);

        barChart = new XYChart(xAxis, yAxis);
        barChart.setAnimated(false);
        barChart.getRenderers().clear();
        //final SegmentDataSetRenderer renderer = new SegmentDataSetRenderer();
        final ErrorDataSetRenderer renderer = new ErrorDataSetRenderer();
        renderer.setPolyLineStyle(LineStyle.NONE);
        renderer.setDrawBars(true);
        renderer.setBarWidthPercentage(70);
        renderer.setDrawMarker(false);
        barChart.getRenderers().add(renderer);
        barChart.legendVisibleProperty().set(false);
        barChart.horizontalGridLinesVisibleProperty().set(false);
        barChart.verticalGridLinesVisibleProperty().set(false);

        barChart.getPlugins().add(new EditAxis());
        final Zoomer zoomer = new Zoomer();
        barChart.getPlugins().add(zoomer);

        root.getChildren().add(barChart);
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
        
        //Script Pane
        Tab scriptTab = new Tab();
        scriptTab.setGraphic(OctIconFactory.get().createIcon(CODE, "1.0em"));
        editorpane = new LanguageSettableEditorPane();
        Context context = archive.getMoleculeArchiveService().getContext();
        ScriptService scriptService = context.getService(ScriptService.class);
		ScriptLanguage lang = scriptService.getLanguageByName("Groovy");
		
		context.inject(editorpane);
        
        editorpane.setLanguage(lang);
        final SwingNode swingNode = new SwingNode();
        createSwingContent(swingNode);
        
        scriptTab.setContent(swingNode);
        
        getTabPane().getTabs().add(scriptTab);
        
        rootPane.setMinSize(250, 250);
        rootPane.setMaxSize(250, 250);
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
	public void load() {
		//final List<String> categories = new ArrayList<>();
		
		//First we need to retrieve the script text and run it
		//Then we need to get the outputs.
		//We also need to inject the archive as an input.
		
		/*
		barChart.getDatasets().clear();
        
        final DefaultErrorDataSet dataSet = new DefaultErrorDataSet("myData");
        dataSet.setStyle("Bar");
        
        for (int index=0;index < values.length; index++) {
        	dataSet.add(index, values[index]);
        	index++;
        }
   
        xAxis.setCategories(categories);
        
        barChart.getDatasets().add(dataSet);
        */
	}

	@Override
	public String getName() {
		return "Tag frequencies";
	}

	@Override
	protected void createIOMaps() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public GlyphIcons getIcon() {
		return BAR_CHART;
	}

}
