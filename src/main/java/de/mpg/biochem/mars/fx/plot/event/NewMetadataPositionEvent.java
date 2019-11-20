package de.mpg.biochem.mars.fx.plot.event;

import de.mpg.biochem.mars.util.PositionOfInterest;
import javafx.event.EventType;

public class NewMetadataPositionEvent extends PlotEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final PositionOfInterest poi;

	public static final EventType<PlotEvent> NEW_METADATA_POSITION = new EventType<>(PLOT_EVENT, "NEW_METADATA_POSITION");

    public NewMetadataPositionEvent(PositionOfInterest poi) {
        super(NEW_METADATA_POSITION);
        this.poi = poi;
    }
    
    public PositionOfInterest getPosition() {
    	return poi;
    }

    @Override
    public void invokeHandler(PlotEventHandler handler) {}
}
