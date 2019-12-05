package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.Molecule;
import javafx.event.EventType;

public class MoleculeTagsChangedEvent extends MoleculeEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeEvent> TAGS_CHANGED = new EventType<>(MOLECULE_EVENT, "TAGS_CHANGED");

    private final Molecule molecule;

    public MoleculeTagsChangedEvent(Molecule molecule) {
        super(TAGS_CHANGED);
        this.molecule = molecule;
    }

    @Override
    public void invokeHandler(MoleculeEventHandler handler) {}
}

