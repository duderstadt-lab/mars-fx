package de.mpg.biochem.mars.fx.molecule.dashboardTab;

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
import de.mpg.biochem.mars.fx.dashboard.MarsDashboardWidget;
import net.imagej.ops.Initializable;

@Plugin( type = MoleculeArchiveDashboardWidget.class, name = "XYChartWidget" )
public class MoleculeArchiveXYChartWidget extends AbstractXYChartWidget implements MoleculeArchiveDashboardWidget, SciJavaPlugin, Initializable {

	@Parameter
	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties> archive;
	
	@Override
	public void initialize() {
		super.initialize();
		
		try {
			loadScript("xychart", "#@ MoleculeArchive(required=false) archive\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void setScriptInputs(ScriptModule module) {
		module.setInput("archive", archive);
	}
	
	public void setArchive(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties> archive) {
		this.archive = archive;
	}
	
	public MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties> getArchive() {
		return archive;
	}
	
	@Override
	public String getName() {
		return "XYChartWidget";
	}
}
