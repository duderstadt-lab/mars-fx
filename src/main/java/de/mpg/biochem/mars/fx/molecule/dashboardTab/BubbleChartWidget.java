package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
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

import net.imagej.ops.Initializable;

@Plugin( type = BubbleChartWidget.class, name = "BubbleChartWidget" )
public class BubbleChartWidget extends AbstractDashboardWidget implements MarsDashboardWidget, SciJavaPlugin, Initializable {

	@Override
	public void initialize() {
		super.initialize();
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void createIOMaps() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Node getIcon() {
		Region xychartIcon = new Region();
		xychartIcon.getStyleClass().add("bubblechartIcon");
		return xychartIcon;
	}

	@Override
	public void run() {
	}
}
