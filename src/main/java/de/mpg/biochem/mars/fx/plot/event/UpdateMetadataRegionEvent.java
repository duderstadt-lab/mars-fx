package de.mpg.biochem.mars.fx.plot.event;

import de.mpg.biochem.mars.util.RegionOfInterest;
import javafx.event.EventType;

public class UpdateMetadataRegionEvent extends PlotEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final RegionOfInterest roi;

	public static final EventType<PlotEvent> UPDATE_METADATA_REGION = new EventType<>(PLOT_EVENT, "UPDATE_METADATA_REGION");

    public UpdateMetadataRegionEvent(RegionOfInterest roi) {
        super(UPDATE_METADATA_REGION);
        this.roi = roi;
    }
    
    public RegionOfInterest getRegion() {
    	return roi;
    }

    @Override
    public void invokeHandler(PlotEventHandler handler) {}
}
