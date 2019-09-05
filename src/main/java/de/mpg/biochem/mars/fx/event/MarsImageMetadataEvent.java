package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import javafx.event.Event;
import javafx.event.EventType;

public abstract class MarsImageMetadataEvent<I extends MarsImageMetadata> extends Event {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final EventType<MarsImageMetadataEvent<?>> MARS_IMAGE_METADATA_EVENT = new EventType<>(ANY);

    public MarsImageMetadataEvent(EventType<? extends Event> eventType) {
        super(eventType);
    }

    public abstract void invokeHandler(MarsImageMetadataEventHandler<I> handler);

}
