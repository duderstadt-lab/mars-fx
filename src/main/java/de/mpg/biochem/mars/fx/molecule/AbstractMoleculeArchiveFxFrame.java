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
	protected StackPane stackPane;
    protected JFXTabPane tabsContainer;
    
    protected boolean lockArchive = false;
    
	protected MenuBar menuBar;
	
	protected DashboardTab dashboardTab;
    protected CommentsTab commentsTab;
    protected SettingsTab settingsTab; 
    
    protected I imageMetadataTab;
    protected M moleculesTab;

    protected double tabWidth = 60.0;

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
    	
        buildMenuBar();
        buildTabs();
        
        stackPane = new StackPane();
        stackPane.getChildren().add(tabsContainer);
        borderPane.setCenter(stackPane);
        
        return new Scene(borderPane);
	}
	
	private void buildTabs() {
		dashboardTab = new DashboardTab();
        dashboardTab.setStyle("-fx-background-color: -fx-focus-color;");

        commentsTab = new CommentsTab();
        settingsTab = new SettingsTab();
        
        imageMetadataTab = createImageMetadataTab();
        moleculesTab = createMoleculesTab();

        tabsContainer.getSelectionModel().selectedItemProperty().addListener(
    		new ChangeListener<Tab>() {

    			@Override
    			public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
    				if (newValue == dashboardTab) {
    					if (dashboardTab.getMenus() == null)
    						System.out.println("dashboard");
    					updateMenus(dashboardTab.getMenus());
    				} else if (newValue == imageMetadataTab) {
    					if (imageMetadataTab.getMenus() == null)
    						System.out.println("imageMetadataTab");
    					updateMenus(imageMetadataTab.getMenus());
    				} else if (newValue == moleculesTab) {
    					if (moleculesTab.getMenus() == null)
    						System.out.println("moleculesTab");
    					updateMenus(moleculesTab.getMenus());
    				} else if (newValue == commentsTab) {
    					if (commentsTab.getMenus() == null)
    						System.out.println("commentsTab");
    					updateMenus(commentsTab.getMenus());
    				} else if (newValue == settingsTab) {
    					if (settingsTab.getMenus() == null)
    						System.out.println("settingsTab");
    					updateMenus(settingsTab.getMenus());
    				}
    			}
    		});
        
        tabsContainer.getTabs().add(dashboardTab);
        tabsContainer.getTabs().add((Tab)imageMetadataTab);
        tabsContainer.getTabs().add((Tab)moleculesTab);
        tabsContainer.getTabs().add(commentsTab);
        tabsContainer.getTabs().add(settingsTab);
        
        dashboardTab.setArchive(archive);
        imageMetadataTab.setArchive(archive);
        moleculesTab.setArchive(archive);
        commentsTab.setArchive(archive);
        settingsTab.setArchive(archive);
    }
	
	protected void buildMenuBar() {
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
	
	public MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> getArchive() {
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
    
    private void handleClose() {
    	archive.getWindow().close();
    	save();
    }
    
    public void updateAll() {
    	moleculesTab.update();
		imageMetadataTab.update();
    }
    
    public void save() {
    	 if (!lockArchive) {
    		 moleculesTab.saveCurrentRecord();
    		 imageMetadataTab.saveCurrentRecord();
        	 
        	 try {
	 			 if (archive.getFile() != null) {
	 				 if(archive.getFile().getName().equals(archive.getName())) {
	 				 	try {
							archive.save();
						} catch (IOException e1) {
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
				e1.printStackTrace();
			 }
 			updateAll();
    	 }
    }
    
    public void saveCopy() {
    	if (!lockArchive) {
    		moleculesTab.saveCurrentRecord();
    		imageMetadataTab.saveCurrentRecord();
    	    
    	    String fileName = archive.getName();
    	    if (fileName.endsWith(".store"))
    	    	fileName = fileName.substring(0, fileName.length() - 5);
    	    
    	    try {
 				if (archive.getFile() != null) {
					saveAs(new File(archive.getFile().getParentFile(), fileName));
 				} else {
 					saveAs(new File(System.getProperty("user.home"), fileName));
 				}
    	    } catch (IOException e1) {
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
    		moleculesTab.saveCurrentRecord();
    		imageMetadataTab.saveCurrentRecord();
 		 	
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
		return dashboardTab;
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
