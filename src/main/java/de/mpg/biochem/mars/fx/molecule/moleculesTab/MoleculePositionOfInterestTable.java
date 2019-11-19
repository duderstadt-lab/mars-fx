package de.mpg.biochem.mars.fx.molecule.moleculesTab;

import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.molecule.AbstractPositionOfInterestTable;
import de.mpg.biochem.mars.fx.event.MoleculeIndicatorChangedEvent;
import de.mpg.biochem.mars.molecule.Molecule;
import javafx.event.Event;

public class MoleculePositionOfInterestTable extends AbstractPositionOfInterestTable implements MoleculeSubPane {

    public MoleculePositionOfInterestTable() {        
        super();
    }
    
    @Override
    public void handle(MoleculeEvent event) {
        event.invokeHandler(this);
        event.consume();
    }

	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}

	@Override
	public void onMoleculeSelectionChangedEvent(Molecule molecule) {
		this.record = molecule;
    	loadData();
	}

	@Override
	protected void fireIndicatorChangedEvent() {
		getNode().fireEvent(new MoleculeIndicatorChangedEvent((Molecule) record));
	}

	@Override
	protected void addEventHandlers() {
		getNode().addEventHandler(MoleculeEvent.MOLECULE_EVENT, this);
	}
}
