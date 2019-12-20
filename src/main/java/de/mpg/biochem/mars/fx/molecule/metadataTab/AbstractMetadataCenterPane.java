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
package de.mpg.biochem.mars.fx.molecule.metadataTab;

import de.mpg.biochem.mars.fx.editor.LogPane;
import de.mpg.biochem.mars.fx.event.MetadataEvent;
import de.mpg.biochem.mars.fx.event.MetadataSelectionChangedEvent;
import de.mpg.biochem.mars.fx.table.MarsTableView;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

import javafx.scene.control.ScrollPane;

public abstract class AbstractMetadataCenterPane<I extends MarsImageMetadata> implements MetadataSubPane {
	
	protected TabPane tabPane;
	protected BorderPane dataTableContainer;
	protected Tab dataTableTab;
	
	protected Tab logTab;
	protected BorderPane logContainer;
	protected LogPane logPane;
	
	protected Tab BdvTab;
	protected BdvViewTable bdvViewTable;
	
	protected I marsImageMetadata;
	
	public AbstractMetadataCenterPane() {
		tabPane = new TabPane();
		tabPane.setFocusTraversable(false);
		
		initializeTabs();
	}
	
	protected void initializeTabs() {
		dataTableTab = new Tab();		
		dataTableTab.setText("DataTable");
		dataTableContainer = new BorderPane();
		dataTableTab.setContent(dataTableContainer);
		
		tabPane.getTabs().add(dataTableTab);
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		
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
		
		BdvTab = new Tab();
		BdvTab.setText("Bdv Views");
		bdvViewTable = new BdvViewTable();
		BdvTab.setContent(bdvViewTable.getNode());
		tabPane.getTabs().add(BdvTab);
		
		tabPane.setStyle("");
		tabPane.getStylesheets().clear();
		tabPane.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/metadataTab/MetaTablesPane.css");
		
		tabPane.getSelectionModel().select(dataTableTab);
		
		getNode().addEventHandler(MetadataEvent.METADATA_EVENT, this);
	}
	
	protected void loadDataTable() {
		MarsTableView metaTable = new MarsTableView(marsImageMetadata.getDataTable());

		//prevent drawing exception of table not fitting on screen..
		//Still the last column is not easily accessed.
		metaTable.getColumns().add(new TableColumn<>("       "));
		metaTable.setPrefWidth(1000);
		dataTableContainer.setCenter(metaTable);
	}
	
	protected void loadLog() {
		logPane.setMarkdown(marsImageMetadata.getLog());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onMetadataSelectionChangedEvent(MarsImageMetadata marsImageMetadata) {
		this.marsImageMetadata = (I) marsImageMetadata;
		loadDataTable();
		loadLog();
		
		bdvViewTable.fireEvent(new MetadataSelectionChangedEvent(marsImageMetadata));
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
