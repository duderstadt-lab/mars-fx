package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.event.Event;
import javafx.event.EventType;

public abstract class MoleculeArchiveEvent extends Event {

	/**
	 * , I extends MarsImageMetadata, P extends MoleculeArchiveProperties
	 */
	private static final long serialVersionUID = 1L;
	public static final EventType<MoleculeArchiveEvent> MOLECULE_ARCHIVE_EVENT = new EventType<>(ANY, "MOLECULE_ARCHIVE_EVENT");
	
	protected final MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive;

    public MoleculeArchiveEvent(EventType<? extends Event> eventType, MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive) {
        super(eventType);
        this.archive = archive;
    }

    public MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> getArchive() {
    	return archive;
    }
    
    public abstract void invokeHandler(MoleculeArchiveEventHandler handler);
}
