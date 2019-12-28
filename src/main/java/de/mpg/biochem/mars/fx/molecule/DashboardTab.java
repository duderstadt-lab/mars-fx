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

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;

import java.util.ArrayList;

import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.molecule.dashboardTab.ArchivePropertiesWidget;
import de.mpg.biochem.mars.fx.molecule.dashboardTab.DashboardWidget;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;

import javafx.scene.control.ScrollPane;
import com.jfoenix.controls.JFXMasonryPane;
import com.jfoenix.controls.JFXScrollPane;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.BorderStroke;
import javafx.scene.paint.Color;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.BorderWidths;

import javafx.scene.layout.FlowPane;

public class DashboardTab extends AbstractMoleculeArchiveTab {
    protected ScrollPane scrollPane;
    
    protected FlowPane flowPane;
    
    protected ObservableList<DashboardWidget> widgets = FXCollections.observableArrayList();
	
    public DashboardTab() {
    	super();
    	setIcon(MaterialIconFactory.get().createIcon(de.jensd.fx.glyphs.materialicons.MaterialIcon.DASHBOARD, "1.3em"));
    	
    	flowPane = new FlowPane();
    	scrollPane = new ScrollPane();
    	
    	flowPane.setPadding(new Insets(10, 10, 10, 10));
    	
    	flowPane.setVgap(10);
    	flowPane.setHgap(10);
    	
    	flowPane.setColumnHalignment(HPos.LEFT);
    	flowPane.setRowValignment(VPos.TOP);
    	
    	scrollPane.setContent(flowPane);
    	
    	scrollPane.setFitToWidth(true);
        
        getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, this);
        
    	getTab().setContent(scrollPane);
    }
    
    public Node getNode() {
		return flowPane;
	}
    
	public ArrayList<Menu> getMenus() {
		return null;
	}
	
	public ObservableList<DashboardWidget> getWidgets() {
		return widgets;
	}
	
	public void addWidget(DashboardWidget widget) {
		widgets.add(widget);
		flowPane.getChildren().add(widget.getNode());
	}
	
	public void removeWidget(DashboardWidget widget) {
		widgets.remove(widget);
		flowPane.getChildren().remove(widget.getNode());
	}
	
    @Override
    public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive) {
    	this.archive = archive;
    	
    	ArchivePropertiesWidget propertiesWidget = new ArchivePropertiesWidget(archive, this);
    	flowPane.getChildren().add(propertiesWidget.getNode());    
    	
    	ArchivePropertiesWidget propertiesWidget2 = new ArchivePropertiesWidget(archive, this);
    	flowPane.getChildren().add(propertiesWidget2.getNode());    
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
