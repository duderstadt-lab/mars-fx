package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.molecule.JsonConvertibleRecord;
import javafx.scene.Node;

import org.scijava.command.Command;
import java.util.concurrent.RunnableFuture;

public interface MarsDashboardWidget extends Command, JsonConvertibleRecord {
	public Node getNode();
	public void setContent(Node node);
	public void setContent(Node icon, Node node);
	public void setParent(DashboardTab parent);
	public DashboardTab getParent();
	public boolean isRunning();
	public boolean cancel();
	public void spin();
	public void stopSpinning();
	public void close();
}
