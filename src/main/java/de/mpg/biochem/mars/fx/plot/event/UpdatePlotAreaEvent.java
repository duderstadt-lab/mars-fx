package de.mpg.biochem.mars.fx.plot.event;

import javafx.event.EventType;

public class UpdatePlotAreaEvent extends PlotEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<PlotEvent> UPDATE_PLOT_AREA = new EventType<>(PLOT_EVENT, "UPDATE_PLOT_AREA");

    public UpdatePlotAreaEvent() {
        super(UPDATE_PLOT_AREA);
    }

    @Override
    public void invokeHandler(PlotEventHandler handler) {
       handler.onUpdatePlotAreaEvent();
    }
}
