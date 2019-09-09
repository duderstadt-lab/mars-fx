package de.mpg.biochem.mars.fx.molecule.moleculesTab;

import de.mpg.biochem.mars.fx.event.MoleculeEventHandler;
import javafx.scene.Node;

public interface MoleculeSubPane extends MoleculeEventHandler {
	public Node getNode();
}

