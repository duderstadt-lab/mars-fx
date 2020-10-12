package de.mpg.biochem.mars.fx.molecule.moleculesTab.dashboard;

import java.io.IOException;

import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.script.ScriptModule;

import net.imagej.ops.Initializable;
import de.mpg.biochem.mars.fx.dashboard.AbstractBeakerWidget;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;

@Plugin( type = MoleculeDashboardWidget.class, name = "MoleculeBeakerWidget" )
public class MoleculeBeakerWidget extends AbstractBeakerWidget implements MoleculeDashboardWidget, SciJavaPlugin, Initializable {

	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;
	protected Molecule molecule;
	
	@Override
	public void initialize() {
		super.initialize();
		
		try {
			loadScript("beaker", "#@ MoleculeArchive archive\n#@ Molecule molecule\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void setScriptInputs(ScriptModule module) {
		module.setInput("archive", archive);
		module.setInput("molecule", molecule);
	}
	
	public void setArchive(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive) {
		this.archive = archive;
	}
	
	public MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> getArchive() {
		return archive;
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
