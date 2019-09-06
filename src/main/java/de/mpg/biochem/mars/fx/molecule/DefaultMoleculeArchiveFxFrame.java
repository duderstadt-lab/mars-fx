package de.mpg.biochem.mars.fx.molecule;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.molecule.MoleculeArchiveService;

public class DefaultMoleculeArchiveFxFrame extends AbstractMoleculeArchiveFxFrame<DefaultMarsImageMetadataTab, DefaultMoleculesTab> {
	public DefaultMoleculeArchiveFxFrame(MoleculeArchive<Molecule,MarsImageMetadata,MoleculeArchiveProperties> archive, MoleculeArchiveService moleculeArchiveService) {
		super(archive, moleculeArchiveService);
	}

	@Override
	public DefaultMarsImageMetadataTab createImageMetadataTab() {
		return new DefaultMarsImageMetadataTab();
	}

	@Override
	public DefaultMoleculesTab createMoleculesTab() {
		return new DefaultMoleculesTab();
	}
}