package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import javafx.event.EventType;

public class MetadataSelectionChangedEvent extends MetadataEvent {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static final EventType<MetadataEvent> METADATA_SELECTION_CHANGED = new EventType<>(METADATA_EVENT, "METADATA_SELECTION_CHANGED");

	    private final MarsImageMetadata marsImageMetadata;

	    public MetadataSelectionChangedEvent(MarsImageMetadata marsImageMetadata) {
	        super(METADATA_SELECTION_CHANGED);
	        this.marsImageMetadata = marsImageMetadata;
	    }
	    
		public MarsImageMetadata getImageMetadata() {
			return this.marsImageMetadata;
		}

	    @Override
	    public void invokeHandler(MetadataEventHandler handler) {
	        handler.onMetadataSelectionChangedEvent(marsImageMetadata);
	    }
	}
