package de.mpg.biochem.mars.fx.molecule;

import static java.util.stream.Collectors.toList;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.prefs.PrefService;
import org.scijava.ui.UIService;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import ij.WindowManager;
import ij.gui.GenericDialog;

import com.jfoenix.controls.JFXTabPane;

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
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.stage.FileChooser;

import javafx.concurrent.Task;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.controlsfx.control.MaskerPane;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.mpg.biochem.mars.fx.dialogs.PropertySelectionDialog;
import de.mpg.biochem.mars.fx.event.InitializeMoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MetadataSelectionChangedEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEventHandler;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveLockEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveSavedEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveSavingEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveUnlockEvent;
import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.event.MoleculeSelectionChangedEvent;
import de.mpg.biochem.mars.fx.event.RefreshMetadataEvent;
import de.mpg.biochem.mars.fx.event.RefreshMoleculeEvent;
import de.mpg.biochem.mars.fx.event.RefreshMoleculePropertiesEvent;
import de.mpg.biochem.mars.fx.molecule.metadataTab.MetadataSubPane;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MarsBdvFrame;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
import de.mpg.biochem.mars.fx.plot.event.NewMetadataRegionEvent;
import de.mpg.biochem.mars.fx.plot.event.NewMoleculeRegionEvent;
import de.mpg.biochem.mars.fx.plot.event.PlotEvent;
import de.mpg.biochem.mars.fx.plot.event.UpdatePlotAreaEvent;
import de.mpg.biochem.mars.fx.util.*;

import de.mpg.biochem.mars.molecule.*;
import de.mpg.biochem.mars.table.MarsTable;

public abstract class AbstractMoleculeArchiveFxFrame<I extends MarsImageMetadataTab<? extends MetadataSubPane, ? extends MetadataSubPane>, 
		M extends MoleculesTab<? extends MoleculeSubPane, ? extends MoleculeSubPane>> implements MoleculeArchiveWindow {
	
	@Parameter
    protected MoleculeArchiveService moleculeArchiveService;
	
    @Parameter
    protected UIService uiService;
    
    @Parameter
    protected PrefService prefService;

	protected MoleculeArchive<Molecule,MarsImageMetadata,MoleculeArchiveProperties> archive;
	
	protected JFrame frame;
	protected String title;
	protected JFXPanel fxPanel;

	protected StackPane maskerStackPane;
	protected MaskerPane masker;
	
	protected BorderPane borderPane;
    protected JFXTabPane tabsContainer;
    
	protected MenuBar menuBar;
	
	protected DashboardTab dashboardTab;
    protected CommentsTab commentsTab; 
    
    protected I imageMetadataTab;
    protected M moleculesTab;
    protected SettingsTab settingsTab;
    
    protected MarsBdvFrame<?> marsBdvFrame;

    protected double tabWidth = 60.0;
    
    protected Menu fileMenu, toolsMenu;

	public AbstractMoleculeArchiveFxFrame(MoleculeArchive<Molecule,MarsImageMetadata,MoleculeArchiveProperties> archive, MoleculeArchiveService moleculeArchiveService) {
		this.title = archive.getName();
		this.archive = archive;
		this.uiService = moleculeArchiveService.getUIService();
		this.prefService = moleculeArchiveService.getPrefService();
		this.moleculeArchiveService = moleculeArchiveService;
		
		archive.setWindow(this);
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
		Scene scene = buildScene();
		this.fxPanel.setScene(scene);
		
		frame.setSize(800, 600);
		frame.setVisible(true);
	}
	
	protected Scene buildScene() {
		borderPane = new BorderPane();
    	
    	masker = new MaskerPane();
    	masker.setVisible(false);
    	
    	maskerStackPane = new StackPane();
    	maskerStackPane.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/MoleculeArchiveFxFrame.css");
    	maskerStackPane.getChildren().add(borderPane);
    	maskerStackPane.getChildren().add(masker);
    	
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
        
        borderPane.setCenter(tabsContainer);
        Scene scene = new Scene(maskerStackPane);

        return scene;
	}
	
	protected void buildTabs() {
		dashboardTab = new DashboardTab();
        dashboardTab.setStyle("-fx-background-color: -fx-focus-color;");

        commentsTab = new CommentsTab();
        settingsTab = new SettingsTab(prefService);
        
        imageMetadataTab = createImageMetadataTab();
        moleculesTab = createMoleculesTab();

        //fire save events for tabs as they are left and update events for new tabs
        tabsContainer.getSelectionModel().selectedItemProperty().addListener(
    		new ChangeListener<Tab>() {
    			@Override
    			public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
    				updateMenus(((MoleculeArchiveTab)newValue).getMenus());
    				if (oldValue == commentsTab) {
    					commentsTab.saveComments();
    				} else if (oldValue == imageMetadataTab) {
    					imageMetadataTab.saveCurrentRecord();
    				} else if (oldValue == moleculesTab) {
    					moleculesTab.saveCurrentRecord();
    				} else if (oldValue == settingsTab) {
    					settingsTab.save();
    					
    					//Update global accelerators...
    					for (HotKeyEntry hotKeyEntry : settingsTab.getHotKeyList()) {
    						Runnable rn = ()-> {
    							if (tabsContainer.getSelectionModel().getSelectedItem() == moleculesTab) {
   		                	 		moleculesTab.getSelectedMolecule().addTag(hotKeyEntry.getTag());
   		                	 		moleculesTab.fireEvent(new RefreshMoleculePropertiesEvent());
    							}
   		                 	};
   		                 	getNode().getScene().getAccelerators().put(hotKeyEntry.getKeyCombination(), rn);
    					}
    				}
    				
	    			if (newValue == imageMetadataTab) {
						imageMetadataTab.fireEvent(new RefreshMetadataEvent());
					} else if (newValue == moleculesTab) {
						moleculesTab.fireEvent(new RefreshMoleculeEvent());
					}
    			}
    		});
        
        tabsContainer.getTabs().add(dashboardTab);
        tabsContainer.getTabs().add((Tab)imageMetadataTab);
        tabsContainer.getTabs().add((Tab)moleculesTab);
        tabsContainer.getTabs().add(commentsTab);
        tabsContainer.getTabs().add(settingsTab);
        
        fireEvent(new InitializeMoleculeArchiveEvent(archive));
    }
	
	protected void buildMenuBar() {
		//Build file menu
		Action fileSaveAction = new Action("save", "Shortcut+S", FLOPPY_ALT, e -> save());
		Action fileSaveCopyAction = new Action("Save a Copy...", null, null, e -> saveCopy());
		Action fileSaveVirtualStoreAction = new Action("Save a Virtual Store Copy...", null, null, e -> saveVirtualStoreCopy());
		Action fileCloseAction = new Action("close", null, null, e -> close());
		
		fileMenu = ActionUtils.createMenu("File",
				fileSaveAction,
				fileSaveCopyAction,
				fileSaveVirtualStoreAction,
				null,
				fileCloseAction);
		
		//Build tools menu
		Action showVideoAction = new Action("Show Video", null, null,
				e -> {
			        SwingUtilities.invokeLater(new Runnable() {
			            @Override
			            public void run() {
			            	GenericDialog dialog = new GenericDialog("Mars Bdv view");
			     			dialog.addStringField("x_parameter", "roi_x", 25);
			     			dialog.addStringField("y_parameter", "roi_y", 25);
			          		dialog.showDialog();
			          		
			          		if (dialog.wasCanceled())
			          			return;
			          		
			          		String xParameter = dialog.getNextString();
			          		String yParameter = dialog.getNextString();
			          		
			            	if (archive != null && moleculesTab.getSelectedMolecule() != null) {
			            		marsBdvFrame = new MarsBdvFrame(archive, moleculesTab.getSelectedMolecule(), xParameter, yParameter);
			            		marsBdvFrame.getFrame().addWindowListener(new java.awt.event.WindowAdapter() {
			            		    @Override
			            		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
			            		        marsBdvFrame = null;
			            		    }
			            		});
			            		moleculesTab.setMarsBdvFrame(marsBdvFrame);
			            	}
			            }
			        });
				}); 
		
		Action deleteMoleculesAction = new Action("Delete Molecules", null, null, e -> deleteMolecules());
		Action deleteMoleculeTagsAction = new Action("Delete Molecule Tags", null, null, e -> deleteMoleculeTags());
		Action deleteMoleculeParametersAction = new Action("Delete Molecule Parameters", null, null, e -> deleteMoleculeParameters());
			
		Action mergeMoleculesAction = new Action("Merge Molecules", null, null, e -> mergeMolecules());
		
		Action rebuildIndexesAction = new Action("Rebuild Indexes", null, null,
			e -> {
				runTask(() -> {
    	            	try {
    						archive.rebuildIndexes();
    					} catch (IOException e1) {
    						e1.printStackTrace();
    					}
    	            }, "Rebuilding Indexes...");					
			});
			
		toolsMenu = ActionUtils.createMenu("Tools",
					deleteMoleculesAction,
					deleteMoleculeTagsAction,
					deleteMoleculeParametersAction,
					mergeMoleculesAction,
					null,
					showVideoAction,
					null,
					rebuildIndexesAction);
		
		menuBar = new MenuBar(fileMenu, toolsMenu);
		borderPane.setTop(menuBar);
	}
	
	protected void deleteMolecules() {
		PropertySelectionDialog dialog = new PropertySelectionDialog(getNode().getScene().getWindow(), 
				archive.getProperties().getTagSet(), "Delete Molecules", "Delete molecules with tags:", "Delete molecules with no tags");

		dialog.showAndWait().ifPresent(result -> {
			runTask(() -> {
				ArrayList<String> deleteUIDs = (ArrayList<String>)archive.getMoleculeUIDs().stream().filter(UID -> {
	        	 	if (result.removeAll() && archive.get(UID).getTags().size() == 0) {
	        	 		return true;
	        	 	}
	        	 
	 				boolean hasTag = false;
	 				List<String> tagList = result.getList();
	 				for (int i=0; i<tagList.size(); i++) {
	 		        	for (String tag : archive.get(UID).getTags()) {
	 		        		if (tagList.get(i).equals(tag)) {
	 		        			hasTag = true;
	 		        		}
	 		        	}
	 		        }
	 				return hasTag;
	 			}).collect(toList());
	             
	             for (String UID : deleteUIDs) {
	            	 archive.remove(UID);
	             }
			}, "Deleting Molecules...");
		});
	}
	
	protected void deleteMoleculeTags() {
		PropertySelectionDialog dialog = new PropertySelectionDialog(getNode().getScene().getWindow(), 
				archive.getProperties().getTagSet(), "Delete Molecule Tags", "Delete molecule tags:", "Delete all tags");

		dialog.showAndWait().ifPresent(result -> {
			runTask(() -> {
				List<String> tagList = result.getList();
	            archive.getMoleculeUIDs().parallelStream().forEach(UID -> {
	            		Molecule molecule = archive.get(UID);
	            	 	if (result.removeAll()) {
	            	 		molecule.removeAllTags();
	            	 	} else {
	     		        	for (int i=0;i<tagList.size();i++) {
	     		        		molecule.removeTag(tagList.get(i));
	     		        	}
	            	 	}
	            	 	archive.put(molecule);
	     			});
			}, "Deleting Molecule Tags...");
		});
	}
	
	protected void deleteMoleculeParameters() {
		PropertySelectionDialog dialog = new PropertySelectionDialog(getNode().getScene().getWindow(), 
				archive.getProperties().getParameterSet(), "Delete Molecule Parameters", "Delete molecule parameters:", "Delete all parameters");

		dialog.showAndWait().ifPresent(result -> {
			runTask(() -> {
				List<String> parameterList = result.getList();
	            archive.getMoleculeUIDs().parallelStream().forEach(UID -> {
	            		Molecule molecule = archive.get(UID);
	            	 	if (result.removeAll()) {
	            	 		molecule.removeAllParameters();
	            	 	} else {
	            	 		for (int i=0;i<parameterList.size();i++) {
	     		        		molecule.removeTag(parameterList.get(i));
	     		        	}
	            	 	}
	            	 	archive.put(molecule);
	     			});
			}, "Deleting Molecule Parameters...");
		});
	}
	
	protected void mergeMolecules() {
		PropertySelectionDialog dialog = new PropertySelectionDialog(getNode().getScene().getWindow(), 
				archive.getProperties().getTagSet(), "Merge Molecules", "Merge molecules with tag:");

		dialog.showAndWait().ifPresent(result -> {
			runTask(() -> {
				String tag = result.getList().get(0);
	     		 
	     		ArrayList<String> mergeUIDs = (ArrayList<String>)archive.getMoleculeUIDs().stream().filter(UID -> archive.moleculeHasTag(UID, tag)).collect(toList());
             
	     		if (mergeUIDs.size() < 2) 
	     			return;
	     		
	     		String mergeNote = "Merged " + mergeUIDs.size() + " molecules \n";
	     		
	     		MarsTable mergedDataTable = archive.get(mergeUIDs.get(0)).getDataTable();
	     		
	     		HashSet<Double> sliceNumbers = new HashSet<Double>();
	     		
	     		//First add all current slices to set
	     		for (int row=0;row<mergedDataTable.getRowCount();row++) {
            		sliceNumbers.add(mergedDataTable.getValue("slice", row));
            	}
	     		
	     		mergeNote += mergeUIDs.get(0).substring(0, 5) + " : slices " + mergedDataTable.getValue("slice", 0) + " " + mergedDataTable.getValue("slice", mergedDataTable.getRowCount()-1) + "\n";
	     		
	            for (int i = 1; i < mergeUIDs.size() ; i++) {
	            	MarsTable nextDataTable = archive.get(mergeUIDs.get(i)).getDataTable();
	            	
	            	for (int row=0;row<nextDataTable.getRowCount();row++) {
	            		if (!sliceNumbers.contains(nextDataTable.getValue("slice", row))) {
	            			mergedDataTable.appendRow();
	            			int mergeLastRow = mergedDataTable.getRowCount() - 1;
	            			
	            			for (int col=0;col<mergedDataTable.getColumnCount();col++) {
	            				String column = mergedDataTable.getColumnHeader(col);
	    	            		mergedDataTable.setValue(column, mergeLastRow, nextDataTable.getValue(column, row));
	    	            	}
	            			
	            			sliceNumbers.add(nextDataTable.getValue("slice", row));
	            		}
	            	}
	            	mergeNote += mergeUIDs.get(i).substring(0, 5) + " : slices " + nextDataTable.getValue("slice", 0) + " " + nextDataTable.getValue("slice", nextDataTable.getRowCount()-1) + "\n";
	            	
	            	archive.remove(mergeUIDs.get(i));
	            }
	            
	            //sort by slice
	            mergedDataTable.sort(true, "slice");
	            
	            archive.get(mergeUIDs.get(0)).setNotes(archive.get(mergeUIDs.get(0)).getNotes() + "\n" + mergeNote);
			}, "Merging Molecules...");
		});
	}
	
	protected void runTask(Runnable process, String message) {
		masker.setText(message);
		masker.setProgress(-1);
		masker.setVisible(true);
    	fireEvent(new MoleculeArchiveLockEvent(archive));
		Task<Void> task = new Task<Void>() {
            @Override
            public Void call() throws Exception {
            	process.run();
                return null;
            }
        };

        task.setOnSucceeded(event -> { 
        	fireEvent(new MoleculeArchiveUnlockEvent(archive));
			masker.setVisible(false);
			System.out.println(archive.getNumberOfMolecules());
        });

        new Thread(task).start();
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
    	if(menus != null && menus.size() > 0) {
    		for (Menu menu : menus)
    			menuBar.getMenus().add(menu);
    	}
    	menuBar.getMenus().add(toolsMenu);
    }

    public void save() {
	   	 try {
			 if (archive.getFile() != null) {
				 if(archive.getFile().getName().equals(archive.getName())) {
					masker.setText("Saving...");
					masker.setProgress(-1);
					masker.setVisible(true);
			    	fireEvent(new MoleculeArchiveLockEvent(archive));
	    		    fireEvent(new MoleculeArchiveSavingEvent(archive));
		    		Task<Void> task = new Task<Void>() {
	     	            @Override
	     	            public Void call() throws Exception {
	     	            	archive.save();	 
	     	                return null;
	     	            }
	     	        };
	
	     	        task.setOnSucceeded(event -> {
			           	fireEvent(new MoleculeArchiveSavedEvent(archive));
			           	fireEvent(new MoleculeArchiveUnlockEvent(archive));
						masker.setVisible(false);
	     	        });
	
	     	        new Thread(task).run();
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
    }
    
    public void saveCopy() {
		lock();
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
	    unlock();
    }
    
	private boolean saveAs(File saveAsFile) throws IOException {
		FileChooser fileChooser = new FileChooser();
		
		if (saveAsFile == null) {
			saveAsFile = new File(System.getProperty("user.home"));
		}
		fileChooser.setInitialDirectory(saveAsFile.getParentFile());
		fileChooser.setInitialFileName(saveAsFile.getName());

		File file = fileChooser.showSaveDialog(this.tabsContainer.getScene().getWindow());
		
		if (file != null) {
			fireEvent(new MoleculeArchiveSavingEvent(archive));
			archive.saveAs(file);
			fireEvent(new MoleculeArchiveSavedEvent(archive));
			return true;
		}
		return false;
	}
    
    public void saveVirtualStoreCopy() {
	 	String name = archive.getName();
	 	
	 	if (name.endsWith(".yama")) {
	 		name += ".store";
	 	} else if (!name.endsWith(".yama.store")) {
 		 	name += ".yama.store";
 		}
	 
		try {
			saveAsVirtualStore(new File(name));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    }
    
	private void saveAsVirtualStore(File saveAsFile) throws IOException {
		FileChooser fileChooser = new FileChooser();
		
		if (saveAsFile == null) {
			saveAsFile = new File(System.getProperty("user.home"));
		}
		fileChooser.setInitialDirectory(saveAsFile.getParentFile());
		fileChooser.setInitialFileName(saveAsFile.getName());

		File virtualDirectory = fileChooser.showSaveDialog(this.tabsContainer.getScene().getWindow());
		
		if (virtualDirectory != null) {	
			lock();
			fireEvent(new MoleculeArchiveSavingEvent(archive));
			archive.saveAsVirtualStore(virtualDirectory);
			fireEvent(new MoleculeArchiveSavedEvent(archive));
			unlock();
		}
	}
	
	public Node getNode() {
		return maskerStackPane;
	}
	
	public abstract I createImageMetadataTab();
	
	public abstract M createMoleculesTab();
	
	//public DashboardTab getDashboard() {
	//	return dashboardTab;
	//}
	
	//Lock, unlock and update event might be called by swing threads
	//so we use Platform.runLater to ensure they are executed on 
	//the javafx thread.
	
	public void lock(String message) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				masker.setText(message);
				masker.setProgress(-1);
				masker.setVisible(true);
		    	fireEvent(new MoleculeArchiveLockEvent(archive));
			}
    	});
	}
	
    public void lock() {
    	Platform.runLater(new Runnable() {
			@Override
			public void run() {
				masker.setProgress(-1);
				masker.setVisible(true);
		    	fireEvent(new MoleculeArchiveLockEvent(archive));
			}
    	});
    }
    
    public void updateLockMessage(String message) {
    	Platform.runLater(new Runnable() {
			@Override
			public void run() {
				masker.setText(message);
			}
    	});
    }
    
    //Not really ideal since a Task and updateProgress would be the best
    //But this is the only way for direct interaction through swing threads.
    public void progress(double progress) {
    	Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (masker.isVisible()) {
					masker.setProgress(progress);
				}
			}
    	});
    }
    
    public void unlock() {
    	Platform.runLater(new Runnable() {
			@Override
			public void run() {
		    	fireEvent(new MoleculeArchiveUnlockEvent(archive));
				masker.setVisible(false);
			}
    	});
    }
    
    public void update() {
    	unlock();
    }

    public void fireEvent(Event event) {
    	dashboardTab.fireEvent(event);
        imageMetadataTab.fireEvent(event);
        moleculesTab.fireEvent(event);
        commentsTab.fireEvent(event);
        settingsTab.fireEvent(event);
    }
}
