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

import java.util.*;

import org.scijava.table.DoubleColumn;

import de.gsi.chart.plugins.AbstractValueIndicator;
import de.gsi.chart.plugins.ChartPlugin;
import de.gsi.chart.plugins.XRangeIndicator;
import de.gsi.chart.plugins.YRangeIndicator;
import de.gsi.chart.plugins.YValueIndicator;
import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
import de.mpg.biochem.mars.fx.plot.event.PlotEvent;
import de.mpg.biochem.mars.fx.plot.tools.MarsDoubleDataSet;
import de.mpg.biochem.mars.fx.plot.tools.MarsWrappedDoubleDataSet;
import de.mpg.biochem.mars.fx.plot.tools.MarsXValueIndicator;
import de.mpg.biochem.mars.fx.util.Utils;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.MarsRecord;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.table.MarsTable;
import de.mpg.biochem.mars.util.MarsPosition;
import de.mpg.biochem.mars.util.MarsRegion;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.paint.Color;

public abstract class AbstractMoleculeSubPlot<M extends Molecule> extends
	AbstractSubPlot implements MoleculeSubPane
{

	protected M molecule;
	protected PlotPane plotPane;

	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;

	public AbstractMoleculeSubPlot(PlotPane plotPane, String plotTitle) {
		super(plotPane, plotTitle);

		this.plotPane = plotPane;

		getNode().addEventHandler(MoleculeEvent.MOLECULE_EVENT, this);
		getNode().addEventHandler(PlotEvent.PLOT_EVENT,
			new EventHandler<PlotEvent>()
			{

				@Override
				public void handle(PlotEvent e) {
					if (e.getEventType().getName().equals("UPDATE_PLOT_AREA")) {
						update();
						e.consume();
					}
				}
			});
	}
	// For the moment we make a copy...
	// maybe long-term we should not make a copy to improve performance.
	// But that might require a bit of a change in how things are store so....

	public void addDataSet(PlotSeries plotSeries) {
		String xColumn = plotSeries.getXColumn();
		String yColumn = plotSeries.getYColumn();

		if (getDataTable() == null || !getDataTable().hasColumn(xColumn) ||
			!getDataTable().hasColumn(yColumn)) return;

		// Check if there are any segment table with these columns
		Set<List<String>> segmentTableNames = new HashSet<List<String>>();
		boolean hasSegmentsTables = false;
		for (List<String> names : molecule.getSegmentsTableNames()) {
			if (names.get(0).equals(plotSeries.getXColumn()) && names.get(1).equals(
				plotSeries.getYColumn()))
			{
				hasSegmentsTables = true;
				segmentTableNames.add(names);
			}
		}

		// Add segments
		if (plotSeries.drawSegments() && hasSegmentsTables) {
			for (List<String> segmentTableName : segmentTableNames) {
				double segmentWidth = Double.valueOf(plotSeries.getSegmentsWidth());

				MarsDoubleDataSet segmentsDataSet = new MarsDoubleDataSet(
					"Segments - " + yColumn + " vs " + xColumn + " - " + segmentTableName
						.get(2), plotSeries.getSegmentsColor(), segmentWidth, "");

				MarsTable segmentsTable = molecule.getSegmentsTable(segmentTableName);

				for (int row = 0; row < segmentsTable.getRowCount(); row++) {
					double x1 = segmentsTable.getValue("X1", row);
					double y1 = segmentsTable.getValue("Y1", row);
					double x2 = segmentsTable.getValue("X2", row);
					double y2 = segmentsTable.getValue("Y2", row);

					if (!Double.isNaN(x1) && !Double.isNaN(y1) && !Double.isNaN(x2) &&
						!Double.isNaN(y2))
					{
						segmentsDataSet.add(x1, y1);
						segmentsDataSet.add(x2, y2);
					}
				}

				segmentsDataSet.setStyle("Segments");
				getChart().getDatasets().add(segmentsDataSet);
			}
		}

		double lineWidth = Double.valueOf(plotSeries.getWidth());

		MarsWrappedDoubleDataSet dataset = new MarsWrappedDoubleDataSet(yColumn +
			" vs " + xColumn, plotSeries.getColor(), lineWidth, plotSeries
				.getLineStyle());

		DoubleColumn xCol = (DoubleColumn) getDataTable().get(xColumn);
		DoubleColumn yCol = (DoubleColumn) getDataTable().get(yColumn);
		int realCount = 0;
		for (int row = 0; row < getDataTable().getRowCount(); row++) {
			if (!Double.isNaN(xCol.getValue(row)) && !Double.isNaN(yCol.getValue(
				row))) realCount++;
		}
		if (realCount < getDataTable().getRowCount()) {
			// There are NaN values in this dataset we must make a copy without NaNs
			// to ensure a continuous plot
			double[] xColumnCopy = new double[realCount];
			double[] yColumnCopy = new double[realCount];
			int count = 0;
			for (int row = 0; row < getDataTable().getRowCount(); row++) {
				if (!Double.isNaN(xCol.getValue(row)) && !Double.isNaN(yCol.getValue(
					row)))
				{
					xColumnCopy[count] = xCol.getValue(row);
					yColumnCopy[count] = yCol.getValue(row);
					count++;
				}
			}
			DoubleColumn noNaNxCol = new DoubleColumn();
			noNaNxCol.fill(xColumnCopy);
			DoubleColumn noNaNyCol = new DoubleColumn();
			noNaNyCol.fill(yColumnCopy);
			dataset.add(noNaNxCol, noNaNyCol);
		}
		else dataset.add((DoubleColumn) getDataTable().get(xColumn),
			(DoubleColumn) getDataTable().get(yColumn));

		dataset.setStyle(plotSeries.getType());
		if (plotPane.getPlotOptionsPane().downsample()) dataset.downsample(xAxis,
			plotPane.getPlotOptionsPane().getMinDownsamplePoints());
		getChart().getDatasets().add(dataset);
	}

	@SuppressWarnings("unchecked")
	public void addIndicators(Set<String> xAxisList, Set<String> yAxisList) {

		MarsRecord record;

		if (this.getDatasetOptionsPane().isMoleculeIndicators()) record = molecule;
		else if (this.getDatasetOptionsPane().isMetadataIndicators()) {
			record = ((AbstractMoleculePlotPane<Molecule, SubPlot>) plotPane)
				.getArchive().getMetadata(molecule.getMetadataUID());
		}
		else return;

		if (record == null) return;

		ArrayList<String> regionNames = new ArrayList<>(record.getRegionNames());

		String newStyleSheet = "";

		for (int index = 0; index < regionNames.size(); index++) {
			String regionName = regionNames.get(index);
			MarsRegion roi = record.getRegion(regionName);

			if (xAxisList.contains(roi.getColumn())) {
				XRangeIndicator xRangeIndicator = new XRangeIndicator(this.xAxis, roi
					.getStart(), roi.getEnd(), roi.getName());
				xRangeIndicator.setLabelVerticalPosition(0.2);

				Color color = Color.web(roi.getColor());
				newStyleSheet += String.format(Locale.US,
					".x-range-indicator-rect%d { -fx-stroke: transparent; -fx-fill: rgba(%d, %d, %d, %f); }\n",
					index, Math.round(color.getRed() * 255), Math.round(color.getGreen() *
						255), Math.round(color.getBlue() * 255), roi.getOpacity());

				Color solidColor = Utils.rgba2rgb((int) Math.round(color.getRed() *
					255), (int) Math.round(color.getGreen() * 255), (int) Math.round(color
						.getBlue() * 255), roi.getOpacity());

				newStyleSheet += String.format(Locale.US,
					".x-range-indicator-label%d { -fx-background-color: rgb(%d, %d, %d); }\n",
					index, Math.round(solidColor.getRed() * 255), Math.round(solidColor
						.getGreen() * 255), Math.round(solidColor.getBlue() * 255));

				getChart().getPlugins().add(xRangeIndicator);
			}

			if (yAxisList.contains(roi.getColumn())) {
				YRangeIndicator yRangeIndicator = new YRangeIndicator(this.yAxis, roi
					.getStart(), roi.getEnd(), roi.getName());
				yRangeIndicator.setLabelHorizontalPosition(0.2);

				Color color = Color.web(roi.getColor());
				newStyleSheet += String.format(Locale.US,
					".y-range-indicator-rect%d { -fx-stroke: transparent; -fx-fill: rgba(%d, %d, %d, %f); }",
					index, Math.round(color.getRed() * 255), Math.round(color.getGreen() *
						255), Math.round(color.getBlue() * 255), roi.getOpacity());

				Color solidColor = Utils.rgba2rgb((int) Math.round(color.getRed() *
					255), (int) Math.round(color.getGreen() * 255), (int) Math.round(color
						.getBlue() * 255), roi.getOpacity());

				newStyleSheet += String.format(Locale.US,
					".y-range-indicator-label%d { -fx-background-color: rgb(%d, %d, %d); }\n",
					index, Math.round(solidColor.getRed() * 255), Math.round(solidColor
						.getGreen() * 255), Math.round(solidColor.getBlue() * 255));

				getChart().getPlugins().add(yRangeIndicator);
			}
		}

		ArrayList<String> positionNames = new ArrayList<>(record
			.getPositionNames());
		for (int index = 0; index < positionNames.size(); index++) {
			String positionName = positionNames.get(index);
			MarsPosition poi = record.getPosition(positionName);

			if (xAxisList.contains(poi.getColumn())) {
				MarsXValueIndicator xValueIndicator = new MarsXValueIndicator(
					this.xAxis, poi.getPosition(), poi.getName(), datasetOptionsPane);
				xValueIndicator.setLabelPosition(0.2);

				Color color = Color.web(poi.getColor());
				newStyleSheet += String.format(Locale.US,
					".x-value-indicator-line%d { -fx-stroke: rgba(%d, %d, %d, %f); -fx-stroke-width: %f;}",
					index, Math.round(color.getRed() * 255), Math.round(color.getGreen() *
						255), Math.round(color.getBlue() * 255), color.getOpacity(), poi
							.getStroke());

				newStyleSheet += String.format(Locale.US,
					".x-value-indicator-label%d { -fx-text-fill: rgba(%d, %d, %d); -fx-background-color: white; }\n",
					index, Math.round(color.getRed() * 255), Math.round(color.getGreen() *
						255), Math.round(color.getBlue() * 255));

				getChart().getPlugins().add(xValueIndicator);
			}

			if (yAxisList.contains(poi.getColumn())) {
				YValueIndicator yValueIndicator = new YValueIndicator(this.yAxis, poi
					.getPosition(), poi.getName());
				yValueIndicator.setLabelPosition(0.2);

				Color color = Color.web(poi.getColor());
				newStyleSheet += String.format(Locale.US,
					".y-value-indicator-line%d { -fx-stroke: rgba(%d, %d, %d, %f); -fx-stroke-width: %f;}",
					index, Math.round(color.getRed() * 255), Math.round(color.getGreen() *
						255), Math.round(color.getBlue() * 255), color.getOpacity(), poi
							.getStroke());

				newStyleSheet += String.format(Locale.US,
					".x-value-indicator-label%d { -fx-stroke: rgba(%d, %d, %d); -fx-background-color: white; }\n",
					index, Math.round(color.getRed() * 255), Math.round(color.getGreen() *
						255), Math.round(color.getBlue() * 255));

				getChart().getPlugins().add(yValueIndicator);
			}
		}

		getChart().getStylesheets().add(getPlotPane().getStyleSheetUpdater()
			.getStyleSheetURL(newStyleSheet));
	}

	@Override
	protected DatasetOptionsPane createDatasetOptionsPane(Set<String> columns) {
		return new DatasetOptionsPane(columns, this);
	}

	@Override
	public void update() {
		for (ChartPlugin plugin : getChart().getPlugins())
			if (plugin instanceof MarsMoleculePlotPlugin)
				((MarsMoleculePlotPlugin) plugin).setMolecule(molecule);
		super.update();
	}

	@Override
	public void removeIndicators() {
		ArrayList<Object> indicators = new ArrayList<Object>();
		for (ChartPlugin plugin : getChart().getPlugins())
			if (plugin instanceof AbstractValueIndicator) indicators.add(plugin);
		getChart().getPlugins().removeAll(indicators);
		// remove indicator stylesheets
		while (getChart().getStylesheets().size() > 1)
			getChart().getStylesheets().remove(1);
	}

	@Override
	public void handle(MoleculeEvent event) {
		event.invokeHandler(this);
		event.consume();
	}

	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onMoleculeSelectionChangedEvent(Molecule molecule) {
		this.molecule = (M) molecule;
		if (molecule != null) update();
		else {
			removeIndicators();
			chartPane.getDatasets().clear();
			chartPane.layout();
		}
	}

	@Override
	protected MarsTable getDataTable() {
		if (molecule != null) {
			return molecule.getTable();
		}
		else return null;
	}
}
