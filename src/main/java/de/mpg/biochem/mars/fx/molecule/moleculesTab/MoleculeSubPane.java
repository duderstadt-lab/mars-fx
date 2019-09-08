package de.mpg.biochem.mars.fx.molecule.moleculesTab;

import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;

public interface MoleculeSubPane<M extends Molecule> extends EventHandler<MoleculeEvent> {
	public void setArchive(MoleculeArchive<Molecule,MarsImageMetadata,MoleculeArchiveProperties> archive);
	public void setMolecule(M molecule);
	public Node getNode();
	public void fireEvent(Event event);
	public void onMoleculeSelectionChangedEvent(Molecule molecule);
}

