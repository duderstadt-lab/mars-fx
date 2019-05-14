package de.mpg.biochem.mars.gui.molecule.moleculesTab;

import de.mpg.biochem.mars.gui.molecule.MoleculeArchiveSubTab;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

public class NotesTabController implements MoleculeSubTab {
	
	private BorderPane borderPane;
	private TextArea textArea;
	
	private Molecule molecule;
	
	public NotesTabController() {
		borderPane = new BorderPane();
		textArea = new TextArea();
        borderPane.setCenter(textArea);
	}
	
	public Node getNode() {
		return borderPane;
	}
	
	@Override
	public void setMolecule(Molecule molecule) {
		this.molecule = molecule;
	}

}
