package de.mpg.biochem.mars.fx.molecule;

import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;

public interface MoleculesTab<M extends Molecule, I extends MarsImageMetadata, P extends MoleculeArchiveProperties, C extends MoleculeSubPane<M>, O extends MoleculeSubPane<M>> extends MoleculeArchiveTab<M,I,P> {

}