package de.mpg.biochem.mars.fx.molecule.metadataTab.dashboard;

import de.mpg.biochem.mars.fx.dashboard.MarsDashboardWidget;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;

public interface MarsMetadataDashboardWidget extends MarsDashboardWidget {
	public void setMetadata(MarsMetadata marsMetadata);
	public MarsMetadata getMetadata();
}
