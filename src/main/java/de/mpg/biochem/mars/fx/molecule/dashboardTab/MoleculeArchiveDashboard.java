package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import org.scijava.Context;
import org.scijava.plugin.Parameter;

import de.mpg.biochem.mars.fx.dashboard.AbstractDashboard;
import de.mpg.biochem.mars.fx.dashboard.MarsDashboardWidget;
import de.mpg.biochem.mars.fx.dashboard.MarsDashboardWidgetService;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.molecule.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.event.Event;
import javafx.event.EventHandler;

public class MoleculeArchiveDashboard extends AbstractDashboard<MoleculeArchiveDashboardWidget> {
	
	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties> archive;
	
	public MoleculeArchiveDashboard(final Context context) {
		super(context);
		
		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, new EventHandler<MoleculeArchiveEvent>() {
			@Override
			public void handle(MoleculeArchiveEvent e) {
				if (e.getEventType().getName().equals("INITIALIZE_MOLECULE_ARCHIVE")) {
					archive = e.getArchive();
			   		discoverWidgets();
			   		e.consume();
			   	}
			} 
	    });
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

	public Set<String> getWidgetNames() {
		return marsDashboardWidgetService.getWidgetNames(MoleculeArchiveDashboardWidget.class);
	}
}
