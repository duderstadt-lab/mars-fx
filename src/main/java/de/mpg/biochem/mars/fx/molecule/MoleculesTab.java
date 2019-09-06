package de.mpg.biochem.mars.fx.molecule;

import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
import de.mpg.biochem.mars.molecule.Molecule;

public interface MoleculesTab<C extends MoleculeSubPane<? extends Molecule>, O extends MoleculeSubPane<? extends Molecule>> extends MoleculeArchiveTab {
	public void update();
	public void saveCurrentRecord();
}