package de.mpg.biochem.mars.fx.molecule;

import de.mpg.biochem.mars.fx.molecule.metadataTab.DefaultMetadataCenterPane;
import de.mpg.biochem.mars.fx.molecule.metadataTab.DefaultMetadataPropertiesPane;
import de.mpg.biochem.mars.fx.molecule.metadataTab.MetadataSubPane;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;

public class DefaultMarsImageMetadataTab extends AbstractMarsImageMetadataTab<Molecule, MarsImageMetadata, MoleculeArchiveProperties, DefaultMetadataCenterPane, DefaultMetadataPropertiesPane> {
	
	public DefaultMarsImageMetadataTab() {
		super();
	}

	@Override
	public DefaultMetadataCenterPane createMetadataCenterPane() {
		return new DefaultMetadataCenterPane();
	}

	@Override
	public DefaultMetadataPropertiesPane createMetadataPropertiesPane() {
		return new DefaultMetadataPropertiesPane();
	}
}
