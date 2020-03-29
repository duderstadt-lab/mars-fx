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
import java.util.Collections;

import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.molecule.dashboardTab.ArchivePropertiesWidget;
import de.mpg.biochem.mars.fx.molecule.dashboardTab.CategoryChartWidget;
import de.mpg.biochem.mars.fx.molecule.dashboardTab.MarsDashboardWidget;
import de.mpg.biochem.mars.fx.molecule.dashboardTab.MarsDashboardWidgetService;
import de.mpg.biochem.mars.fx.molecule.dashboardTab.TagFrequencyWidget;
import de.mpg.biochem.mars.fx.plot.PlotSeries;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.molecule.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.molecule.MoleculeArchiveService;
import de.mpg.biochem.mars.util.MarsUtil;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.layout.BorderPane;

import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToolBar;

import com.fasterxml.jackson.core.JsonToken;
import com.jfoenix.controls.JFXMasonryPane;
import com.jfoenix.controls.JFXScrollPane;
import javafx.geometry.BoundingBox;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.scijava.plugin.Parameter;

import javafx.scene.control.ButtonBase;
import java.util.List;
import java.io.IOException;
import java.util.*;

import de.mpg.biochem.mars.fx.molecule.dashboardTab.*;

public class DashboardTab extends AbstractMoleculeArchiveTab {
	private BorderPane borderPane;
	
    private ScrollPane scrollPane;
    private JFXMasonryPane widgetPane;
    private ToolBar toolbar;
    private MarsDashboardWidgetService marsDashboardWidgetService;
    
    private final int MAX_THREADS = 1;
    
    @Parameter
    private MoleculeArchiveService moleculeArchiveService;
    
    private final ArrayList<String> widgetToolbarOrder = new ArrayList<String>( 
            Arrays.asList("ArchivePropertiesWidget", 
                    "TagFrequencyWidget", 
                    "CategoryChartWidget",
                    "HistogramWidget",
                    "XYChartWidget",
                    "BubbleChartWidget"));
    
    private final List<WidgetRunnable> activeWidgets = Collections.synchronizedList(new ArrayList<>());

    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS, runnable -> {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        return t ;
    });
    
    protected ObservableList<MarsDashboardWidget> widgets = FXCollections.observableArrayList();
	
    public DashboardTab(MoleculeArchiveService moleculeArchiveService) {
    	super();
    	
    	this.moleculeArchiveService = moleculeArchiveService;
    	
    	setIcon(MaterialIconFactory.get().createIcon(de.jensd.fx.glyphs.materialicons.MaterialIcon.DASHBOARD, "1.3em"));
    	
    	borderPane = new BorderPane();
    	
    	Action removeAllWidgets = new Action("Remove all", null, BOMB,
				e -> {
					widgets.stream().filter(widget -> widget.isRunning()).forEach(widget -> stopWidget(widget));
					widgets.clear();
					widgetPane.getChildren().clear();
				});
    	
    	Action stopAllWidgets = new Action("Stop all", null, STOP,
				e -> widgets.stream().filter(widget -> widget.isRunning()).forEach(widget -> stopWidget(widget)));
    	
    	Action reloadWidgets = new Action("Reload", null, REFRESH,
				e -> {
					//executor.shutdownNow();
					widgets.stream().filter(widget -> !widget.isRunning()).forEach(widget -> runWidget(widget));
				});
    	
    	toolbar = new ToolBar();
    	toolbar.getStylesheets().add("de/mpg/biochem/mars/fx/MarkdownWriter.css");
    	
    	// horizontal spacer
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		toolbar.getItems().add(spacer);
		
    	toolbar.getItems().addAll(ActionUtils.createToolBarButton(removeAllWidgets), 
    			ActionUtils.createToolBarButton(stopAllWidgets),
    			ActionUtils.createToolBarButton(reloadWidgets));
    	
    	borderPane.setTop(toolbar);
    	  	
    	widgetPane = new JFXMasonryPane();
    	widgetPane.setLayoutMode(JFXMasonryPane.LayoutMode.BIN_PACKING);
    	//default below ensure they stay in order
    	//BIN_PACKING default to fitting them all in...
    	//widgetPane.setLayoutMode(JFXMasonryPane.LayoutMode.MASONRY);
    	widgetPane.setPadding(new Insets(10, 10, 10, 10));
    	
    	scrollPane = new ScrollPane();
    	scrollPane.setContent(widgetPane);
    	scrollPane.setFitToWidth(true);
    	borderPane.setCenter(scrollPane);
    	
        getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, this);
        
    	getTab().setContent(borderPane);
    }
    
    public void runWidget(MarsDashboardWidget widget) {
    	executor.execute(new WidgetRunnable(widget));
    }
    
    public void stopWidget(MarsDashboardWidget widget) {
    	activeWidgets.stream().filter(wr -> wr.getWidget().equals(widget)).findFirst().ifPresent(activeWidget -> activeWidget.stop());
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
    public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties> archive) {
    	this.archive = archive;  
        marsDashboardWidgetService = moleculeArchiveService.getContext().getService(MarsDashboardWidgetService.class);
    	
    	//Loop through all available widgets and add them to the toolbar
    	//use preferred order
    	Set<String> discoveredWidgets = marsDashboardWidgetService.getWidgetNames();
    	//widgetToolbarOrder
    	
    	ArrayList<Node> widgetButtons = new ArrayList<Node>();
    	
    	//Add all the expected widgets in the order defined by widgetToolbarOrder
    	widgetToolbarOrder.stream().filter(widgetName -> discoveredWidgets.contains(widgetName)).forEach(widgetName ->
    		widgetButtons.add(createWidgetButton(widgetName)));
    	
    	//Now add any newly discovered widgets besides the default set
    	discoveredWidgets.stream().filter(widgetName -> !widgetToolbarOrder.contains(widgetName)).forEach(widgetName ->
		widgetButtons.add(createWidgetButton(widgetName)));

    	toolbar.getItems().addAll(0, widgetButtons);
    }
    
    public ButtonBase createWidgetButton(String widgetName) {
    	//HACK to get the Icon for the toolbar before any widgets have been added to
		//the Dashboard...
		//We create a dummy widget just to get the Icon but never use it.
		//What is the workaround - can't seem to use a static method because that couldn't be in the interface.
		MarsDashboardWidget dummyWidgetForIcon = marsDashboardWidgetService.createWidget(widgetName);
		
		ButtonBase widgetButton = ActionUtils.createToolBarButton(widgetName, dummyWidgetForIcon.getIcon(),
				e -> {
					MarsDashboardWidget widget = marsDashboardWidgetService.createWidget(widgetName);
					widget.setArchive(archive);
					widget.setParent(this);
					widget.initialize();
			    	addWidget(widget);
				}, null);
		
		return widgetButton;
    }

	@Override
	protected void createIOMaps() {
		outputMap.put("Widgets", MarsUtil.catchConsumerException(jGenerator -> {
			jGenerator.writeArrayFieldStart("Widgets");
			for (MarsDashboardWidget widget : widgets) {
				jGenerator.writeStartObject();
				jGenerator.writeStringField("Name", widget.getName());
				jGenerator.writeFieldName("Settings");
				widget.toJSON(jGenerator);
				jGenerator.writeEndObject();
			}
			jGenerator.writeEndArray();
		}, IOException.class));
		
		inputMap.put("Widgets", MarsUtil.catchConsumerException(jParser -> {
			while (jParser.nextToken() != JsonToken.END_ARRAY) {
				while (jParser.nextToken() != JsonToken.END_OBJECT) {
					MarsDashboardWidget widget = null;
					
					if ("Name".equals(jParser.getCurrentName())) {
			    		jParser.nextToken();
			    		widget = marsDashboardWidgetService.createWidget(jParser.getText());
						widget.setArchive(archive);
						widget.setParent(this);
						widget.initialize();
				    	addWidget(widget);
					}
					
					jParser.nextToken();
					
					if ("Settings".equals(jParser.getCurrentName())) {
						jParser.nextToken();
						if (widget != null)
							widget.fromJSON(jParser);
					}
				}
	    	}
		}, IOException.class));
	}
	
	@Override
	public String getName() {
		return "DashboardTab";
	}
	
	class WidgetRunnable implements Runnable {
		
	    private final MarsDashboardWidget runnable;
	    
	    private Thread thread;
	    private AtomicBoolean canceled = new AtomicBoolean(false);

	    public WidgetRunnable(MarsDashboardWidget runnable) {
	        this.runnable = runnable;
	    	runnable.setRunning(true);
	    	runnable.spin();
	        activeWidgets.add(this);
	    }

	    @Override
	    public void run() {
	    	if (canceled.get())
	    		return;
	    	thread = Thread.currentThread();
	    	runnable.run();
	        Platform.runLater(new Runnable() {
				@Override
				public void run() {
					runnable.stopSpinning();
				}
			});
	        activeWidgets.remove(this);
	        runnable.setRunning(false);
	    }
	    
	    public MarsDashboardWidget getWidget() {
	    	return runnable;
	    }
	    
	    public void stop() {
	    	if (thread != null)
	    		thread.interrupt();
	    	canceled.set(true);
	    	runnable.stopSpinning();
	    	runnable.setRunning(false);
	    }
	}
}
