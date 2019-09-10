package de.mpg.biochem.mars.fx.molecule.moleculesTab;

import java.util.ArrayList;

import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.event.MoleculeSelectionChangedEvent;
import de.mpg.biochem.mars.fx.plot.PlotPane;
import de.mpg.biochem.mars.fx.table.MarsTableView;
import de.mpg.biochem.mars.molecule.Molecule;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.BorderPane;

public abstract class AbstractMoleculeCenterPane<M extends Molecule, P extends PlotPane> implements MoleculeSubPane {
	private TabPane tabPane;
	private Tab dataTableTab;
	private Tab plotTab;
	
	private BorderPane dataTableContainer;
	private P plotPane;
	
	private M molecule;
	
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
		plotPane = createPlotPane();
		plotTab.setContent(plotPane.getNode());
		
		tabPane.getTabs().add(dataTableTab);
		tabPane.getTabs().add(plotTab);
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		
		tabPane.setStyle("");
		tabPane.getStylesheets().clear();
		tabPane.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/moleculesTab/MoleculeTablesPane.css");
		
		tabPane.getSelectionModel().select(dataTableTab);
		
		getNode().addEventHandler(MoleculeEvent.MOLECULE_EVENT, this);
	}
	
	public void loadSegmentTables() {
		//Clear all segment tabs
		tabPane.getTabs().remove(2, tabPane.getTabs().size());
		
		//Load segment tables
		for (ArrayList<String> segmentTableName : molecule.getSegmentTableNames()) {
			Tab segmentTableTab = new Tab(segmentTableName.get(1) + " vs " + segmentTableName.get(0));
			BorderPane segmentTableContainer = new BorderPane();
			segmentTableTab.setContent(segmentTableContainer);
			segmentTableContainer.setCenter(new MarsTableView(molecule.getSegmentsTable(segmentTableName)));
			
			tabPane.getTabs().add(segmentTableTab);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void onMoleculeSelectionChangedEvent(Molecule molecule) {
		this.molecule = (M) molecule;
		
		//Update DataTable
		dataTableContainer.setCenter(new MarsTableView(molecule.getDataTable()));
		
		//Update Plot
		plotPane.fireEvent(new MoleculeSelectionChangedEvent(molecule));
		
		//Load SegmentTables
		loadSegmentTables();
	}
	
	public abstract P createPlotPane();
	
	@Override
	public Node getNode() {
		return tabPane;
	}
	
	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}
   
   @Override
   public void handle(MoleculeEvent event) {
	   event.invokeHandler(this);
	   event.consume();
   }
}
