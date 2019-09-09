package de.mpg.biochem.mars.fx.molecule.metadataTab;

import de.mpg.biochem.mars.fx.event.MarsImageMetadataEventHandler;
import javafx.scene.Node;

public interface MetadataSubPane extends MarsImageMetadataEventHandler {
	public Node getNode();
}
