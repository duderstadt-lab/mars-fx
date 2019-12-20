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

import javafx.geometry.Insets;
import java.util.ArrayList;

import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
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

import javafx.scene.layout.BorderStroke;
import javafx.scene.paint.Color;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.BorderWidths;

public class DashboardTab extends AbstractMoleculeArchiveTab {
    protected ScrollPane scrollPane;
    
    protected JFXMasonryPane masonryPane;
	
    public DashboardTab() {
    	super();
    	setIcon(MaterialIconFactory.get().createIcon(de.jensd.fx.glyphs.materialicons.MaterialIcon.DASHBOARD, "1.3em"));
    	
    	masonryPane = new JFXMasonryPane();
    	scrollPane = new ScrollPane();
    	
    	scrollPane.setContent(masonryPane);
        JFXScrollPane.smoothScrolling(scrollPane);
        
        getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, this);
        
    	getTab().setContent(scrollPane);
    }
    
    public BorderPane buildInfoCard() {
    	BorderPane borderPane = new BorderPane();
    	VBox vbox = new VBox();
        double width = 450;
        borderPane.setMinWidth(width);
        double height = 200;
        borderPane.setMinHeight(height);
        
        borderPane.setPadding(new Insets(15, 15, 15, 15));
        vbox.setBorder(new Border(new BorderStroke(Color.BLACK, 
                BorderStrokeStyle.SOLID, new CornerRadii(20), new BorderWidths(1))));
        vbox.setPadding(new Insets(20, 20, 20, 20));
		vbox.setSpacing(5);
		
		BorderPane iconContainer = new BorderPane();
		iconContainer.setCenter(FontAwesomeIconFactory.get().createIcon(INFO_CIRCLE, "2em"));

		vbox.getChildren().add(iconContainer);
		
        vbox.getChildren().add(new Label(archive.getName()));
        vbox.getChildren().add(new Label(archive.getClass().getName()));
        vbox.getChildren().add(new Label(archive.getNumberOfMolecules() + " Molecules"));
        vbox.getChildren().add(new Label(archive.getNumberOfImageMetadataRecords() + " Metadata"));
        
		if (archive.isVirtual()) {
			vbox.getChildren().add(new Label("Virtual memory store"));
		} else {
			vbox.getChildren().add(new Label("Normal memory"));
		}
		
		borderPane.setCenter(vbox);
		
		return borderPane;
    }
    
    public Node getNode() {
		return scrollPane;
	}
    
	public ArrayList<Menu> getMenus() {
		return null;
	}
	
    @Override
    public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive) {
    	this.archive = archive;
    	masonryPane.getChildren().add(buildInfoCard());    
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
