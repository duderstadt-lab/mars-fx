package de.mpg.biochem.mars.fx.plot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javafx.scene.paint.Color;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.ChartPlugin;
import de.gsi.chart.plugins.AbstractValueIndicator;
import de.gsi.chart.plugins.XRangeIndicator;
import de.gsi.chart.plugins.XValueIndicator;
import de.gsi.chart.plugins.YRangeIndicator;
import de.gsi.chart.plugins.YValueIndicator;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.DoubleDataSet;
import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
import de.mpg.biochem.mars.fx.plot.event.PlotEvent;
import de.mpg.biochem.mars.fx.plot.tools.MarsDoubleDataSet;
import de.mpg.biochem.mars.fx.plot.tools.SegmentDataSetRenderer;
import de.mpg.biochem.mars.fx.util.Utils;
//import de.mpg.biochem.mars.fx.plot.tools.MarsRegionSelectionTool;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.table.MarsTable;
import de.mpg.biochem.mars.util.PositionOfInterest;
import de.mpg.biochem.mars.util.RegionOfInterest;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Cursor;

public abstract class AbstractMoleculeSubPlot<M extends Molecule> extends AbstractSubPlot implements MoleculeSubPane {
	
	protected M molecule;
	
	public AbstractMoleculeSubPlot(PlotPane plotPane, String plotTitle) {
		super(plotPane, plotTitle);
		
		getNode().addEventHandler(MoleculeEvent.MOLECULE_EVENT, this);
		getNode().addEventHandler(PlotEvent.PLOT_EVENT, new EventHandler<PlotEvent>() { 
			   @Override 
			   public void handle(PlotEvent e) { 
				   if (e.getEventType().getName().equals("UPDATE_PLOT_AREA")) {
					   	removeIndicators();
						update();
						e.consume();
				   }
			   } 
			});
	}
	//For the moment we make a copy...
	//maybe long-term we should no make a copy to improve performance.
	//But that might require a bit change in how things are store so....
	
	public void addDataSet(PlotSeries plotSeries) {
		String xColumn = plotSeries.getXColumn();
		String yColumn = plotSeries.getYColumn();
		
		if (!getDataTable().hasColumn(xColumn) || !getDataTable().hasColumn(yColumn))
			return;
		
		//Add segments
		if (plotSeries.drawSegments() && molecule.hasSegmentsTable(plotSeries.getXColumn(), plotSeries.getYColumn())) {
			double segmentWidth = Double.valueOf(plotSeries.getSegmentsWidth());
			
			MarsDoubleDataSet segmentsDataSet = new MarsDoubleDataSet("Segments - " + yColumn + " vs " + xColumn, plotSeries.getSegmentsColor(), segmentWidth);
			
			MarsTable segmentsTable = molecule.getSegmentsTable(xColumn, yColumn);
			
			for (int row=0;row<segmentsTable.getRowCount();row++) {
				double x1 = segmentsTable.getValue("x1", row);
				double y1 = segmentsTable.getValue("y1", row);
				double x2 = segmentsTable.getValue("x2", row);
				double y2 = segmentsTable.getValue("y2", row);
				
				if (!Double.isNaN(x1) && !Double.isNaN(y1) && !Double.isNaN(x2) && !Double.isNaN(y2)) {
					segmentsDataSet.add(x1, y1);
					segmentsDataSet.add(x2, y2);
				}
			}
			
			segmentsDataSet.setStyle("Segments");
			getChart().getDatasets().add(segmentsDataSet);
		}
		
		double lineWidth = Double.valueOf(plotSeries.getWidth());
		
		MarsDoubleDataSet dataset = new MarsDoubleDataSet(yColumn + " vs " + xColumn, plotSeries.getColor(), lineWidth);
		
		for (int row=0;row<getDataTable().getRowCount();row++) {
			double x = getDataTable().getValue(xColumn, row);
			double y = getDataTable().getValue(yColumn, row);
			
			if (!Double.isNaN(x) && !Double.isNaN(y)) {
				dataset.add(x, y);
			}
		}

		dataset.setStyle(plotSeries.getType());
		getChart().getDatasets().add(dataset);	
	}
	
	public void addIndicators(Set<String> xAxisList, Set<String> yAxisList) {
		ArrayList<String> regionNames = new ArrayList<>(molecule.getRegionNames());
		
		String newStyleSheet = "";
		
		for (int index=0; index<regionNames.size(); index++) {
			String regionName = regionNames.get(index);
			RegionOfInterest roi = molecule.getRegion(regionName);
			
			if (xAxisList.contains(roi.getColumn())) {
				XRangeIndicator xRangeIndicator = new XRangeIndicator(this.xAxis, roi.getStart(), roi.getEnd(), roi.getName());
				xRangeIndicator.setLabelVerticalPosition(0.2);
				
				Color color = Color.web(roi.getColor());
				newStyleSheet += String.format(".x-range-indicator-rect%d { -fx-stroke: transparent; -fx-fill: rgba(%d, %d, %d, %f); }\n", 
						index, Math.round(color.getRed()*255), Math.round(color.getGreen()*255), Math.round(color.getBlue()*255), roi.getOpacity());
				
				Color solidColor = Utils.rgba2rgb((int) Math.round(color.getRed()*255), (int) Math.round(color.getGreen()*255), (int) Math.round(color.getBlue()*255), roi.getOpacity());
				
				newStyleSheet += String.format(".x-range-indicator-label%d { -fx-background-color: rgb(%d, %d, %d); }\n", 
						index, Math.round(solidColor.getRed()*255), Math.round(solidColor.getGreen()*255), Math.round(solidColor.getBlue()*255));
				
				getChart().getPlugins().add(xRangeIndicator);
			} 
			
			if (yAxisList.contains(roi.getColumn())) {
				YRangeIndicator yRangeIndicator = new YRangeIndicator(this.yAxis, roi.getStart(), roi.getEnd(), roi.getName());
				yRangeIndicator.setLabelHorizontalPosition(0.2);

				Color color = Color.web(roi.getColor());
				newStyleSheet += String.format(".y-range-indicator-rect%d { -fx-stroke: transparent; -fx-fill: rgba(%d, %d, %d, %f); }", 
						index, Math.round(color.getRed()*255), Math.round(color.getGreen()*255), Math.round(color.getBlue()*255), roi.getOpacity());
				
				Color solidColor = Utils.rgba2rgb((int) Math.round(color.getRed()*255), (int) Math.round(color.getGreen()*255), (int) Math.round(color.getBlue()*255), roi.getOpacity());
				
				newStyleSheet += String.format(".y-range-indicator-label%d { -fx-background-color: rgb(%d, %d, %d); }\n", 
						index, Math.round(solidColor.getRed()*255), Math.round(solidColor.getGreen()*255), Math.round(solidColor.getBlue()*255));
				
				getChart().getPlugins().add(yRangeIndicator);
			}
		}
		
		ArrayList<String> positionNames = new ArrayList<>(molecule.getPositionNames());
		for (int index=0; index<positionNames.size(); index++) {
			String positionName = positionNames.get(index);
			PositionOfInterest poi = molecule.getPosition(positionName);
			
			if (xAxisList.contains(poi.getColumn())) {
				XValueIndicator xValueIndicator = new XValueIndicator(this.xAxis, poi.getPosition(), poi.getName());
				xValueIndicator.setLabelPosition(0.2);
				
				Color color = Color.web(poi.getColor());
				newStyleSheet += String.format(".x-value-indicator-line%d { -fx-stroke: rgba(%d, %d, %d, %f); }", 
						index, Math.round(color.getRed()*255), Math.round(color.getGreen()*255), Math.round(color.getBlue()*255), color.getOpacity());
				
				getChart().getPlugins().add(xValueIndicator);
			}
			
			if (yAxisList.contains(poi.getColumn())) {
				YValueIndicator yValueIndicator = new YValueIndicator(this.yAxis, poi.getPosition(), poi.getName());
				yValueIndicator.setLabelPosition(0.2);
				
				Color color = Color.web(poi.getColor());
				newStyleSheet += String.format(".y-value-indicator-line%d { -fx-stroke: rgba(%d, %d, %d, %f); }", 
						index, Math.round(color.getRed()*255), Math.round(color.getGreen()*255), Math.round(color.getBlue()*255), color.getOpacity());
				
				getChart().getPlugins().add(yValueIndicator);
			}
		}
		
		getChart().getStylesheets().add(getPlotPane().getStyleSheetUpdater().getStyleSheetURL(newStyleSheet));
	}

	@Override
	public void removeIndicators() {
		ArrayList<Object> indicators = new ArrayList<Object>();
    	for (ChartPlugin plugin : getChart().getPlugins())
			if (plugin instanceof AbstractValueIndicator)
				indicators.add(plugin);
		getChart().getPlugins().removeAll(indicators);
		//remove indicator stylesheets
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
		removeIndicators();
		getDatasetOptionsPane().setTable(molecule.getDataTable());
		for (ChartPlugin plugin : getChart().getPlugins())
			if (plugin instanceof MarsMoleculePlotPlugin)
				((MarsMoleculePlotPlugin) plugin).setMolecule(molecule);
		update();
		resetXYZoom();
	}

	@Override
	protected MarsTable getDataTable() {
		if (molecule != null) {
			return molecule.getDataTable();
		} else 
			return null;
	}
}
