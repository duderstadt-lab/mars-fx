package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import de.jensd.fx.glyphs.GlyphIcons;
import static de.jensd.fx.glyphs.octicons.OctIcon.BEAKER;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.fx.util.MarsAnimation;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import net.imagej.ops.Initializable;

@Plugin( type = DefaultWidget.class, name = "DefaultWidget" )
public class DefaultWidget extends AbstractDashboardWidget implements MarsDashboardWidget, SciJavaPlugin, Initializable {

	@Override
	public void initialize() {
		super.initialize();
		
		//MarsAnimation marsAnimation = new MarsAnimation();
		
     //   setContent(getIcon(), marsAnimation);
        
       // rootPane.setMinSize(250, 250);
        //rootPane.setMaxSize(250, 250);
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
