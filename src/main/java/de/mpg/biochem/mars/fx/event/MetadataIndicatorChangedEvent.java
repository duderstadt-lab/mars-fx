package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import javafx.event.EventType;

public class MetadataIndicatorChangedEvent extends MetadataEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MetadataEvent> INDICATOR_CHANGED = new EventType<>(METADATA_EVENT, "INDICATOR_CHANGED");

	private final MarsImageMetadata marsImageMetadata;

	public MetadataIndicatorChangedEvent(MarsImageMetadata marsImageMetadata) {
        super(INDICATOR_CHANGED);
        this.marsImageMetadata = marsImageMetadata;
    }

    @Override
    public void invokeHandler(MetadataEventHandler handler) {
    }
}

