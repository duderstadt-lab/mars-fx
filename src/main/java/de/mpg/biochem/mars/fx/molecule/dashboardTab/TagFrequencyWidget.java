package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

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
import de.gsi.chart.utils.DecimalStringConverter;
import de.gsi.dataset.spi.DefaultErrorDataSet;
import de.gsi.dataset.testdata.spi.RandomDataGenerator;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Tab;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class TagFrequencyWidget extends AbstractDashboardWidget {
	
	final XYChart barChart;
	final MarsCategoryAxis xAxis;
	final MarsNumericAxis yAxis;

	public TagFrequencyWidget(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive,
			DashboardTab parent) {
		super(archive, parent);
		
		//final StackPane root = new StackPane();
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

        //root.getChildren().add(barChart);
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
	}

	@Override
	protected void build() {
		final List<String> categories = new ArrayList<>();
        
        HashMap<String, Double> tagFrequency = new HashMap<String, Double>();
        
        archive.getMoleculeUIDs().stream().forEach(UID -> {
        	Molecule molecule = archive.get(UID);
        	for (String tag : molecule.getTags()) {
        		if (tagFrequency.containsKey(tag)) {
        			tagFrequency.put(tag, tagFrequency.get(tag) + 1);
        		} else 
    				tagFrequency.put(tag, 1.0);
        	}
        });
        
        final DefaultErrorDataSet dataSet = new DefaultErrorDataSet("myData");
        dataSet.setStyle("strokeColor:#add8e6;fillColor:#add8e6;strokeWidth=0;");
        
        int index = 0;
        for (String tag : tagFrequency.keySet()) {
        	categories.add(tag);
        	dataSet.add(index, tagFrequency.get(tag));
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
		return "Tag frequencies";
	}

	@Override
	protected void createIOMaps() {
		// TODO Auto-generated method stub
	}

	@Override
	public GlyphIcons getIcon() {
		return TAG;
	}
}
