package de.mpg.biochem.mars.gui.molecule.moleculesTab;

import java.util.ArrayList;

import de.mpg.biochem.mars.gui.plot.Plot;
import de.mpg.biochem.mars.gui.table.MARSTableView;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.table.MARSResultsTable;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class MoleculeTablesPane implements MoleculeSubTab {
	private TabPane tabPane;
	private Tab dataTableTab;
	private Tab plotTab;
	
	private BorderPane dataTableContainer;
	private Plot plot;
	
	private Molecule molecule;
	
	public MoleculeTablesPane() {
		tabPane = new TabPane();
		tabPane.setFocusTraversable(false);
		
		initializeTabs();
	}
	
	private void initializeTabs() {
		dataTableTab = new Tab();		
		dataTableTab.setText("DataTable");
		dataTableContainer = new BorderPane();
		dataTableTab.setContent(dataTableContainer);
		
		plotTab = new Tab();
		plotTab.setText("Plot");
		plot = new Plot();
		plotTab.setContent(plot);
		
		tabPane.getTabs().add(dataTableTab);
		tabPane.getTabs().add(plotTab);
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		
		tabPane.setStyle("");
		tabPane.getStylesheets().clear();
		tabPane.getStylesheets().add("de/mpg/biochem/mars/gui/molecule/moleculesTab/MoleculeTablesPane.css");
		
		tabPane.getSelectionModel().select(dataTableTab);
	}
	
	public void loadDataTable() {
		dataTableContainer.setCenter(new MARSTableView(molecule.getDataTable()));
	}
	
	public void loadPlot() {
		plot.setMolecule(molecule);
	}
	
	public void loadSegmentTables() {
		//Clear all segment tabs
		tabPane.getTabs().remove(2, tabPane.getTabs().size());
		
		//Load segment tables
		for (ArrayList<String> segmentTableName : molecule.getSegmentTableNames()) {
			Tab segmentTableTab = new Tab(segmentTableName.get(1) + " vs " + segmentTableName.get(0));
			BorderPane segmentTableContainer = new BorderPane();
			segmentTableTab.setContent(segmentTableContainer);
			segmentTableContainer.setCenter(new MARSTableView(molecule.getSegmentsTable(segmentTableName)));
			
			tabPane.getTabs().add(segmentTableTab);
		}
	}
	
	Node getNode() {
		return tabPane;
	}
	
   public void setMolecule(Molecule molecule) {
		this.molecule = molecule;
		loadDataTable();
		loadPlot();
		loadSegmentTables();
	}
}
