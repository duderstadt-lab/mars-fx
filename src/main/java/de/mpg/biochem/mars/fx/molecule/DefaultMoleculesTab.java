package de.mpg.biochem.mars.fx.molecule;

import de.mpg.biochem.mars.fx.molecule.moleculesTab.DefaultMoleculeCenterPane;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.DefaultMoleculePropertiesPane;
import de.mpg.biochem.mars.molecule.Molecule;

public class DefaultMoleculesTab extends AbstractMoleculesTab<Molecule, DefaultMoleculeCenterPane, DefaultMoleculePropertiesPane> {
	public DefaultMoleculesTab() {
		super();
	}

	@Override
	public DefaultMoleculeCenterPane createMoleculeCenterPane() {
		return new DefaultMoleculeCenterPane();
	}

	@Override
	public DefaultMoleculePropertiesPane createMoleculePropertiesPane() {
		return new DefaultMoleculePropertiesPane();
	}
}