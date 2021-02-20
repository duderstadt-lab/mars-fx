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
package de.mpg.biochem.mars.fx.plot;

import java.util.HashSet;
import java.util.Set;

import de.mpg.biochem.mars.fx.plot.tools.MarsDoubleDataSet;
import de.mpg.biochem.mars.table.MarsTable;
import javafx.event.Event;

public class MarsTableSubPlot extends AbstractSubPlot {
	protected MarsTable table;
	
	public MarsTableSubPlot(PlotPane plotPane, String plotTitle, MarsTable table) {
		super(plotPane, plotTitle);
		this.table = table;
		getDatasetOptionsPane().setColumns(new HashSet<String>(table.getColumnHeadingList()));
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
	protected DatasetOptionsPane createDatasetOptionsPane(Set<String> columns) {
		return new DatasetOptionsPane(columns, this, true);
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
