package de.mpg.biochem.mars.gui.molecule;

import com.jfoenix.controls.JFXToggleButton;

import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.fxml.FXML;

public class ImageMetaDataTabController implements MoleculeArchiveSubTab {
	
	MoleculeArchive archive;

	@Override
	public void setArchive(MoleculeArchive archive) {
		this.archive = archive;
	}
}
