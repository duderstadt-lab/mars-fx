package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.event.Event;
import javafx.event.EventHandler;

public interface MoleculeArchiveEventHandler extends EventHandler<MoleculeArchiveEvent> {
	public void fireEvent(Event event);
	
	public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive);
	
    public void onMoleculeArchiveLockEvent();
    public void onMoleculeArchiveUnlockEvent();
    
    public void onMoleculeArchiveSavingEvent();
    public void onMoleculeArchiveSavedEvent();
}
