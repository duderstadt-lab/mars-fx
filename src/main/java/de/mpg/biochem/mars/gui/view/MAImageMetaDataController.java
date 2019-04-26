package de.mpg.biochem.mars.gui.view;

import com.jfoenix.controls.JFXToggleButton;

import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.fxml.FXML;

public class MAImageMetaDataController implements MAPaneController {
	
	MoleculeArchive archive;

	@Override
	public void setArchive(MoleculeArchive archive) {
		this.archive = archive;
	}
}
