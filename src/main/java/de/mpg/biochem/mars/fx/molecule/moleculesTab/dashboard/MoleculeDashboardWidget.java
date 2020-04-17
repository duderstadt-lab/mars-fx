package de.mpg.biochem.mars.fx.molecule.moleculesTab.dashboard;

import de.mpg.biochem.mars.fx.dashboard.MarsDashboardWidget;
import de.mpg.biochem.mars.molecule.Molecule;

public interface MoleculeDashboardWidget extends MarsDashboardWidget {
	public void setMolecule(Molecule molecule);
	public Molecule getMolecule();
}
