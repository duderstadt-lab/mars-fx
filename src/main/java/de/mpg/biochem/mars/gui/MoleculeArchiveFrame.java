package de.mpg.biochem.mars.gui;

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
import de.mpg.biochem.mars.gui.view.*;
import de.mpg.biochem.mars.table.MARSResultsTable;
import de.mpg.biochem.mars.table.ResultsTableService;
import ij.WindowManager;

import de.mpg.biochem.mars.molecule.*;

public class MoleculeArchiveFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	@Parameter
	private LogService log;
	
	@Parameter
    private ResultsTableService resultsTableService;
	
    @Parameter
    private UIService uiService;

	private MoleculeArchive archive;

	private JFXPanel fxPanel;

	public MoleculeArchiveFrame(MoleculeArchive archive, MoleculeArchiveService moleculeArchiveService) {
		//ij.context().inject(this);
		//this.ij = ij;
		this.archive = archive;
		this.uiService = moleculeArchiveService.getUIService();
		//table.setWindow(this);
		
		// add window to window manager
		// IJ1 style IJ2 doesn't seem to work...
		if (!uiService.isHeadless())
			WindowManager.addWindow(this);
	}

	/**
	 * JFXPanel creates a link between Swing and JavaFX.
	 */
	public void init() {
		//TODO add PlotPanel for JFXPanel..
		
		this.fxPanel = new JFXPanel();
		this.add(this.fxPanel);
		this.setVisible(true);

		// The call to runLater() avoid a mix between JavaFX thread and Swing thread.
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
			loader.setLocation(MAFrameController.class.getResource("MAFrameLayout.fxml"));
			StackPane root = (StackPane) loader.load();
			
			Scene scene = new Scene(root);
			this.fxPanel.setScene(scene);
			this.setSize(600, 600);
			
			MAFrameController controller = loader.getController();
            controller.setArchive(archive);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	public MoleculeArchive getArchive() {
		return archive;
	}
}