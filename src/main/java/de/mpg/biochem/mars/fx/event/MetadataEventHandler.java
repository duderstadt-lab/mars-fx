package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import javafx.event.Event;
import javafx.event.EventHandler;

public interface MetadataEventHandler extends EventHandler<MetadataEvent> {
	public void fireEvent(Event event);
	public void onMetadataSelectionChangedEvent(MarsImageMetadata marsImageMetadata);
}
