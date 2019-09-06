package de.mpg.biochem.mars.fx.molecule.moleculesTab;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LIST_ALT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CLIPBOARD;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.INFO_CIRCLE;

import java.io.IOException;
import java.net.URL;

import com.jfoenix.controls.JFXTabPane;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.event.MoleculeEventHandler;
import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
//import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public abstract class AbstractMoleculePropertiesPane<M extends Molecule> implements MoleculeSubPane<M> {
	
	protected StackPane stackPane;
	protected JFXTabPane tabsContainer;
	protected Tab generalTab;
	protected AnchorPane generalTabContainer;
	
	protected Tab propertiesTab;
	protected AnchorPane propertiesTabContainer;
	
	protected MoleculeGeneralTabController moleculeGeneralTabController;
	protected MoleculePropertiesTable moleculePropertiesTable;
	
	protected MoleculeArchive<M,MarsImageMetadata,MoleculeArchiveProperties> archive;
	
	protected M molecule;
	
    private double tabWidth = 60.0;
    public static int lastSelectedTabIndex = 0;
	
	public AbstractMoleculePropertiesPane() {
		stackPane = new StackPane();
		stackPane.prefWidth(220.0);
		stackPane.prefHeight(220.0);
		stackPane.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/moleculesTab/MoleculeOverview.css");
		
		tabsContainer = new JFXTabPane();
		tabsContainer.prefHeight(350.0);
		tabsContainer.prefWidth(220.0);
		tabsContainer.setSide(Side.TOP);
		tabsContainer.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		tabsContainer.setTabMinWidth(tabWidth);
        tabsContainer.setTabMaxWidth(tabWidth);
        tabsContainer.setTabMinHeight(30.0);
        tabsContainer.setTabMaxHeight(30.0);
        tabsContainer.disableAnimationProperty();
        
        stackPane.getChildren().add(tabsContainer);
        
        stackPane.addEventHandler(MoleculeEvent.MOLECULE_EVENT, new MoleculeEventHandler() { 
		    @SuppressWarnings("unchecked")
			@Override
		    public void onMoleculeSelectionChangedEvent(Molecule molecule) {
		        setMolecule((M) molecule);
		    }
		});
		
		configureTabs();
   }
	
	private void configureTabs() {
		//Build general Tab
        BorderPane tabPane = new BorderPane();
        tabPane.setMaxWidth(tabWidth);
        tabPane.setCenter(FontAwesomeIconFactory.get().createIcon(INFO_CIRCLE, "1.1em"));

        generalTab.setText("");
        generalTab.setGraphic(tabPane);
        generalTab.closableProperty().set(false);
        
        URL resourceURL = getClass().getResource("MoleculeGeneralTab.fxml");
        
        generalTabContainer = new AnchorPane();
        generalTabContainer.minHeight(0.0);
        generalTabContainer.minWidth(0.0);
        generalTabContainer.prefHeight(250.0);
        generalTabContainer.prefWidth(220.0);
        
        try {
        	FXMLLoader loader = new FXMLLoader();
	        loader.setLocation(resourceURL);
            Parent contentView = loader.load();
            
            moleculeGeneralTabController = (MoleculeGeneralTabController) loader.getController();
            generalTabContainer.getChildren().add(contentView);
            
            AnchorPane.setTopAnchor(contentView, 0.0);
            AnchorPane.setBottomAnchor(contentView, 0.0);
            AnchorPane.setRightAnchor(contentView, 0.0);
            AnchorPane.setLeftAnchor(contentView, 0.0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        //Build properties Tab
        moleculePropertiesTable = new MoleculePropertiesTable();
       	
    	BorderPane propertiesTabPane = new BorderPane();
    	propertiesTabPane.setMaxWidth(tabWidth);
    	propertiesTabPane.setCenter(FontAwesomeIconFactory.get().createIcon(LIST_ALT, "1.1em"));

        propertiesTab.setText("");
        propertiesTab.setGraphic(tabPane);
        propertiesTab.closableProperty().set(false);
        
        propertiesTabContainer = new AnchorPane();
        propertiesTabContainer.minHeight(0.0);
        propertiesTabContainer.minWidth(0.0);
        propertiesTabContainer.prefHeight(250.0);
        propertiesTabContainer.prefWidth(220.0);
        
        propertiesTabContainer.getChildren().add(moleculePropertiesTable.getNode());
        AnchorPane.setTopAnchor(moleculePropertiesTable.getNode(), 0.0);
        AnchorPane.setBottomAnchor(moleculePropertiesTable.getNode(), 0.0);
        AnchorPane.setRightAnchor(moleculePropertiesTable.getNode(), 0.0);
        AnchorPane.setLeftAnchor(moleculePropertiesTable.getNode(), 0.0);

        tabsContainer.getTabs().add(generalTab);
        tabsContainer.getTabs().add(propertiesTab);
	}
   
   public void setMolecule(M molecule) {
		this.molecule = molecule;
		
		moleculeGeneralTabController.setMolecule(molecule);
		moleculePropertiesTable.setMolecule(molecule);
	}

	@Override
	public void setArchive(MoleculeArchive<M, MarsImageMetadata, MoleculeArchiveProperties> archive) {
		this.archive = archive;
	}
}
