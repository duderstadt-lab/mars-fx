package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import de.jensd.fx.glyphs.GlyphIcons;
import static de.jensd.fx.glyphs.octicons.OctIcon.BEAKER;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.scene.Node;
import javafx.scene.layout.Region;

import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TAG;

import org.scijava.Cancelable;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;

import net.imagej.ops.Initializable;

@Plugin( type = DefaultWidget.class, name = "DefaultWidget" )
public class DefaultWidget extends AbstractDashboardWidget implements MarsDashboardWidget, SciJavaPlugin, Initializable {

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
		return (Node) OctIconFactory.get().createIcon(BEAKER, "1.2em");
	}

	@Override
	public void run() {
	}

	@Override
	public String getName() {
		return "DefaultWidget";
	}
}
