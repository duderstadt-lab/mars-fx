package de.mpg.biochem.mars.fx.dashboard;

import com.jfoenix.controls.JFXMasonryPane;

public interface MarsDashboard<W extends MarsDashboardWidget> {
	public void runWidget(W widget);
	public void stopWidget(W widget);
	public void removeWidget(W widget);
	public JFXMasonryPane getWidgetPane();
}
