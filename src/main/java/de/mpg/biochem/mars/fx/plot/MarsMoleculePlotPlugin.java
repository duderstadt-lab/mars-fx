package de.mpg.biochem.mars.fx.plot;

import de.mpg.biochem.mars.molecule.Molecule;

public interface MarsMoleculePlotPlugin extends MarsPlotPlugin {
	public void setMolecule(Molecule molecule);
}
