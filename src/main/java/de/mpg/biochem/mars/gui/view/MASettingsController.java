package de.mpg.biochem.mars.gui.view;

import com.jfoenix.controls.JFXToggleButton;

import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.fxml.FXML;

public class MASettingsController implements MAPaneController {
	
	MoleculeArchive archive;
	
	@FXML
	private JFXToggleButton smileEncodingButton;

	@Override
	public void setArchive(MoleculeArchive archive) {
		this.archive = archive;
		
		smileEncodingButton.setSelected(archive.isSMILEOutputEncoding());
	}
	
	public void handleToggleSmileEncoding() {
		System.out.println("Toggled");
		if (smileEncodingButton.isSelected()) {
			archive.setSMILEOutputEncoding();
		} else {
			archive.unsetSMILEOutputEncoding();
		}
	}

}
