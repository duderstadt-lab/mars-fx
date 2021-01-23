package de.mpg.biochem.mars.fx.bdv;

import bdv.util.BdvOverlay;
import de.mpg.biochem.mars.molecule.Molecule;

public interface MarsBdvCard {
	String getCardName();
	void setMolecule(Molecule molecule);
	BdvOverlay getBdvOverlay();
}
