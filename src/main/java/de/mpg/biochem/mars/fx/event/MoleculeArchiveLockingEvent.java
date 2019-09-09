package de.mpg.biochem.mars.fx.event;

import javafx.event.EventType;

public class MoleculeArchiveLockingEvent extends MoleculeArchiveEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeArchiveEvent> MOLECULE_ARCHIVE_LOCKING = new EventType<>(MOLECULE_ARCHIVE_EVENT, "MOLECULE_ARCHIVE_LOCKING");

    public MoleculeArchiveLockingEvent() {
        super(MOLECULE_ARCHIVE_LOCKING);
    }

    @Override
    public void invokeHandler(MoleculeArchiveEventHandler handler) {
        handler.onMoleculeArchiveLockingEvent();
    }

}
