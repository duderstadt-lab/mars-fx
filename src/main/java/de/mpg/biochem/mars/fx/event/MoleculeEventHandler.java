package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.Molecule;
import javafx.event.Event;
import javafx.event.EventHandler;

public interface MoleculeEventHandler extends EventHandler<MoleculeEvent> {
	public void fireEvent(Event event);
	public void onMoleculeSelectionChangedEvent(Molecule molecule);
}
