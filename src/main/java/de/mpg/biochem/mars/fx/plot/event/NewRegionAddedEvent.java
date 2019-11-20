package de.mpg.biochem.mars.fx.plot.event;

import de.mpg.biochem.mars.util.RegionOfInterest;
import javafx.event.EventType;

public class NewRegionAddedEvent extends PlotEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final RegionOfInterest roi;

	public static final EventType<PlotEvent> NEW_REGION_ADDED = new EventType<>(PLOT_EVENT, "NEW_REGION_ADDED");

    public NewRegionAddedEvent(RegionOfInterest roi) {
        super(NEW_REGION_ADDED);
        this.roi = roi;
    }
    
    public RegionOfInterest getRegion() {
    	return roi;
    }

    @Override
    public void invokeHandler(PlotEventHandler handler) {
       //handler.onNewRegionAddedEvent();
    }
}
