/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2021 Karl Duderstadt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package de.mpg.biochem.mars.fx.table.dashboard;

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
	
	public MarsTableDashboard(final Context context, MarsTable table) {
		super(context);
		this.table = table;
		
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
	            Arrays.asList("MarsTableCategoryChartWidget",
	                    "MarsTableHistogramWidget",
	                    "MarsTableXYChartWidget",
	                    "MarsTableBubbleChartWidget"));
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
