package de.mpg.biochem.mars.fx.molecule.imageMetaDataTab;

import java.io.IOException;
import java.util.ArrayList;

import com.jfoenix.controls.JFXToggleButton;

import de.mpg.biochem.mars.fx.molecule.MoleculeArchiveSubTab;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeIndexTableController;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubTab;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeTablesPane;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SplitPane;

public class ImageMetaDataTabController implements MoleculeArchiveSubTab {
	
	private SplitPane splitPane;
	private MoleculeArchive archive;
	private ImageMetaDataIndexTableController metaIndexTableController;
	private ArrayList<ImageMetaDataSubTab> metaSubTabControllers;
	
	private ArrayList<MoleculeArchiveSubTab> moleculeArchiveSubTabControllers;
	
	private ImageMetaDataTablesPane metaTablesPane;

	public ImageMetaDataTabController() {
		metaSubTabControllers = new ArrayList<ImageMetaDataSubTab>();
		moleculeArchiveSubTabControllers = new ArrayList<MoleculeArchiveSubTab>();

		metaIndexTableController = new ImageMetaDataIndexTableController();
		
		try {
			//Load ImageMetaDataOverview Pane that will have UID, Tags, Parameter etc info...
			FXMLLoader loader = new FXMLLoader();
        	loader.setLocation(getClass().getResource("ImageMetaDataOverview.fxml"));
			Parent metaOverviewNode = loader.load();
			ImageMetaDataSubTab metaOverview = (ImageMetaDataSubTab) loader.getController();
			
			//Add MoleculeArchiveSubTab Controllers to a list
			moleculeArchiveSubTabControllers.add((MoleculeArchiveSubTab) loader.getController());
			moleculeArchiveSubTabControllers.add((MoleculeArchiveSubTab) metaIndexTableController);
			
			//Load ImageMetaData Tables/Plot Pane
			metaTablesPane = new ImageMetaDataTablesPane();
			
			//Add ImageMetaDataSubTab Controllers to a list
			metaSubTabControllers.add(metaOverview);
			metaSubTabControllers.add(metaTablesPane);
			
			//Give the list to the ImageMetaDataIndexTableController 
			//so when a new ImageMetaData is selected all sub tabs are updated
			metaIndexTableController.setMetaSubTabList(metaSubTabControllers);
			
			//Create split pane
			splitPane = new SplitPane();
			ObservableList<Node> splitItems = splitPane.getItems();
			
			//SplitPane.setResizableWithParent(ImageMetaDataIndexTableController.getNode(), Boolean.FALSE);
			splitItems.add(metaIndexTableController.getNode());
			
			splitItems.add(metaTablesPane.getNode());
			
			SplitPane.setResizableWithParent(metaOverviewNode, Boolean.FALSE);
			splitItems.add(metaOverviewNode);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Node getNode() {
		return splitPane;
	}

	@Override
	public void setArchive(MoleculeArchive archive) {
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
