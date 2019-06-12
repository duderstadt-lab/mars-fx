package de.mpg.biochem.mars.fx.molecule.imageMetaDataTab;

import java.util.ArrayList;

import de.mpg.biochem.mars.fx.plot.PlotPane;
import de.mpg.biochem.mars.fx.table.MARSTableFxView;
import de.mpg.biochem.mars.molecule.MARSImageMetaData;
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

public class ImageMetaDataTablesPane implements ImageMetaDataSubTab {
	private TabPane tabPane;
	private Tab dataTableTab;
	
	private BorderPane dataTableContainer;
	
	private MARSImageMetaData meta;
	
	public ImageMetaDataTablesPane() {
		tabPane = new TabPane();
		tabPane.setFocusTraversable(false);
		
		initializeTabs();
	}
	
	private void initializeTabs() {
		dataTableTab = new Tab();		
		dataTableTab.setText("DataTable");
		dataTableContainer = new BorderPane();
		dataTableTab.setContent(dataTableContainer);
		
		tabPane.getTabs().add(dataTableTab);
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		
		tabPane.setStyle("");
		tabPane.getStylesheets().clear();
		tabPane.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/imageMetaDataTab/MetaTablesPane.css");
		
		tabPane.getSelectionModel().select(dataTableTab);
	}
	
	public void loadDataTable() {
		dataTableContainer.setCenter(new MARSTableFxView(meta.getDataTable()));
	}
	
	Node getNode() {
		return tabPane;
	}

	@Override
	public void setImageMetaData(MARSImageMetaData meta) {
		this.meta = meta;
		loadDataTable();
		//load log ??
	}
}
