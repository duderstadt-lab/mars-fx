package de.mpg.biochem.mars.fx.molecule.moleculesTab;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LIST_ALT;
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
import de.mpg.biochem.mars.fx.plot.PlotSeries;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.beans.value.ChangeListener;
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
	
	protected Tab regionsTab;
	protected AnchorPane regionsTabContainer;
	
	protected Tab positionsTab;
	protected AnchorPane positionsTabContainer;
	
	protected MoleculeGeneralTabController moleculeGeneralTabController;
	protected MoleculePropertiesTable moleculePropertiesTable;
	protected MoleculeRegionOfInterestTable regionOfInterestTable;
	protected MoleculePositionOfInterestTable positionOfInterestTable;
	
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
        		regionOfInterestTable.fireEvent(new InitializeMoleculeArchiveEvent(newArchive));
        		positionOfInterestTable.fireEvent(new InitializeMoleculeArchiveEvent(newArchive));
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
        
        //Build regions Tab
        regionOfInterestTable = new MoleculeRegionOfInterestTable();
       	
    	BorderPane regionsTabPane = new BorderPane();
    	regionsTabPane.setMaxWidth(tabWidth);
    	regionsTabPane.setCenter(FontAwesomeIconFactory.get().createIcon(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.SQUARE, "1.1em"));

    	regionsTab = new Tab();
    	regionsTab.setText("");
    	regionsTab.setGraphic(regionsTabPane);
    	regionsTab.closableProperty().set(false);
        
    	regionsTabContainer = new AnchorPane();
    	regionsTabContainer.minHeight(0.0);
    	regionsTabContainer.minWidth(0.0);
    	regionsTabContainer.prefHeight(250.0);
    	regionsTabContainer.prefWidth(220.0);
        
    	regionsTabContainer.getChildren().add(regionOfInterestTable.getNode());
        AnchorPane.setTopAnchor(regionOfInterestTable.getNode(), 0.0);
        AnchorPane.setBottomAnchor(regionOfInterestTable.getNode(), 0.0);
        AnchorPane.setRightAnchor(regionOfInterestTable.getNode(), 0.0);
        AnchorPane.setLeftAnchor(regionOfInterestTable.getNode(), 0.0);
        regionsTab.setContent(regionsTabContainer);
        
        //Build positions Tab
        positionOfInterestTable = new MoleculePositionOfInterestTable();
       	
    	BorderPane positionTabPane = new BorderPane();
    	positionTabPane.setMaxWidth(tabWidth);
    	positionTabPane.setCenter(FontAwesomeIconFactory.get().createIcon(de.jensd.fx.glyphs.octicons.OctIcon.MILESTONE, "1.1em"));

    	positionsTab = new Tab();
    	positionsTab.setText("");
    	positionsTab.setGraphic(positionTabPane);
    	positionsTab.closableProperty().set(false);
        
    	positionsTabContainer = new AnchorPane();
    	positionsTabContainer.minHeight(0.0);
    	positionsTabContainer.minWidth(0.0);
    	positionsTabContainer.prefHeight(250.0);
    	positionsTabContainer.prefWidth(220.0);
        
    	positionsTabContainer.getChildren().add(positionOfInterestTable.getNode());
        AnchorPane.setTopAnchor(positionOfInterestTable.getNode(), 0.0);
        AnchorPane.setBottomAnchor(positionOfInterestTable.getNode(), 0.0);
        AnchorPane.setRightAnchor(positionOfInterestTable.getNode(), 0.0);
        AnchorPane.setLeftAnchor(positionOfInterestTable.getNode(), 0.0);
        positionsTab.setContent(positionsTabContainer);
        
        tabsContainer.getTabs().add(generalTab);
        tabsContainer.getTabs().add(propertiesTab);
        tabsContainer.getTabs().add(regionsTab);
        tabsContainer.getTabs().add(positionsTab);
        
        tabsContainer.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            onMoleculeSelectionChangedEvent(molecule);
        });
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
		
		Tab selectedTab = tabsContainer.getSelectionModel().selectedItemProperty().get();
		
		//only update active tab to minimize performance loss during tab switching.
		if (selectedTab.equals(generalTab)) {
			moleculeGeneralTabController.fireEvent(new MoleculeSelectionChangedEvent(molecule));
		} else if (selectedTab.equals(propertiesTab)) {
			moleculePropertiesTable.fireEvent(new MoleculeSelectionChangedEvent(molecule));
		} else if (selectedTab.equals(regionsTab)) {
			regionOfInterestTable.fireEvent(new MoleculeSelectionChangedEvent(molecule));
		} else if (selectedTab.equals(positionsTab)) {
			positionOfInterestTable.fireEvent(new MoleculeSelectionChangedEvent(molecule));
		}
    }
   
   @Override
   public void handle(MoleculeEvent event) {
	   event.invokeHandler(this);
	   event.consume();
   } 
}
