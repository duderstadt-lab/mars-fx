package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import javafx.event.EventType;

public class MarsImageMetadataSelectionChangedEvent extends MarsImageMetadataEvent {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static final EventType<MarsImageMetadataEvent> MARS_IMAGE_METADATA_SELECTION_CHANGED = new EventType<>(MARS_IMAGE_METADATA_EVENT, "MARS_IMAGE_METADATA_SELECTION_CHANGED");

	    private final MarsImageMetadata marsImageMetadata;

	    public MarsImageMetadataSelectionChangedEvent(MarsImageMetadata marsImageMetadata) {
	        super(MARS_IMAGE_METADATA_SELECTION_CHANGED);
	        this.marsImageMetadata = marsImageMetadata;
	    }

	    @Override
	    public void invokeHandler(MarsImageMetadataEventHandler handler) {
	        handler.onMarsImageMetadataSelectionChangedEvent(marsImageMetadata);
	    }
	}
