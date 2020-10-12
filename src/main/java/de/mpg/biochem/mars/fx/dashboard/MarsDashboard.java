package de.mpg.biochem.mars.fx.dashboard;

import java.util.ArrayList;
import java.util.Set;

import com.jfoenix.controls.JFXMasonryPane;

import javafx.collections.ObservableList;
import javafx.scene.Node;

public interface MarsDashboard<W extends MarsDashboardWidget> {
	void runWidget(W widget);

	void stopWidget(W widget);

	void removeWidget(W widget);
	
	Node getNode();
	
	JFXMasonryPane getWidgetPane();

	ObservableList<W> getWidgets();
	
	void addWidget(W widget);
	
	W createWidget(String widgetName);

	ArrayList<String> getWidgetToolbarOrder();

	Set<String> getWidgetNames();
}
