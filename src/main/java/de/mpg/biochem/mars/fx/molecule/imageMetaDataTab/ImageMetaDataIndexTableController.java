package de.mpg.biochem.mars.fx.molecule.imageMetaDataTab;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.controlsfx.control.textfield.CustomTextField;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.molecule.MoleculeArchiveSubTab;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
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

public class ImageMetaDataIndexTableController implements MoleculeArchiveSubTab {
    
	private MoleculeArchive archive;
	
	private BorderPane borderPane;
	
    private CustomTextField filterField;
    private Label nOfHitCountLabel;
    private TableView<MetaIndexRow> metaIndexTable;
    private ObservableList<MetaIndexRow> metaRowList = FXCollections.observableArrayList();
    
    private FilteredList<MetaIndexRow> filteredData;
    
    private ArrayList<ImageMetaDataSubTab> metaSubTabControllers;

    public ImageMetaDataIndexTableController() {        
        initialize();
    }

    private void initialize() {
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
            		updateMetaSubTabs(newMetaIndexRow);
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
        
        
        filterField.setStyle(
                "-fx-background-radius: 2em; "
        );

        borderPane = new BorderPane();
        Insets insets = new Insets(5);
       
        borderPane.setTop(filterField);
        BorderPane.setMargin(filterField, insets);
        
        borderPane.setCenter(metaIndexTable);
    }
    
    public Node getNode() {
    	return borderPane;
    }
    
    public void updateMetaSubTabs(MetaIndexRow metaIndexRow) {
    	if (metaSubTabControllers == null)
    		return;
    	
		for (ImageMetaDataSubTab controller : metaSubTabControllers)
			controller.setImageMetaData(archive.getImageMetadata(metaIndexRow.getUID()));
    }
    
    public void loadData() {
    	metaRowList.clear();

    	for (int index = 0; index < archive.getNumberOfImageMetadataRecords(); index++) {
        	metaRowList.add(new MetaIndexRow(index));
        }
	}
    
    public void setArchive(MoleculeArchive archive) {
    	this.archive = archive;
    	loadData();
    }
    
    public void setMetaSubTabList(ArrayList<ImageMetaDataSubTab> moleculeSubTabControllers) {
    	this.metaSubTabControllers = moleculeSubTabControllers;
    }
    
    private class MetaIndexRow {
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
