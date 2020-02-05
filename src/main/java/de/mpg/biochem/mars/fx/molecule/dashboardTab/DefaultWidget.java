package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import static de.jensd.fx.glyphs.octicons.OctIcon.BEAKER;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import javafx.scene.Node;

import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import net.imagej.ops.Initializable;

@Plugin( type = DefaultWidget.class, name = "DefaultWidget" )
public class DefaultWidget extends AbstractDashboardWidget implements MarsDashboardWidget, SciJavaPlugin, Initializable {

	@Override
	public void initialize() {
		super.initialize();
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
