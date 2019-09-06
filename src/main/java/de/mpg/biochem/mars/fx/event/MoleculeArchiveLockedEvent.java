package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.event.EventType;

public class MoleculeArchiveLockedEvent extends MoleculeArchiveEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeArchiveLockedEvent> MOLECULE_ARCHIVE_LOCKED = new EventType<>(MOLECULE_ARCHIVE_EVENT, "MoleculeArchiveLocked");

    private final MoleculeArchive<?,?,?> archive;

    public MoleculeArchiveLockedEvent(MoleculeArchive<?,?,?> archive) {
        super(MOLECULE_ARCHIVE_LOCKED);
        this.archive = archive;
    }

    @Override
    public void invokeHandler(MoleculeArchiveEventHandler handler) {
        handler.onMoleculeArchiveLockedEvent(archive);
    }

}
