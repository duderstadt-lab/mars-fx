package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.event.EventType;

public class InitializeMoleculeArchiveEvent extends MoleculeArchiveEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive;

	public static final EventType<MoleculeArchiveEvent> INITIALIZE_MOLECULE_ARCHIVE = new EventType<>(MOLECULE_ARCHIVE_EVENT, "INITIALIZE_MOLECULE_ARCHIVE");

    public InitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive) {
        super(INITIALIZE_MOLECULE_ARCHIVE);
        this.archive = archive;
    }

    @Override
    public void invokeHandler(MoleculeArchiveEventHandler handler) {
        handler.onInitializeMoleculeArchiveEvent(archive);
    }
}