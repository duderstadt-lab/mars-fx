package de.mpg.biochem.mars.fx.molecule;

import java.util.ArrayList;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.scene.control.Menu;

public interface MoleculeArchiveTab<M extends Molecule, I extends MarsImageMetadata, P extends MoleculeArchiveProperties> extends ViewableNode {
	public void setArchive(MoleculeArchive<M, I, P> archive);
	public ArrayList<Menu> getMenus();
}
