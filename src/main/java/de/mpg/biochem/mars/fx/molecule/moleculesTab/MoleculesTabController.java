package de.mpg.biochem.mars.fx.molecule.moleculesTab;

import de.mpg.biochem.mars.fx.molecule.MoleculeArchiveFxFrameController;
import de.mpg.biochem.mars.fx.molecule.MoleculeArchiveSubTab;
import de.mpg.biochem.mars.fx.preview.MarkdownPreviewPane;
import de.mpg.biochem.mars.fx.preview.MarkdownPreviewPane.Type;

import java.io.IOException;
import java.util.ArrayList;

import com.jfoenix.controls.JFXToggleButton;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;

public class MoleculesTabController implements MoleculeArchiveSubTab {
	
	private SplitPane splitPane;
	private MoleculeArchive<?,?,?> archive;
	private MoleculeIndexTableController moleculeIndexTableController;
	private ArrayList<MoleculeSubTab> moleculeSubTabControllers;
	
	private ArrayList<MoleculeArchiveSubTab> moleculeArchiveSubTabControllers;
	
	private MoleculeTablesPane moleculeTablesPane;

	public MoleculesTabController() {
		moleculeSubTabControllers = new ArrayList<MoleculeSubTab>();
		moleculeArchiveSubTabControllers = new ArrayList<MoleculeArchiveSubTab>();

		moleculeIndexTableController = new MoleculeIndexTableController();
		
		try {
			//Load MoleculeOverview Pane that will have UID, Tags, Parameter etc info...
			FXMLLoader loader = new FXMLLoader();
        	loader.setLocation(getClass().getResource("MoleculeOverview.fxml"));
			Parent moleculeOverviewNode = loader.load();
			MoleculeSubTab moleculeOverview = (MoleculeSubTab) loader.getController();
			
			//Add MoleculeArchiveSubTab Controllers to a list
			moleculeArchiveSubTabControllers.add((MoleculeArchiveSubTab) loader.getController());
			moleculeArchiveSubTabControllers.add((MoleculeArchiveSubTab) moleculeIndexTableController);
			
			//Load Molecule Tables/Plot Pane
			moleculeTablesPane = new MoleculeTablesPane();
			
			//Add MoleculeSubTab Controllers to a list
			moleculeSubTabControllers.add(moleculeOverview);
			moleculeSubTabControllers.add(moleculeTablesPane);
			
			//Give the list to the moleculeIndexTableController 
			//so when a new molecule is selected all sub tabs are updated
			moleculeIndexTableController.setMoleculeSubTabList(moleculeSubTabControllers);
			
			//Create split pane
			splitPane = new SplitPane();
			ObservableList<Node> splitItems = splitPane.getItems();
			
			//SplitPane.setResizableWithParent(moleculeIndexTableController.getNode(), Boolean.FALSE);
			splitItems.add(moleculeIndexTableController.getNode());
			
			splitItems.add(moleculeTablesPane.getNode());
			
			SplitPane.setResizableWithParent(moleculeOverviewNode, Boolean.FALSE);
			splitItems.add(moleculeOverviewNode);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Node getNode() {
		return splitPane;
	}
	
	public void saveCurrentRecord() {
		moleculeIndexTableController.saveMolecule();
	}

	@Override
	public void setArchive(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive) {
		this.archive = archive;
		
		if (moleculeArchiveSubTabControllers == null)
			return;
		
		for (MoleculeArchiveSubTab controller: moleculeArchiveSubTabControllers)
			controller.setArchive(archive);
	}

	public ArrayList<Menu> getMenus() {
		return new ArrayList<Menu>();
	}
}
