package de.mpg.biochem.mars.fx.molecule;

import java.util.ArrayList;

import org.controlsfx.control.textfield.CustomTextField;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.event.MoleculeSelectionChangedEvent;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
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

public abstract class AbstractMoleculesTab<M extends Molecule, I extends MarsImageMetadata, P extends MoleculeArchiveProperties, C extends MoleculeSubPane<?>, O extends MoleculeSubPane<?>> extends AbstractMoleculeArchiveTab<M,I,P> {
	protected SplitPane splitPane;
	protected C moleculeCenterPane;
	protected O moleculePropertiesPane;
	
	protected MoleculeArchive<M, I, P> archive;
	
	protected M molecule;
	
	protected CustomTextField filterField;
	protected Label nOfHitCountLabel;
    protected TableView<MoleculeIndexRow> moleculeIndexTable;
    protected ObservableList<MoleculeIndexRow> moleculeRowList = FXCollections.observableArrayList();
    
	protected FilteredList<MoleculeIndexRow> filteredData;

	public AbstractMoleculesTab() {
		super();
		
		splitPane = new SplitPane();
		ObservableList<Node> splitItems = splitPane.getItems();
		
		splitItems.add(buildMoleculeTableIndex());
		
		moleculeCenterPane = createMoleculeCenterPane();
		splitItems.add(moleculeCenterPane.getNode());
		
		moleculePropertiesPane = createMoleculePropertiesPane();
		SplitPane.setResizableWithParent(moleculePropertiesPane.getNode(), Boolean.FALSE);
		splitItems.add(moleculePropertiesPane.getNode());	
	}
	
	protected Node buildMoleculeTableIndex() {
		moleculeIndexTable = new TableView<MoleculeIndexRow>();
    	
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
                if (newMoleculeIndexRow != null) {
                	molecule = archive.get(newMoleculeIndexRow.getUID());
                	
                	//Update center pane and properties pane.
                	moleculeCenterPane.getNode().fireEvent(new MoleculeSelectionChangedEvent(molecule));
                	moleculePropertiesPane.getNode().fireEvent(new MoleculeSelectionChangedEvent(molecule));
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
    	if (molecule != null)
    		archive.put(molecule);
    }
    
    public void update() {
    	if (archive.getNumberOfImageMetadataRecords() > 0) {
    		MoleculeIndexRow newMoleculeIndexRow = new MoleculeIndexRow(0);
    		molecule = archive.get(newMoleculeIndexRow.getUID());
        	
    		//Update center pane and properties pane.
        	moleculeCenterPane.getNode().fireEvent(new MoleculeSelectionChangedEvent(molecule));
        	moleculePropertiesPane.getNode().fireEvent(new MoleculeSelectionChangedEvent(molecule));
    		Platform.runLater(() -> {
    			moleculeIndexTable.requestFocus();
    		});
    	}
    }
	
	public Node getNode() {
		return splitPane;
	}

	@Override
	public void setArchive(MoleculeArchive<M, I, P> archive) {
		this.archive = archive;
		loadData();
	}
    
    public void loadData() {
    	moleculeRowList.clear();

    	for (int index = 0; index < archive.getNumberOfMolecules(); index++) {
        	moleculeRowList.add(new MoleculeIndexRow(index));
        }
	}
    
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
}