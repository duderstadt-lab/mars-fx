package de.mpg.biochem.mars.fx.molecule.moleculesTab.dashboard;

import java.io.IOException;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.script.ScriptModule;

import de.mpg.biochem.mars.fx.dashboard.AbstractBubbleChartWidget;
import de.mpg.biochem.mars.fx.dashboard.MarsDashboardWidget;
import de.mpg.biochem.mars.molecule.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import net.imagej.ops.Initializable;

@Plugin( type = MoleculeDashboardWidget.class, name = "MoleculeBubbleChartWidget" )
public class MoleculeBubbleChartWidget extends AbstractBubbleChartWidget implements MoleculeDashboardWidget, SciJavaPlugin, Initializable {

	protected Molecule molecule;
	
	@Override
	public void initialize() {
		super.initialize();
		
		try {
			loadScript("bubblechart", "#@ Molecule molecule\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void setScriptInputs(ScriptModule module) {
		module.setInput("molecule", molecule);
	}
	
	public void setMolecule(Molecule molecule) {
		this.molecule = molecule;
	}
	
	public Molecule getMolecule() {
		return molecule;
	}
	
	@Override
	public String getName() {
		return "MoleculeBubbleChartWidget";
	}
}
