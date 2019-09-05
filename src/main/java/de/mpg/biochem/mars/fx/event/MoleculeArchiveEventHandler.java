package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.event.EventHandler;

public abstract class MoleculeArchiveEventHandler<A extends MoleculeArchive<?, ?, ?>> implements EventHandler<MoleculeArchiveEvent<A>> {

    public abstract void onNewMoleculeArchiveEvent(A archive);
    
    public abstract void onMoleculeArchiveLockingEvent(A archive);
    public abstract void onMoleculeArchiveLockedEvent(A archive);
    
    public abstract void onMoleculeArchiveUnlockingEvent(A archive);
    public abstract void onMoleculeArchiveUnlockedEvent(A archive);
    
    public abstract void onMoleculeArchiveSavingEvent(A archive);
    public abstract void onMoleculeArchiveSavedEvent(A archive);

    @Override
    public void handle(MoleculeArchiveEvent<A> event) {
        event.invokeHandler(this);
    }
}
