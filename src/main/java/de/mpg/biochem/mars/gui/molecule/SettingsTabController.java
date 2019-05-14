package de.mpg.biochem.mars.gui.molecule;

import com.jfoenix.controls.JFXToggleButton;

import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.fxml.FXML;

public class SettingsTabController implements MoleculeArchiveSubTab {
	
	MoleculeArchive archive;
	
	@FXML
	private JFXToggleButton smileEncodingButton;

	@Override
	public void setArchive(MoleculeArchive archive) {
		this.archive = archive;
		
		smileEncodingButton.setSelected(archive.isSMILEOutputEncoding());
	}
	
	public void handleToggleSmileEncoding() {
		if (smileEncodingButton.isSelected()) {
			archive.setSMILEOutputEncoding();
		} else {
			archive.unsetSMILEOutputEncoding();
		}
	}

}
