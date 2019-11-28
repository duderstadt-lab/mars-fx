package de.mpg.biochem.mars.fx.event;

import javafx.event.EventType;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;

public class MoleculeArchiveLockEvent extends MoleculeArchiveEvent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final EventType<MoleculeArchiveEvent> MOLECULE_ARCHIVE_LOCK = new EventType<>(MOLECULE_ARCHIVE_EVENT, "MOLECULE_ARCHIVE_LOCK");

	public String message;
	
    public MoleculeArchiveLockEvent(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive, String message) {
        super(MOLECULE_ARCHIVE_LOCK, archive);
        if (message != null)
        	this.message = message;
        else
        	this.message = "Please Wait...";
    }
    
    public String getMessage() {
    	return message;
    }

    @Override
    public void invokeHandler(MoleculeArchiveEventHandler handler) {
        handler.onMoleculeArchiveLockEvent();
    }

}
