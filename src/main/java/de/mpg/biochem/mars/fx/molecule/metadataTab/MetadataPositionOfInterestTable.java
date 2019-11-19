package de.mpg.biochem.mars.fx.molecule.metadataTab;

import de.mpg.biochem.mars.fx.molecule.AbstractPositionOfInterestTable;
import de.mpg.biochem.mars.fx.event.MetadataEvent;
import de.mpg.biochem.mars.fx.event.MetadataIndicatorChangedEvent;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import javafx.event.Event;

public class MetadataPositionOfInterestTable extends AbstractPositionOfInterestTable implements MetadataSubPane {

    public MetadataPositionOfInterestTable() {        
        super();
    }

	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}

	@Override
	protected void fireIndicatorChangedEvent() {
		getNode().fireEvent(new MetadataIndicatorChangedEvent((MarsImageMetadata) record));
	}

	@Override
	protected void addEventHandlers() {
		getNode().addEventHandler(MetadataEvent.METADATA_EVENT, this);
	}

	@Override
	public void onMetadataSelectionChangedEvent(MarsImageMetadata marsImageMetadata) {
		this.record = marsImageMetadata;
    	loadData();
	}

	@Override
	public void handle(MetadataEvent event) {
		event.invokeHandler(this);
        event.consume();
	}
}
