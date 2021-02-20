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
package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

import de.jensd.fx.glyphs.GlyphIcons;
import de.mpg.biochem.mars.fx.dashboard.AbstractDashboardWidget;
import de.mpg.biochem.mars.fx.dashboard.MarsDashboardWidget;
import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.fx.plot.tools.MarsCategoryAxis;
import de.mpg.biochem.mars.fx.plot.tools.MarsNumericAxis;
import de.mpg.biochem.mars.fx.plot.tools.MarsZoomer;
import de.mpg.biochem.mars.fx.plot.tools.SegmentDataSetRenderer;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
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
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.util.StringConverter;
import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;

import net.imagej.ops.Initializable;

@Plugin( type = MoleculeArchiveDashboardWidget.class, name = "TagFrequencyWidget" )
public class TagFrequencyWidget extends AbstractDashboardWidget implements MoleculeArchiveDashboardWidget, SciJavaPlugin, Initializable {
	
	@Parameter
	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;
	
	protected XYChart barChart;
	protected MarsCategoryAxis xAxis;
	protected MarsNumericAxis yAxis;

	@Override
	public void initialize() {
		super.initialize();
		
		//final StackPane root = new StackPane();
        xAxis = new MarsCategoryAxis("Tag");
        xAxis.setOverlapPolicy(AxisLabelOverlapPolicy.SHIFT_ALT);
        yAxis = new MarsNumericAxis();
        yAxis.setName("Molecules");
        yAxis.setMinorTickVisible(false);
        yAxis.setForceZeroInRange(true);
        yAxis.setAutoRanging(true);
        yAxis.setAutoRangeRounding(false);
        yAxis.setTickLabelFormatter(new StringConverter<Number>() {
        	private final DecimalFormat format = new DecimalFormat("#");
        	
            @Override
            public Number fromString(String string) {
                return null;
            }

            @Override
            public String toString(Number num) {
                return format.format(num);
            }
        });

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
        barChart.setLegend(null);
        barChart.horizontalGridLinesVisibleProperty().set(false);
        barChart.verticalGridLinesVisibleProperty().set(false);

        //barChart.getPlugins().add(new EditAxis());
        //final Zoomer zoomer = new Zoomer();
        //barChart.getPlugins().add(zoomer);
        
        barChart.setTriggerDistance(0);

        //root.getChildren().add(barChart);
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
		final List<String> categories = new ArrayList<>();
        
        HashMap<String, Double> tagFrequency = new HashMap<String, Double>();
        
        archive.getMoleculeUIDs().stream().forEach(UID -> {
        	for (String tag : archive.moleculeTags(UID)) {
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
        
        if (tagFrequency.size() == 1) {
        	categories.add("");
        	dataSet.add(index, 0);
        }
   
        Platform.runLater(new Runnable() {
			@Override
			public void run() {
				xAxis.setCategories(categories);
			    barChart.getDatasets().clear();
			    barChart.getDatasets().add(dataSet);
			    
			    xAxis.layout();
			}
    	});
	}

	@Override
	protected void createIOMaps() {
		// TODO Auto-generated method stub
	}
	
	public void setArchive(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive) {
		this.archive = archive;
	}
	
	public MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> getArchive() {
		return archive;
	}

	@Override
	public Node getIcon() {
		return (Node) FontAwesomeIconFactory.get().createIcon(TAG, "1.2em");
	}

	@Override
	public String getName() {
		return "TagFrequencyWidget";
	}
}
