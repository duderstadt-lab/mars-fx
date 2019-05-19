package de.mpg.biochem.mars.gui.molecule.moleculesTab.plot;

import com.jfoenix.controls.JFXButton;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public class PlotNumberView {
	private BorderPane borderPane;
	
	private ViewCallBack onBackButton = null;
	
	public PlotNumberView() {
		// somewhere in your code
		JFXButton b = new JFXButton();
		
		//TODO add icon
		/*
		b.setOnAction(new EventHandler<ActionEvent>(){
		    public void handle(ActionEvent event){
		        callBack();
		    }
		});
		*/
	}

	public void setOnBackButton(ViewCallBack callback){ onBackButton = callback; }

	public void callBack() { if (onBackButton != null) onBackButton.call(); }

	public Node getNode() {
		return borderPane;
	}
}
