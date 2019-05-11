package de.mpg.biochem.mars.gui.molecule;

import de.mpg.biochem.mars.gui.preview.MarkdownPreviewPane;
import de.mpg.biochem.mars.gui.preview.MarkdownPreviewPane.Type;

import com.jfoenix.controls.JFXToggleButton;

import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;

public class MAMoleculesController implements MAPaneController {
	
	private SplitPane splitPane;
	private MoleculeArchive archive;
	private MoleculeIndexTableController moleculeIndexTableController;
	
	public MAMoleculesController() {
		splitPane = new SplitPane();
		
		ObservableList<Node> splitItems = splitPane.getItems();
		
		moleculeIndexTableController = new MoleculeIndexTableController();
		
		splitItems.add(moleculeIndexTableController.getNode());
		//splitItems.remove(previewPane);
	}
	
	public Node getNode() {
		return splitPane;
	}

	@Override
	public void setArchive(MoleculeArchive archive) {
		this.archive = archive;
		this.moleculeIndexTableController.setArchive(archive);
	}

}
