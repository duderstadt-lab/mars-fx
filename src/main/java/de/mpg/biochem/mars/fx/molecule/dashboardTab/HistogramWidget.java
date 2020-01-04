package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import de.jensd.fx.glyphs.GlyphIcons;
import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.scene.Node;
import javafx.scene.layout.Region;

public class HistogramWidget extends AbstractDashboardWidget {

	public HistogramWidget(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive,
			DashboardTab parent) {
		super(archive, parent);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean build() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void createIOMaps() {
		// TODO Auto-generated method stub
		
	}

	public static Node getIcon() {
		Region barchartIcon = new Region();
		barchartIcon.getStyleClass().add("barchartIcon");
		return barchartIcon;
	}
}
