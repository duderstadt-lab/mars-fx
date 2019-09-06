package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.event.EventHandler;

public abstract class MoleculeArchiveEventHandler implements EventHandler<MoleculeArchiveEvent> {

    public abstract void onNewMoleculeArchiveEvent(MoleculeArchive<?,?,?> archive);
    
    public abstract void onMoleculeArchiveLockingEvent(MoleculeArchive<?,?,?> archive);
    public abstract void onMoleculeArchiveLockedEvent(MoleculeArchive<?,?,?> archive);
    
    public abstract void onMoleculeArchiveUnlockingEvent(MoleculeArchive<?,?,?> archive);
    public abstract void onMoleculeArchiveUnlockedEvent(MoleculeArchive<?,?,?> archive);
    
    public abstract void onMoleculeArchiveSavingEvent(MoleculeArchive<?,?,?> archive);
    public abstract void onMoleculeArchiveSavedEvent(MoleculeArchive<?,?,?> archive);

    @Override
    public void handle(MoleculeArchiveEvent event) {
        event.invokeHandler(this);
    }
}
