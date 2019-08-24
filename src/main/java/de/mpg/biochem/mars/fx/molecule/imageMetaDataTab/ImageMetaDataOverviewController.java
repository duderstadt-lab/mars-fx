package de.mpg.biochem.mars.fx.molecule.imageMetaDataTab;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LIST_ALT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CLIPBOARD;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.INFO_CIRCLE;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import com.jfoenix.controls.JFXTabPane;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.molecule.MoleculeArchiveFxFrameController;
import de.mpg.biochem.mars.fx.molecule.MoleculeArchiveSubTab;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
//import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class ImageMetaDataOverviewController implements ImageMetaDataSubTab, MoleculeArchiveSubTab {
	
	@FXML
	private JFXTabPane tabContainer;

	@FXML
    private Tab generalTab;
	
	@FXML
    private AnchorPane generalTabContainer;
	
	@FXML
    private Tab propertiesTab;
	
	@FXML
    private AnchorPane propertiesTabContainer;
	
	private ImageMetaDataPropertiesTableController propertiesTableController;
	
	private ArrayList<ImageMetaDataSubTab> metaTabControllers;
	
	private ArrayList<MoleculeArchiveSubTab> moleculeArchiveTabControllers;
	
    private double tabWidth = 60.0;
    public static int lastSelectedTabIndex = 0;
	
	@FXML
    public void initialize() {
		metaTabControllers = new ArrayList<ImageMetaDataSubTab>();
		moleculeArchiveTabControllers = new ArrayList<MoleculeArchiveSubTab>();
        configureTabs();
    }
	
	private void configureTabs() {
        tabContainer.setTabMinWidth(tabWidth);
        tabContainer.setTabMaxWidth(tabWidth);
        tabContainer.setTabMinHeight(30.0);
        tabContainer.setTabMaxHeight(30.0);
        tabContainer.disableAnimationProperty();

        EventHandler<Event> replaceBackgroundColorHandler = event -> {
            lastSelectedTabIndex = tabContainer.getSelectionModel().getSelectedIndex();

            Tab currentTab = (Tab) event.getTarget();
            if (currentTab.isSelected()) {
                currentTab.setStyle("-fx-background-color: -fx-focus-color;");
            } else {
                currentTab.setStyle("-fx-background-color: -fx-accent;");
            }
        };
        
        configureTab(generalTab, "General", FontAwesomeIconFactory.get().createIcon(INFO_CIRCLE, "1.1em"), generalTabContainer, getClass().getResource("ImageMetaDataGeneralTab.fxml"), replaceBackgroundColorHandler);
        configurePropertiesTab(propertiesTab, "Properties", FontAwesomeIconFactory.get().createIcon(LIST_ALT, "1.1em"), propertiesTabContainer, replaceBackgroundColorHandler);
   }
	
   private void configureTab(Tab tab, String title, Node icon, AnchorPane containerPane, URL resourceURL, EventHandler<Event> onSelectionChangedEvent) {
        BorderPane tabPane = new BorderPane();
        tabPane.setMaxWidth(tabWidth);
        tabPane.setCenter(icon);

        tab.setText("");
        tab.setGraphic(tabPane);

        if (containerPane != null && resourceURL != null) {
            try {
            	FXMLLoader loader = new FXMLLoader();
    	        loader.setLocation(resourceURL);
                Parent contentView = loader.load();
                
                moleculeArchiveTabControllers.add((MoleculeArchiveSubTab) loader.getController());
                
                ImageMetaDataSubTab controller = (ImageMetaDataSubTab) loader.getController();
                metaTabControllers.add(controller);
                containerPane.getChildren().add(contentView);
                
                AnchorPane.setTopAnchor(contentView, 0.0);
                AnchorPane.setBottomAnchor(contentView, 0.0);
                AnchorPane.setRightAnchor(contentView, 0.0);
                AnchorPane.setLeftAnchor(contentView, 0.0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
   
   private void configurePropertiesTab(Tab tab, String title, Node icon, AnchorPane containerPane, EventHandler<Event> onSelectionChangedEvent) {
	   propertiesTableController = new ImageMetaDataPropertiesTableController();
	   metaTabControllers.add(propertiesTableController);
   	
   	   BorderPane tabPane = new BorderPane();
       tabPane.setMaxWidth(tabWidth);
       tabPane.setCenter(icon);

       tab.setText("");
       tab.setGraphic(tabPane);
       
       containerPane.getChildren().add(propertiesTableController.getNode());
       AnchorPane.setTopAnchor(propertiesTableController.getNode(), 0.0);
       AnchorPane.setBottomAnchor(propertiesTableController.getNode(), 0.0);
       AnchorPane.setRightAnchor(propertiesTableController.getNode(), 0.0);
       AnchorPane.setLeftAnchor(propertiesTableController.getNode(), 0.0);
   }

	@Override
	public void setArchive(MoleculeArchive archive) {
		if (moleculeArchiveTabControllers == null)
			return;
		
		for (MoleculeArchiveSubTab controller: moleculeArchiveTabControllers)
			controller.setArchive(archive);
	}

	@Override
	public void setImageMetaData(MarsImageMetadata meta) {
		if (metaTabControllers == null)
			return;
		
		for (ImageMetaDataSubTab controller: metaTabControllers)
			controller.setImageMetaData(meta);
	}
}
