package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.Molecule;
import javafx.event.Event;
import javafx.event.EventType;

public abstract class MoleculeEvent extends Event {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final EventType<MoleculeEvent> MOLECULE_EVENT = new EventType<>(ANY, "MOLECULE_EVENT");

    public MoleculeEvent(EventType<? extends Event> eventType) {
        super(eventType);
    }

    public abstract void invokeHandler(MoleculeEventHandler handler);
}
