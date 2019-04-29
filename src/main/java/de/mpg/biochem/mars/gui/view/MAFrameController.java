package de.mpg.biochem.mars.gui.view;

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
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.mpg.biochem.mars.gui.MARSResultsTableFrame;
import de.mpg.biochem.mars.gui.MoleculeArchiveFrame;
import de.mpg.biochem.mars.molecule.MoleculeArchive;


public class MAFrameController {

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
    
	@FXML
	private MoleculeArchive archive;
	
	private ArrayList<MAPaneController> tabPaneControllers;
	
    private MACommentsController commentsController;

    private double tabWidth = 60.0;
    public static int lastSelectedTabIndex = 0;

    /// Life cycle

    @FXML
    public void initialize() {
    	tabPaneControllers = new ArrayList<MAPaneController>();
        configureView();
    }
    
	public void setArchive(MoleculeArchive archive) {
		this.archive = archive;
		
		for (MAPaneController controller :tabPaneControllers)
			controller.setArchive(archive);
	}

    /// Private

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
        configureTab(moleculesTab, "Molecules", moleculeIcon, moleculesContainer, getClass().getResource("MAMolecules.fxml"), replaceBackgroundColorHandler);
        configureCommentsTab(commentsTab, "Comments", bookIcon, commentsContainer, replaceBackgroundColorHandler);
        configureTab(settingsTab, "Settings", FontAwesomeIconFactory.get().createIcon(COG, "1.3em"), settingsContainer, getClass().getResource("MASettings.fxml"), replaceBackgroundColorHandler);
        
        dashboardTab.setStyle("-fx-background-color: -fx-focus-color;");
        
    }
    
    //Here we manually create the Comment Controller so we can load the MarkDown editor.
    private void configureCommentsTab(Tab tab, String title, Node icon, AnchorPane containerPane, EventHandler<Event> onSelectionChangedEvent) {
    	commentsController = new MACommentsController(archive);
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
                
                MAPaneController controller = loader.getController();
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

}