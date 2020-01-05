package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import org.scijava.plugin.Plugin;

import de.jensd.fx.glyphs.GlyphIcons;
import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.scene.Node;
import javafx.scene.layout.Region;

import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.Cancelable;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;

@Plugin( type = HistogramWidget.class, name = "HistogramWidget" )
public class HistogramWidget extends AbstractDashboardWidget implements MarsDashboardWidget, SciJavaPlugin {

	public HistogramWidget(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive,
			DashboardTab parent) {
		super(archive, parent);
		// TODO Auto-generated constructor stub
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

	@Override
	public void run() {
		running.set(true);
	    rt.stop();
	    running.set(false);
	}
}
