package de.mpg.biochem.mars.fx.molecule;

import de.mpg.biochem.mars.fx.molecule.metadataTab.MetadataSubPane;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;

public interface MarsImageMetadataTab<M extends Molecule, I extends MarsImageMetadata, P extends MoleculeArchiveProperties, C extends MetadataSubPane<I>, O extends MetadataSubPane<I>> extends MoleculeArchiveTab<M,I,P> {

}
