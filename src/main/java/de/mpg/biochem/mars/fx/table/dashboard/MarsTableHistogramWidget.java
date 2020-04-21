package de.mpg.biochem.mars.fx.table.dashboard;

import java.io.IOException;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.script.ScriptModule;

import de.mpg.biochem.mars.molecule.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.table.MarsTable;
import de.mpg.biochem.mars.fx.dashboard.AbstractHistogramWidget;
import net.imagej.ops.Initializable;

@Plugin( type = MarsTableDashboardWidget.class, name = "MarsTableHistogramWidget" )
public class MarsTableHistogramWidget extends AbstractHistogramWidget implements MarsTableDashboardWidget, SciJavaPlugin, Initializable {

	protected MarsTable table;
	
	@Override
	public void initialize() {
		super.initialize();
		
		try {
			loadScript("histogramchart", "#@ MarsTable table\n");
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
		return "MarsTableHistogramWidget";
	}
}
