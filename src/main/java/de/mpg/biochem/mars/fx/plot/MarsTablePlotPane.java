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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonToken;

import static java.util.stream.Collectors.toList;

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

	@Override
	protected void createIOMaps() {
		
		setJsonField("NumberSubPlots", 
			jGenerator -> {
				jGenerator.writeNumberField("NumberSubPlots", charts.size());
			}, 
			jParser -> {
		        int numberSubPlots = jParser.getNumberValue().intValue();
		        int subPlotIndex = 1;
		        while (subPlotIndex < numberSubPlots) {
		        	addChart();
		        	subPlotIndex++;
		        }
			});
		
		setJsonField("SubPlots", 
			jGenerator -> {
				jGenerator.writeArrayFieldStart("SubPlots");
				for (SubPlot subplot : charts) {
					jGenerator.writeStartObject();
					if (!subplot.getDatasetOptionsPane().getTitle().equals(""))
						jGenerator.writeStringField("Title", subplot.getDatasetOptionsPane().getTitle());
					
					if (!subplot.getDatasetOptionsPane().getXAxisName().equals(""))
						jGenerator.writeStringField("xAxisName", subplot.getDatasetOptionsPane().getXAxisName());
					
					if (!subplot.getDatasetOptionsPane().getYAxisName().equals(""))
						jGenerator.writeStringField("yAxisName", subplot.getDatasetOptionsPane().getYAxisName());
					
					if (subplot.getDatasetOptionsPane().getPlotSeriesList().size() > 0) {
						jGenerator.writeArrayFieldStart("PlotSeries");
						for (PlotSeries plotSeries : subplot.getDatasetOptionsPane().getPlotSeriesList()) 
							plotSeries.toJSON(jGenerator);
						jGenerator.writeEndArray();
					}
					jGenerator.writeEndObject();
				}
				jGenerator.writeEndArray();
			}, 
			jParser -> {
				int subPlotIndex = 0;
				while (jParser.nextToken() != JsonToken.END_ARRAY) {
					while (jParser.nextToken() != JsonToken.END_OBJECT) {
						if ("Title".equals(jParser.getCurrentName())) {
				    		jParser.nextToken();
				    		charts.get(subPlotIndex).getDatasetOptionsPane().setTitle(jParser.getText());
						}
						
						if ("xAxisName".equals(jParser.getCurrentName())) {
				    		jParser.nextToken();
				    		charts.get(subPlotIndex).getDatasetOptionsPane().setXAxisName(jParser.getText());
						}
						
						if ("yAxisName".equals(jParser.getCurrentName())) {
				    		jParser.nextToken();
				    		charts.get(subPlotIndex).getDatasetOptionsPane().setYAxisName(jParser.getText());
						}
						
						if ("PlotSeries".equals(jParser.getCurrentName())) {
							while (jParser.nextToken() != JsonToken.END_ARRAY) {
								PlotSeries series = new PlotSeries(getColumnNames());
								series.fromJSON(jParser);
								charts.get(subPlotIndex).getPlotSeriesList().add(series);
					    	}
						}
					}
					subPlotIndex++;
		    	}
		 	});
		
	}

	@Override
	public ArrayList<String> getColumnNames() {
		return (ArrayList<String>) table.getColumnHeadingList().stream().sorted().collect(toList());
	}
}
