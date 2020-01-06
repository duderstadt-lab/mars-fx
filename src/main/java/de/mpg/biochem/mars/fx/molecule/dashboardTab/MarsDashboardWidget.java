package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.molecule.JsonConvertibleRecord;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.scene.Node;

import org.scijava.command.Command;
import java.util.concurrent.RunnableFuture;
import net.imagej.ops.Initializable;

public interface MarsDashboardWidget extends Command, JsonConvertibleRecord, Initializable {
	public Node getNode();
	public void setContent(Node node);
	public void setContent(Node icon, Node node);
	public void setArchive(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive);
	public MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> getArchive();
	public void setParent(DashboardTab parent);
	public DashboardTab getParent();
	public boolean isRunning();
	public void setRunning(boolean running);
	public Node getIcon();
	public void spin();
	public void stopSpinning();
	public void close();
}
