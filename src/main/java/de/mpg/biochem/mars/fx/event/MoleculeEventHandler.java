package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.Molecule;
import javafx.event.EventHandler;

public abstract class MoleculeEventHandler<M extends Molecule> implements EventHandler<MoleculeEvent<M>> {

    public abstract void onMoleculeSelectionChangedEvent(M molecule);

    @Override
    public void handle(MoleculeEvent<M> event) {
        event.invokeHandler(this);
    }
}
