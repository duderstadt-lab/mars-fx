package de.mpg.biochem.mars.fx.molecule;

import java.util.ArrayList;

import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.layout.BorderPane;

public class DashboardTab extends AbstractMoleculeArchiveTab {
    protected BorderPane rootPane;
	
    public DashboardTab() {
    	super();
    	setIcon(MaterialIconFactory.get().createIcon(de.jensd.fx.glyphs.materialicons.MaterialIcon.DASHBOARD, "1.3em"));
    	
    	rootPane = new BorderPane();
    	rootPane.setCenter(new Label("Coming soon..."));
    	
    	setContent(rootPane);
    }
    
    public Node getNode() {
		return rootPane;
	}
    
	public ArrayList<Menu> getMenus() {
		return null;
	}
}
