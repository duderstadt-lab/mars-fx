package de.mpg.biochem.mars.fx.event;

import javafx.event.EventType;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;

public class MoleculeArchiveSavedEvent extends MoleculeArchiveEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeArchiveEvent> MOLECULE_ARCHIVE_SAVED = new EventType<>(MOLECULE_ARCHIVE_EVENT, "MOLECULE_ARCHIVE_SAVED");

    public MoleculeArchiveSavedEvent(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive) {
        super(MOLECULE_ARCHIVE_SAVED, archive);
    }

    @Override
    public void invokeHandler(MoleculeArchiveEventHandler handler) {
        handler.onMoleculeArchiveSavedEvent();
    }

}
