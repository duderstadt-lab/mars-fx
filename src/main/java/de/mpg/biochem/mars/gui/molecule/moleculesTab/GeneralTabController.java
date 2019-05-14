package de.mpg.biochem.mars.gui.molecule.moleculesTab;

import java.util.ArrayList;

import de.mpg.biochem.mars.gui.molecule.MoleculeArchiveSubTab;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;

public class GeneralTabController implements MoleculeSubTab {
	
	private Molecule molecule;
	
	public GeneralTabController() {
		
	}

	@Override
	public void setMolecule(Molecule molecule) {
		this.molecule = molecule;
	}
}
