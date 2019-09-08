package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.fx.molecule.MoleculeArchiveTab;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.event.EventType;

public class MoleculeArchiveUnlockingEvent extends MoleculeArchiveEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeArchiveEvent> MOLECULE_ARCHIVE_UNLOCKING = new EventType<>(MOLECULE_ARCHIVE_EVENT, "MOLECULE_ARCHIVE_UNLOCKING");

    private final MoleculeArchive<?,?,?> archive;

    public MoleculeArchiveUnlockingEvent(MoleculeArchive<?,?,?> archive) {
        super(MOLECULE_ARCHIVE_UNLOCKING);
        this.archive = archive;
    }

    @Override
    public void invokeHandler(MoleculeArchiveTab handler) {
        handler.onMoleculeArchiveUnlockingEvent(archive);
    }

}