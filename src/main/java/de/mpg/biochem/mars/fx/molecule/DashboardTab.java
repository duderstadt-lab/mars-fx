package de.mpg.biochem.mars.fx.molecule;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

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
import javafx.scene.layout.BorderPane;

import javafx.scene.control.ScrollPane;
import com.jfoenix.controls.JFXMasonryPane;
import com.jfoenix.controls.JFXScrollPane;
import javafx.scene.layout.VBox;
import javafx.application.Platform;

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
        
    	setContent(scrollPane);
    }
    
    public VBox buildInfoCard() {
    	VBox vbox = new VBox();
        double width = 200;
        vbox.setPrefWidth(width);
        double height = 300;
        vbox.setPrefHeight(height);
    	
        vbox.getChildren().add(new Label("                                   "));
        vbox.getChildren().add(new Label("                                   "));
        vbox.getChildren().add(new Label("Archive Name                       " + archive.getName()));
        vbox.getChildren().add(new Label("Archive Type                       " + archive.getClass().getName()));
        vbox.getChildren().add(new Label("Number of Molecules                " + archive.getNumberOfMolecules()));
        vbox.getChildren().add(new Label("Number of Image Metadata Items     " + archive.getNumberOfImageMetadataRecords()));
        vbox.getChildren().add(new Label("                                   "));
        
		if (archive.isVirtual()) {
			vbox.getChildren().add(new Label("Working from the virtual memory store: "));
		} else {
			vbox.getChildren().add(new Label("This archive is stored in normal memory."));
		}
		
		return vbox;
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
    	Platform.runLater(() -> masonryPane.requestLayout());
    	Platform.runLater(() -> scrollPane.requestLayout());
    }
}
