package de.mpg.biochem.mars.gui;

import java.io.IOException;

import javax.swing.JFrame;

import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import java.util.HashMap;
import java.util.Map;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import net.imagej.ImageJ;
import net.imagej.display.ImageDisplay;

import cern.extjfx.samples.chart.*;

public class PlotFrame extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Parameter
	private LogService log;

	private final ImageJ ij;

	private JFXPanel fxPanel;

	public PlotFrame(ImageJ ij) {
		ij.context().inject(this);
		this.ij = ij;
	}

	/**
	 * Create the JFXPanel that make the link between Swing (IJ) and JavaFX plugin.
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
		//Scene scene = new Scene(new LargeDataSetsSample());
		Scene scene = new Scene(new Plot());
		
		this.fxPanel.setScene(scene);
		this.setSize(1000, 600);
		
		/*
		// Init the root layout
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainAppFrame.class.getResource("/sc/fiji/omevisual/gui/view/RootLayout.fxml"));
			AnchorPane rootLayout = (AnchorPane) loader.load();

			// Get the controller and add an ImageJ context to it.
			RootLayoutController controller = loader.getController();
			controller.setContext(ij.context());
			controller.setImage(this.image);

			// Show the scene containing the root layout.
			Scene scene = new Scene(rootLayout);
			this.fxPanel.setScene(scene);

			// Resize the JFrame to the JavaFX scene
			this.setSize((int) scene.getWidth(), (int) scene.getHeight());

			controller.fill(md);

		} catch (IOException e) {
			e.printStackTrace();
		} 
		*/
	}
}
