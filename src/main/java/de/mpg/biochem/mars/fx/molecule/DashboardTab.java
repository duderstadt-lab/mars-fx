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
package de.mpg.biochem.mars.fx.molecule;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

import javafx.geometry.Insets;

import java.util.ArrayList;

import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.molecule.dashboardTab.ArchivePropertiesWidget;
import de.mpg.biochem.mars.fx.molecule.dashboardTab.CategoryChartWidget;
import de.mpg.biochem.mars.fx.molecule.dashboardTab.MarsDashboardWidget;
import de.mpg.biochem.mars.fx.molecule.dashboardTab.TagFrequencyWidget;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.layout.BorderPane;

import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToolBar;

import com.jfoenix.controls.JFXMasonryPane;
import com.jfoenix.controls.JFXScrollPane;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javafx.scene.control.ButtonBase;

public class DashboardTab extends AbstractMoleculeArchiveTab {
	private BorderPane borderPane;
	
    private ScrollPane scrollPane;
    private JFXMasonryPane widgetPane;
    private ToolBar toolbar;
    
    private final int MAX_THREADS = 4;

    private final Executor executor = Executors.newFixedThreadPool(MAX_THREADS, runnable -> {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        return t ;
    });
    
    protected ObservableList<MarsDashboardWidget> widgets = FXCollections.observableArrayList();
	
    public DashboardTab() {
    	super();
    	setIcon(MaterialIconFactory.get().createIcon(de.jensd.fx.glyphs.materialicons.MaterialIcon.DASHBOARD, "1.3em"));
    	
    	borderPane = new BorderPane();
    	
    	ButtonBase archivePropertiesWidget = ActionUtils.createToolBarButton("Properties", ArchivePropertiesWidget.getIcon(),
				e -> {
					ArchivePropertiesWidget propertiesWidget = new ArchivePropertiesWidget(archive, this);
			    	addWidget(propertiesWidget);
				}, null);
    	
    	
    	ButtonBase tagFrequencyWidget = ActionUtils.createToolBarButton("Tag frequency", TagFrequencyWidget.getIcon(),
				e -> {
			    	addWidget(new TagFrequencyWidget(archive, this));
				}, null);
    	
    	ButtonBase categoryChartWidget = ActionUtils.createToolBarButton("Category Chart", CategoryChartWidget.getIcon(),
				e -> {
			    	addWidget(new CategoryChartWidget(archive, this));
				}, null);
    	
    	Action removeAllWidgets = new Action("Remove all", null, BOMB,
				e -> {
					widgets.stream().filter(widget -> widget.isRunning()).forEach(widget -> widget.cancel());
					widgets.clear();
					widgetPane.getChildren().clear();
				});
    	
    	Action reloadWidgets = new Action("Reload", null, REFRESH,
				e -> {
					widgets.stream().filter(widget -> !widget.isRunning()).forEach(widget -> {
						//Make a new Task and run it by adding it to the executor....
						//Also add a reference of the task to the widget....
						widget.spin();
						runWidget(widget);
					});
				});
    	
    	toolbar = ActionUtils.createToolBar(archivePropertiesWidget, tagFrequencyWidget, categoryChartWidget);
    	toolbar.getStylesheets().add("de/mpg/biochem/mars/fx/MarkdownWriter.css");
    	
    	
    	// horizontal spacer
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		toolbar.getItems().add(spacer);
		
    	toolbar.getItems().addAll(ActionUtils.createToolBarButton(removeAllWidgets), ActionUtils.createToolBarButton(reloadWidgets));
    	
    	borderPane.setTop(toolbar);
    	  	
    	widgetPane = new JFXMasonryPane();
    	widgetPane.setPadding(new Insets(10, 10, 10, 10));
    	
    	scrollPane = new ScrollPane();
    	scrollPane.setContent(widgetPane);
    	scrollPane.setFitToWidth(true);
    	borderPane.setCenter(scrollPane);
    	
        getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, this);
        
    	getTab().setContent(borderPane);
    }
    
    public void runWidget(MarsDashboardWidget widget) {
    	//Runn the spinning thing here ? Start it here !!
    	//This is the javafx thread so it should directly start spinning.
    	
    	executor.execute(widget);
    }
    
    public Node getNode() {
		return borderPane;
	}
    
    public JFXMasonryPane getWidgetPane() {
    	return widgetPane;
    }
    
	public ArrayList<Menu> getMenus() {
		return null;
	}
	
	public ObservableList<MarsDashboardWidget> getWidgets() {
		return widgets;
	}
	
	public void addWidget(MarsDashboardWidget widget) {
		widgets.add(widget);
		widgetPane.getChildren().add(widget.getNode());
	}
	
	public void removeWidget(MarsDashboardWidget widget) {
		widgets.remove(widget);
		widgetPane.getChildren().remove(widget.getNode());
	}
	
    @Override
    public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive) {
    	this.archive = archive;  
    }

	@Override
	protected void createIOMaps() {
		// TODO Auto-generated method stub
	}
	
	@Override
	public String getName() {
		return "DashboardTab";
	}
}
