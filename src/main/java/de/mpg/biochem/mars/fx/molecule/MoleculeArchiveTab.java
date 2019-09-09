package de.mpg.biochem.mars.fx.molecule;

import java.util.ArrayList;

import de.mpg.biochem.mars.fx.event.MoleculeArchiveEventHandler;
import javafx.scene.Node;
import javafx.scene.control.Menu;

public interface MoleculeArchiveTab extends MoleculeArchiveEventHandler {
	public ArrayList<Menu> getMenus();
	public Node getNode();
}
