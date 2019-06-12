 package de.mpg.biochem.mars.fx.molecule;

import com.jfoenix.controls.JFXTabPane;

import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.mpg.biochem.mars.fx.molecule.imageMetaDataTab.ImageMetaDataTabController;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculesTabController;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.fx.util.*;


public class MoleculeArchiveFxFrameController {

	@FXML
	private BorderPane borderPane;
	
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
    
	private MenuBar menuBar;
	
	private ArrayList<MoleculeArchiveSubTab> tabPaneControllers;
	
	private DashboardTabController dashboardTabController;
	private ImageMetaDataTabController imageMetaDataTabController;
    private MoleculesTabController moleculesController;
    private CommentsTabController commentsController;
    private SettingsTabController settingsTabController; 
    
    private MoleculeArchive archive;

    private double tabWidth = 60.0;
    public static int lastSelectedTabIndex = 0;
    
    @FXML
    public void initialize() {
    	tabPaneControllers = new ArrayList<MoleculeArchiveSubTab>();
        configureView();
        buildMenuBar();
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
        
        configureDashBoardTab(dashboardTab, "Dashboard", MaterialIconFactory.get().createIcon(de.jensd.fx.glyphs.materialicons.MaterialIcon.DASHBOARD, "1.3em"), dashboardContainer, getClass().getResource("DashboardTab.fxml"), replaceBackgroundColorHandler);
        configureImageMetaDataTab(imageMetaDataTab, "ImageMetaData", microscopeIcon, imageMetaDataContainer, getClass().getResource("ImageMetaDataTab.fxml"), replaceBackgroundColorHandler);
        configureMoleculesTab(moleculesTab, "Molecules", moleculeIcon, moleculesContainer, replaceBackgroundColorHandler);
        configureCommentsTab(commentsTab, "Comments", bookIcon, commentsContainer, replaceBackgroundColorHandler);
        configureSettingsTab(settingsTab, "Settings", FontAwesomeIconFactory.get().createIcon(COG, "1.3em"), settingsContainer, replaceBackgroundColorHandler);
        
        dashboardTab.setStyle("-fx-background-color: -fx-focus-color;");
        
        tabContainer.getSelectionModel().selectedItemProperty().addListener(
        		new ChangeListener<Tab>() {

        			@Override
        			public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
        				if (newValue == dashboardTab) {
        					System.out.println("Dashboard");
        					updateMenus(dashboardTabController.getMenus());
        				} else if (newValue == imageMetaDataTab) {
        					System.out.println("ImageMetaData");
        				} else if (newValue == moleculesTab) {
        					System.out.println("Molecules");
        				} else if (newValue == commentsTab) {
        					System.out.println("Comments");
        				} else if (newValue == settingsTab) {
        					System.out.println("Settings");
        				}
        			}
        		});
    }
    
    private void buildMenuBar() {
		// File actions
		Action fileSaveAction = new Action("save", "Shortcut+S", FLOPPY_ALT, e -> save());
		Action fileSaveAsAction = new Action("Save As", null, null, e -> saveAs());
		Action fileCloseAction = new Action("close", null, null, e -> handleClose());
		
		Menu fileMenu = ActionUtils.createMenu("File",
				fileSaveAction,
				fileSaveAsAction,
				null,
				fileCloseAction);
		
		menuBar = new MenuBar(fileMenu);
		
		borderPane.setTop(menuBar);
    }
    
    public void updateMenus(ArrayList<Menu> menus) {
    	while (menuBar.getMenus().size() > 1)
    		menuBar.getMenus().remove(1);
    	if(menus.size() > 0) {
    		for (Menu menu : menus)
    			menuBar.getMenus().add(menu);
    	}
    }
    
    private void configureDashBoardTab(Tab tab, String title, Node icon, AnchorPane containerPane, URL resourceURL, EventHandler<Event> onSelectionChangedEvent) {
    	dashboardTabController = new DashboardTabController();
    	tabPaneControllers.add(dashboardTabController);
    	
    	buildTab(tab, icon, onSelectionChangedEvent);
        
        containerPane.getChildren().add(dashboardTabController.getNode());
        configureAnchorPane(dashboardTabController.getNode());
    }
    
    private void configureImageMetaDataTab(Tab tab, String title, Node icon, AnchorPane containerPane, URL resourceURL, EventHandler<Event> onSelectionChangedEvent) {
    	imageMetaDataTabController = new ImageMetaDataTabController();
    	tabPaneControllers.add(imageMetaDataTabController);
    	
    	buildTab(tab, icon, onSelectionChangedEvent);
        
        containerPane.getChildren().add(imageMetaDataTabController.getNode());
        configureAnchorPane(imageMetaDataTabController.getNode());
    }
    
    private void configureMoleculesTab(Tab tab, String title, Node icon, AnchorPane containerPane, EventHandler<Event> onSelectionChangedEvent) {
    	moleculesController = new MoleculesTabController();
    	tabPaneControllers.add(moleculesController);
    	
    	buildTab(tab, icon, onSelectionChangedEvent);
        
        containerPane.getChildren().add(moleculesController.getNode());
        configureAnchorPane(moleculesController.getNode());
    }
    
    private void configureCommentsTab(Tab tab, String title, Node icon, AnchorPane containerPane, EventHandler<Event> onSelectionChangedEvent) {
    	commentsController = new CommentsTabController();
    	tabPaneControllers.add(commentsController);
    	
    	buildTab(tab, icon, onSelectionChangedEvent);
        
        containerPane.getChildren().add(commentsController);
        configureAnchorPane(commentsController);
    }
    
    private void configureSettingsTab(Tab tab, String title, Node icon, AnchorPane containerPane, EventHandler<Event> onSelectionChangedEvent) {
    	settingsTabController = new SettingsTabController();
    	tabPaneControllers.add(settingsTabController);
    	
    	buildTab(tab, icon, onSelectionChangedEvent);
        
        containerPane.getChildren().add(settingsTabController.getNode());
        configureAnchorPane(settingsTabController.getNode());
    }
    
    private void buildTab(Tab tab, Node icon, EventHandler<Event> onSelectionChangedEvent) {
    	BorderPane tabPane = new BorderPane();
        tabPane.setRotate(90.0);
        tabPane.setMaxWidth(tabWidth);
        tabPane.setCenter(icon);

        tab.setText("");
        tab.setGraphic(tabPane);

        tab.setOnSelectionChanged(onSelectionChangedEvent);
    }
    
    private void configureAnchorPane(Node node) {
    	AnchorPane.setTopAnchor(node, 0.0);
        AnchorPane.setBottomAnchor(node, 0.0);
        AnchorPane.setRightAnchor(node, 0.0);
        AnchorPane.setLeftAnchor(node, 0.0);
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
    
    public void saveAs() {
    	
    }
    
    public void lockArchive() {
    	
    }
    
    public void unlockArchive() {
    	
    }
}