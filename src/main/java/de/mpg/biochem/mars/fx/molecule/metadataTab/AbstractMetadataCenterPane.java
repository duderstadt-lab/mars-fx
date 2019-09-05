package de.mpg.biochem.mars.fx.molecule.metadataTab;

import de.mpg.biochem.mars.fx.editor.LogPane;
import de.mpg.biochem.mars.fx.event.MarsImageMetadataEvent;
import de.mpg.biochem.mars.fx.event.MarsImageMetadataEventHandler;
import de.mpg.biochem.mars.fx.table.MarsTableFxView;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.BorderPane;

public abstract class AbstractMetadataCenterPane<I extends MarsImageMetadata> implements MetadataSubPane<I> {
	
	protected TabPane tabPane;
	protected BorderPane dataTableContainer;
	protected Tab dataTableTab;
	
	protected Tab logTab;
	protected BorderPane logContainer;
	protected LogPane logPane;
	
	protected MoleculeArchive<Molecule,I,MoleculeArchiveProperties> archive;
	
	protected I imageMetadata;
	
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
		tabPane.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/imageMetadataTab/MetaTablesPane.css");
		
		tabPane.getSelectionModel().select(dataTableTab);
		
		tabPane.addEventHandler(MarsImageMetadataEvent.MARS_IMAGE_METADATA_EVENT, new MarsImageMetadataEventHandler() {
		    @Override
		    public void onMarsImageMetadataSelectionChangedEvent(MarsImageMetadata marsImageMetadata) {
		        setMetadata((I)marsImageMetadata);
		    }
		});
	}
	
	protected void loadDataTable() {
		dataTableContainer.setCenter(new MarsTableFxView(imageMetadata.getDataTable()));
	}
	
	protected void loadLog() {
		logPane.setMarkdown(imageMetadata.getLog());
	}
	
	public void setArchive(MoleculeArchive<Molecule,I,MoleculeArchiveProperties> archive) {
		this.archive = archive;
	}

	public void setMetadata(I imageMetadata) {
		//Need to fix this...
		this.imageMetadata = imageMetadata;
		loadDataTable();
		loadLog();
	}
	
	public Node getNode() {
		return tabPane;
	}
}
