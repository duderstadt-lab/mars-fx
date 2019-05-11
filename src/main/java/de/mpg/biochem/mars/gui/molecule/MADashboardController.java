package de.mpg.biochem.mars.gui.molecule;

import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;

public class MADashboardController implements MAPaneController {
	
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
