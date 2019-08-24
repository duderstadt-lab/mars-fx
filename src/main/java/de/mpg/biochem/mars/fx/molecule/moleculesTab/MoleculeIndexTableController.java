package de.mpg.biochem.mars.fx.molecule.moleculesTab;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.controlsfx.control.textfield.CustomTextField;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.molecule.MoleculeArchiveSubTab;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

public class MoleculeIndexTableController implements MoleculeArchiveSubTab {
    
	private MoleculeArchive<Molecule,MarsImageMetadata,MoleculeArchiveProperties> archive;
	
	private BorderPane borderPane;
	
    private CustomTextField filterField;
    private Label nOfHitCountLabel;
    private TableView<MoleculeIndexRow> moleculeIndexTable;
    private ObservableList<MoleculeIndexRow> moleculeRowList = FXCollections.observableArrayList();
    
    private FilteredList<MoleculeIndexRow> filteredData;
    
    private ArrayList<MoleculeSubTab> moleculeSubTabControllers;
    
    private Molecule molecule;

    public MoleculeIndexTableController() {        
        initialize();
    }

    private void initialize() {
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
                	saveMolecule();
            		updateMoleculeSubTabs(newMoleculeIndexRow);
            		Platform.runLater(() -> {
            			moleculeIndexTable.requestFocus();
            		});
                }
        });
        /*
        moleculeIndexTable.focusedProperty().addListener(
                new ChangeListener<Boolean>() {
					@Override
					public void changed(ObservableValue<? extends Boolean> observable, Boolean wasFocused, Boolean isFocused) {
						if (!isFocused) {
			            	 moleculeIndexTable.requestFocus();
			            }
					}
                }
            );
        */
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

        borderPane = new BorderPane();
        Insets insets = new Insets(5);
       
        borderPane.setTop(filterField);
        BorderPane.setMargin(filterField, insets);
        
        borderPane.setCenter(moleculeIndexTable);
    }
    
    public Node getNode() {
    	return borderPane;
    }
    
    public void saveMolecule() {
    	archive.put(molecule);
    }
    
    public void updateMoleculeSubTabs(MoleculeIndexRow moleculeIndexRow) {
    	if (moleculeSubTabControllers == null)
    		return;
    	
    	molecule = archive.get(moleculeIndexRow.getUID());
    	
		for (MoleculeSubTab controller : moleculeSubTabControllers)
			controller.setMolecule(molecule);
    }
    
    public void loadData() {
    	moleculeRowList.clear();

    	for (int index = 0; index < archive.getNumberOfMolecules(); index++) {
        	moleculeRowList.add(new MoleculeIndexRow(index));
        }
	}
    
    public void setArchive(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive) {
    	this.archive = archive;
    	loadData();
    }
    
    public void setMoleculeSubTabList(ArrayList<MoleculeSubTab> moleculeSubTabControllers) {
    	this.moleculeSubTabControllers = moleculeSubTabControllers;
    }
    
    private class MoleculeIndexRow {
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
