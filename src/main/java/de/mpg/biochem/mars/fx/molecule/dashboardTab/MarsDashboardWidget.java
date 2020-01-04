package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.molecule.JsonConvertibleRecord;
import javafx.scene.Node;

import org.scijava.command.Command;

public interface MarsDashboardWidget extends Command, JsonConvertibleRecord {
	public Node getNode();
	public void setContent(Node node);
	public void setContent(Node icon, Node node);
	public void setParent(DashboardTab parent);
	public DashboardTab getParent();
	public void load();
	public boolean isLoading();
	public void interrupt();
	public void close();
}
