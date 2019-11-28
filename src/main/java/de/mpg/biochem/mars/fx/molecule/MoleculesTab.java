package de.mpg.biochem.mars.fx.molecule;

import de.mpg.biochem.mars.fx.molecule.moleculesTab.MarsBdvFrame;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
import de.mpg.biochem.mars.molecule.Molecule;

public interface MoleculesTab<C extends MoleculeSubPane, O extends MoleculeSubPane> extends MoleculeArchiveTab {
	public void saveCurrentRecord();
	public Molecule getSelectedMolecule();
	public void setMarsBdvFrame(MarsBdvFrame<?> marsBdvFrame);
}