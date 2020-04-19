package de.mpg.biochem.mars.fx.molecule.moleculesTab.dashboard;

import java.util.ArrayList;
import java.util.Arrays;

import de.mpg.biochem.mars.fx.dashboard.AbstractDashboard;
import de.mpg.biochem.mars.fx.dashboard.MarsDashboardWidgetService;
import de.mpg.biochem.mars.fx.event.InitializeMoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveUnlockEvent;
import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.event.MoleculeSelectionChangedEvent;
import de.mpg.biochem.mars.fx.molecule.dashboardTab.MoleculeArchiveDashboardWidget;
import java.util.Set;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
import de.mpg.biochem.mars.fx.plot.SubPlot;
import de.mpg.biochem.mars.molecule.Molecule;
import javafx.event.Event;
import javafx.event.EventHandler;

public class MoleculeDashboard<M extends Molecule> extends AbstractDashboard<MoleculeDashboardWidget> implements MoleculeSubPane {
	
	protected M molecule;
	
	public MoleculeDashboard() {
		super();
		
		getNode().addEventHandler(MoleculeEvent.MOLECULE_EVENT, this);
		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, new EventHandler<MoleculeArchiveEvent>() {
			@Override
			public void handle(MoleculeArchiveEvent e) {
				if (e.getEventType().getName().equals("INITIALIZE_MOLECULE_ARCHIVE")) {
			   		marsDashboardWidgetService = e.getArchive().getMoleculeArchiveService().getContext().getService(MarsDashboardWidgetService.class);
			   		discoverWidgets();
			   		e.consume();
			   	}
			} 
        });
	}

	@Override
	public MoleculeDashboardWidget createWidget(String widgetName) {
		MoleculeDashboardWidget widget = (MoleculeDashboardWidget) marsDashboardWidgetService.createWidget(widgetName);
		widget.setMolecule(molecule);
		widget.setParent(this);
		widget.initialize();
		return widget;
	}

	@Override
	public ArrayList<String> getWidgetToolbarOrder() {
		return new ArrayList<String>( 
	            Arrays.asList("MoleculeCategoryChartWidget",
	                    "MoleculeHistogramWidget",
	                    "MoleculeXYChartWidget",
	                    "MoleculeBubbleChartWidget"));
	}
	
	@Override
	public void handle(MoleculeEvent event) {
		event.invokeHandler(this);
		event.consume();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onMoleculeSelectionChangedEvent(Molecule molecule) {
		this.molecule = (M) molecule;
	}

	public Set<String> getWidgetNames() {
		return marsDashboardWidgetService.getWidgetNames(MoleculeDashboardWidget.class);
	}

	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}
}