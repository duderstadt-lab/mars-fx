package de.mpg.biochem.mars.fx.event;

import javafx.event.Event;
import javafx.event.EventType;

public abstract class MetadataEvent extends Event {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final EventType<MetadataEvent> METADATA_EVENT = new EventType<>(ANY, "METADATA_EVENT");

    public MetadataEvent(EventType<? extends Event> eventType) {
        super(eventType);
    }

    public abstract void invokeHandler(MetadataEventHandler handler);
}
