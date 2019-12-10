package de.mpg.biochem.mars.fx.plot.event;

import de.mpg.biochem.mars.util.MarsPosition;
import javafx.event.EventType;

public class NewMetadataPositionEvent extends PlotEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final MarsPosition poi;

	public static final EventType<PlotEvent> NEW_METADATA_POSITION = new EventType<>(PLOT_EVENT, "NEW_METADATA_POSITION");

    public NewMetadataPositionEvent(MarsPosition poi) {
        super(NEW_METADATA_POSITION);
        this.poi = poi;
    }
    
    public MarsPosition getPosition() {
    	return poi;
    }

    @Override
    public void invokeHandler(PlotEventHandler handler) {}
}
