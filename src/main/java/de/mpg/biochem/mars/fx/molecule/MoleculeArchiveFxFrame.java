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

import de.mpg.biochem.mars.molecule.*;

public class MoleculeArchiveFxFrame {

	private static final long serialVersionUID = 1L;
	
	@Parameter
    private MoleculeArchiveService moleculeArchiveService;
	
    @Parameter
    private UIService uiService;

	private MoleculeArchive<Molecule,MarsImageMetadata,MoleculeArchiveProperties> archive;
	
	private MoleculeArchiveFxTabs controller;
	
	private JFrame frame;
	private String title;

	private JFXPanel fxPanel;

	public MoleculeArchiveFxFrame(MoleculeArchive<Molecule,MarsImageMetadata,MoleculeArchiveProperties> archive, MoleculeArchiveService moleculeArchiveService) {
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
		MoleculeArchiveFxTabs moleculeArchiveFxTabs = new MoleculeArchiveFxTabs(archive);
		Scene scene = new Scene((BorderPane) moleculeArchiveFxTabs.getNode());
		this.fxPanel.setScene(scene);
		frame.setSize(800, 600);
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
}