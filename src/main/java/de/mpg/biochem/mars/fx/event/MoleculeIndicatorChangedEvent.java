package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.Molecule;
import javafx.event.EventType;

public class MoleculeIndicatorChangedEvent extends MoleculeEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeEvent> INDICATOR_CHANGED = new EventType<>(MOLECULE_EVENT, "INDICATOR_CHANGED");

    private final Molecule molecule;

    public MoleculeIndicatorChangedEvent(Molecule molecule) {
        super(INDICATOR_CHANGED);
        this.molecule = molecule;
    }

    @Override
    public void invokeHandler(MoleculeEventHandler handler) {
        //handler.onMoleculeSelectionChangedEvent(molecule);
    }
}
