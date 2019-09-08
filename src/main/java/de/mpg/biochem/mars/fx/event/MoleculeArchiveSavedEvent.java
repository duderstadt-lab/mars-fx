package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.fx.molecule.MoleculeArchiveTab;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.event.EventType;

public class MoleculeArchiveSavedEvent extends MoleculeArchiveEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeArchiveEvent> MOLECULE_ARCHIVE_SAVED = new EventType<>(MOLECULE_ARCHIVE_EVENT, "MOLECULE_ARCHIVE_SAVED");

    private final MoleculeArchive<?,?,?> archive;

    public MoleculeArchiveSavedEvent(MoleculeArchive<?,?,?> archive) {
        super(MOLECULE_ARCHIVE_SAVED);
        this.archive = archive;
    }

    @Override
    public void invokeHandler(MoleculeArchiveTab handler) {
        handler.onMoleculeArchiveSavedEvent(archive);
    }

}
