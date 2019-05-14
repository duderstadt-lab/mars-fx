package de.mpg.biochem.mars.gui.molecule.moleculesTab;

import de.mpg.biochem.mars.gui.molecule.MoleculeArchiveSubTab;
import de.mpg.biochem.mars.molecule.MoleculeArchive;

public class PropertiesTabController implements MoleculeArchiveSubTab {
	
	private MoleculeArchive archive;
	
	public PropertiesTabController() {
		
	}
	
	@Override
	public void setArchive(MoleculeArchive archive) {
		this.archive = archive;
	}
}
