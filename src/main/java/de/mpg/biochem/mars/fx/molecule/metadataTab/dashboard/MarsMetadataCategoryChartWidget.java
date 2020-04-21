package de.mpg.biochem.mars.fx.molecule.metadataTab.dashboard;

import java.io.IOException;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.script.ScriptModule;

import de.mpg.biochem.mars.molecule.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.fx.dashboard.AbstractCategoryChartWidget;
import net.imagej.ops.Initializable;

@Plugin( type = MarsMetadataDashboardWidget.class, name = "MarsMetadataCategoryChartWidget" )
public class MarsMetadataCategoryChartWidget extends AbstractCategoryChartWidget implements MarsMetadataDashboardWidget, SciJavaPlugin, Initializable {

	protected MarsMetadata marsMetadata;
	
	@Override
	public void initialize() {
		super.initialize();
		
		try {
			loadScript("categorychart", "#@ MarsMetadata marsMetadata\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void setScriptInputs(ScriptModule module) {
		module.setInput("marsMetadata", marsMetadata);
	}
	
	public void setMetadata(MarsMetadata marsMetadata) {
		this.marsMetadata = marsMetadata;
	}
	
	public MarsMetadata getMetadata() {
		return marsMetadata;
	}
	
	@Override
	public String getName() {
		return "MarsMetadataCategoryChartWidget";
	}
}
