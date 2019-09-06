package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import javafx.event.EventHandler;

public abstract class MarsImageMetadataEventHandler implements EventHandler<MarsImageMetadataEvent> {

    public abstract void onMarsImageMetadataSelectionChangedEvent(MarsImageMetadata marsImageMetadata);

    @Override
    public void handle(MarsImageMetadataEvent event) {
        event.invokeHandler(this);
    }
}
