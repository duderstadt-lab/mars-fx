package de.mpg.biochem.mars.fx.molecule.moleculesTab.dashboard;

import java.util.ArrayList;
import java.util.Arrays;

import de.mpg.biochem.mars.fx.dashboard.AbstractDashboard;
import de.mpg.biochem.mars.molecule.Molecule;

public class MoleculeDashboard extends AbstractDashboard<MoleculeDashboardWidget> {
	
	protected Molecule molecule;
	
	public MoleculeDashboard() {
		super();
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
	
	public void setMolecule(Molecule molecule) {
		this.molecule = molecule;
	}
	
	public Molecule getMolecule() {
		return molecule;
	}
}