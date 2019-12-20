/*******************************************************************************
 * Copyright (C) 2019, Duderstadt Lab
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package de.mpg.biochem.mars.fx.molecule.moleculesTab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import de.mpg.biochem.mars.fx.event.InitializeMoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.event.MoleculeSelectionChangedEvent;
import de.mpg.biochem.mars.fx.plot.PlotPane;
import de.mpg.biochem.mars.fx.plot.SubPlot;
import de.mpg.biochem.mars.fx.plot.event.PlotEvent;
import de.mpg.biochem.mars.fx.plot.event.UpdatePlotAreaEvent;
import de.mpg.biochem.mars.fx.table.MarsTableView;
import de.mpg.biochem.mars.molecule.AbstractJsonConvertibleRecord;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.util.MarsUtil;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.BorderPane;

public abstract class AbstractMoleculeCenterPane<M extends Molecule, P extends PlotPane> extends AbstractJsonConvertibleRecord implements MoleculeSubPane {
	protected TabPane tabPane;
	protected Tab dataTableTab;
	protected Tab plotTab;
	
	protected BorderPane dataTableContainer;
	protected P plotPane;
	
	protected HashSet<ArrayList<String>> segmentTableNames;
	protected HashSet<String> refreshedTabs;
	protected HashMap<String, ArrayList<String>> tabNameToSegmentName;
	
	protected M molecule;
	
	public AbstractMoleculeCenterPane() {
		tabPane = new TabPane();
		tabPane.setFocusTraversable(false);
		
		initializeTabs();
	}
	
	protected void initializeTabs() {
		dataTableTab = new Tab();		
		dataTableTab.setText("DataTable");
		dataTableContainer = new BorderPane();
		dataTableTab.setContent(dataTableContainer);
		
		plotTab = new Tab();
		plotTab.setText("Plot");
		plotPane = createPlotPane();
		plotTab.setContent(plotPane.getNode());
		
		tabPane.getTabs().add(dataTableTab);
		tabPane.getTabs().add(plotTab);
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		
		tabPane.setStyle("");
		tabPane.getStylesheets().clear();
		tabPane.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/moleculesTab/MoleculeTablesPane.css");
		
		tabPane.getSelectionModel().select(dataTableTab);
		
		segmentTableNames = new HashSet<ArrayList<String>>();
		refreshedTabs = new HashSet<String>();
		tabNameToSegmentName = new HashMap<String, ArrayList<String>>();
		
		getNode().addEventHandler(MoleculeEvent.MOLECULE_EVENT, this);
		getNode().addEventHandler(PlotEvent.PLOT_EVENT, new EventHandler<PlotEvent>() { 
			   @Override 
			   public void handle(PlotEvent e) { 
				   	if (e.getEventType().getName().equals("UPDATE_PLOT_AREA")) {
				   		plotPane.fireEvent(new UpdatePlotAreaEvent());
				   		e.consume();
				   	}
			   }
		});
		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, new EventHandler<MoleculeArchiveEvent>() {
			@Override
			public void handle(MoleculeArchiveEvent e) {
				if (e.getEventType().getName().equals("INITIALIZE_MOLECULE_ARCHIVE")) {
			   		plotPane.fireEvent(new InitializeMoleculeArchiveEvent(e.getArchive()));
			   		e.consume();
			   	}
			} 
        });
		
		tabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            refreshSelectedTab();
        });
	}
	
	public void refreshSelectedTab() {
		Tab selectedTab = tabPane.getSelectionModel().selectedItemProperty().get();
		String tabName = selectedTab.getText();
		
		//Tab has already been refreshed
		if (refreshedTabs.contains(tabName))
			return;
		
		if (selectedTab.equals(dataTableTab)) {
			dataTableContainer.setCenter(new MarsTableView(molecule.getDataTable()));
		} else if (selectedTab.equals(plotTab)) {
			plotPane.fireEvent(new MoleculeSelectionChangedEvent(molecule));
		} else {
			((BorderPane) selectedTab.getContent()).setCenter(new MarsTableView(molecule.getSegmentsTable(tabNameToSegmentName.get(tabName))));
		}
		refreshedTabs.add(tabName);
	}

	protected Tab buildSegmentTab(ArrayList<String> segmentTableName) {		
		String tabName;
		if (segmentTableName.get(2).equals(""))
			tabName = segmentTableName.get(1) + " vs " + segmentTableName.get(0);
		else 
			tabName = segmentTableName.get(1) + " vs " + segmentTableName.get(0) + " - " + segmentTableName.get(2);
		tabNameToSegmentName.put(tabName, segmentTableName);
				
		Tab segmentTableTab = new Tab(tabName);
		BorderPane segmentTableContainer = new BorderPane();
		segmentTableTab.setContent(segmentTableContainer);
		
		return segmentTableTab;
	}
	
	protected void updateSegmentTables() {
		//Build new segment table list
		HashSet<ArrayList<String>> newSegmentTableNames = new HashSet<ArrayList<String>>();
		for (ArrayList<String> segmentTableName : molecule.getSegmentTableNames())
			newSegmentTableNames.add(segmentTableName);
		
		//Remove segment table tabs that are not needed 
		segmentTableNames.stream().filter(segmentTableName -> !newSegmentTableNames.contains(segmentTableName))
			.forEach(segmentTableName -> tabPane.getTabs().stream().filter(tab -> {
				String tabName;
				if (segmentTableName.get(2).equals(""))
					tabName = segmentTableName.get(1) + " vs " + segmentTableName.get(0);
				else 
					tabName = segmentTableName.get(1) + " vs " + segmentTableName.get(0) + " - " + segmentTableName.get(2);
				return tab.getText().equals(tabName);
			})
			.findFirst()
			.ifPresent(tabToRemove -> tabPane.getTabs().remove(tabToRemove)));
		
		//Add new segment table tabs that are needed
		newSegmentTableNames.stream().filter(name -> !segmentTableNames.contains(name))
			.forEach(name -> tabPane.getTabs().add(buildSegmentTab(name)));
		
		segmentTableNames = newSegmentTableNames;
	}
	
	@SuppressWarnings("unchecked")
	public void onMoleculeSelectionChangedEvent(Molecule molecule) {
		this.molecule = (M) molecule;
		
		//all tabs are now stale
		refreshedTabs.clear();
		
		updateSegmentTables();
		
		refreshSelectedTab();
	}
	
	@Override
	protected void createIOMaps() {
		ArrayList<SubPlot> subplots = plotPane.getCharts();

		outputMap.put("SubPlots", MarsUtil.catchConsumerException(jGenerator -> {
			jGenerator.writeArrayFieldStart("SubPlots");
			for (SubPlot subplot : subplots)
				subplot.toJSON(jGenerator);
			jGenerator.writeEndArray();
		}, IOException.class));
	}
	
	public abstract P createPlotPane();
	
	@Override
	public Node getNode() {
		return tabPane;
	}
	
	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}
   
   @Override
   public void handle(MoleculeEvent event) {
	   event.invokeHandler(this);
	   event.consume();
   }
}
