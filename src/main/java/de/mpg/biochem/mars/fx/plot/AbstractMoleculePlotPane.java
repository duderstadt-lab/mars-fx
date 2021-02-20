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

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ARROWS_V;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.SQUARE_ALT;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.scijava.Context;

import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.core.JsonToken;

import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.plugins.Zoomer;
import de.mpg.biochem.mars.fx.event.InitializeMoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.event.MoleculeSelectionChangedEvent;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
import de.mpg.biochem.mars.fx.plot.event.PlotEvent;
import de.mpg.biochem.mars.fx.plot.event.UpdatePlotAreaEvent;
import de.mpg.biochem.mars.fx.plot.tools.MarsPositionSelectionPlugin;
import de.mpg.biochem.mars.fx.plot.tools.MarsRegionSelectionPlugin;
//import de.mpg.biochem.mars.fx.plot.tools.MarsPositionSelectionTool;
//import de.mpg.biochem.mars.fx.plot.tools.MarsRegionSelectionTool;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.util.MarsUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Node;

public abstract class AbstractMoleculePlotPane<M extends Molecule, S extends SubPlot> extends AbstractPlotPane implements MoleculeSubPane {
	
	protected M molecule;
	
	protected BooleanProperty regionSelected;
	protected BooleanProperty positionSelected;
	
	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;
	
	public AbstractMoleculePlotPane(final Context context) {
		super();
		context.inject(this);

		addChart();
		
		getNode().addEventHandler(MoleculeEvent.MOLECULE_EVENT, this);
		getNode().addEventHandler(PlotEvent.PLOT_EVENT, new EventHandler<PlotEvent>() { 
			   @Override 
			   public void handle(PlotEvent e) { 
				   	if (e.getEventType().getName().equals("UPDATE_PLOT_AREA")) {
				   		e.consume();
				   		for (SubPlot subPlot : charts) 
							subPlot.fireEvent(new UpdatePlotAreaEvent());
				   	}
			   };
		});
		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, new EventHandler<MoleculeArchiveEvent>() {
			@Override
			public void handle(MoleculeArchiveEvent e) {
				if (e.getEventType().getName().equals("INITIALIZE_MOLECULE_ARCHIVE")) {
					archive = e.getArchive();
					for (SubPlot subplot : charts)
						subplot.getDatasetOptionsPane().setColumns(archive.properties().getColumnSet());
			   		e.consume();
			   	} else if (e.getEventType().getName().equals("MOLECULE_ARCHIVE_UNLOCK")) {
			   		for (SubPlot subplot : charts)
						subplot.getDatasetOptionsPane().setColumns(archive.properties().getColumnSet());
			   		e.consume();
			   	}
			} 
        });
	}
	
	@Override
	protected void buildTools() {
		super.buildTools();
		
		regionSelected = new SimpleBooleanProperty();
		Action regionSelectionCursor = new Action("region", "Shortcut+R", SQUARE_ALT, 
				e -> setTool(regionSelected, () -> {
					MarsRegionSelectionPlugin tool = new MarsRegionSelectionPlugin(AxisMode.X);
					return tool;
				}, Cursor.DEFAULT), 
				null, regionSelected);
		addTool(regionSelectionCursor);
		
		positionSelected = new SimpleBooleanProperty();
		Action positionSelectionCursor = new Action("position", "Shortcut+P", de.jensd.fx.glyphs.octicons.OctIcon.MILESTONE, 
				e -> setTool(positionSelected, () -> {
					MarsPositionSelectionPlugin tool = new MarsPositionSelectionPlugin();
					return tool;
				}, Cursor.DEFAULT),
				null, positionSelected);
		addTool(positionSelectionCursor);
		
	}
	
	public MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> getArchive() {
		return archive;
	}
	
	@Override
	protected void createIOMaps() {
		
		setJsonField("numberSubPlots", 
			jGenerator -> {
				jGenerator.writeNumberField("numberSubPlots", charts.size());
			}, 
			jParser -> {
		        int numberSubPlots = jParser.getNumberValue().intValue();
		        int subPlotIndex = 1;
		        while (subPlotIndex < numberSubPlots) {
		        	addChart();
		        	subPlotIndex++;
		        }
			});
		
		setJsonField("subPlots", 
			jGenerator -> {
				jGenerator.writeArrayFieldStart("subPlots");
				for (SubPlot subplot : charts) {
					jGenerator.writeStartObject();
					if (!subplot.getDatasetOptionsPane().getTitle().equals(""))
						jGenerator.writeStringField("title", subplot.getDatasetOptionsPane().getTitle());
					
					if (!subplot.getDatasetOptionsPane().getXAxisName().equals(""))
						jGenerator.writeStringField("xAxisName", subplot.getDatasetOptionsPane().getXAxisName());
					
					if (!subplot.getDatasetOptionsPane().getYAxisName().equals(""))
						jGenerator.writeStringField("yAxisName", subplot.getDatasetOptionsPane().getYAxisName());
					
					if (!subplot.getDatasetOptionsPane().getYAxisName().equals(""))
						jGenerator.writeStringField("indicators", subplot.getDatasetOptionsPane().getSelectedIndicator());
					
					if (subplot.getDatasetOptionsPane().getPlotSeriesList().size() > 0) {
						jGenerator.writeArrayFieldStart("plotSeries");
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
						if ("title".equals(jParser.getCurrentName())) {
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
						
						if ("indicators".equals(jParser.getCurrentName())) {
				    		jParser.nextToken();
				    		charts.get(subPlotIndex).getDatasetOptionsPane().setSelectedIndicator(jParser.getText());
						}
						
						if ("plotSeries".equals(jParser.getCurrentName())) {
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
		
		/*
		 * 
		 * The fields below are needed for backwards compatibility.
		 * 
		 * Please remove for a future release.
		 * 
		 */
		
		setJsonField("NumberSubPlots", null, 
				jParser -> {
			        int numberSubPlots = jParser.getNumberValue().intValue();
			        int subPlotIndex = 1;
			        while (subPlotIndex < numberSubPlots) {
			        	addChart();
			        	subPlotIndex++;
			        }
				});
			
		setJsonField("SubPlots", null, 
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
						
						if ("Indicators".equals(jParser.getCurrentName())) {
				    		jParser.nextToken();
				    		charts.get(subPlotIndex).getDatasetOptionsPane().setSelectedIndicator(jParser.getText());
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
	public void handle(MoleculeEvent event) {
		event.invokeHandler(this);
		event.consume();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onMoleculeSelectionChangedEvent(Molecule molecule) {
		this.molecule = (M) molecule;
		chartsPane.getChildren().clear();
		
		for (SubPlot subPlot : charts) {
			subPlot.fireEvent(new MoleculeSelectionChangedEvent(molecule));
			chartsPane.getChildren().add(subPlot.getNode());
		}
		resetXYZoom();
	}

	@Override
	public void addChart() {
		SubPlot subplot = createSubPlot();
		if (molecule != null)
			subplot.fireEvent(new MoleculeSelectionChangedEvent(molecule));
		addChart(subplot);
	}
	
	@Override
	public ArrayList<String> getColumnNames() {
		if (archive != null)
			return (ArrayList<String>) archive.properties().getColumnSet().stream().sorted().collect(toList());
		else 
			return new ArrayList<String>();
	}
	
	public abstract S createSubPlot();
}
