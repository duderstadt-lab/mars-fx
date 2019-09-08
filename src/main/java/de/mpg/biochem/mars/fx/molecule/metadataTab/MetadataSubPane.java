package de.mpg.biochem.mars.fx.molecule.metadataTab;

import de.mpg.biochem.mars.fx.event.MarsImageMetadataEvent;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;

public interface MetadataSubPane<I extends MarsImageMetadata> extends EventHandler<MarsImageMetadataEvent> {
	public void setArchive(MoleculeArchive<Molecule,MarsImageMetadata,MoleculeArchiveProperties> archive);
	public void setMetadata(I imageMetadata);
	public Node getNode();
	public void fireEvent(Event event);
	public void onMarsImageMetadataSelectionChangedEvent(MarsImageMetadata marsImageMetadata);
}
