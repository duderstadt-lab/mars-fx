package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.event.EventType;

public class MoleculeArchiveUnlockedEvent<A extends MoleculeArchive<?,?,?>> extends MoleculeArchiveEvent<A> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeArchiveUnlockedEvent<?>> MOLECULE_ARCHIVE_UNLOCKED = new EventType<>(MOLECULE_ARCHIVE_EVENT, "MoleculeArchiveUnlocked");

    private final A archive;

    public MoleculeArchiveUnlockedEvent(A archive) {
        super(MOLECULE_ARCHIVE_UNLOCKED);
        this.archive = archive;
    }

    @Override
    public void invokeHandler(MoleculeArchiveEventHandler<A> handler) {
        handler.onMoleculeArchiveUnlockedEvent(archive);
    }

}
