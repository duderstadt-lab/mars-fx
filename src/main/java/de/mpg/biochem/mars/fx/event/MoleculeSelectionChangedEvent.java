package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.Molecule;
import javafx.event.EventType;

public class MoleculeSelectionChangedEvent<M extends Molecule> extends MoleculeEvent<M> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeSelectionChangedEvent<?>> MOLECULE_SELECTION_CHANGED = new EventType<>(MOLECULE_EVENT, "MoleculeSelectionChanged");

    private final M molecule;

    public MoleculeSelectionChangedEvent(M molecule) {
        super(MOLECULE_SELECTION_CHANGED);
        this.molecule = molecule;
    }

    @Override
    public void invokeHandler(MoleculeEventHandler<M> handler) {
        handler.onMoleculeSelectionChangedEvent(molecule);
    }
}
