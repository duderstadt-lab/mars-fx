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
import de.mpg.biochem.mars.table.MARSResultsTable;
import de.mpg.biochem.mars.table.ResultsTableService;
import ij.WindowManager;

import de.mpg.biochem.mars.molecule.*;

public class MoleculeArchiveFrame {

	private static final long serialVersionUID = 1L;
	
	@Parameter
    private MoleculeArchiveService moleculeArchiveService;
	
    @Parameter
    private UIService uiService;

	private MoleculeArchive archive;
	
	private MoleculeArchiveFrameController controller;
	
	private JFrame frame;
	private String title;

	private JFXPanel fxPanel;

	public MoleculeArchiveFrame(MoleculeArchive archive, MoleculeArchiveService moleculeArchiveService) {
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
					//try {
						//if (archive.isLocked())
						close();
					//} catch (IOException e1) {
						// TODO Auto-generated catch block
					//	e1.printStackTrace();
					//}
	         }
	      });
		
		this.fxPanel = new JFXPanel();
		frame.add(this.fxPanel);
		frame.setVisible(true);
		
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
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MoleculeArchiveFrameController.class.getResource("MoleculeArchiveFrameLayout.fxml"));
			BorderPane root = (BorderPane) loader.load();
			
			Scene scene = new Scene(root);
			this.fxPanel.setScene(scene);
			frame.setSize(600, 600);
			
			controller = loader.getController();
            controller.setArchive(archive);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	public MoleculeArchive getArchive() {
		return archive;
	}
	
	public MoleculeArchiveFrameController getController() {
		return controller;
	}
	
	public JFrame getFrame() {
		return frame;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void close() {
		moleculeArchiveService.removeArchive(archive.getName());
		
		//TODO make sure active archive contents is saved on close !!!!
		/*
		if (archive.isVirtual()) {
			imageMetaDataPanel.saveCurrentRecord();
			moleculePanel.saveCurrentRecord();
			archive.save();
		}
		*/

		if (!uiService.isHeadless())
			WindowManager.removeWindow(frame);
		
		frame.setVisible(false);
		frame.dispose();
	}
}