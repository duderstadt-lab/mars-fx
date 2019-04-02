package de.mpg.biochem.mars.gui;

import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

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
import net.imagej.ImageJ;
import cern.extjfx.samples.chart.*;

@Plugin(type = Command.class, label = "Test GUI", menu = {
		@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT,
				mnemonic = MenuConstants.PLUGINS_MNEMONIC),
		@Menu(label = "MoleculeArchive Suite", weight = MenuConstants.PLUGINS_WEIGHT,
			mnemonic = 's'),
		@Menu(label = "JavaFX", weight = 20,
			mnemonic = 'i'),
		@Menu(label = "Test GUI", weight = 10, mnemonic = 'g')})
public class TestFxChart extends DynamicCommand implements Command {
	
	@Parameter
	private ImageJ ij;
	
	@Override
	public void run() {
		// Launch JavaFX interface
		JavaFxFrame app = new JavaFxFrame(ij);
		app.setTitle("JavaFX");
		app.init();
	}
}
