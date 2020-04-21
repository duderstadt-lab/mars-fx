package de.mpg.biochem.mars.fx.molecule.metadataTab.dashboard;

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

@Plugin( type = MarsMetadataDashboardWidget.class, name = "MarsMetadataBubbleChartWidget" )
public class MarsMetadataBubbleChartWidget extends AbstractBubbleChartWidget implements MarsMetadataDashboardWidget, SciJavaPlugin, Initializable {

	protected MarsMetadata marsMetadata;
	
	@Override
	public void initialize() {
		super.initialize();
		
		try {
			loadScript("bubblechart", "#@ MarsMetadata marsMetadata\n");
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
		return "MarsMetadataBubbleChartWidget";
	}
}
