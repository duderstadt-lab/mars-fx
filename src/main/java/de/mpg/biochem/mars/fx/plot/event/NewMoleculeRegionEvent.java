package de.mpg.biochem.mars.fx.plot.event;

import de.mpg.biochem.mars.util.MarsRegion;
import javafx.event.EventType;

public class NewMoleculeRegionEvent extends PlotEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final MarsRegion roi;

	public static final EventType<PlotEvent> NEW_MOLECULE_REGION = new EventType<>(PLOT_EVENT, "NEW_MOLECULE_REGION");

    public NewMoleculeRegionEvent(MarsRegion roi) {
        super(NEW_MOLECULE_REGION);
        this.roi = roi;
    }
    
    public MarsRegion getRegion() {
    	return roi;
    }

    @Override
    public void invokeHandler(PlotEventHandler handler) {}
}
