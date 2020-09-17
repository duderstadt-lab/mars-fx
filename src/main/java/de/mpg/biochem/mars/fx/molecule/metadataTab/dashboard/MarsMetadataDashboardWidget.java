package de.mpg.biochem.mars.fx.molecule.metadataTab.dashboard;

import de.mpg.biochem.mars.fx.dashboard.MarsDashboardWidget;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;

public interface MarsMetadataDashboardWidget extends MarsDashboardWidget {
	public void setArchive(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties> archive);
	public MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties> getArchive();
	public void setMetadata(MarsMetadata marsMetadata);
	public MarsMetadata getMetadata();
}
