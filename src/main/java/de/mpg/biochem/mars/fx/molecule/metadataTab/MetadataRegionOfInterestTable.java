package de.mpg.biochem.mars.fx.molecule.metadataTab;

import de.mpg.biochem.mars.fx.event.MetadataEvent;
import de.mpg.biochem.mars.fx.event.MetadataIndicatorChangedEvent;
import de.mpg.biochem.mars.fx.molecule.AbstractRegionOfInterestTable;
import de.mpg.biochem.mars.fx.event.MoleculeIndicatorChangedEvent;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import javafx.event.Event;

public class MetadataRegionOfInterestTable extends AbstractRegionOfInterestTable implements MetadataSubPane {

    public MetadataRegionOfInterestTable() {        
        super();
    }
    
    @Override
    public void handle(MetadataEvent event) {
        event.invokeHandler(this);
        event.consume();
    }

	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}

	@Override
	public void onMetadataSelectionChangedEvent(MarsImageMetadata marsImageMetadata) {
		this.record = marsImageMetadata;
    	loadData();
	}

	@Override
	protected void fireIndicatorChangedEvent() {
		getNode().fireEvent(new MetadataIndicatorChangedEvent((MarsImageMetadata) record));
	}

	@Override
	protected void addEventHandlers() {
		getNode().addEventHandler(MetadataEvent.METADATA_EVENT, this);
	}
}
