package de.mpg.biochem.mars.gui.molecule.moleculesTab.plot;

import com.jfoenix.controls.JFXButton;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

public class PlotSelectorView {
	private VBox vbox;
	public PlotSelectorView() {
		JFXButton singleCurveButton = new JFXButton("Single Curve");
		
		JFXButton multipleCurvesButton = new JFXButton("Multiple Curves");
		
		JFXButton multiplePlotsButton = new JFXButton("Multiple Plots");
		
		vbox = new VBox(8);
		
		vbox.getChildren().add(singleCurveButton);
		vbox.getChildren().add(multipleCurvesButton);
		vbox.getChildren().add(multiplePlotsButton);
		
		vbox.setAlignment(Pos.CENTER);
	}
	
	public Node getNode() {
		return vbox;
	}
}
