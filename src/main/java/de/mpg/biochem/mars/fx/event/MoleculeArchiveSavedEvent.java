package de.mpg.biochem.mars.fx.event;

import javafx.event.EventType;

public class MoleculeArchiveSavedEvent extends MoleculeArchiveEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeArchiveEvent> MOLECULE_ARCHIVE_SAVED = new EventType<>(MOLECULE_ARCHIVE_EVENT, "MOLECULE_ARCHIVE_SAVED");

    public MoleculeArchiveSavedEvent() {
        super(MOLECULE_ARCHIVE_SAVED);
    }

    @Override
    public void invokeHandler(MoleculeArchiveEventHandler handler) {
        handler.onMoleculeArchiveSavedEvent();
    }

}
