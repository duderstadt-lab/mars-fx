package de.mpg.biochem.mars.fx.plot.event;

import javafx.event.Event;
import javafx.event.EventType;

public abstract class PlotEvent extends Event {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final EventType<PlotEvent> PLOT_EVENT = new EventType<>(ANY, "PLOT_EVENT");

    public PlotEvent(EventType<? extends Event> eventType) {
        super(eventType);
    }

    public abstract void invokeHandler(PlotEventHandler handler);
}
