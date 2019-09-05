package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import javafx.event.EventType;

public class MarsImageMetadataSelectionChangedEvent<I extends MarsImageMetadata> extends MarsImageMetadataEvent<I> {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static final EventType<MarsImageMetadataSelectionChangedEvent<?>> MARS_IMAGE_METADATA_SELECTION_CHANGED = new EventType<>(MARS_IMAGE_METADATA_EVENT, "MarsImageMetadataSelectionChanged");

	    private final I marsImageMetadata;

	    public MarsImageMetadataSelectionChangedEvent(I marsImageMetadata) {
	        super(MARS_IMAGE_METADATA_SELECTION_CHANGED);
	        this.marsImageMetadata = marsImageMetadata;
	    }

	    @Override
	    public void invokeHandler(MarsImageMetadataEventHandler<I> handler) {
	        handler.onMarsImageMetadataSelectionChangedEvent(marsImageMetadata);
	    }
	}
