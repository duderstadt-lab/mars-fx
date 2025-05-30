/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2025 Karl Duderstadt
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

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TAG;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.fair_acc.chartfx.axes.spi.CategoryAxis;
import io.fair_acc.dataset.utils.DataSetStyleBuilder;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.AxisLabelOverlapPolicy;
import io.fair_acc.chartfx.renderer.LineStyle;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.dataset.spi.DefaultErrorDataSet;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.dashboard.AbstractDashboardWidget;
import de.mpg.biochem.mars.fx.plot.tools.MarsNumericAxis;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;
import net.imagej.ops.Initializable;

@Plugin(type = MoleculeArchiveDashboardWidget.class,
	name = "TagFrequencyWidget")
public class TagFrequencyWidget extends AbstractDashboardWidget implements
	MoleculeArchiveDashboardWidget, SciJavaPlugin, Initializable
{

	@Parameter
	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;

	protected XYChart barChart;
	protected CategoryAxis xAxis;
	protected MarsNumericAxis yAxis;

	@Override
	public void initialize() {
		super.initialize();

		// final StackPane root = new StackPane();
		xAxis = new CategoryAxis("Tag");
		xAxis.setUnit(null);
		xAxis.setAutoRangePadding(0.2);
		xAxis.setOverlapPolicy(AxisLabelOverlapPolicy.SHIFT_ALT);

		yAxis = new MarsNumericAxis();
		yAxis.setName("Molecules");
		yAxis.setMinorTickCount(0);
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

		// Make sure this is set to false. Otherwise second to last points seems to
		// be lost :(...
		renderer.pointReductionProperty().set(false);
		barChart.getRenderers().add(renderer);
		barChart.setLegendVisible(false);
		barChart.getGridRenderer().getHorizontalMajorGrid().setVisible(false);
		barChart.getGridRenderer().getVerticalMajorGrid().setVisible(false);

		// Prevent chartfx tools panel from opening by setting HiddenSidesPane to zero.
		barChart.getPlotArea().setTriggerDistance(0);

		// root.getChildren().add(barChart);
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
				}
				else tagFrequency.put(tag, 1.0);
			}
		});

		final DefaultErrorDataSet dataSet = new DefaultErrorDataSet("myData");
		dataSet.setStyle(DataSetStyleBuilder.instance().setDatasetColor("#add8e6").build());

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

	public void setArchive(
		MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive)
	{
		this.archive = archive;
	}

	public
		MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>>
		getArchive()
	{
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
