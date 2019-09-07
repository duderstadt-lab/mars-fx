package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.event.EventType;

public class MoleculeArchiveUnlockedEvent extends MoleculeArchiveEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeArchiveEvent> MOLECULE_ARCHIVE_UNLOCKED = new EventType<>(MOLECULE_ARCHIVE_EVENT, "MOLECULE_ARCHIVE_UNLOCKED");

    private final MoleculeArchive<?,?,?> archive;

    public MoleculeArchiveUnlockedEvent(MoleculeArchive<?,?,?> archive) {
        super(MOLECULE_ARCHIVE_UNLOCKED);
        this.archive = archive;
    }

    @Override
    public void invokeHandler(MoleculeArchiveEventHandler handler) {
        handler.onMoleculeArchiveUnlockedEvent(archive);
    }

}
