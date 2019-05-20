package de.mpg.biochem.mars.gui.molecule.moleculesTab;

import java.awt.Color;
import java.util.ArrayList;

import de.mpg.biochem.mars.gui.options.Options;
import de.mpg.biochem.mars.gui.plot.PlotProperties;
import javafx.scene.control.ChoiceBox;

import org.tbee.javafx.scene.layout.fxml.MigPane;

public class DatasetOptionsPane extends MigPane {

	ArrayList<PlotProperties> plotPropertiesList = new ArrayList<PlotProperties>();

	public DatasetOptionsPane() {
		setLayout("insets dialog");

		//for (PlotProperties props : plotPropertiesList) {
			

			//add(ext.toggleSwitch, "grow, wrap");
		//}
	}
	
	private class PlotSettings {
		private ChoiceBox<String> xColumnField, yColumnField, typeField;
		private ChoiceBox<Color> colorField, segmentField;
		private PlotProperties properties;
		
		PlotSettings(PlotProperties properties) {
			this.properties = properties;
			initComponents();
			load();
		}
		
		void initComponents() {
			xColumnField = new ChoiceBox<>();
			yColumnField = new ChoiceBox<>();
			colorField = new ChoiceBox<>();
			segmentField = new ChoiceBox<>();
			typeField = new ChoiceBox<>();
		}
		
		void load() {
			xColumnField.getSelectionModel().select(properties.xColumnName());
			yColumnField.getSelectionModel().select(properties.yColumnName());
			colorField.getSelectionModel().select(properties.getColor());
			segmentField.getSelectionModel().select(properties.getSegmentsColor());
			typeField.getSelectionModel().select(properties.getType());
		}
	}
}

