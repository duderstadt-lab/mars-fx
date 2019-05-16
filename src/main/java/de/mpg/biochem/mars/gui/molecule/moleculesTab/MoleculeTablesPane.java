package de.mpg.biochem.mars.gui.molecule.moleculesTab;

import de.mpg.biochem.mars.gui.molecule.moleculesTab.PropertiesTabController.ParameterRow;
import de.mpg.biochem.mars.gui.projects.ProjectManager;
import de.mpg.biochem.mars.molecule.Molecule;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;

public class MoleculeTablesPane {
	private TabPane tabPane;
	private Tab dataTableTab;
	private Tab plotTab;
	
	private Molecule molecule;
	
	public MoleculeTablesPane() {
		tabPane = new TabPane();
		tabPane.setFocusTraversable(false);
		
		initializeTabs();
	}
	
	private void initializeTabs() {
		dataTableTab = new Tab("DataTable");
		plotTab = new Tab("Plot");
		
		tabPane.getTabs().add(dataTableTab);
		tabPane.getTabs().add(plotTab);
		
		tabPane.getSelectionModel().select(dataTableTab);
	}
	
	public void loadData() {
		parameterRowList.clear();

    	for (String parameter : molecule.getParameters().keySet()) {
        	parameterRowList.add(new ParameterRow(parameter));
        }
	}
	
	Node getNode() {
		return tabPane;
	}
	
   public void setMolecule(Molecule molecule) {
		this.molecule = molecule;
		loadData();
	}
}
