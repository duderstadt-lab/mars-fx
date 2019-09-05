package de.mpg.biochem.mars.fx.molecule;

import java.util.ArrayList;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.scene.Node;
import javafx.scene.control.Menu;

public abstract class AbstractMoleculeArchiveTab<M extends Molecule, I extends MarsImageMetadata, P extends MoleculeArchiveProperties> implements MoleculeArchiveTab<M,I,P> {
	
	protected MoleculeArchive<M,I,P> archive;
	
	public AbstractMoleculeArchiveTab() {
		super();
    }
    
    public abstract Node getNode();
    
    public abstract ArrayList<Menu> getMenus();

	public void setArchive(MoleculeArchive<M,I,P> archive) {
		this.archive = archive;
	}
	
}
