package de.mpg.biochem.mars.fx.event;

import javafx.event.EventType;

public class MoleculeArchiveUnlockingEvent extends MoleculeArchiveEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeArchiveEvent> MOLECULE_ARCHIVE_UNLOCKING = new EventType<>(MOLECULE_ARCHIVE_EVENT, "MOLECULE_ARCHIVE_UNLOCKING");

    public MoleculeArchiveUnlockingEvent() {
        super(MOLECULE_ARCHIVE_UNLOCKING);
    }

    @Override
    public void invokeHandler(MoleculeArchiveEventHandler handler) {
        handler.onMoleculeArchiveUnlockingEvent();
    }

}