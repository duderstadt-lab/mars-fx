package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.Molecule;
import javafx.event.EventHandler;

public abstract class MoleculeEventHandler implements EventHandler<MoleculeEvent> {

    public abstract void onMoleculeSelectionChangedEvent(Molecule molecule);

    @Override
    public void handle(MoleculeEvent event) {
        event.invokeHandler(this);
    }
}
