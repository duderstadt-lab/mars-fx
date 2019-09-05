package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.event.EventType;

public class MoleculeArchiveSavedEvent<A extends MoleculeArchive<?,?,?>> extends MoleculeArchiveEvent<A> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeArchiveSavedEvent<?>> MOLECULE_ARCHIVE_SAVED = new EventType<>(MOLECULE_ARCHIVE_EVENT, "MoleculeArchiveSaved");

    private final A archive;

    public MoleculeArchiveSavedEvent(A archive) {
        super(MOLECULE_ARCHIVE_SAVED);
        this.archive = archive;
    }

    @Override
    public void invokeHandler(MoleculeArchiveEventHandler<A> handler) {
        handler.onMoleculeArchiveSavedEvent(archive);
    }

}
