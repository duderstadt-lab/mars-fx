package de.mpg.biochem.mars.fx.plot;

import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.event.MoleculeSelectionChangedEvent;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
import de.mpg.biochem.mars.molecule.Molecule;
import javafx.scene.Node;

public abstract class AbstractMoleculePlotPane<M extends Molecule, S extends SubPlot> extends AbstractPlotPane implements MoleculeSubPane {
	
	protected M molecule;
	
	public AbstractMoleculePlotPane() {
		super();
		
		addChart();
		
		getNode().addEventHandler(MoleculeEvent.MOLECULE_EVENT, this);
	}
	
	@Override
	public Node getNode() {
		return this;
	}

	@Override
	public void handle(MoleculeEvent event) {
		event.invokeHandler(this);
		event.consume();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onMoleculeSelectionChangedEvent(Molecule molecule) {
		this.molecule = (M) molecule;
		for (SubPlot subPlot : charts) 
			subPlot.fireEvent(new MoleculeSelectionChangedEvent(molecule));
	}

	@Override
	public void addChart() {
		SubPlot subplot = createSubPlot();
		if (molecule != null)
			subplot.fireEvent(new MoleculeSelectionChangedEvent(molecule));
		addChart(subplot);
	}
	
	public abstract S createSubPlot();
}
