package de.mpg.biochem.mars.fx.molecule.moleculesTab.dashboard;

import java.io.IOException;

import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.script.ScriptModule;

import de.mpg.biochem.mars.molecule.Molecule;
import net.imagej.ops.Initializable;

import de.mpg.biochem.mars.fx.dashboard.AbstractBeakerWidget;

@Plugin( type = MoleculeDashboardWidget.class, name = "MoleculeBeakerWidget" )
public class MoleculeBeakerWidget extends AbstractBeakerWidget implements MoleculeDashboardWidget, SciJavaPlugin, Initializable {

	protected Molecule molecule;
	
	@Override
	public void initialize() {
		super.initialize();
		
		try {
			loadScript("beaker", "#@ Molecule molecule\n");
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
		return "MoleculeBeakerWidget";
	}
}
