package de.mpg.biochem.mars.fx.molecule.imageMetaDataTab;

import java.util.ArrayList;

import com.vladsch.flexmark.parser.Parser;

import de.mpg.biochem.mars.fx.controls.BottomSlidePane;
import de.mpg.biochem.mars.fx.editor.LogPane;
import de.mpg.biochem.mars.fx.plot.PlotPane;
import de.mpg.biochem.mars.fx.table.MarsTableFxView;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.table.MarsTable;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class ImageMetaDataTablesPane implements ImageMetaDataSubTab {
	private TabPane tabPane;
	private Tab dataTableTab;
	private BorderPane dataTableContainer;
	
	private Tab logTab;
	private BorderPane logContainer;
	
	private LogPane logPane;
	
	private MarsImageMetadata meta;
	
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
		
		logTab = new Tab();
		logTab.setText("Log");
		logContainer = new BorderPane();
		logTab.setContent(logContainer);
		
		logPane = new LogPane();
		//logPane.setEditable(false);
		logContainer.setCenter(logPane.getNode());
		
		tabPane.getTabs().add(logTab);
		
		tabPane.setStyle("");
		tabPane.getStylesheets().clear();
		tabPane.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/imageMetaDataTab/MetaTablesPane.css");
		
		tabPane.getSelectionModel().select(dataTableTab);
	}
	
	public void loadDataTable() {
		dataTableContainer.setCenter(new MarsTableFxView(meta.getDataTable()));
	}
	
	public void loadLog() {
		logPane.setMarkdown(meta.getLog());
	}
	
	Node getNode() {
		return tabPane;
	}

	@Override
	public void setImageMetaData(MarsImageMetadata meta) {
		this.meta = meta;
		loadDataTable();
		loadLog();
	}
}
