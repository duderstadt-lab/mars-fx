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

package de.mpg.biochem.mars.fx.plot;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LINE_CHART;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.jfoenix.controls.JFXBadge;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.plugins.AbstractValueIndicator;
import io.fair_acc.chartfx.plugins.ChartPlugin;
import io.fair_acc.chartfx.renderer.datareduction.DefaultDataReducer;
import de.mpg.biochem.mars.fx.dialogs.RoverErrorDialog;
import de.mpg.biochem.mars.fx.plot.tools.MarsNumericAxis;
import de.mpg.biochem.mars.fx.plot.tools.MarsZoomer;
import de.mpg.biochem.mars.fx.plot.tools.SegmentDataSetRenderer;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.table.MarsTable;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.Cursor;
import javafx.scene.Node;

public abstract class AbstractSubPlot implements SubPlot {

	protected MarsNumericAxis xAxis, yAxis;
	protected XYChart chartPane;

	protected JFXBadge datasetOptionsButton;
	protected BooleanProperty datasetOptionsSelected =
		new SimpleBooleanProperty();
	protected DatasetOptionsPane datasetOptionsPane;

	protected PlotPane plotPane;

	public AbstractSubPlot(PlotPane plotPane, String plotTitle) {
		this.plotPane = plotPane;
		datasetOptionsPane = createDatasetOptionsPane(new HashSet<String>(plotPane
			.getColumnNames()));

		datasetOptionsButton = new JFXBadge(ActionUtils.createToolBarButton(
			new Action("Dataset", "Shortcut+C", LINE_CHART, e -> {
				if (datasetOptionsSelected.get()) plotPane.showSubPlotOptions(
					datasetOptionsPane);
			}, null, datasetOptionsSelected)));

		xAxis = createAxis();
		yAxis = createAxis();

		chartPane = new XYChart(xAxis, yAxis);
		chartPane.setAnimated(false);

		chartPane.setMaxHeight(Double.MAX_VALUE);
		chartPane.setMaxWidth(Double.MAX_VALUE);
		// For the moment lets hide the legend
		chartPane.setLegend(null);

		SegmentDataSetRenderer renderer = new SegmentDataSetRenderer();

		final DefaultDataReducer reductionAlgorithm = (DefaultDataReducer) renderer
			.getRendererDataReducer();
		// final RamanDouglasPeukerDataReducer reductionAlgorithm = new
		// RamanDouglasPeukerDataReducer();
		reductionAlgorithm.setMinPointPixelDistance(0);
		// renderer.setRendererDataReducer(reductionAlgorithm);
		renderer.setMinRequiredReductionSize(500);
		renderer.setDrawMarker(false);

		chartPane.getRenderers().setAll(renderer);

		// unbind PlotArea hiddenSidesPane from Chart HiddenSidesPane
		// and then set default distance. This will allow zoom slider to come up.
		chartPane.getPlotArea().triggerDistanceProperty().unbindBidirectional(
			chartPane.triggerDistanceProperty());
		chartPane.getPlotArea().setTriggerDistance(50);
		chartPane.setTriggerDistance(-1);

		chartPane.getGridRenderer().getHorizontalMajorGrid().setStroke(chartPane
			.getGridRenderer().getHorizontalMajorGrid().getStroke());
		chartPane.getGridRenderer().getVerticalMajorGrid().setStroke(chartPane
			.getGridRenderer().getVerticalMajorGrid().getStroke());

		chartPane.getYAxis().minProperty().addListener((ob, o, n) -> {
			if (!datasetOptionsPane.fixYBounds().get()) datasetOptionsPane.setYMin(n
				.doubleValue());
		});
		chartPane.getYAxis().maxProperty().addListener((ob, o, n) -> {
			if (!datasetOptionsPane.fixYBounds().get()) datasetOptionsPane.setYMax(n
				.doubleValue());
		});
	}

	protected MarsNumericAxis createAxis() {
		MarsNumericAxis axis = new MarsNumericAxis();
		axis.setMinorTickCount(0);
		axis.setAutoRangeRounding(false);
		axis.setForceZeroInRange(false);
		axis.setAnimated(false);
		return axis;
	}

	public void addSeries(PlotSeries plotSeries) {
		getPlotSeriesList().add(plotSeries);
	}

	public void setTitle(String name) {
		chartPane.setTitle(name);
	}

	public ObservableList<PlotSeries> getPlotSeriesList() {
		return datasetOptionsPane.getPlotSeriesList();
	}

	public void update() {
		boolean firstUpdate = (chartPane.getDatasets().size() == 0) ? true : false;

		removeIndicators();
		chartPane.getDatasets().clear();

		Set<String> xAxisList = new HashSet<String>();
		Set<String> yAxisList = new HashSet<String>();

		for (int i = 0; i < getPlotSeriesList().size(); i++) {
			PlotSeries plotSeries = getPlotSeriesList().get(i);

			if (plotSeries.xColumnField().getSelectionModel()
				.getSelectedIndex() != -1 && plotSeries.yColumnField()
					.getSelectionModel().getSelectedIndex() != -1)
			{
				addDataSet(plotSeries);
				xAxisList.add(plotSeries.getXColumn());
				yAxisList.add(plotSeries.getYColumn());
			}
			else {
				// This must be put in a runLater block to ensure the scene is not null
				Platform.runLater(() -> {
					RoverErrorDialog alert = new RoverErrorDialog(getNode().getScene()
						.getWindow(),
						"One or more plot series are missing column selections. " +
							" Please select columns to be plotted.");
					alert.show();
				});
				return;
			}
		}
		if (!datasetOptionsPane.getTitle().equals("")) setTitle(datasetOptionsPane
			.getTitle());
		if (datasetOptionsPane.getXAxisName() != null && !datasetOptionsPane
			.getXAxisName().equals("")) setXLabel(datasetOptionsPane.getXAxisName());
		if (datasetOptionsPane.getYAxisName() != null && !datasetOptionsPane
			.getYAxisName().equals("")) setYLabel(datasetOptionsPane.getYAxisName());

		if (datasetOptionsPane.fixYBounds().get()) {
			chartPane.getYAxis().setAutoRanging(false);
			chartPane.getYAxis().set(datasetOptionsPane.getYMin(), datasetOptionsPane
				.getYMax());
		}

		// Make sure to update the layout before adding the indicators the first
		// time
		if (firstUpdate) chartPane.layout();
		if (chartPane.getDatasets().size() != 0) addIndicators(xAxisList,
			yAxisList);

		chartPane.layout();
	}

	@Override
	public void setTool(ChartPlugin plugin, Cursor cursor) {
		removeTools();
		if (plugin instanceof MarsPlotPlugin) {
			((MarsPlotPlugin) plugin).setDatasetOptionsPane(datasetOptionsPane);
		}
		chartPane.getPlugins().add(plugin);
		chartPane.setCursor(cursor);
	}

	@Override
	public void removeTools() {
		chartPane.getPlugins().stream().filter(
			plugin -> plugin instanceof MarsZoomer).forEach(
				plugin -> ((MarsZoomer) plugin).removeSliderListeners(chartPane));

		List<ChartPlugin> plugins = chartPane.getPlugins().stream().filter(
			plugin -> !(plugin instanceof AbstractValueIndicator)).collect(Collectors
				.toList());
		chartPane.getPlugins().removeAll(plugins);
		chartPane.setCursor(Cursor.DEFAULT);
	}

	public void setXLabel(String xAxisLabel) {
		chartPane.getXAxis().setName(xAxisLabel);
	}

	public void setYLabel(String yAxisLabel) {
		chartPane.getYAxis().setName(yAxisLabel);
	}

	public MarsNumericAxis getXAxis() {
		return xAxis;
	}

	public MarsNumericAxis getYAxis() {
		return yAxis;
	}

	@Override
	public Node getNode() {
		return chartPane;
	}

	public XYChart getChart() {
		return chartPane;
	}

	public PlotPane getPlotPane() {
		return plotPane;
	}

	public DatasetOptionsPane getDatasetOptionsPane() {
		return datasetOptionsPane;
	}

	public JFXBadge getDatasetOptionsButton() {
		return datasetOptionsButton;
	}

	public BooleanProperty getDatasetOptionsSelected() {
		return datasetOptionsSelected;
	}

	public abstract void addDataSet(PlotSeries plotSeries);

	public abstract void addIndicators(Set<String> AxisList,
		Set<String> yAxisList);

	public abstract void removeIndicators();

	protected abstract MarsTable getDataTable();

	protected abstract DatasetOptionsPane createDatasetOptionsPane(
		Set<String> columns);
}
