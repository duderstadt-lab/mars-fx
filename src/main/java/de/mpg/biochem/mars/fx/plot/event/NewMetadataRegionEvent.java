package de.mpg.biochem.mars.fx.plot.event;

import de.mpg.biochem.mars.util.MarsRegion;
import javafx.event.EventType;

public class NewMetadataRegionEvent extends PlotEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final MarsRegion roi;

	public static final EventType<PlotEvent> NEW_METADATA_REGION = new EventType<>(PLOT_EVENT, "NEW_METADATA_REGION");

    public NewMetadataRegionEvent(MarsRegion roi) {
        super(NEW_METADATA_REGION);
        this.roi = roi;
    }
    
    public MarsRegion getRegion() {
    	return roi;
    }

    @Override
    public void invokeHandler(PlotEventHandler handler) {}
}
