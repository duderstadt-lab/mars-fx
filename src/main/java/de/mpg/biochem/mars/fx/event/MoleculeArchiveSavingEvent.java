package de.mpg.biochem.mars.fx.event;

import javafx.event.EventType;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;

public class MoleculeArchiveSavingEvent extends MoleculeArchiveEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeArchiveEvent> MOLECULE_ARCHIVE_SAVING = new EventType<>(MOLECULE_ARCHIVE_EVENT, "MOLECULE_ARCHIVE_SAVING");

    public MoleculeArchiveSavingEvent(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive) {
        super(MOLECULE_ARCHIVE_SAVING, archive);
    }

    @Override
    public void invokeHandler(MoleculeArchiveEventHandler handler) {
        handler.onMoleculeArchiveSavingEvent();
    }

}

