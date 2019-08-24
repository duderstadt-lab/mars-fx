package de.mpg.biochem.mars.fx.molecule;

import java.util.ArrayList;

import com.jfoenix.controls.JFXToggleButton;

import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;

public class SettingsTabController implements MoleculeArchiveSubTab {
	
	private MoleculeArchive archive;
	
	private JFXToggleButton smileEncodingButton;
	
	private BorderPane borderPane;
	
	public SettingsTabController() {
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

	@Override
	public void setArchive(MoleculeArchive<?,?,?> archive) {
		this.archive = archive;
		
		smileEncodingButton.setSelected(archive.isSMILEOutputEncoding());
	}
	
	public Node getNode() {
		return this.borderPane;
	}
	
	public ArrayList<Menu> getMenus() {
		return new ArrayList<Menu>();
	}
}
