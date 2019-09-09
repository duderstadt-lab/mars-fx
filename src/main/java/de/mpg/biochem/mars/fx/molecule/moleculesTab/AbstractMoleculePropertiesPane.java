package de.mpg.biochem.mars.fx.molecule.moleculesTab;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LIST_ALT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CLIPBOARD;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.INFO_CIRCLE;

import java.io.IOException;
import java.net.URL;

import com.jfoenix.controls.JFXTabPane;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.DefaultMoleculeArchiveEventHandler;
import de.mpg.biochem.mars.fx.event.InitializeMoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.event.MoleculeSelectionChangedEvent;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public abstract class AbstractMoleculePropertiesPane<M extends Molecule> implements MoleculeSubPane {
	
	protected StackPane rootPane;
	protected JFXTabPane tabsContainer;
	protected Tab generalTab;
	protected AnchorPane generalTabContainer;
	
	protected Tab propertiesTab;
	protected AnchorPane propertiesTabContainer;
	
	protected MoleculeGeneralTabController moleculeGeneralTabController;
	protected MoleculePropertiesTable moleculePropertiesTable;
	
	protected M molecule;
	
	protected MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive;
	
    private double tabWidth = 60.0;
    public static int lastSelectedTabIndex = 0;
	
	public AbstractMoleculePropertiesPane() {
		rootPane = new StackPane();
		rootPane.prefWidth(220.0);
		rootPane.prefHeight(220.0);
		rootPane.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/moleculesTab/MoleculeOverview.css");
		
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
        
        rootPane.getChildren().add(tabsContainer);
        
        getNode().addEventHandler(MoleculeEvent.MOLECULE_EVENT, this);
        getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, new DefaultMoleculeArchiveEventHandler() {
        	@Override
        	public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> newArchive) {
        		archive = newArchive;
        		moleculeGeneralTabController.fireEvent(new InitializeMoleculeArchiveEvent(newArchive));
        		moleculePropertiesTable.fireEvent(new InitializeMoleculeArchiveEvent(newArchive));
        	}
        });
        
		configureTabs();
   }
	
	private void configureTabs() {
		//Build general Tab
        BorderPane tabPane = new BorderPane();
        tabPane.setMaxWidth(tabWidth);
        tabPane.setCenter(FontAwesomeIconFactory.get().createIcon(INFO_CIRCLE, "1.1em"));

        generalTab = new Tab();
        generalTab.setText("");
        generalTab.setGraphic(tabPane);
        generalTab.closableProperty().set(false);
        
        URL resourceURL = getClass().getResource("MoleculeGeneralTab.fxml");
        
        generalTabContainer = new AnchorPane();
        generalTabContainer.minHeight(0.0);
        generalTabContainer.minWidth(0.0);
        generalTabContainer.prefHeight(250.0);
        generalTabContainer.prefWidth(220.0);
        generalTab.setContent(generalTabContainer);
        
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

    	propertiesTab = new Tab();
        propertiesTab.setText("");
        propertiesTab.setGraphic(propertiesTabPane);
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
        propertiesTab.setContent(propertiesTabContainer);
        
        tabsContainer.getTabs().add(generalTab);
        tabsContainer.getTabs().add(propertiesTab);
	}
	
	@Override
	public Node getNode() {
		return rootPane;
	}
	
	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onMoleculeSelectionChangedEvent(Molecule molecule) {
		this.molecule = (M) molecule;
		
		moleculeGeneralTabController.fireEvent(new MoleculeSelectionChangedEvent(molecule));
		moleculePropertiesTable.fireEvent(new MoleculeSelectionChangedEvent(molecule));
    }
   
   @Override
   public void handle(MoleculeEvent event) {
	   event.invokeHandler(this);
	   event.consume();
   } 
}
