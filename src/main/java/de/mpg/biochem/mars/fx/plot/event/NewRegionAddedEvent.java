package de.mpg.biochem.mars.fx.plot.event;

import javafx.event.EventType;

public class NewRegionAddedEvent extends PlotEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<PlotEvent> NEW_REGION_ADDED = new EventType<>(PLOT_EVENT, "NEW_REGION_ADDED");

    public NewRegionAddedEvent() {
        super(NEW_REGION_ADDED);
    }

    @Override
    public void invokeHandler(PlotEventHandler handler) {
       //handler.onNewRegionAddedEvent();
    }
}
