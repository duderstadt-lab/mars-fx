package de.mpg.biochem.mars.fx.plot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.dataset.spi.DoubleDataSet;
import de.mpg.biochem.mars.fx.plot.tools.MarsDoubleDataSet;
import de.mpg.biochem.mars.fx.plot.tools.SegmentDataSetRenderer;
import de.mpg.biochem.mars.table.MarsTable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;

public class MarsTableSubPlot extends AbstractSubPlot {
	protected MarsTable table;
	
	public MarsTableSubPlot(PlotPane plotPane, String plotTitle, MarsTable table) {
		super(plotPane, plotTitle);
		this.table = table;
		getDatasetOptionsPane().setTable(table);
		update();
	}
	
	public void addDataSet(PlotSeries plotSeries) {
		String xColumn = plotSeries.getXColumn();
		String yColumn = plotSeries.getYColumn();
		
		if (!getDataTable().hasColumn(xColumn) || !getDataTable().hasColumn(yColumn))
			return;
		
		double lineWidth = Double.valueOf(plotSeries.getWidth());
		
		MarsDoubleDataSet dataset = new MarsDoubleDataSet(yColumn + " vs " + xColumn, plotSeries.getColor(), lineWidth, plotSeries.getLineStyle());
		
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
	
	@Override
	protected MarsTable getDataTable() {
		return table;
	}

	@Override
	public void fireEvent(Event event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeIndicators() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addIndicators(Set<String> AxisList, Set<String> yAxisList) {
		// TODO Auto-generated method stub
		
	}
}
