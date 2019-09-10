package de.mpg.biochem.mars.fx.molecule.moleculesTab;

import de.mpg.biochem.mars.fx.plot.DefaultMoleculePlotPane;
import de.mpg.biochem.mars.molecule.Molecule;

public class DefaultMoleculeCenterPane extends AbstractMoleculeCenterPane<Molecule, DefaultMoleculePlotPane> {
	public DefaultMoleculeCenterPane() {
		super();
	}

	@Override
	public DefaultMoleculePlotPane createPlotPane() {
		return new DefaultMoleculePlotPane();
	}
}
