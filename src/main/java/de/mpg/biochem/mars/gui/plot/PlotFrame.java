package de.mpg.biochem.mars.gui.plot;

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

	//private final ImageJ ij;

	private JFXPanel fxPanel;
	
	private PlotPane plot;

	public PlotFrame() {
		//ij.context().inject(this);
		//this.ij = ij;
	}

	public void init() {
		//TODO add PlotPanel for JFXPanel..
		
		this.fxPanel = new JFXPanel();
		this.add(this.fxPanel);
		this.setVisible(true);

		//Scene scene = new Scene(new LargeDataSetsSample());
		plot = new PlotPane();
		Scene scene = new Scene(plot);
		this.fxPanel.setScene(scene);
		this.setSize(1000, 600);

	}
	
	public PlotPane getPlot() {
		return plot;
	}
}
