package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.event.EventType;

public class MoleculeArchiveSavingEvent extends MoleculeArchiveEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeArchiveSavingEvent> MOLECULE_ARCHIVE_SAVING = new EventType<>(MOLECULE_ARCHIVE_EVENT, "MoleculeArchiveSaving");

    private final MoleculeArchive<?,?,?> archive;

    public MoleculeArchiveSavingEvent(MoleculeArchive<?,?,?> archive) {
        super(MOLECULE_ARCHIVE_SAVING);
        this.archive = archive;
    }

    @Override
    public void invokeHandler(MoleculeArchiveEventHandler handler) {
        handler.onMoleculeArchiveSavingEvent(archive);
    }

}

