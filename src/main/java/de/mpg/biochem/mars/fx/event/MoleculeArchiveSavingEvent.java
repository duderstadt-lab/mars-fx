package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.event.EventType;

public class MoleculeArchiveSavingEvent<A extends MoleculeArchive<?,?,?>> extends MoleculeArchiveEvent<A> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeArchiveSavingEvent<?>> MOLECULE_ARCHIVE_SAVING = new EventType<>(MOLECULE_ARCHIVE_EVENT, "MoleculeArchiveSaving");

    private final A archive;

    public MoleculeArchiveSavingEvent(A archive) {
        super(MOLECULE_ARCHIVE_SAVING);
        this.archive = archive;
    }

    @Override
    public void invokeHandler(MoleculeArchiveEventHandler<A> handler) {
        handler.onMoleculeArchiveSavingEvent(archive);
    }

}

