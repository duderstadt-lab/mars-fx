package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import java.util.ArrayList;
import java.util.Arrays;

import org.scijava.plugin.Parameter;

import de.mpg.biochem.mars.fx.dashboard.AbstractDashboard;
import de.mpg.biochem.mars.fx.dashboard.MarsDashboardWidget;
import de.mpg.biochem.mars.molecule.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;

public class MoleculeArchiveDashboard extends AbstractDashboard<MoleculeArchiveDashboardWidget> {
	
	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties> archive;
	
	public MoleculeArchiveDashboard() {
		super();
	}

	@Override
	public MoleculeArchiveDashboardWidget createWidget(String widgetName) {
		MoleculeArchiveDashboardWidget widget = (MoleculeArchiveDashboardWidget) marsDashboardWidgetService.createWidget(widgetName);
		widget.setArchive(archive);
		widget.setParent(this);
		widget.initialize();
		return widget;
	}

	@Override
	public ArrayList<String> getWidgetToolbarOrder() {
		return new ArrayList<String>( 
	            Arrays.asList("ArchivePropertiesWidget", 
	                    "TagFrequencyWidget", 
	                    "CategoryChartWidget",
	                    "HistogramWidget",
	                    "XYChartWidget",
	                    "BubbleChartWidget"));
	}
	
	public void setArchive(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties> archive) {
		this.archive = archive;
	}
	
	public MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties> getArchive() {
		return archive;
	}
}
