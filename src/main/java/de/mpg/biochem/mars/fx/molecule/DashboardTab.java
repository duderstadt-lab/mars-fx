package de.mpg.biochem.mars.fx.molecule;

import java.util.ArrayList;

import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.layout.BorderPane;

public class DashboardTab extends AbstractMoleculeArchiveTab {
    protected BorderPane borderPane;
	
    public DashboardTab() {
    	super();
    	setIcon(MaterialIconFactory.get().createIcon(de.jensd.fx.glyphs.materialicons.MaterialIcon.DASHBOARD, "1.3em"));
    	
    	borderPane = new BorderPane();
    	borderPane.setCenter(new Label("Coming soon..."));
    	
    	setContent(borderPane);
    }
    
    public Node getNode() {
		return borderPane;
	}
    
	public ArrayList<Menu> getMenus() {
		return new ArrayList<Menu>();
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
