package de.mpg.biochem.mars.fx.molecule;

import de.mpg.biochem.mars.fx.molecule.metadataTab.MetadataSubPane;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;

public interface MarsImageMetadataTab<C extends MetadataSubPane<? extends MarsImageMetadata>, O extends MetadataSubPane<? extends MarsImageMetadata>> extends MoleculeArchiveTab {
	public void update();
	public void saveCurrentRecord();
}
