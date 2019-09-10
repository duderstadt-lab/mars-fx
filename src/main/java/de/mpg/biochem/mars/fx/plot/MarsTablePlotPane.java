package de.mpg.biochem.mars.fx.plot;

import de.mpg.biochem.mars.table.MarsTable;

public class MarsTablePlotPane extends AbstractPlotPane {
	private MarsTable table;
	
	public MarsTablePlotPane(MarsTable table) {
		super();
		this.table = table;
		addChart();
	}

	@Override
	public void addChart() {
		SubPlot subplot = new MarsTableSubPlot(this, "Plot " + (charts.size() + 1), table);
		addChart(subplot);
	}
}
