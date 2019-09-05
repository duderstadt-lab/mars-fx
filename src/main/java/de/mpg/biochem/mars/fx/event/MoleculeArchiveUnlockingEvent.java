package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.event.EventType;

public class MoleculeArchiveUnlockingEvent<A extends MoleculeArchive<?,?,?>> extends MoleculeArchiveEvent<A> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeArchiveUnlockingEvent<?>> MOLECULE_ARCHIVE_UNLOCKING = new EventType<>(MOLECULE_ARCHIVE_EVENT, "MoleculeArchiveUnlocking");

    private final A archive;

    public MoleculeArchiveUnlockingEvent(A archive) {
        super(MOLECULE_ARCHIVE_UNLOCKING);
        this.archive = archive;
    }

    @Override
    public void invokeHandler(MoleculeArchiveEventHandler<A> handler) {
        handler.onMoleculeArchiveUnlockingEvent(archive);
    }

}