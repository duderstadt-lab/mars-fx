package de.mpg.biochem.mars.fx.molecule.moleculesTab;

import java.util.ArrayList;
import de.mpg.biochem.mars.fx.plot.PlotPane;
import de.mpg.biochem.mars.fx.table.MarsTableFxView;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.table.MarsTable;
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

public abstract class AbstractMoleculeCenterPane implements MoleculeSubPane {
	private TabPane tabPane;
	private Tab dataTableTab;
	private Tab plotTab;
	
	private BorderPane dataTableContainer;
	private PlotPane plot;
	
	protected MoleculeArchive<Molecule,MarsImageMetadata,MoleculeArchiveProperties> archive;
	
	private Molecule molecule;
	
	public AbstractMoleculeCenterPane() {
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
		plot = new PlotPane();
		plotTab.setContent(plot);
		
		tabPane.getTabs().add(dataTableTab);
		tabPane.getTabs().add(plotTab);
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		
		tabPane.setStyle("");
		tabPane.getStylesheets().clear();
		tabPane.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/moleculesTab/MoleculeTablesPane.css");
		
		tabPane.getSelectionModel().select(dataTableTab);
	}
	
	public void loadDataTable() {
		dataTableContainer.setCenter(new MarsTableFxView(molecule.getDataTable()));
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
			segmentTableContainer.setCenter(new MarsTableFxView(molecule.getSegmentsTable(segmentTableName)));
			
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
