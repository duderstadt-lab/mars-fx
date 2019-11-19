package de.mpg.biochem.mars.fx.event;

import javafx.event.EventType;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;

public class MoleculeArchiveLockingEvent extends MoleculeArchiveEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeArchiveEvent> MOLECULE_ARCHIVE_LOCKING = new EventType<>(MOLECULE_ARCHIVE_EVENT, "MOLECULE_ARCHIVE_LOCKING");

    public MoleculeArchiveLockingEvent(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive) {
        super(MOLECULE_ARCHIVE_LOCKING, archive);
    }

    @Override
    public void invokeHandler(MoleculeArchiveEventHandler handler) {
        handler.onMoleculeArchiveLockingEvent();
    }

}
