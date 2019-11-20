package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import javafx.event.EventType;

public class RefreshMetadataEvent extends MetadataEvent {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static final EventType<MetadataEvent> REFRESH_METADATA_EVENT = new EventType<>(METADATA_EVENT, "REFRESH_METADATA_EVENT");

	    public RefreshMetadataEvent() {
	        super(REFRESH_METADATA_EVENT);
	    }

	    @Override
	    public void invokeHandler(MetadataEventHandler handler) {}
	}
