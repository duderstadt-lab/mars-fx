package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import javafx.event.EventHandler;

public abstract class MarsImageMetadataEventHandler<I extends MarsImageMetadata> implements EventHandler<MarsImageMetadataEvent<I>> {

    public abstract void onMarsImageMetadataSelectionChangedEvent(I marsImageMetadata);

    @Override
    public void handle(MarsImageMetadataEvent<I> event) {
        event.invokeHandler(this);
    }
}
