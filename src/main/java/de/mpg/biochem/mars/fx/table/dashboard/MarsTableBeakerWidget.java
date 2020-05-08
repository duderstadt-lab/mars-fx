package de.mpg.biochem.mars.fx.table.dashboard;

import java.io.IOException;

import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.script.ScriptModule;

import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.table.MarsTable;
import net.imagej.ops.Initializable;

import de.mpg.biochem.mars.fx.dashboard.AbstractBeakerWidget;

@Plugin( type = MarsTableDashboardWidget.class, name = "MarsTableBeakerWidget" )
public class MarsTableBeakerWidget extends AbstractBeakerWidget implements MarsTableDashboardWidget, SciJavaPlugin, Initializable {

	protected MarsTable table;
	
	@Override
	public void initialize() {
		super.initialize();
		
		try {
			loadScript("beaker", "#@ MarsTable table\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void setScriptInputs(ScriptModule module) {
		module.setInput("table", table);
	}
	
	public void setTable(MarsTable table) {
		this.table = table;
	}
	
	public MarsTable getTable() {
		return table;
	}
	
	@Override
	public String getName() {
		return "MarsTableBeakerWidget";
	}
}
