package de.mpg.biochem.mars.fx.molecule;

import de.mpg.biochem.mars.fx.molecule.metadataTab.MetadataSubPane;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;

public interface MarsImageMetadataTab<C extends MetadataSubPane, O extends MetadataSubPane> extends MoleculeArchiveTab {
	public void saveCurrentRecord();
	public MarsImageMetadata getSelectedMetadata();
}
