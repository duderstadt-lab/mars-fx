package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.event.Event;

/*
 * This abstract class can be directly instanced in AddEventHandler methods. 
 * Methods implements are left blank on purpose to allow for selective overriding.
 * 
 */
public abstract class DefaultMoleculeArchiveEventHandler implements MoleculeArchiveEventHandler {

	@Override
	public void fireEvent(Event event) {}

	@Override
	public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive) {}

	@Override
	public void onMoleculeArchiveLockEvent() {}

	@Override
	public void onMoleculeArchiveUnlockEvent() {}

	@Override
	public void onMoleculeArchiveSavingEvent() {}

	@Override
	public void onMoleculeArchiveSavedEvent() {}

	@Override
	public void handle(MoleculeArchiveEvent event) {
		event.invokeHandler(this);
		event.consume();
	}
}
