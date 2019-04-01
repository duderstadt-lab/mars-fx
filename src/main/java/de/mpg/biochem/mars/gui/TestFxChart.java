package de.mpg.biochem.mars.gui;

import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
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

import cern.extjfx.samples.chart.*;

// NEED TO Implement - https://github.com/hadim/OMEVisual/blob/master/src/main/java/sc/fiji/omevisual/gui/MainAppFrame.java

@Plugin(type = Command.class, label = "Test GUI", menu = {
		@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT,
				mnemonic = MenuConstants.PLUGINS_MNEMONIC),
		@Menu(label = "MoleculeArchive Suite", weight = MenuConstants.PLUGINS_WEIGHT,
			mnemonic = 's'),
		@Menu(label = "JavaFX", weight = 20,
			mnemonic = 'i'),
		@Menu(label = "Test GUI", weight = 10, mnemonic = 'g')})
public class TestFxChart extends DynamicCommand implements Command {
    private ListView<String> samplesSelectionList;
    private Map<String, Node> samplesMap = new HashMap<>();
    private BorderPane rootPane;
	
	@Override
	public void run() {
		ChartSamplesApp app = new ChartSamplesApp();
		app.setTitle("test");
		app.init();
		
		Scene scene = new Scene(createRootPane(), 1000, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("ExtJFX Chart Samples");
        primaryStage.show();
	}
	
	private Parent createRootPane() {
        rootPane = new BorderPane();
        rootPane.setPadding(new Insets(5));

        samplesSelectionList = new ListView<>();
        samplesSelectionList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> showSample(newVal));
        samplesSelectionList.setPrefWidth(150);
        BorderPane.setMargin(samplesSelectionList, new Insets(5));
        
        rootPane.setLeft(samplesSelectionList);
        
        registerSample(new DataIndicatorsSample());
        registerSample(new HeatMapChartSample());
        registerSample(new OverlayChartSample());
        registerSample(new LargeDataSetsSample());
        registerSample(new LogarithmicAxisSample());
        
        return rootPane;
    }
    
    private void showSample(String sampleName) {
        if (sampleName != null) {
            rootPane.setCenter(samplesMap.get(sampleName));
        }
    }

    private void registerSample(AbstractSamplePane sample) {
        samplesMap.put(sample.getName(), sample);
        samplesSelectionList.getItems().add(sample.getName());
        BorderPane.setMargin(sample, new Insets(5));
    }

}
