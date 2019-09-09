package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import javafx.event.Event;
import javafx.event.EventHandler;

public interface MarsImageMetadataEventHandler extends EventHandler<MarsImageMetadataEvent> {
	public void fireEvent(Event event);
	public void onMarsImageMetadataSelectionChangedEvent(MarsImageMetadata marsImageMetadata);
}
