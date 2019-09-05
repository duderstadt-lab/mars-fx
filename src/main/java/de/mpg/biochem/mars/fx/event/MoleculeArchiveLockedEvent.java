package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.event.EventType;

public class MoleculeArchiveLockedEvent<A extends MoleculeArchive<?,?,?>> extends MoleculeArchiveEvent<A> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeArchiveLockedEvent<?>> MOLECULE_ARCHIVE_LOCKED = new EventType<>(MOLECULE_ARCHIVE_EVENT, "MoleculeArchiveLocked");

    private final A archive;

    public MoleculeArchiveLockedEvent(A archive) {
        super(MOLECULE_ARCHIVE_LOCKED);
        this.archive = archive;
    }

    @Override
    public void invokeHandler(MoleculeArchiveEventHandler<A> handler) {
        handler.onMoleculeArchiveLockedEvent(archive);
    }

}
