package de.mpg.biochem.mars.fx.molecule.metadataTab;

import de.mpg.biochem.mars.fx.molecule.ViewableNode;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;

public interface MetadataSubPane<I extends MarsImageMetadata> extends ViewableNode {
	public void setArchive(MoleculeArchive<Molecule,MarsImageMetadata,MoleculeArchiveProperties> archive);
	public void setMetadata(I imageMetadata);
}
