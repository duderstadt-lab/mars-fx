package de.mpg.biochem.mars.fx.plot.event;

import de.mpg.biochem.mars.util.MarsRegion;
import javafx.event.EventType;

public class UpdateMetadataRegionEvent extends PlotEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final MarsRegion roi;

	public static final EventType<PlotEvent> UPDATE_METADATA_REGION = new EventType<>(PLOT_EVENT, "UPDATE_METADATA_REGION");

    public UpdateMetadataRegionEvent(MarsRegion roi) {
        super(UPDATE_METADATA_REGION);
        this.roi = roi;
    }
    
    public MarsRegion getRegion() {
    	return roi;
    }

    @Override
    public void invokeHandler(PlotEventHandler handler) {}
}
