package de.mpg.biochem.mars.fx.molecule;

import java.util.ArrayList;

import com.jfoenix.controls.JFXToggleButton;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;

public class SettingsTab implements MoleculeArchiveTab {
	
	private MoleculeArchive<?,?,?> archive;
	
	private JFXToggleButton smileEncodingButton;
	
	private BorderPane borderPane;
	
	public SettingsTab() {
		smileEncodingButton = new JFXToggleButton();
		borderPane = new BorderPane();
		borderPane.setCenter(smileEncodingButton);
	}
	
	public void handleToggleSmileEncoding() {
		if (smileEncodingButton.isSelected()) {
			archive.setSMILEOutputEncoding();
		} else {
			archive.unsetSMILEOutputEncoding();
		}
	}
	
	public Node getNode() {
		return this.borderPane;
	}
	
	public ArrayList<Menu> getMenus() {
		return new ArrayList<Menu>();
	}

	@Override
	public void setArchive(MoleculeArchive archive) {
		this.archive = archive;
		
		smileEncodingButton.setSelected(archive.isSMILEOutputEncoding());
	}
}
