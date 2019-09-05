package de.mpg.biochem.mars.fx.molecule.moleculesTab;

import de.mpg.biochem.mars.fx.molecule.ViewableNode;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;

public interface MoleculeSubPane<M extends Molecule> extends ViewableNode {
	public void setArchive(MoleculeArchive<M,MarsImageMetadata,MoleculeArchiveProperties> archive);
	public void setMolecule(M molecule);
}

