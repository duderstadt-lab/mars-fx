package de.mpg.biochem.mars.fx.molecule.metadataTab;

import de.mpg.biochem.mars.fx.editor.LogPane;
import de.mpg.biochem.mars.fx.event.MarsImageMetadataEvent;
import de.mpg.biochem.mars.fx.table.MarsTableFxView;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.BorderPane;

public abstract class AbstractMetadataCenterPane<I extends MarsImageMetadata> implements MetadataSubPane {
	
	protected TabPane tabPane;
	protected BorderPane dataTableContainer;
	protected Tab dataTableTab;
	
	protected Tab logTab;
	protected BorderPane logContainer;
	protected LogPane logPane;
	
	protected I marsImageMetadata;
	
	public AbstractMetadataCenterPane() {
		tabPane = new TabPane();
		tabPane.setFocusTraversable(false);
		
		initializeTabs();
	}
	
	protected void initializeTabs() {
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
		tabPane.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/metadataTab/MetaTablesPane.css");
		
		tabPane.getSelectionModel().select(dataTableTab);
		
		tabPane.addEventHandler(MarsImageMetadataEvent.MARS_IMAGE_METADATA_EVENT, this);
	}
	
	protected void loadDataTable() {
		dataTableContainer.setCenter(new MarsTableFxView(marsImageMetadata.getDataTable()));
	}
	
	protected void loadLog() {
		logPane.setMarkdown(marsImageMetadata.getLog());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onMarsImageMetadataSelectionChangedEvent(MarsImageMetadata marsImageMetadata) {
		this.marsImageMetadata = (I) marsImageMetadata;
		loadDataTable();
		loadLog();
	}
	
	@Override
	public Node getNode() {
		return tabPane;
	}
	
	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}
	
	@Override
    public void handle(MarsImageMetadataEvent event) {
	   event.invokeHandler(this);
	   event.consume();
    } 
}
