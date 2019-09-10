package de.mpg.biochem.mars.fx.plot;

import de.mpg.biochem.mars.molecule.Molecule;

public class DefaultMoleculePlotPane extends AbstractMoleculePlotPane<Molecule, DefaultMoleculeSubPlot> {
	
	public DefaultMoleculePlotPane() {
		super();
	}

	@Override
	public DefaultMoleculeSubPlot createSubPlot() {
		return new DefaultMoleculeSubPlot(this, "Plot " + (charts.size() + 1));
	}
}
