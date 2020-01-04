package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.molecule.JsonConvertibleRecord;
import javafx.scene.Node;

public interface DashboardWidget extends JsonConvertibleRecord {
	public Node getNode();
	public void setContent(Node node);
	public void setParent(DashboardTab parent);
	public DashboardTab getParent();
	public void load();
	public boolean isLoading();
	public void interrupt();
	public void close();
	public String getName();
}
