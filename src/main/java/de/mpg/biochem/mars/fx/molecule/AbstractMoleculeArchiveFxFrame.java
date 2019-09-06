package de.mpg.biochem.mars.fx.molecule;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFrame;

import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import de.mpg.biochem.mars.table.MarsTable;
import de.mpg.biochem.mars.table.MarsTableService;
import ij.WindowManager;

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
import javafx.scene.control.TabPane.TabClosingPolicy;
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
import javafx.scene.control.SingleSelectionModel;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.commons.io.FilenameUtils;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;
import org.scijava.widget.FileWidget;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.molecule.MoleculeArchiveService;
import de.mpg.biochem.mars.fx.molecule.metadataTab.MetadataSubPane;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
import de.mpg.biochem.mars.fx.util.*;

import de.mpg.biochem.mars.molecule.*;

public abstract class AbstractMoleculeArchiveFxFrame<I extends MarsImageMetadataTab<? extends MetadataSubPane<? extends MarsImageMetadata>, ? extends MetadataSubPane<? extends MarsImageMetadata>>, 
		M extends MoleculesTab<? extends MoleculeSubPane<? extends Molecule>, ? extends MoleculeSubPane<? extends Molecule>>> {
	
	@Parameter
    protected MoleculeArchiveService moleculeArchiveService;
	
    @Parameter
    protected UIService uiService;

	protected MoleculeArchive<Molecule,MarsImageMetadata,MoleculeArchiveProperties> archive;
	
	protected JFrame frame;
	protected String title;
	protected JFXPanel fxPanel;

	protected BorderPane borderPane;
    protected JFXTabPane tabsContainer;

    protected Tab dashboardTab;
    protected AnchorPane dashboardContainer;
    
    protected Tab imageMetadataTab;
    protected AnchorPane imageMetadataContainer;
    
    protected Tab moleculesTab;
    protected AnchorPane moleculesContainer;
    
    protected Tab commentsTab;
    protected AnchorPane commentsContainer;

    protected Tab settingsTab;
    protected AnchorPane settingsContainer;
    
    protected boolean lockArchive = false;
    
	protected MenuBar menuBar;
	
	protected DashboardTab dashboardTabController;
    protected CommentsTab commentsTabController;
    protected SettingsTab settingsTabController; 
    
    protected I imageMetadataTabController;
    protected M moleculesTabController;

    protected double tabWidth = 60.0;
    public static int lastSelectedTabIndex = 0;

	public AbstractMoleculeArchiveFxFrame(MoleculeArchive<Molecule,MarsImageMetadata,MoleculeArchiveProperties> archive, MoleculeArchiveService moleculeArchiveService) {
		this.title = archive.getName();
		this.archive = archive;
		this.uiService = moleculeArchiveService.getUIService();
		this.moleculeArchiveService = moleculeArchiveService;
	}

	/**
	 * JFXPanel creates a link between Swing and JavaFX.
	 */
	public void init() {
		frame = new JFrame(title);
		
		frame.addWindowListener(new WindowAdapter() {
	         public void windowClosing(WindowEvent e) {
				close();
	         }
	    });
		
		this.fxPanel = new JFXPanel();
		frame.add(this.fxPanel);
		
		if (!uiService.isHeadless())
			WindowManager.addWindow(frame);
		
		// The call to runLater() avoid a mix between JavaFX thread and Swing thread.
		// Allows multiple runLaters in the same session...
		// Suggested here - https://stackoverflow.com/questions/29302837/javafx-platform-runlater-never-running
		Platform.setImplicitExit(false);
		
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				initFX(fxPanel);
			}
		});

	}

	public void initFX(JFXPanel fxPanel) {	
		this.fxPanel.setScene(buildScene());
		frame.setSize(800, 600);
		frame.setVisible(true);
	}
	
	protected Scene buildScene() {
		borderPane = new BorderPane();
    	borderPane.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/MoleculeArchiveFxFrame.css");
    	
    	tabsContainer = new JFXTabPane();
		tabsContainer.prefHeight(128.0);
		tabsContainer.prefWidth(308.0);
		tabsContainer.setSide(Side.LEFT);
		tabsContainer.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		tabsContainer.setTabMinWidth(tabWidth);
        tabsContainer.setTabMaxWidth(tabWidth);
        tabsContainer.setTabMinHeight(tabWidth);
        tabsContainer.setTabMaxHeight(tabWidth);
        tabsContainer.setRotateGraphic(true);
    	
        buildTabs();
        buildMenuBar();
        
        return new Scene(borderPane);
	}
	
	private void buildTabs() {
        EventHandler<Event> replaceBackgroundColorHandler = event -> {
            lastSelectedTabIndex = tabsContainer.getSelectionModel().getSelectedIndex();

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
        
        dashboardTabController = new DashboardTab();
        configureTab(dashboardTab, dashboardTabController, "Dashboard", MaterialIconFactory.get().createIcon(de.jensd.fx.glyphs.materialicons.MaterialIcon.DASHBOARD, "1.3em"), dashboardContainer, replaceBackgroundColorHandler);
        dashboardTab.setStyle("-fx-background-color: -fx-focus-color;");
        
        commentsTabController = new CommentsTab();
        configureTab(commentsTab, commentsTabController, "Comments", bookIcon, commentsContainer, replaceBackgroundColorHandler);

        settingsTabController = new SettingsTab();
        configureTab(settingsTab, settingsTabController, "Settings", FontAwesomeIconFactory.get().createIcon(COG, "1.3em"), settingsContainer, replaceBackgroundColorHandler);
        
        imageMetadataTabController = createImageMetadataTab();
        configureTab(imageMetadataTab, imageMetadataTabController, "ImageMetadata", microscopeIcon, imageMetadataContainer, replaceBackgroundColorHandler);
        
        moleculesTabController = createMoleculesTab();
        configureTab(moleculesTab, moleculesTabController, "Molecules", moleculeIcon, moleculesContainer, replaceBackgroundColorHandler);
        
        tabsContainer.getSelectionModel().selectedItemProperty().addListener(
    		new ChangeListener<Tab>() {

    			@Override
    			public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
    				if (newValue == dashboardTab) {
    					updateMenus(dashboardTabController.getMenus());
    				} else if (newValue == imageMetadataTab) {
    					updateMenus(imageMetadataTabController.getMenus());
    				} else if (newValue == moleculesTab) {
    					updateMenus(moleculesTabController.getMenus());
    				} else if (newValue == commentsTab) {
    					updateMenus(commentsTabController.getMenus());
    				} else if (newValue == settingsTab) {
    					updateMenus(settingsTabController.getMenus());
    				}
    			}
    		});
    }
	
	protected void buildMenuBar() {
		// File actions
		Action fileSaveAction = new Action("save", "Shortcut+S", FLOPPY_ALT, e -> save());
		Action fileSaveCopyAction = new Action("Save a Copy...", null, null, e -> saveCopy());
		Action fileSaveVirtualStoreAction = new Action("Save a Virtual Store Copy...", null, null, e -> saveVirtualStoreCopy());
		Action fileCloseAction = new Action("close", null, null, e -> handleClose());
		
		Menu fileMenu = ActionUtils.createMenu("File",
				fileSaveAction,
				fileSaveCopyAction,
				fileSaveVirtualStoreAction,
				null,
				fileCloseAction);
		
		menuBar = new MenuBar(fileMenu);
		
		borderPane.setTop(menuBar);
	}
	
	public MoleculeArchive<Molecule,MarsImageMetadata,MoleculeArchiveProperties> getArchive() {
		return archive;
	}
	
	public JFrame getFrame() {
		return frame;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void close() {
		moleculeArchiveService.removeArchive(archive.getName());

		if (!uiService.isHeadless())
			WindowManager.removeWindow(frame);
		
		frame.setVisible(false);
		frame.dispose();
	}

	public void updateMenus(ArrayList<Menu> menus) {
    	while (menuBar.getMenus().size() > 1)
    		menuBar.getMenus().remove(1);
    	if(menus.size() > 0) {
    		for (Menu menu : menus)
    			menuBar.getMenus().add(menu);
    	}
    }
    
    private void configureTab(Tab tab, MoleculeArchiveTab controller, String title, Node icon, AnchorPane containerPane, EventHandler<Event> onSelectionChangedEvent) {
    	BorderPane tabPane = new BorderPane();
        tabPane.setRotate(90.0);
        tabPane.setMaxWidth(tabWidth);
        tabPane.setCenter(icon);
        
        tab = new Tab();
        tab.setText("");
        tab.setGraphic(tabPane);
        tab.setOnSelectionChanged(onSelectionChangedEvent);
        tab.closableProperty().set(false);
        
        Node node = controller.getNode();
        
        containerPane = new AnchorPane();
        containerPane.minHeight(0);
        containerPane.minWidth(0);
        containerPane.prefHeight(180.0);
        containerPane.prefWidth(200.0);
        containerPane.getChildren().add(node);
        
        AnchorPane.setTopAnchor(node, 0.0);
        AnchorPane.setBottomAnchor(node, 0.0);
        AnchorPane.setRightAnchor(node, 0.0);
        AnchorPane.setLeftAnchor(node, 0.0);
    }
    
    public Node getNode() {
    	return borderPane;
    }
    
    private void handleClose() {
    	archive.getWindow().close();
    	save();
    }
    
    public void updateAll() {
    	moleculesTabController.update();
		imageMetadataTabController.update();
    }
    
    public void save() {
    	 if (!lockArchive) {
    		 moleculesTabController.saveCurrentRecord();
    		 imageMetadataTabController.saveCurrentRecord();
        	 
        	 try {
	 			 if (archive.getFile() != null) {
	 				 if(archive.getFile().getName().equals(archive.getName())) {
	 				 	try {
							archive.save();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
	 				 } else {
	 					 //the archive name has changed... so let's check with the user about the new name...
						saveAs(archive.getFile());
	 				 }
	 			 } else {
	 				saveAs(new File(archive.getName()));
	 			 }
        	 } catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			 }
 			updateAll();
    	 }
    }
    
    public void saveCopy() {
    	if (!lockArchive) {
    		moleculesTabController.saveCurrentRecord();
    		imageMetadataTabController.saveCurrentRecord();
    	    
    	    String fileName = archive.getName();
    	    if (fileName.endsWith(".store"))
    	    	fileName = fileName.substring(0, fileName.length() - 5);
    	    
    	    System.out.println("fN" + fileName);
    	    System.out.println("AP" + archive.getFile().getParentFile().toString());
    	    
    	    try {
 				if (archive.getFile() != null) {
					saveAs(new File(archive.getFile().getParentFile(), fileName));
 				} else {
 					saveAs(new File(System.getProperty("user.home"), fileName));
 				}
    	    } catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
				updateAll();
    	}
    }
    
	private boolean saveAs(File saveAsFile) throws IOException {
		FileChooser fileChooser = new FileChooser();
		
		if (saveAsFile == null) {
			saveAsFile = new File(System.getProperty("user.home"));
		}
		fileChooser.setInitialDirectory(saveAsFile.getParentFile());
		fileChooser.setInitialFileName(saveAsFile.getName());
		//FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(exporter.getExtensionDescription(),
		//		exporter.getExtensionFilters());
		//fileChooser.getExtensionFilters().add(extFilter);

		File file = fileChooser.showSaveDialog(this.tabsContainer.getScene().getWindow());
		
		if (file != null) {
			archive.saveAs(file);
			return true;
		}
		return false;
	}
    
    public void saveVirtualStoreCopy() {
    	 if (!lockArchive) {
    		moleculesTabController.saveCurrentRecord();
    		imageMetadataTabController.saveCurrentRecord();
 		 	
 		 	String name = archive.getName();
 		 	
 		 	if (name.endsWith(".yama")) {
 		 		name += ".store";
 		 	} else if (!name.endsWith(".yama.store")) {
     		 	name += ".yama.store";
     		}
 		 
			try {
				saveAsVirtualStore(new File(name));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    	 }
    }
    
	private void saveAsVirtualStore(File saveAsFile) throws IOException {
		FileChooser fileChooser = new FileChooser();
		
		if (saveAsFile == null) {
			saveAsFile = new File(System.getProperty("user.home"));
		}
		fileChooser.setInitialDirectory(saveAsFile.getParentFile());
		fileChooser.setInitialFileName(saveAsFile.getName());
		//FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(exporter.getExtensionDescription(),
		//		exporter.getExtensionFilters());
		//fileChooser.getExtensionFilters().add(extFilter);

		File virtualDirectory = fileChooser.showSaveDialog(this.tabsContainer.getScene().getWindow());
		
		if (virtualDirectory != null) {	
			archive.saveAsVirtualStore(virtualDirectory);
		}
	}
	
	public abstract I createImageMetadataTab();
	
	public abstract M createMoleculesTab();
	
	public DashboardTab getDashboard() {
		return dashboardTabController;
	}
	
    public void lockArchive() {
    	lockArchive = true;
		//We move to the dashboard Tab
    	tabsContainer.getSelectionModel().select(0);
    }
    
    public void unlockArchive() {
    	updateAll();
		lockArchive = false;
    }

}
