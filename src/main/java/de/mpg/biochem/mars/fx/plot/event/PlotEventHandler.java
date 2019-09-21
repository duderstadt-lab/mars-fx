package de.mpg.biochem.mars.fx.plot.event;

import javafx.event.Event;
import javafx.event.EventHandler;

public interface PlotEventHandler extends EventHandler<PlotEvent> {
	public void fireEvent(Event event);
	public void onUpdatePlotAreaEvent();
}
