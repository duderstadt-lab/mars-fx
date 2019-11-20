package de.mpg.biochem.mars.fx.molecule;

import java.util.ArrayList;

import org.controlsfx.control.textfield.CustomTextField;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.event.InitializeMoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MetadataEvent;
import de.mpg.biochem.mars.fx.event.MetadataSelectionChangedEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.event.MoleculeSelectionChangedEvent;
import de.mpg.biochem.mars.fx.event.RefreshMetadataEvent;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
import de.mpg.biochem.mars.fx.plot.event.PlotEvent;
import de.mpg.biochem.mars.fx.plot.event.UpdatePlotAreaEvent;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

import de.mpg.biochem.mars.fx.plot.event.NewMoleculeRegionEvent;
import de.mpg.biochem.mars.fx.plot.event.NewMetadataRegionEvent;
import de.mpg.biochem.mars.fx.plot.event.NewMoleculePositionEvent;
import de.mpg.biochem.mars.fx.plot.event.NewMetadataPositionEvent;

public  abstract class AbstractMoleculesTab<M extends Molecule, C extends MoleculeSubPane, O extends MoleculeSubPane> extends AbstractMoleculeArchiveTab implements MoleculesTab<C, O> {
	protected SplitPane rootPane;
	protected C moleculeCenterPane;
	protected O moleculePropertiesPane;
	
	protected M molecule;
	
	protected CustomTextField filterField;
	protected Label nOfHitCountLabel;
    protected TableView<MoleculeIndexRow> moleculeIndexTable;
    protected ObservableList<MoleculeIndexRow> moleculeRowList = FXCollections.observableArrayList();
    
	protected FilteredList<MoleculeIndexRow> filteredData;

	public AbstractMoleculesTab() {
		super();
		
		Region moleculeIcon = new Region();
        moleculeIcon.getStyleClass().add("moleculeIcon");
        
        setIcon(moleculeIcon);
		
		rootPane = new SplitPane();
		ObservableList<Node> splitItems = rootPane.getItems();
		
		Node moleculeTableIndexContainer = buildMoleculeTableIndex();
		SplitPane.setResizableWithParent(moleculeTableIndexContainer, Boolean.FALSE);
		splitItems.add(moleculeTableIndexContainer);
		
		moleculeCenterPane = createMoleculeCenterPane();
		splitItems.add(moleculeCenterPane.getNode());
		
		moleculePropertiesPane = createMoleculePropertiesPane();
		//moleculePropertiesPane.getNode().maxWidth(220);
		SplitPane.setResizableWithParent(moleculePropertiesPane.getNode(), Boolean.FALSE);
		splitItems.add(moleculePropertiesPane.getNode());	
		
		rootPane.setDividerPositions(0.15f, 0.85f);
		
		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, this);
		getNode().addEventFilter(PlotEvent.PLOT_EVENT, new EventHandler<PlotEvent>() { 
			   @Override 
			   public void handle(PlotEvent e) { 
				   	if (e.getEventType().getName().equals("NEW_MOLECULE_REGION")) {
				   		molecule.putRegion(((NewMoleculeRegionEvent) e).getRegion());
				   		moleculePropertiesPane.fireEvent(new MoleculeSelectionChangedEvent(molecule));
				   		moleculeCenterPane.fireEvent(new UpdatePlotAreaEvent());
				   		e.consume();
				   	} else if (e.getEventType().getName().equals("NEW_METADATA_REGION")) {
				   		MarsImageMetadata metaData = archive.getImageMetadata(molecule.getImageMetadataUID());
				   		metaData.putRegion(((NewMetadataRegionEvent) e).getRegion());
				   		
				   		//Here we save the record in case we are working virtually
				   		archive.putImageMetadata(metaData);
				   		moleculeCenterPane.fireEvent(new UpdatePlotAreaEvent());
				   		//Remove this consume if we want to catch events in archive frame and redirect them to the metadata pane.
				   		e.consume();
				   	} else if (e.getEventType().getName().equals("NEW_MOLECULE_POSITION")) {
				   		molecule.putPosition(((NewMoleculePositionEvent) e).getPosition());
				   		moleculePropertiesPane.fireEvent(new MoleculeSelectionChangedEvent(molecule));
				   		moleculeCenterPane.fireEvent(new UpdatePlotAreaEvent());
				   		e.consume();
				   	} else if (e.getEventType().getName().equals("NEW_METADATA_POSITION")) {
				   		MarsImageMetadata metaData = archive.getImageMetadata(molecule.getImageMetadataUID());
				   		metaData.putPosition(((NewMetadataPositionEvent) e).getPosition());
				   		
				   		//Here we save the record in case we are working virtually
				   		archive.putImageMetadata(metaData);
				   		moleculeCenterPane.fireEvent(new UpdatePlotAreaEvent());
				   		//Remove this consume if we want to catch events in archive frame and redirect them to the metadata pane.
				   		e.consume();
				   	}
			   };
        });
		getNode().addEventFilter(MoleculeEvent.MOLECULE_EVENT, new EventHandler<MoleculeEvent>() { 
			   @SuppressWarnings("unchecked")
			@Override 
			   public void handle(MoleculeEvent e) { 
				   if (e.getEventType().getName().equals("INDICATOR_CHANGED")) {
				   		moleculeCenterPane.fireEvent(new UpdatePlotAreaEvent());
				   		e.consume();
				   } else if (e.getEventType().getName().equals("REFRESH_MOLECULE_EVENT")) {
					   //Reload molecule due to changes in the virtual store copy on the disk..
					    molecule = (M) archive.get(molecule.getUID());
				    	
					    moleculeCenterPane.fireEvent(new MoleculeSelectionChangedEvent(molecule));
				    	moleculePropertiesPane.fireEvent(new MoleculeSelectionChangedEvent(molecule));
						Platform.runLater(() -> {
							moleculeIndexTable.requestFocus();
						});
						e.consume();
				   }
			   };
		});
		
		setContent(rootPane);
	}
	
	@SuppressWarnings("unchecked")
	protected Node buildMoleculeTableIndex() {
		moleculeIndexTable = new TableView<MoleculeIndexRow>();
		moleculeIndexTable.setStyle("-fx-selection-bar: #c3c3c3;");// -fx-selection-bar-non-focused: salmon;");
    	
        TableColumn<MoleculeIndexRow, Integer> rowIndexCol = new TableColumn<>("Index");
        rowIndexCol.setCellValueFactory(molIndexRow ->
                new ReadOnlyObjectWrapper<>(molIndexRow.getValue().getIndex())
        );
        rowIndexCol.setPrefWidth(50);
        rowIndexCol.setSortable(false);
        moleculeIndexTable.getColumns().add(rowIndexCol);

        TableColumn<MoleculeIndexRow, String> UIDColumn = new TableColumn<>("UID");
        UIDColumn.setCellValueFactory(molIndexRow ->
                new ReadOnlyObjectWrapper<>(molIndexRow.getValue().getUID())
        );
        UIDColumn.setSortable(false);
        moleculeIndexTable.getColumns().add(UIDColumn);
        
        TableColumn<MoleculeIndexRow, String> TagsColumn = new TableColumn<>("Tags");
        TagsColumn.setCellValueFactory(molIndexRow ->
                new ReadOnlyObjectWrapper<>(molIndexRow.getValue().getTags())
        );
        TagsColumn.setSortable(false);
        moleculeIndexTable.getColumns().add(TagsColumn);
        
        TableColumn<MoleculeIndexRow, String>  metaUIDColumn = new TableColumn<>("metaUID");
        metaUIDColumn.setCellValueFactory(molIndexRow ->
                new ReadOnlyObjectWrapper<>(molIndexRow.getValue().getImageMetaDataUID())
        );
        metaUIDColumn.setSortable(false);
        moleculeIndexTable.getColumns().add(metaUIDColumn);
        
        moleculeIndexTable.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldMoleculeIndexRow, newMoleculeIndexRow) -> {
            	//Need to save the current record when we change in the case the virtual storage.
            	saveCurrentRecord();
            	
                if (newMoleculeIndexRow != null) {
                	molecule = (M) archive.get(newMoleculeIndexRow.getUID());
                	
                	//Update center pane and properties pane.
                	moleculeCenterPane.fireEvent(new MoleculeSelectionChangedEvent(molecule));
                	moleculePropertiesPane.fireEvent(new MoleculeSelectionChangedEvent(molecule));
            		Platform.runLater(() -> {
            			moleculeIndexTable.requestFocus();
            		});
                }
        });

        filteredData = new FilteredList<>(moleculeRowList, p -> true);
        
        filterField = new CustomTextField();
        nOfHitCountLabel = new Label();
        
        filterField.setLeft(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.SEARCH));
        filterField.setRight(nOfHitCountLabel);
        filterField.getStyleClass().add("find");
        filterField.textProperty().addListener((observable, oldValue, newValue) -> {
        	//If we don't clear the selection while we are searching the table will
        	//steal the focus after every letter we type.
        	moleculeIndexTable.getSelectionModel().clearSelection();
        	
            filteredData.setPredicate(molIndexRow -> {
                // If filter text is empty, display everything.
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                if (Integer.toString(molIndexRow.getIndex()).contains(newValue)) {
                    return true;
                } else if (molIndexRow.getUID().contains(newValue)) {
                    return true;
                } else if (molIndexRow.getTags().contains(newValue)) {
                	return true;
                } else if (molIndexRow.getImageMetaDataUID().contains(newValue)) {
                	return true;
                }
                return false;
            });
            nOfHitCountLabel.setText(filterField.getText().isEmpty() ? "" : "" + filteredData.size());
        });
        
        moleculeIndexTable.setItems(filteredData);
        
        
        filterField.setStyle(
                "-fx-background-radius: 2em; "
        );

        BorderPane borderPane = new BorderPane();
        Insets insets = new Insets(5);
       
        borderPane.setTop(filterField);
        BorderPane.setMargin(filterField, insets);
        
        borderPane.setCenter(moleculeIndexTable);
        
        return borderPane;
	}
	
	public void saveCurrentRecord() {
		System.out.println("Saving record with region number " + molecule.getRegionNames().size());
    	if (molecule != null)
    		archive.put(molecule);
    }
	
	public Molecule getSelectedMolecule() {
		return molecule;
	}
	
    @Override
	public Node getNode() {
		return rootPane;
	}
    
    @Override
    public void fireEvent(Event event) {
    	getNode().fireEvent(event);
    }

    @Override
    public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive) {
    	super.onInitializeMoleculeArchiveEvent(archive);
    	
    	moleculeCenterPane.fireEvent(new InitializeMoleculeArchiveEvent(archive));
    	moleculePropertiesPane.fireEvent(new InitializeMoleculeArchiveEvent(archive));
    	onMoleculeArchiveUnlockingEvent();
    }
    
    @Override
	public ArrayList<Menu> getMenus() {
		return new ArrayList<Menu>();
	}
	
	public abstract C createMoleculeCenterPane();
	
	public abstract O createMoleculePropertiesPane();
	
	protected class MoleculeIndexRow {
    	private int index;
    	
    	MoleculeIndexRow(int index) {
    		this.index = index;
    	}
    	
    	int getIndex() {
    		return index;
    	}
    	
    	String getUID() {
    		return archive.getUIDAtIndex(index);
    	}
    	
    	String getTags() {
    		return archive.getTagList(archive.getUIDAtIndex(index));
    	}
    	
    	String getImageMetaDataUID() {
    		return archive.getImageMetadataUIDforMolecule(archive.getUIDAtIndex(index));
    	}
    }

	@Override
	public void onMoleculeArchiveLockingEvent() {
		saveCurrentRecord();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onMoleculeArchiveUnlockingEvent() {
		moleculeRowList.clear();
		if (archive.getNumberOfMolecules() > 0) {
	    	for (int index = 0; index < archive.getNumberOfMolecules(); index++) {
	        	moleculeRowList.add(new MoleculeIndexRow(index));
	        }
	    	
    		MoleculeIndexRow newMoleculeIndexRow = new MoleculeIndexRow(0);
    		molecule = (M) archive.get(newMoleculeIndexRow.getUID());
	    	
	    	moleculeCenterPane.fireEvent(new MoleculeSelectionChangedEvent(molecule));
	    	moleculePropertiesPane.fireEvent(new MoleculeSelectionChangedEvent(molecule));
    	}
		Platform.runLater(() -> {
			moleculeIndexTable.requestFocus();
		});
	}

	@Override
	public void onMoleculeArchiveSavingEvent() {
		saveCurrentRecord();
	}
}