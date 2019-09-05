package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.event.Event;
import javafx.event.EventType;

public abstract class MoleculeArchiveEvent<A extends MoleculeArchive<?, ?, ?>> extends Event {

	/**
	 * , I extends MarsImageMetadata, P extends MoleculeArchiveProperties
	 */
	private static final long serialVersionUID = 1L;
	public static final EventType<MoleculeArchiveEvent<?>> MOLECULE_ARCHIVE_EVENT = new EventType<>(ANY);

    public MoleculeArchiveEvent(EventType<? extends Event> eventType) {
        super(eventType);
    }

    public abstract void invokeHandler(MoleculeArchiveEventHandler<A> handler);

}
