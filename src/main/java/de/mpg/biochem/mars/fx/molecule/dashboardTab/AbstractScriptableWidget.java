package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;

public abstract class AbstractScriptableWidget extends AbstractDashboardWidget {
	
	
	public AbstractScriptableWidget(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive, DashboardTab parent) {
		super(archive, parent);
		
		
	}
}
