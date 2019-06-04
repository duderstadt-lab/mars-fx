package de.mpg.biochem.mars.fx.molecule;

import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;

public class DashboardTabController implements MoleculeArchiveSubTab {
	
	MoleculeArchive archive;
	
    @FXML
    public void initialize() {
        
    }

	@Override
	public void setArchive(MoleculeArchive archive) {
		this.archive = archive;
	}
	/*
	public void handleXXXX() {

	}
*/
}
