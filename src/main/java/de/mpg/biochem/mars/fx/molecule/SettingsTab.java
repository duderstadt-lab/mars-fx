package de.mpg.biochem.mars.fx.molecule;

import java.util.ArrayList;

import com.jfoenix.controls.JFXToggleButton;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.layout.BorderPane;

public class SettingsTab extends AbstractMoleculeArchiveTab implements MoleculeArchiveTab {
	
	private MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive;
	
	private JFXToggleButton smileEncodingButton;
	
	private BorderPane borderPane;
	
	public SettingsTab() {
		super();
		setIcon(FontAwesomeIconFactory.get().createIcon(COG, "1.3em"));
		
		smileEncodingButton = new JFXToggleButton();
		borderPane = new BorderPane();
		borderPane.setCenter(smileEncodingButton);
		
		setContent(borderPane);
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
	public void setArchive(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive) {
		this.archive = archive;
		
		smileEncodingButton.setSelected(archive.isSMILEOutputEncoding());
	}

	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}

	@Override
	public void onMoleculeArchiveLockingEvent(MoleculeArchive<?, ?, ?> archive) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMoleculeArchiveLockedEvent(MoleculeArchive<?, ?, ?> archive) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMoleculeArchiveUnlockingEvent(MoleculeArchive<?, ?, ?> archive) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMoleculeArchiveUnlockedEvent(MoleculeArchive<?, ?, ?> archive) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMoleculeArchiveSavingEvent(MoleculeArchive<?, ?, ?> archive) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMoleculeArchiveSavedEvent(MoleculeArchive<?, ?, ?> archive) {
		// TODO Auto-generated method stub
		
	}
}
