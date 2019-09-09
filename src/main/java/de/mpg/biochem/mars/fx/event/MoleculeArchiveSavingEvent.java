package de.mpg.biochem.mars.fx.event;

import javafx.event.EventType;

public class MoleculeArchiveSavingEvent extends MoleculeArchiveEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeArchiveEvent> MOLECULE_ARCHIVE_SAVING = new EventType<>(MOLECULE_ARCHIVE_EVENT, "MOLECULE_ARCHIVE_SAVING");

    public MoleculeArchiveSavingEvent() {
        super(MOLECULE_ARCHIVE_SAVING);
    }

    @Override
    public void invokeHandler(MoleculeArchiveEventHandler handler) {
        handler.onMoleculeArchiveSavingEvent();
    }

}

