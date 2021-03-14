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
package de.mpg.biochem.mars.fx.molecule.metadataTab;

import java.io.IOException;
import java.util.HashSet;

import org.scijava.Context;
import org.scijava.plugin.Parameter;

import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.mpg.biochem.mars.fx.editor.LogPane;
import de.mpg.biochem.mars.fx.event.InitializeMoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MetadataEvent;
import de.mpg.biochem.mars.fx.event.MetadataSelectionChangedEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.molecule.metadataTab.dashboard.MarsMetadataDashboard;
import de.mpg.biochem.mars.fx.table.MarsTableView;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.AbstractJsonConvertibleRecord;
import de.mpg.biochem.mars.util.MarsUtil;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.BorderPane;

public abstract class AbstractMetadataCenterPane<I extends MarsMetadata> extends AbstractJsonConvertibleRecord implements MetadataSubPane {
	
	protected TabPane tabPane;
	protected BorderPane OMEContainer;
	protected Tab OMETab;
	protected MarsOMEView OMEView;
	
	protected Tab logTab;
	protected BorderPane logContainer;
	protected LogPane logPane;
	
	protected Tab marsMetadataDashboardTab;
	protected MarsMetadataDashboard<I> marsMetadataDashboardPane;
	
	protected Tab BdvTab;
	protected BdvViewTable bdvViewTable;
	
	protected HashSet<String> refreshedTabs;
	
	protected I marsMetadata;
	
	@Parameter
	protected Context context;
	
	public AbstractMetadataCenterPane(final Context context) {
		super();
		context.inject(this);
		tabPane = new TabPane();
		tabPane.setFocusTraversable(false);

		OMETab = new Tab();		
		OMETab.setText("OME");
		OMEContainer = new BorderPane();
		OMEView = new MarsOMEView(context);
		OMEContainer.setCenter(OMEView.getNode());
		OMETab.setContent(OMEContainer);
		
		tabPane.getTabs().add(OMETab);
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		
		BdvTab = new Tab();
		BdvTab.setText("Bdv Sources");
		bdvViewTable = new BdvViewTable();
		BdvTab.setContent(bdvViewTable.getNode());
		tabPane.getTabs().add(BdvTab);
		
		logTab = new Tab();
		logTab.setText("Log");
		logContainer = new BorderPane();
		logContainer.getStylesheets().add("de/mpg/biochem/mars/fx/MarkdownWriter.css");
		//logContainer.setStyle(".find-replace { -fx-border-width: 0 0 1 0; }");

		logTab.setContent(logContainer);
		
		logPane = new LogPane();
		//logPane.setEditable(false);
		logContainer.setCenter(logPane.getNode());
		
		tabPane.getTabs().add(logTab);
		
		marsMetadataDashboardTab = new Tab();
		marsMetadataDashboardTab.setText("");
		marsMetadataDashboardTab.setGraphic(MaterialIconFactory.get().createIcon(de.jensd.fx.glyphs.materialicons.MaterialIcon.DASHBOARD, "1.0em"));
		marsMetadataDashboardPane = new MarsMetadataDashboard<I>(context);
		marsMetadataDashboardTab.setContent(marsMetadataDashboardPane.getNode());
		
		tabPane.getTabs().add(marsMetadataDashboardTab);
		
		tabPane.setStyle("");
		tabPane.getStylesheets().clear();
		tabPane.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/metadataTab/MetaTablesPane.css");
		
		tabPane.getSelectionModel().select(OMETab);
		
		refreshedTabs = new HashSet<String>();
		
		getNode().addEventHandler(MetadataEvent.METADATA_EVENT, this);
		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, new EventHandler<MoleculeArchiveEvent>() {
			@Override
			public void handle(MoleculeArchiveEvent e) {
				if (e.getEventType().getName().equals("INITIALIZE_MOLECULE_ARCHIVE")) {
			   		marsMetadataDashboardPane.fireEvent(new InitializeMoleculeArchiveEvent(e.getArchive()));
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
		
		if (selectedTab.equals(OMETab)) {
			loadOMEMetadata();
		} else if (selectedTab.equals(logTab)) {
			loadLog();
		} else if (selectedTab.equals(marsMetadataDashboardTab)) {
			marsMetadataDashboardPane.fireEvent(new MetadataSelectionChangedEvent(marsMetadata));
		} else {
			
		}
		refreshedTabs.add(tabName);
	}
	
	protected void loadOMEMetadata() {
		OMEView.fill(marsMetadata);
	}
	
	@Override
	protected void createIOMaps() {
		
		setJsonField("marsMetadataDashboard", 
			jGenerator -> {
				jGenerator.writeFieldName("marsMetadataDashboard");
				marsMetadataDashboardPane.toJSON(jGenerator);
			}, 
			jParser -> marsMetadataDashboardPane.fromJSON(jParser));
		 
		/*
		 * 
		 * The fields below are needed for backwards compatibility.
		 * 
		 * Please remove for a future release.
		 * 
		 */
		
		setJsonField("MarsMetadataDashboard", null, 
			jParser -> marsMetadataDashboardPane.fromJSON(jParser));
	}
	
	protected void loadLog() {
		logPane.setMarkdown(marsMetadata.getLog());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onMetadataSelectionChangedEvent(MarsMetadata marsMetadata) {
		this.marsMetadata = (I) marsMetadata;
		//all tabs are now stale
		refreshedTabs.clear();
		
		refreshSelectedTab();
		
		bdvViewTable.fireEvent(new MetadataSelectionChangedEvent(marsMetadata));
		marsMetadataDashboardPane.fireEvent(new MetadataSelectionChangedEvent(marsMetadata));
	}
	
	@Override
	public Node getNode() {
		return tabPane;
	}
	
	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}
	
	@Override
    public void handle(MetadataEvent event) {
	   event.invokeHandler(this);
	   event.consume();
    } 
}
