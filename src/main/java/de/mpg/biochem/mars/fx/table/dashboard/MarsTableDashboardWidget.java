package de.mpg.biochem.mars.fx.table.dashboard;

import de.mpg.biochem.mars.fx.dashboard.MarsDashboardWidget;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.table.MarsTable;

public interface MarsTableDashboardWidget extends MarsDashboardWidget {
	public void setTable(MarsTable table);
	public MarsTable getTable();
}
