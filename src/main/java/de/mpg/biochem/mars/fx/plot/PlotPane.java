package de.mpg.biochem.mars.fx.plot;

import de.mpg.biochem.mars.fx.util.StyleSheetUpdater;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.event.Event;
import javafx.scene.Node;

public interface PlotPane {
	public StyleSheetUpdater getStyleSheetUpdater();
	public Node getNode();
	public void fireEvent(Event event);
}
