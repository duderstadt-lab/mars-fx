package de.mpg.biochem.mars.fx.molecule.moleculesTab.dashboard;

import java.io.IOException;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.script.ScriptModule;

import de.mpg.biochem.mars.molecule.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.fx.dashboard.AbstractHistogramWidget;
import de.mpg.biochem.mars.fx.dashboard.AbstractXYChartWidget;
import net.imagej.ops.Initializable;

@Plugin( type = AbstractXYChartWidget.class, name = "MoleculeXYChartWidget" )
public class MoleculeXYChartWidget extends AbstractHistogramWidget implements MoleculeDashboardWidget, SciJavaPlugin, Initializable {

	protected Molecule molecule;
	
	@Override
	public void initialize() {
		super.initialize();
		
		try {
			loadScript("xychart");
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
		return "MoleculeXYChartWidget";
	}
}
