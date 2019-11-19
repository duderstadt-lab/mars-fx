package de.mpg.biochem.mars.fx.molecule.metadataTab;

import de.mpg.biochem.mars.fx.event.MetadataEventHandler;
import javafx.scene.Node;

public interface MetadataSubPane extends MetadataEventHandler {
	public Node getNode();
}
