package de.mpg.biochem.mars.fx.molecule.metadataTab.dashboard;

import java.util.ArrayList;
import java.util.Arrays;

import de.mpg.biochem.mars.fx.dashboard.AbstractDashboard;
import de.mpg.biochem.mars.fx.event.MetadataEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.molecule.metadataTab.MetadataSubPane;

import java.util.Set;

import org.scijava.Context;

import de.mpg.biochem.mars.molecule.MarsMetadata;
import javafx.event.Event;
import javafx.event.EventHandler;

public class MarsMetadataDashboard<I extends MarsMetadata> extends AbstractDashboard<MarsMetadataDashboardWidget> implements MetadataSubPane {
	
	protected I marsMetadata;
	
	public MarsMetadataDashboard(final Context context) {
		super(context);
		
		getNode().addEventHandler(MetadataEvent.METADATA_EVENT, this);
		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, new EventHandler<MoleculeArchiveEvent>() {
			@Override
			public void handle(MoleculeArchiveEvent e) {
				if (e.getEventType().getName().equals("INITIALIZE_MOLECULE_ARCHIVE")) {
			   		discoverWidgets();
			   		e.consume();
			   	}
			} 
        });
	}

	@Override
	public MarsMetadataDashboardWidget createWidget(String widgetName) {
		MarsMetadataDashboardWidget widget = (MarsMetadataDashboardWidget) marsDashboardWidgetService.createWidget(widgetName);
		widget.setMetadata(marsMetadata);
		widget.setParent(this);
		widget.initialize();
		return widget;
	}

	@Override
	public ArrayList<String> getWidgetToolbarOrder() {
		return new ArrayList<String>( 
	            Arrays.asList("MarsMetadataCategoryChartWidget",
	                    "MarsMetadataHistogramWidget",
	                    "MarsMetadataXYChartWidget",
	                    "MarsMetadataBubbleChartWidget"));
	}
	
	@Override
	public void handle(MetadataEvent event) {
		event.invokeHandler(this);
		event.consume();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onMetadataSelectionChangedEvent(MarsMetadata marsMetadata) {
		this.marsMetadata = (I) marsMetadata;
		widgets.forEach(widget -> widget.setMetadata(marsMetadata));
	}

	public Set<String> getWidgetNames() {
		return marsDashboardWidgetService.getWidgetNames(MarsMetadataDashboardWidget.class);
	}

	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}
}