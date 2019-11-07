package de.mpg.biochem.mars.fx.plot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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
//import de.mpg.biochem.mars.fx.plot.tools.MarsRegionSelectionTool;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.PositionOfInterest;
import de.mpg.biochem.mars.molecule.RegionOfInterest;
import de.mpg.biochem.mars.table.MarsTable;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Cursor;

public abstract class AbstractMoleculeSubPlot<M extends Molecule> extends AbstractSubPlot implements MoleculeSubPane {
	
	protected M molecule;
	
	//Keep track of axes for which indicators have already been added.
	protected HashSet<String> namesOfActiveRegions, namesOfActivePositions;
	
	public AbstractMoleculeSubPlot(PlotPane plotPane, String plotTitle) {
		super(plotPane, plotTitle);
		
		namesOfActiveRegions = new HashSet<String>();
		namesOfActivePositions = new HashSet<String>();
		
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
	
	protected void addDataSet(SegmentDataSetRenderer renderer, PlotSeries plotSeries) {
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
			renderer.getDatasets().add(segmentsDataSet);
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
		renderer.getDatasets().add(dataset);	
	}

	//For the moment we make a copy...
	//maybe long-term we should no make a copy to improve performance.
	//But that might require a bit change in how things are store so....

	
	protected void addRegionsOfInterest(String xColumn, String yColumn) {
		for (String regionName : molecule.getRegionNames()) {
			if (molecule.getRegion(regionName).getColumn().equals(xColumn) && !namesOfActiveRegions.contains(regionName)) {
				RegionOfInterest roi = molecule.getRegion(regionName);
				XRangeIndicator xRangeIndicator = new XRangeIndicator(this.globalXAxis, roi.getStart(), roi.getEnd(), roi.getName());
				xRangeIndicator.setLabelVerticalPosition(0.2);
				namesOfActiveRegions.add(regionName);
				chartPane.getPlugins().add(xRangeIndicator);
			} else if (molecule.getRegion(regionName).getColumn().equals(yColumn) && !namesOfActiveRegions.contains(regionName)) {
				RegionOfInterest roi = molecule.getRegion(regionName);
				YRangeIndicator yRangeIndicator = new YRangeIndicator(this.globalYAxis, roi.getStart(), roi.getEnd(), roi.getName());
				yRangeIndicator.setLabelVerticalPosition(0.2);
				namesOfActiveRegions.add(regionName);
				chartPane.getPlugins().add(yRangeIndicator);
			}
		}
	}
	
	protected void addPositionsOfInterest(String xColumn, String yColumn) {
		for (String positionName : molecule.getPositionNames()) {
			if (molecule.getPosition(positionName).getColumn().equals(xColumn) && !namesOfActivePositions.contains(positionName)) {
				PositionOfInterest poi = molecule.getPosition(positionName);
				XValueIndicator xValueIndicator = new XValueIndicator(this.globalXAxis, poi.getPosition(), poi.getName());
				xValueIndicator.setLabelPosition(0.2);
				namesOfActivePositions.add(positionName);
				chartPane.getPlugins().add(xValueIndicator);
			} else if (molecule.getPosition(positionName).getColumn().equals(yColumn) && !namesOfActivePositions.contains(positionName)) {
				PositionOfInterest poi = molecule.getPosition(positionName);
				YValueIndicator yValueIndicator = new YValueIndicator(this.globalYAxis, poi.getPosition(), poi.getName());
				yValueIndicator.setLabelPosition(0.2);
				namesOfActivePositions.add(positionName);
				chartPane.getPlugins().add(yValueIndicator);
			}
		}
	}
	
	@Override
	public void removeIndicators() {
		ArrayList<Object> indicators = new ArrayList<Object>();
    	for (ChartPlugin plugin : chartPane.getPlugins())
			if (plugin instanceof AbstractValueIndicator)
				indicators.add(plugin);
		chartPane.getPlugins().removeAll(indicators);
		namesOfActiveRegions.clear();
		namesOfActivePositions.clear();
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
		for (ChartPlugin plugin : chartPane.getPlugins())
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
