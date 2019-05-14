package de.mpg.biochem.mars.gui.molecule;

import com.jfoenix.controls.JFXTabPane;

import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.mpg.biochem.mars.gui.molecule.moleculesTab.MoleculesTabController;
import de.mpg.biochem.mars.gui.table.MARSResultsTableFrame;
import de.mpg.biochem.mars.molecule.MoleculeArchive;


public class MoleculeArchiveFrameController {

	@FXML
    private JFXTabPane tabContainer;

	@FXML
    private Tab dashboardTab;
	
	@FXML
    private AnchorPane dashboardContainer;

	@FXML
    private Tab imageMetaDataTab;
    
    @FXML
    private AnchorPane imageMetaDataContainer;

    @FXML
    private Tab moleculesTab;
    
    @FXML
    private AnchorPane moleculesContainer;
    
    @FXML
    private Tab commentsTab;
    
    @FXML
    private AnchorPane commentsContainer;
    
    @FXML
    private Tab settingsTab;
    
    @FXML
    private AnchorPane settingsContainer;
	
	private ArrayList<MoleculeArchiveSubTab> tabPaneControllers;
	
    private CommentsTabController commentsController;
    
    private MoleculesTabController moleculesController;
    
    private MoleculeArchive archive;

    private double tabWidth = 60.0;
    public static int lastSelectedTabIndex = 0;
    
    @FXML
    public void initialize() {
    	tabPaneControllers = new ArrayList<MoleculeArchiveSubTab>();
        configureView();
    }
    
	public void setArchive(MoleculeArchive archive) {
		this.archive = archive;
		for (MoleculeArchiveSubTab controller: tabPaneControllers)
			controller.setArchive(archive);
	}
	
    private void configureView() {
        tabContainer.setTabMinWidth(tabWidth);
        tabContainer.setTabMaxWidth(tabWidth);
        tabContainer.setTabMinHeight(tabWidth);
        tabContainer.setTabMaxHeight(tabWidth);
        tabContainer.setRotateGraphic(true);

        EventHandler<Event> replaceBackgroundColorHandler = event -> {
            lastSelectedTabIndex = tabContainer.getSelectionModel().getSelectedIndex();

            Tab currentTab = (Tab) event.getTarget();
            if (currentTab.isSelected()) {
                currentTab.setStyle("-fx-background-color: -fx-focus-color;");
            } else {
                currentTab.setStyle("-fx-background-color: -fx-accent;");
            }
        };
        
        Region microscopeIcon = new Region();
        microscopeIcon.getStyleClass().add("microscopeIcon");

        Region moleculeIcon = new Region();
        moleculeIcon.getStyleClass().add("moleculeIcon");
        
        Region bookIcon = new Region();
        bookIcon.getStyleClass().add("bookIcon");
        
        configureTab(dashboardTab, "Dashboard", MaterialIconFactory.get().createIcon(de.jensd.fx.glyphs.materialicons.MaterialIcon.DASHBOARD, "1.3em"), dashboardContainer, getClass().getResource("MADashboard.fxml"), replaceBackgroundColorHandler);
        configureTab(imageMetaDataTab, "ImageMetaData", microscopeIcon, imageMetaDataContainer, getClass().getResource("MAImageMetaData.fxml"), replaceBackgroundColorHandler);
        configureMoleculesTab(moleculesTab, "Molecules", moleculeIcon, moleculesContainer, replaceBackgroundColorHandler);
        configureCommentsTab(commentsTab, "Comments", bookIcon, commentsContainer, replaceBackgroundColorHandler);
        configureTab(settingsTab, "Settings", FontAwesomeIconFactory.get().createIcon(COG, "1.3em"), settingsContainer, getClass().getResource("MASettings.fxml"), replaceBackgroundColorHandler);
        
        dashboardTab.setStyle("-fx-background-color: -fx-focus-color;");
        
    }
    
    //Here we manually create the Comment Controller so we can load the MarkDown editor.
    private void configureCommentsTab(Tab tab, String title, Node icon, AnchorPane containerPane, EventHandler<Event> onSelectionChangedEvent) {
    	commentsController = new CommentsTabController();
    	tabPaneControllers.add(commentsController);
    	
    	BorderPane tabPane = new BorderPane();
        tabPane.setRotate(90.0);
        tabPane.setMaxWidth(tabWidth);
        tabPane.setCenter(icon);

        tab.setText("");
        tab.setGraphic(tabPane);

        tab.setOnSelectionChanged(onSelectionChangedEvent);
        
        containerPane.getChildren().add(commentsController);
        AnchorPane.setTopAnchor(commentsController, 0.0);
        AnchorPane.setBottomAnchor(commentsController, 0.0);
        AnchorPane.setRightAnchor(commentsController, 0.0);
        AnchorPane.setLeftAnchor(commentsController, 0.0);
    }
    
  //Here we manually create the Molecules Controller.
    private void configureMoleculesTab(Tab tab, String title, Node icon, AnchorPane containerPane, EventHandler<Event> onSelectionChangedEvent) {
    	moleculesController = new MoleculesTabController();
    	tabPaneControllers.add(moleculesController);
    	
    	BorderPane tabPane = new BorderPane();
        tabPane.setRotate(90.0);
        tabPane.setMaxWidth(tabWidth);
        tabPane.setCenter(icon);

        tab.setText("");
        tab.setGraphic(tabPane);

        tab.setOnSelectionChanged(onSelectionChangedEvent);
        
        containerPane.getChildren().add(moleculesController.getNode());
        AnchorPane.setTopAnchor(moleculesController.getNode(), 0.0);
        AnchorPane.setBottomAnchor(moleculesController.getNode(), 0.0);
        AnchorPane.setRightAnchor(moleculesController.getNode(), 0.0);
        AnchorPane.setLeftAnchor(moleculesController.getNode(), 0.0);
    }
    
    private void configureTab(Tab tab, String title, Node icon, AnchorPane containerPane, URL resourceURL, EventHandler<Event> onSelectionChangedEvent) {
        BorderPane tabPane = new BorderPane();
        tabPane.setRotate(90.0);
        tabPane.setMaxWidth(tabWidth);
        tabPane.setCenter(icon);

        tab.setText("");
        tab.setGraphic(tabPane);

        tab.setOnSelectionChanged(onSelectionChangedEvent);

        if (containerPane != null && resourceURL != null) {
            try {
            	FXMLLoader loader = new FXMLLoader();
    	        loader.setLocation(resourceURL);
                Parent contentView = loader.load();
                
                MoleculeArchiveSubTab controller = loader.getController();
                tabPaneControllers.add(controller);
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
    
    @FXML
    private void handleClose() {
    	archive.getWindow().close();
    	save();
    }
    
    public void updateAll() {
    	
    }
    
    public void save() {
    	
    }
    
    public void lockArchive() {
    	
    }
    
    public void unlockArchive() {
    	
    }
}