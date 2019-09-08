package de.mpg.biochem.mars.fx.molecule;

import java.util.ArrayList;

import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Menu;

public interface MoleculeArchiveTab extends EventHandler<MoleculeArchiveEvent> {
	public void setArchive(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive);
	public ArrayList<Menu> getMenus();
	public Node getNode();
	public void fireEvent(Event event);
	
	public void onMoleculeArchiveLockingEvent(MoleculeArchive<?,?,?> archive);
    public void onMoleculeArchiveLockedEvent(MoleculeArchive<?,?,?> archive);
    
    public void onMoleculeArchiveUnlockingEvent(MoleculeArchive<?,?,?> archive);
    public void onMoleculeArchiveUnlockedEvent(MoleculeArchive<?,?,?> archive);
    
    public void onMoleculeArchiveSavingEvent(MoleculeArchive<?,?,?> archive);
    public void onMoleculeArchiveSavedEvent(MoleculeArchive<?,?,?> archive);
}
