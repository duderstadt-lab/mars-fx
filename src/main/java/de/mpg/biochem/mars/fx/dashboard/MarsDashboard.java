package de.mpg.biochem.mars.fx.dashboard;

import com.jfoenix.controls.JFXMasonryPane;

public interface MarsDashboard {
	public void runWidget(MarsDashboardWidget widget);
	public void stopWidget(MarsDashboardWidget widget);
	public void removeWidget(MarsDashboardWidget widget);
	public JFXMasonryPane getWidgetPane();
}
