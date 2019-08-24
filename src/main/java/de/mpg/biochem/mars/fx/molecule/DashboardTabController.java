package de.mpg.biochem.mars.fx.molecule;

import java.util.ArrayList;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;

public class DashboardTabController implements MoleculeArchiveSubTab {
	
	private MoleculeArchive<Molecule,MarsImageMetadata,MoleculeArchiveProperties> archive;
	
	private BorderPane borderPane;
	
    public DashboardTabController() {
    	borderPane = new BorderPane();
    	borderPane.setCenter(new Label("Coming soon..."));
    }
    
    public Node getNode() {
    	return borderPane;
    }

	@Override
	public void setArchive(MoleculeArchive<Molecule,MarsImageMetadata,MoleculeArchiveProperties> archive) {
		this.archive = archive;
	}
	
	public ArrayList<Menu> getMenus() {
		return new ArrayList<Menu>();
	}
}
