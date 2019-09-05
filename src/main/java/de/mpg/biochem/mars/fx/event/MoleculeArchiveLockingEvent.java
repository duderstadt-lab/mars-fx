package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.event.EventType;

public class MoleculeArchiveLockingEvent<A extends MoleculeArchive<?,?,?>> extends MoleculeArchiveEvent<A> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeArchiveLockingEvent<?>> MOLECULE_ARCHIVE_LOCKING = new EventType<>(MOLECULE_ARCHIVE_EVENT, "MoleculeArchiveLocking");

    private final A archive;

    public MoleculeArchiveLockingEvent(A archive) {
        super(MOLECULE_ARCHIVE_LOCKING);
        this.archive = archive;
    }

    @Override
    public void invokeHandler(MoleculeArchiveEventHandler<A> handler) {
        handler.onMoleculeArchiveLockingEvent(archive);
    }

}
