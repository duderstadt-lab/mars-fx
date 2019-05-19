package de.mpg.biochem.mars.gui.molecule.moleculesTab.plot;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public class PlotOptionsController {
	
	private BorderPane borderPane;
	
	public PlotOptionsController() {
		borderPane = new BorderPane();
		showPlotSelectorView();
	}
	
	public void showPlotSelectorView(){
	    PlotSelectorView plotSelectorView = new PlotSelectorView();
	    
	    borderPane.setCenter(plotSelectorView.getNode());
	}

	public void showPlotNumberView(){
	    PlotNumberView plotNumberView = new PlotNumberView();
	    plotNumberView.setOnBackButton(new ViewCallBack(){
	        public void call(){
	        	showPlotSelectorView();
	        }
	    });
	    borderPane.setCenter(plotNumberView.getNode());
	}
	
	public Node getNode() {
		return borderPane;
	}
}
