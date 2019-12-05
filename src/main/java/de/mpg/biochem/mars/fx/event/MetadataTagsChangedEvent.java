package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import javafx.event.EventType;

public class MetadataTagsChangedEvent extends MetadataEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MetadataEvent> TAGS_CHANGED = new EventType<>(METADATA_EVENT, "TAGS_CHANGED");

	private final MarsImageMetadata marsImageMetadata;

	public MetadataTagsChangedEvent(MarsImageMetadata marsImageMetadata) {
        super(TAGS_CHANGED);
        this.marsImageMetadata = marsImageMetadata;
    }
	
	public MarsImageMetadata getImageMetadata() {
		return this.marsImageMetadata;
	}

    @Override
    public void invokeHandler(MetadataEventHandler handler) {
    }
}

