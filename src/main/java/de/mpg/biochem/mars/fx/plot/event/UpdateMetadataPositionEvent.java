package de.mpg.biochem.mars.fx.plot.event;

import de.mpg.biochem.mars.util.MarsPosition;
import javafx.event.EventType;

public class UpdateMetadataPositionEvent extends PlotEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final MarsPosition poi;

	public static final EventType<PlotEvent> UPDATE_METADATA_POSITION = new EventType<>(PLOT_EVENT, "UPDATE_METADATA_POSITION");

    public UpdateMetadataPositionEvent(MarsPosition poi) {
        super(UPDATE_METADATA_POSITION);
        this.poi = poi;
    }
    
    public MarsPosition getPosition() {
    	return poi;
    }

    @Override
    public void invokeHandler(PlotEventHandler handler) {}
}
