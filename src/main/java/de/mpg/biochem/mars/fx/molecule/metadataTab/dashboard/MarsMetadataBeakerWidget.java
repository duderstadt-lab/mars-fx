package de.mpg.biochem.mars.fx.molecule.metadataTab.dashboard;

import java.io.IOException;

import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.script.ScriptModule;

import net.imagej.ops.Initializable;
import de.mpg.biochem.mars.fx.dashboard.AbstractBeakerWidget;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;

@Plugin( type = MarsMetadataDashboardWidget.class, name = "MarsMetadataBeakerWidget" )
public class MarsMetadataBeakerWidget extends AbstractBeakerWidget implements MarsMetadataDashboardWidget, SciJavaPlugin, Initializable {

	protected MarsMetadata marsMetadata;
	
	@Override
	public void initialize() {
		super.initialize();
		
		try {
			loadScript("beaker", "#@ MarsMetadata marsMetadata\n");
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
		return "MarsMetadataBeakerWidget";
	}
}
