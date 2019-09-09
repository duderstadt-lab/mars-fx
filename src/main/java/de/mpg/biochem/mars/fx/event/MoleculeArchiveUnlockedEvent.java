package de.mpg.biochem.mars.fx.event;

import javafx.event.EventType;

public class MoleculeArchiveUnlockedEvent extends MoleculeArchiveEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeArchiveEvent> MOLECULE_ARCHIVE_UNLOCKED = new EventType<>(MOLECULE_ARCHIVE_EVENT, "MOLECULE_ARCHIVE_UNLOCKED");

    public MoleculeArchiveUnlockedEvent() {
        super(MOLECULE_ARCHIVE_UNLOCKED);
    }

    @Override
    public void invokeHandler(MoleculeArchiveEventHandler handler) {
        handler.onMoleculeArchiveUnlockedEvent();
    }

}
