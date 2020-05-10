package de.mpg.biochem.mars.fx.dashboard;

import com.jfoenix.controls.JFXMasonryPane;
import javafx.scene.Node;

public interface MarsDashboard<W extends MarsDashboardWidget> {
	public void runWidget(W widget);

	public void stopWidget(W widget);

	public void removeWidget(W widget);
	
	public Node getNode();

	public JFXMasonryPane getWidgetPane();
}
