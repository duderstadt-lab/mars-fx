package de.mpg.biochem.mars.fx.plot.event;

import de.mpg.biochem.mars.util.PositionOfInterest;
import javafx.event.EventType;

public class UpdateMoleculePositionEvent extends PlotEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final PositionOfInterest poi;

	public static final EventType<PlotEvent> UPDATE_MOLECULE_POSITION = new EventType<>(PLOT_EVENT, "UPDATE_MOLECULE_POSITION");

    public UpdateMoleculePositionEvent(PositionOfInterest poi) {
        super(UPDATE_MOLECULE_POSITION);
        this.poi = poi;
    }
    
    public PositionOfInterest getPosition() {
    	return poi;
    }

    @Override
    public void invokeHandler(PlotEventHandler handler) {}
}
