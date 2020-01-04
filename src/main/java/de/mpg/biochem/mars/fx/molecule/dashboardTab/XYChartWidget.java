package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.BAR_CHART;

import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.scene.Node;

public class XYChartWidget extends AbstractDashboardWidget {

	public XYChartWidget(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive,
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
		return null;
	}
}
