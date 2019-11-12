package de.mpg.biochem.mars.fx.plot;

import javafx.event.Event;
import javafx.scene.Node;

public interface PlotPane {
	public Node getNode();
	public void fireEvent(Event event);
}
