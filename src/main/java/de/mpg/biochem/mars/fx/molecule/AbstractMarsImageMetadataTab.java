package de.mpg.biochem.mars.fx.molecule;

import java.util.ArrayList;
import org.controlsfx.control.textfield.CustomTextField;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import de.mpg.biochem.mars.fx.event.MarsImageMetadataSelectionChangedEvent;
import de.mpg.biochem.mars.fx.molecule.metadataTab.*;

public abstract class AbstractMarsImageMetadataTab<I extends MarsImageMetadata, C extends MetadataSubPane<? extends MarsImageMetadata>, O extends MetadataSubPane<? extends MarsImageMetadata>> extends AbstractMoleculeArchiveTab implements MarsImageMetadataTab<C,O> {
	
	protected SplitPane splitPane;
	protected C metadataCenterPane;
	protected O metadataPropertiesPane;
	
	protected I marsImageMetadata;
	
	protected CustomTextField filterField;
	protected Label nOfHitCountLabel;
	protected TableView<MetaIndexRow> metaIndexTable;
	protected ObservableList<MetaIndexRow> metaRowList = FXCollections.observableArrayList();
    
	protected FilteredList<MetaIndexRow> filteredData;

	public AbstractMarsImageMetadataTab() {
		super();
		
		Region microscopeIcon = new Region();
        microscopeIcon.getStyleClass().add("microscopeIcon");
        
		setIcon(microscopeIcon);
		
		splitPane = new SplitPane();
		ObservableList<Node> splitItems = splitPane.getItems();
		
		splitItems.add(buildMetadataTableIndex());
		
		metadataCenterPane = createMetadataCenterPane();
		splitItems.add(metadataCenterPane.getNode());
		
		metadataPropertiesPane = createMetadataPropertiesPane();
		SplitPane.setResizableWithParent(metadataPropertiesPane.getNode(), Boolean.FALSE);
		splitItems.add(metadataPropertiesPane.getNode());	
		
		setContent(splitPane);
	}
	
	@SuppressWarnings("unchecked")
	protected Node buildMetadataTableIndex() {
    	metaIndexTable = new TableView<MetaIndexRow>();
    	
        TableColumn<MetaIndexRow, Integer> rowIndexCol = new TableColumn<>("Index");
        rowIndexCol.setCellValueFactory(metaIndexRow ->
                new ReadOnlyObjectWrapper<>(metaIndexRow.getValue().getIndex())
        );
        rowIndexCol.setPrefWidth(50);
        rowIndexCol.setSortable(false);
        metaIndexTable.getColumns().add(rowIndexCol);

        TableColumn<MetaIndexRow, String> UIDColumn = new TableColumn<>("UID");
        UIDColumn.setCellValueFactory(metaIndexRow ->
                new ReadOnlyObjectWrapper<>(metaIndexRow.getValue().getUID())
        );
        UIDColumn.setSortable(false);
        metaIndexTable.getColumns().add(UIDColumn);
        
        TableColumn<MetaIndexRow, String> TagsColumn = new TableColumn<>("Tags");
        TagsColumn.setCellValueFactory(metaIndexRow ->
                new ReadOnlyObjectWrapper<>(metaIndexRow.getValue().getTags())
        );
        TagsColumn.setSortable(false);
        metaIndexTable.getColumns().add(TagsColumn);
        
        metaIndexTable.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldMetaIndexRow, newMetaIndexRow) -> {
                if (newMetaIndexRow != null) {
                	marsImageMetadata = (I) archive.getImageMetadata(newMetaIndexRow.getUID());
                	
                	//Update center pane and properties pane.
                	metadataCenterPane.getNode().fireEvent(new MarsImageMetadataSelectionChangedEvent(marsImageMetadata));
                	metadataPropertiesPane.getNode().fireEvent(new MarsImageMetadataSelectionChangedEvent(marsImageMetadata));
            		Platform.runLater(() -> {
            			metaIndexTable.requestFocus();
            		});
                }
        });
        
        filteredData = new FilteredList<>(metaRowList, p -> true);
        
        filterField = new CustomTextField();
        nOfHitCountLabel = new Label();
        
        filterField.setLeft(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.SEARCH));
        filterField.setRight(nOfHitCountLabel);
        filterField.getStyleClass().add("find");
        filterField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(metaIndexRow -> {
                // If filter text is empty, display everything.
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                if (Integer.toString(metaIndexRow.getIndex()).contains(newValue)) {
                    return true;
                } else if (metaIndexRow.getUID().contains(newValue)) {
                    return true;
                } else if (metaIndexRow.getTags().contains(newValue)) {
                	return true;
                }
                return false;
            });
            nOfHitCountLabel.setText(filterField.getText().isEmpty() ? "" : "" + filteredData.size());
        });
        
        metaIndexTable.setItems(filteredData);
        
        filterField.setStyle("-fx-background-radius: 2em; ");

        BorderPane borderPane = new BorderPane();
        Insets insets = new Insets(5);
       
        borderPane.setTop(filterField);
        BorderPane.setMargin(filterField, insets);
        
        borderPane.setCenter(metaIndexTable);
        
        return borderPane;
	}
    
    public void saveCurrentRecord() {
    	if (marsImageMetadata != null)
    		archive.putImageMetadata(marsImageMetadata);
    }
    
    @SuppressWarnings("unchecked")
	public void update() {
    	if (archive.getNumberOfImageMetadataRecords() > 0) {
    		MetaIndexRow newMetaIndexRow = new MetaIndexRow(0);
    		marsImageMetadata = (I) archive.getImageMetadata(newMetaIndexRow.getUID());
        	
        	//Update center pane and properties pane.
        	metadataCenterPane.getNode().fireEvent(new MarsImageMetadataSelectionChangedEvent(marsImageMetadata));
        	metadataPropertiesPane.getNode().fireEvent(new MarsImageMetadataSelectionChangedEvent(marsImageMetadata));
			Platform.runLater(() -> {
				metaIndexTable.requestFocus();
			});
    	}
    }

	@Override
	public void setArchive(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive) {
		this.archive = archive;
		metaRowList.clear();

    	for (int index = 0; index < archive.getNumberOfImageMetadataRecords(); index++) {
        	metaRowList.add(new MetaIndexRow(index));
        }
    	metadataCenterPane.setArchive(archive);
    	metadataPropertiesPane.setArchive(archive);
    	update();
	}
	
	@Override
	public Node getNode() {
		return splitPane;
	}
	
	@Override
	public ArrayList<Menu> getMenus() {
		return new ArrayList<Menu>();
	}
	
	public abstract C createMetadataCenterPane();
	
	public abstract O createMetadataPropertiesPane();
	
	protected class MetaIndexRow {
    	private int index;
    	
    	MetaIndexRow(int index) {
    		this.index = index;
    	}
    	
    	int getIndex() {
    		return index;
    	}
    	
    	String getUID() {
    		return archive.getImageMetadataUIDAtIndex(index);
    	}
    	
    	String getTags() {
    		return archive.getImageMetadataTagList(archive.getImageMetadataUIDAtIndex(index));
    	}
    }
}
