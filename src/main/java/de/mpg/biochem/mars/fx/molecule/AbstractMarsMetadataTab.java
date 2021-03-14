/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2021 Karl Duderstadt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package de.mpg.biochem.mars.fx.molecule;

import java.io.IOException;
import java.util.ArrayList;
import org.controlsfx.control.textfield.CustomTextField;
import org.scijava.Context;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.util.MarsUtil;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import de.mpg.biochem.mars.fx.event.InitializeMoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MetadataEvent;
import de.mpg.biochem.mars.fx.event.MetadataSelectionChangedEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.molecule.AbstractMoleculesTab.MoleculeIndexRow;
import de.mpg.biochem.mars.fx.molecule.metadataTab.*;
import de.mpg.biochem.mars.fx.plot.event.PlotEvent;
import de.mpg.biochem.mars.fx.plot.event.UpdatePlotAreaEvent;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.JsonConvertibleRecord;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;

public abstract class AbstractMarsMetadataTab<I extends MarsMetadata, C extends MetadataSubPane, O extends MetadataSubPane> extends AbstractMoleculeArchiveTab implements MarsMetadataTab<C,O> {
	
	protected SplitPane rootPane;
	protected C metadataCenterPane;
	protected O metadataPropertiesPane;
	
	protected I marsMetadata;
	
	protected CustomTextField filterField;
	protected Label nOfHitCountLabel;
	protected TableView<MetaIndexRow> metaIndexTable;
	protected ObservableList<MetaIndexRow> metaRowList = FXCollections.observableArrayList();
    
	protected FilteredList<MetaIndexRow> filteredData;
	
	protected ChangeListener<MetaIndexRow> metaIndexTableListener;
	
	protected double propertiesDividerPostion = 0.87f;

	public AbstractMarsMetadataTab(final Context context) {
		super(context);
		
		Region microscopeIcon = new Region();
        microscopeIcon.getStyleClass().add("microscopeIcon");
        
		setIcon(microscopeIcon);
		
		rootPane = new SplitPane();
		
		Node metadataTableIndexContainer = buildMetadataTableIndex();
		SplitPane.setResizableWithParent(metadataTableIndexContainer, Boolean.FALSE);
		rootPane.getItems().add(metadataTableIndexContainer);
		
		metadataCenterPane = createMetadataCenterPane(context);
		rootPane.getItems().add(metadataCenterPane.getNode());
		
		metadataPropertiesPane = createMetadataPropertiesPane(context);
		SplitPane.setResizableWithParent(metadataPropertiesPane.getNode(), Boolean.FALSE);
		rootPane.getItems().add(metadataPropertiesPane.getNode());
		
		rootPane.setDividerPositions(0.15f, 0.87f);
		
		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, this);
		getNode().addEventFilter(MetadataEvent.METADATA_EVENT, new EventHandler<MetadataEvent>() { 
			@SuppressWarnings("unchecked")
			@Override
			public void handle(MetadataEvent e) {
				if (e.getEventType().getName().equals("REFRESH_METADATA_EVENT")) {
					//We reload the record from the archive.. If virtual this will reload from disk...
					marsMetadata = (I) archive.getMetadata(marsMetadata.getUID());
		        	
		        	//Update center pane and properties pane.
		        	metadataCenterPane.fireEvent(new MetadataSelectionChangedEvent(marsMetadata));
		        	metadataPropertiesPane.fireEvent(new MetadataSelectionChangedEvent(marsMetadata));
					Platform.runLater(() -> {
						metaIndexTable.requestFocus();
						//metaIndexTable.getSelectionModel().select(metaIndexTable.getSelectionModel().selectedItemProperty().get());
					});
					e.consume();
				} else if (e.getEventType().getName().equals("TAGS_CHANGED")) {
					metaIndexTable.refresh();
				    e.consume();
			    }
			}
		});
		
		getTab().setContent(rootPane);
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
        
        metaIndexTableListener = new ChangeListener<MetaIndexRow> () {
        	public void changed(ObservableValue<? extends MetaIndexRow> observable, MetaIndexRow oldMetaIndexRow, MetaIndexRow newMetaIndexRow) {
        		//Need to save the current record when we change in the case the virtual storage.
	        	saveCurrentRecord();
        		
        		if (newMetaIndexRow != null) {
                	marsMetadata = (I) archive.getMetadata(newMetaIndexRow.getUID());
                	
                	//Update center pane and properties pane.
                	metadataCenterPane.getNode().fireEvent(new MetadataSelectionChangedEvent(marsMetadata));
                	metadataPropertiesPane.getNode().fireEvent(new MetadataSelectionChangedEvent(marsMetadata));
            		Platform.runLater(() -> {
            			metaIndexTable.requestFocus();
            		});
                }
        	}
        };
        
        metaIndexTable.getSelectionModel().selectedItemProperty().addListener(metaIndexTableListener);
        
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
	
	public void showProperties() {
		if (!rootPane.getItems().contains(metadataPropertiesPane.getNode())) {
			rootPane.getItems().add(metadataPropertiesPane.getNode());
			rootPane.setDividerPosition(1, propertiesDividerPostion);
		}
	}
	
	public void hideProperties() {
		if (rootPane.getItems().contains(metadataPropertiesPane.getNode())) {
			propertiesDividerPostion = rootPane.getDividerPositions()[1];
			rootPane.getItems().remove(metadataPropertiesPane.getNode());
		}
	}
    
    public void saveCurrentRecord() {
    	if (marsMetadata != null)
    		archive.putMetadata(marsMetadata);
    }
    
    public MarsMetadata getSelectedMetadata() {
    	return marsMetadata;
    }

    @Override
    public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive) {
    	super.onInitializeMoleculeArchiveEvent(archive);

    	metadataCenterPane.fireEvent(new InitializeMoleculeArchiveEvent(archive));
    	metadataPropertiesPane.fireEvent(new InitializeMoleculeArchiveEvent(archive));
    	onMoleculeArchiveUnlockEvent();
    }
	
	@Override
	public Node getNode() {
		return rootPane;
	}
	
	@Override
	public ArrayList<Menu> getMenus() {
		return new ArrayList<Menu>();
	}
	
	@Override
	protected void createIOMaps() {
		
		setJsonField("searchField", 
			jGenerator -> jGenerator.writeStringField("searchField", filterField.getText()),
			jParser -> filterField.setText(jParser.getText()));		
		
		setJsonField("marsMetadataSelectionUID", 
			jGenerator -> jGenerator.writeStringField("marsMetadataSelectionUID", marsMetadata.getUID()),
			jParser -> {
		        String moleculeSelectionUID = jParser.getText();
		    	for (int index = 0; index < filteredData.size(); index++) {
		    		if (filteredData.get(index).getUID().equals(moleculeSelectionUID)) {
		    			metaIndexTable.getSelectionModel().select(index);
		    			metaIndexTable.scrollTo(index);
		    		}
		    	}
			});
			
		setJsonField("centerPane", 
			jGenerator -> {
				jGenerator.writeFieldName("centerPane");
				if (metadataCenterPane instanceof JsonConvertibleRecord)
					((JsonConvertibleRecord) metadataCenterPane).toJSON(jGenerator);
			}, 
			jParser -> {
				if (metadataCenterPane instanceof JsonConvertibleRecord)
					((JsonConvertibleRecord) metadataCenterPane).fromJSON(jParser);
		 	});
		
		/*
		 * 
		 * The fields below are needed for backwards compatibility.
		 * 
		 * Please remove for a future release.
		 * 
		 */
		
		setJsonField("SearchField", null, 
			jParser -> filterField.setText(jParser.getText()));		
			
		setJsonField("MarsMetadataSelectionUID", null,
			jParser -> {
		        String moleculeSelectionUID = jParser.getText();
		    	for (int index = 0; index < filteredData.size(); index++) {
		    		if (filteredData.get(index).getUID().equals(moleculeSelectionUID)) {
		    			metaIndexTable.getSelectionModel().select(index);
		    			metaIndexTable.scrollTo(index);
		    		}
		    	}
			});
			
		setJsonField("CenterPane", null, 
			jParser -> {
				if (metadataCenterPane instanceof JsonConvertibleRecord)
					((JsonConvertibleRecord) metadataCenterPane).fromJSON(jParser);
		 	});
			
	}
	
	public abstract C createMetadataCenterPane(final Context context);
	
	public abstract O createMetadataPropertiesPane(final Context context);
	
	protected class MetaIndexRow {
    	private int index;
    	
    	MetaIndexRow(int index) {
    		this.index = index;
    	}
    	
    	int getIndex() {
    		return index;
    	}
    	
    	String getUID() {
    		return archive.getMetadataUIDAtIndex(index);
    	}
    	
    	String getTags() {
    		return archive.getMetadataTagList(archive.getMetadataUIDAtIndex(index));
    	}
    }

	@Override
	public void onMoleculeArchiveLockEvent() {
		saveCurrentRecord();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onMoleculeArchiveUnlockEvent() {
		metaIndexTable.getSelectionModel().selectedItemProperty().removeListener(metaIndexTableListener);
    	String currentUID = "";
    	if (metaIndexTable.getItems().size() > 0)
    		currentUID = metaIndexTable.getSelectionModel().getSelectedItem().getUID();
    	metaRowList.clear();
    	if (archive.getNumberOfMetadatas() > 0) {
    		for (int index = 0; index < archive.getNumberOfMetadatas(); index++) {
    			MetaIndexRow row = new MetaIndexRow(index);
    			metaRowList.add(row);
    		}
    		
    		int newIndex = 0;
	    	for (int index = 0; index < filteredData.size(); index++) {
	    		if (filteredData.get(index).getUID().equals(currentUID))
	    			newIndex = index;
	    	}
		
    		
    		metaIndexTable.getSelectionModel().select(newIndex);
    		marsMetadata = (I) archive.getMetadata(metaIndexTable.getSelectionModel().getSelectedItem().getUID());
        	metadataCenterPane.fireEvent(new MetadataSelectionChangedEvent(marsMetadata));
        	metadataPropertiesPane.fireEvent(new MetadataSelectionChangedEvent(marsMetadata));
    	}
		Platform.runLater(() -> {
			metaIndexTable.requestFocus();
		});
		metaIndexTable.getSelectionModel().selectedItemProperty().addListener(metaIndexTableListener);
	}

	@Override
	public void onMoleculeArchiveSavingEvent() {
		saveCurrentRecord();
	}
	
	@Override
	public String getName() {
		return "MetadataTab";
	}
}
