package de.mpg.biochem.mars.fx.table;

import java.util.ArrayList;
import java.util.Arrays;

import de.mpg.biochem.mars.fx.dashboard.AbstractDashboard;
import de.mpg.biochem.mars.fx.dashboard.MarsDashboardWidgetService;
import de.mpg.biochem.mars.fx.molecule.dashboardTab.MoleculeArchiveDashboardWidget;

import java.util.Set;

import org.scijava.Context;

import de.mpg.biochem.mars.table.MarsTable;

public class MarsTableDashboard extends AbstractDashboard<MarsTableDashboardWidget> {
	
	protected MarsTable table;
	
	public MarsTableDashboard(final Context context) {
		super(context);
		
		discoverWidgets();
	}
	
	@Override
	public MarsTableDashboardWidget createWidget(String widgetName) {
		MarsTableDashboardWidget widget = (MarsTableDashboardWidget) marsDashboardWidgetService.createWidget(widgetName);
		widget.setTable(table);
		widget.setParent(this);
		widget.initialize();
		return widget;
	}

	@Override
	public ArrayList<String> getWidgetToolbarOrder() {
		return new ArrayList<String>( 
	            Arrays.asList("TableCategoryChartWidget",
	                    "TableHistogramWidget",
	                    "TableXYChartWidget",
	                    "TableBubbleChartWidget"));
	}
	
	public void setTable(MarsTable table) {
		this.table = table;
	}
	
	public MarsTable getTable() {
		return table;
	}

	public Set<String> getWidgetNames() {
		return marsDashboardWidgetService.getWidgetNames(MarsTableDashboardWidget.class);
	}
}
