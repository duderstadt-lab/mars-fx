package de.mpg.biochem.mars.fx.plot;

import de.mpg.biochem.mars.fx.util.StyleSheetUpdater;
import javafx.event.Event;
import javafx.scene.Node;

public interface PlotPane {
	public StyleSheetUpdater getStyleSheetUpdater();
	public Node getNode();
	public void fireEvent(Event event);
}
