package de.mpg.biochem.mars.gui.molecule.moleculesTab;

import de.mpg.biochem.mars.gui.molecule.MoleculeArchiveSubTab;
import de.mpg.biochem.mars.gui.preview.MarkdownPreviewPane;
import de.mpg.biochem.mars.gui.preview.MarkdownPreviewPane.Type;

import java.io.IOException;
import java.util.ArrayList;

import com.jfoenix.controls.JFXToggleButton;

import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;

public class MoleculesTabController implements MoleculeArchiveSubTab {
	
	private SplitPane splitPane;
	private MoleculeArchive archive;
	private MoleculeIndexTableController moleculeIndexTableController;
	private MoleculeOverviewController moleculeOverviewController;
	
	private ArrayList<MoleculeSubTab> moleculeSubTabControllers;

	public MoleculesTabController() {
		moleculeSubTabControllers = new ArrayList<MoleculeSubTab>();
		
		splitPane = new SplitPane();
		ObservableList<Node> splitItems = splitPane.getItems();
		
		//Initialize MoleculeSubTab Controllers
		moleculeIndexTableController = new MoleculeIndexTableController();
		moleculeOverviewController = new MoleculeOverviewController();
		
		//Add MoleculeSubTab Controllers to a list
		moleculeSubTabControllers.add(moleculeOverviewController);
		
		//Give the list to the moleculeIndexTableController 
		//so when a new molecule is selected all sub tabs are updated
		moleculeIndexTableController.setMoleculeSubTabList(moleculeSubTabControllers);
		
		try {
			FXMLLoader loader = new FXMLLoader();
        	loader.setLocation(getClass().getResource("MoleculeOverview.fxml"));
		
			Parent propertiesPane = loader.load();
		
			splitItems.add(moleculeIndexTableController.getNode());
			splitItems.add(propertiesPane);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
