package de.mpg.biochem.mars.fx.molecule;

import de.mpg.biochem.mars.fx.molecule.metadataTab.MetadataSubPane;

public interface MarsImageMetadataTab<C extends MetadataSubPane, O extends MetadataSubPane> extends MoleculeArchiveTab {
	public void saveCurrentRecord();
}
