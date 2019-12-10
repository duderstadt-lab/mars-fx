package de.mpg.biochem.mars.fx.plot.event;

import de.mpg.biochem.mars.util.MarsPosition;
import javafx.event.EventType;

public class NewMoleculePositionEvent extends PlotEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final MarsPosition poi;

	public static final EventType<PlotEvent> NEW_MOLECULE_POSITION = new EventType<>(PLOT_EVENT, "NEW_MOLECULE_POSITION");

    public NewMoleculePositionEvent(MarsPosition poi) {
        super(NEW_MOLECULE_POSITION);
        this.poi = poi;
    }
    
    public MarsPosition getPosition() {
    	return poi;
    }

    @Override
    public void invokeHandler(PlotEventHandler handler) {}
}
