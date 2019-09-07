package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.event.EventType;

public class MoleculeArchiveLockingEvent extends MoleculeArchiveEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeArchiveEvent> MOLECULE_ARCHIVE_LOCKING = new EventType<>(MOLECULE_ARCHIVE_EVENT, "MOLECULE_ARCHIVE_LOCKING");

    private final MoleculeArchive<?,?,?> archive;

    public MoleculeArchiveLockingEvent(MoleculeArchive<?,?,?> archive) {
        super(MOLECULE_ARCHIVE_LOCKING);
        this.archive = archive;
    }

    @Override
    public void invokeHandler(MoleculeArchiveEventHandler handler) {
        handler.onMoleculeArchiveLockingEvent(archive);
    }

}
