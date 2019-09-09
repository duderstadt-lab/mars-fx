package de.mpg.biochem.mars.fx.event;

import javafx.event.EventType;

public class MoleculeArchiveLockedEvent extends MoleculeArchiveEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeArchiveEvent> MOLECULE_ARCHIVE_LOCKED = new EventType<>(MOLECULE_ARCHIVE_EVENT, "MOLECULE_ARCHIVE_LOCKED");

    public MoleculeArchiveLockedEvent() {
        super(MOLECULE_ARCHIVE_LOCKED);
    }

    @Override
    public void invokeHandler(MoleculeArchiveEventHandler handler) {
        handler.onMoleculeArchiveLockedEvent();
    }

}
