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
import net.imagej.ops.Initializable;

@Plugin( type = MoleculeDashboardWidget.class, name = "MoleculeHistogramWidget" )
public class MoleculeHistogramWidget extends AbstractHistogramWidget implements MoleculeDashboardWidget, SciJavaPlugin, Initializable {

	protected Molecule molecule;
	
	@Override
	public void initialize() {
		super.initialize();
		
		try {
			loadScript("histogramchart", "#@ Molecule molecule\n");
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
		return "MoleculeHistogramWidget";
	}
}
