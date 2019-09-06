package de.mpg.biochem.mars.fx.molecule;

import java.util.ArrayList;

import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.layout.BorderPane;

public class DashboardTab extends AbstractMoleculeArchiveTab {
    protected BorderPane borderPane;
	
    public DashboardTab() {
    	super();
    	setIcon(MaterialIconFactory.get().createIcon(de.jensd.fx.glyphs.materialicons.MaterialIcon.DASHBOARD, "1.3em"));
    	
    	borderPane = new BorderPane();
    	borderPane.setCenter(new Label("Coming soon..."));
    	
    	setContent(borderPane);
    }
    
	public ArrayList<Menu> getMenus() {
		return new ArrayList<Menu>();
	}
}
