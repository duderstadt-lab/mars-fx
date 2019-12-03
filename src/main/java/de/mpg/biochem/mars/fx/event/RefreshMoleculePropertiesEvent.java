package de.mpg.biochem.mars.fx.event;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import javafx.event.EventType;

public class RefreshMoleculePropertiesEvent extends MoleculeEvent {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static final EventType<MoleculeEvent> REFRESH_MOLECULE_PROPERTIES_EVENT = new EventType<>(MOLECULE_EVENT, "REFRESH_MOLECULE_PROPERTIES_EVENT");

	    public RefreshMoleculePropertiesEvent() {
	        super(REFRESH_MOLECULE_PROPERTIES_EVENT);
	    }

	    @Override
	    public void invokeHandler(MoleculeEventHandler handler) {}
	}
