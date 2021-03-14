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
import java.util.List;

import org.controlsfx.control.textfield.CustomTextField;
import org.scijava.Context;

import com.fasterxml.jackson.core.JsonToken;

import javafx.beans.property.ObjectProperty;

import javafx.scene.layout.StackPane;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.Messages;
import de.mpg.biochem.mars.fx.bdv.MarsBdvFrame;
import de.mpg.biochem.mars.fx.event.InitializeMoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MetadataEvent;
import de.mpg.biochem.mars.fx.event.MetadataSelectionChangedEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveLockEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveSavingEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveUnlockEvent;
import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.event.MoleculeSelectionChangedEvent;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
import de.mpg.biochem.mars.fx.plot.event.PlotEvent;
import de.mpg.biochem.mars.fx.plot.event.UpdatePlotAreaEvent;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.JsonConvertibleRecord;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.util.MarsPosition;
import de.mpg.biochem.mars.util.MarsRegion;
import de.mpg.biochem.mars.util.MarsUtil;
import impl.org.controlsfx.skin.CustomTextFieldSkin;
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
import de.mpg.biochem.mars.fx.plot.event.*;

import javax.swing.RowFilter;
import javax.swing.SwingUtilities;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public abstract class AbstractMoleculesTab<M extends Molecule, C extends MoleculeSubPane, O extends MoleculeSubPane> extends AbstractMoleculeArchiveTab implements MoleculesTab<C, O> {
	protected SplitPane rootPane;
	protected C moleculeCenterPane;
	protected O moleculePropertiesPane;
	
	protected M molecule;
	
	protected CustomTextField filterField;
	protected CustomTextFieldSkin filterFieldSkin;
    protected TableView<MoleculeIndexRow> moleculeIndexTable;
    protected ObservableList<MoleculeIndexRow> moleculeRowList = FXCollections.observableArrayList();
    
	protected FilteredList<MoleculeIndexRow> filteredData;
	
	protected MarsBdvFrame[] marsBdvFrames;
	
	protected ChangeListener<MoleculeIndexRow> moleculeIndexTableListener;
	protected double propertiesDividerPostion = 0.87f;

	public AbstractMoleculesTab(final Context context) {
		super(context);
		
		Region moleculeIcon = new Region();
        moleculeIcon.getStyleClass().add("moleculeIcon");
        
        setIcon(moleculeIcon);
		
		rootPane = new SplitPane();
		
		Node moleculeTableIndexContainer = buildMoleculeTableIndex();
		SplitPane.setResizableWithParent(moleculeTableIndexContainer, Boolean.FALSE);
		rootPane.getItems().add(moleculeTableIndexContainer);
		
		moleculeCenterPane = createMoleculeCenterPane(context);
		rootPane.getItems().add(moleculeCenterPane.getNode());
		
		moleculePropertiesPane = createMoleculePropertiesPane(context);
		SplitPane.setResizableWithParent(moleculePropertiesPane.getNode(), Boolean.FALSE);
		rootPane.getItems().add(moleculePropertiesPane.getNode());	
		
		rootPane.setDividerPositions(0.15f, 0.87f);
		
		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, this);
		getNode().addEventFilter(PlotEvent.PLOT_EVENT, new EventHandler<PlotEvent>() { 
			   @Override 
			   public void handle(PlotEvent e) { 
				   	switch (e.getEventType().getName()) {
					   	case "NEW_MOLECULE_REGION":
					   		newMoleculeRegion(e);
					   		break;
					   	case "UPDATE_MOLECULE_REGION":
					   		updateMoleculeRegion(e);
					   		break;
					   	case "NEW_METADATA_REGION":
					   		newMetadataRegion(e);
					   		break;
					   	case "UPDATE_METADATA_REGION":
					   		updateMetadataRegion(e);
					   		break;
					   	case "NEW_MOLECULE_POSITION":
					   		newMoleculePosition(e);
					   		break;
					   	case "UPDATE_MOLECULE_POSITION":
					   		updateMoleculePosition(e);
					   		break;
					   	case "NEW_METADATA_POSITION":
					   		newMetadataPosition(e);
					   		break;
					   	case "UPDATE_METADATA_POSITION":
					   		updateMetadataPosition(e);
					   	default:
					   		return;
				   	}
				   	moleculeCenterPane.fireEvent(new UpdatePlotAreaEvent());
			   		//Remove this consume if we want to catch events in archive frame and redirect them to the metadata pane.
			   		e.consume();
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
				   } else if (e.getEventType().getName().equals("REFRESH_MOLECULE_PROPERTIES_EVENT")) {
				    	moleculePropertiesPane.fireEvent(new MoleculeSelectionChangedEvent(molecule));
						Platform.runLater(() -> {
							moleculeIndexTable.requestFocus();
						});
						e.consume();
				   } else if (e.getEventType().getName().equals("TAGS_CHANGED")) {
					   moleculeIndexTable.refresh();
					   e.consume();
				   }
			   };
		});
		
		getTab().setContent(rootPane);
	}
	
	private void newMoleculeRegion(final PlotEvent e) {
		int num = 1;
		String name = "Region 1";
		while (molecule.hasRegion(name)) {
			num++;
			name = "Region " + num;
		}
		MarsRegion roi = ((NewMoleculeRegionEvent) e).getRegion();
		roi.setName(name);
		molecule.putRegion(roi);
   		moleculePropertiesPane.fireEvent(new MoleculeSelectionChangedEvent(molecule));
	}
	
	private void newMetadataRegion(final PlotEvent e) {
		MarsMetadata metaData = archive.getMetadata(molecule.getMetadataUID());
		int num = 1;
		String name = "Region 1";
		while (metaData.hasRegion(name)) {
			num++;
			name = "Region " + num;
		}
		MarsRegion roi = ((NewMetadataRegionEvent) e).getRegion();
		roi.setName(name);
		metaData.putRegion(roi);
		archive.putMetadata(metaData);
	}
	
	private void newMoleculePosition(final PlotEvent e) {
		int num = 1;
		String name = "Position 1";
		while (molecule.hasPosition(name)) {
			num++;
			name = "Position " + num;
		}
		MarsPosition poi = ((NewMoleculePositionEvent) e).getPosition();
		poi.setName(name);
		molecule.putPosition(poi);
   		moleculePropertiesPane.fireEvent(new MoleculeSelectionChangedEvent(molecule));
	}
	
	private void newMetadataPosition(final PlotEvent e) {
		MarsMetadata metaData = archive.getMetadata(molecule.getMetadataUID());
		int num = 1;
		String name = "Position 1";
		while (metaData.hasPosition(name)) {
			num++;
			name = "Position " + num;
		}
		MarsPosition poi = ((NewMetadataPositionEvent) e).getPosition();
		poi.setName(name);
		metaData.putPosition(poi);
		archive.putMetadata(metaData);
	}
	
	private void updateMoleculeRegion(final PlotEvent e) {
		MarsRegion newRoi = ((UpdateMoleculeRegionEvent) e).getRegion();
   		MarsRegion oldRoi = molecule.getRegion(newRoi.getName());
   		oldRoi.setStart(newRoi.getStart());
   		oldRoi.setEnd(newRoi.getEnd());
   		moleculePropertiesPane.fireEvent(new MoleculeSelectionChangedEvent(molecule));
	}
	
	private void updateMetadataRegion(final PlotEvent e) {
		MarsMetadata metaData = archive.getMetadata(molecule.getMetadataUID());
		MarsRegion newRoi = ((NewMetadataRegionEvent) e).getRegion();
   		MarsRegion oldRoi = metaData.getRegion(newRoi.getName());
   		oldRoi.setStart(newRoi.getStart());
   		oldRoi.setEnd(newRoi.getEnd());
   		archive.putMetadata(metaData);
	}
	
	private void updateMoleculePosition(final PlotEvent e) {
   		MarsPosition newPoi = ((UpdateMoleculePositionEvent) e).getPosition();
   		MarsPosition oldPoi = molecule.getPosition(newPoi.getName());
   		oldPoi.setPosition(newPoi.getPosition());
   		moleculePropertiesPane.fireEvent(new MoleculeSelectionChangedEvent(molecule));
	}
	
	private void updateMetadataPosition(final PlotEvent e) {
		MarsMetadata metaData = archive.getMetadata(molecule.getMetadataUID());
		MarsPosition newPoi = ((UpdateMetadataPositionEvent) e).getPosition();
		MarsPosition oldPoi = metaData.getPosition(newPoi.getName());
   		oldPoi.setPosition(newPoi.getPosition());
   		archive.putMetadata(metaData);
	}
	
	@SuppressWarnings("unchecked")
	protected Node buildMoleculeTableIndex() {
        BorderPane borderPane = new BorderPane();
		
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
        
        moleculeIndexTableListener = new ChangeListener<MoleculeIndexRow> () {
        	public void changed(ObservableValue<? extends MoleculeIndexRow> observable, MoleculeIndexRow oldMoleculeIndexRow, MoleculeIndexRow newMoleculeIndexRow) {
	        	//Need to save the current record when we change in the case the virtual storage.
	        	saveCurrentRecord();
	        	
	            if (newMoleculeIndexRow != null) {
	            	molecule = (M) archive.get(newMoleculeIndexRow.getUID());
	            	
	            	//Update center pane and properties pane.
	            	moleculeCenterPane.fireEvent(new MoleculeSelectionChangedEvent(molecule));
	            	moleculePropertiesPane.fireEvent(new MoleculeSelectionChangedEvent(molecule));
	            	if (marsBdvFrames != null) {
	            		SwingUtilities.invokeLater(new Runnable() {
	    		            @Override
	    		            public void run() {
			            		if (molecule != null)
			            			for (int i=0; i<marsBdvFrames.length; i++)
			            				if (marsBdvFrames[i] != null)
			            					marsBdvFrames[i].setMolecule(molecule);
	    		            }
	    		        });
	            	}
	            		
	        		Platform.runLater(() -> {
	        			moleculeIndexTable.requestFocus();
	        		});
	            }
        	}
        };
        
        moleculeIndexTable.getSelectionModel().selectedItemProperty().addListener(moleculeIndexTableListener);

        filteredData = new FilteredList<>(moleculeRowList, p -> true);
        filterField = new CustomTextField();
        filterFieldSkin = new CustomTextFieldSkin(filterField) {
            @Override public ObjectProperty<Node> leftProperty() {
                return filterField.leftProperty();
            }
            
            @Override public ObjectProperty<Node> rightProperty() {
                return filterField.rightProperty();
            }
        };
        filterField.setSkin(filterFieldSkin);
        filterField.setLeft(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.SEARCH));
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
                
            	String[] searchList = newValue.split(",");
            	for (String str : searchList) {
            		if (!molIndexRow.contains(str.trim()))
            			return false;
            	}
                return true;
            });
            
            //Super hacky way of ensuring the textfield can resize when not selected with no text.
            if (filterField.getText().isEmpty()) {
            	//filterField.setRight(null);
            	Node nodeToRemove = null;
            	for (Node node : filterFieldSkin.getChildren()) {
            		if (node instanceof StackPane)
            			if (((StackPane) node).getStyleClass().get(0).equals("right-pane"))
            				nodeToRemove = node;
            	}
            	if (nodeToRemove != null)
            		filterFieldSkin.getChildren().remove(nodeToRemove);
            } else {
            	filterField.setRight(new Label(filteredData.size() + " "));
            }
        });
        
        moleculeIndexTable.setItems(filteredData);
        
        filterField.setStyle(
                "-fx-background-radius: 2em; "
        );
        
        Insets insets = new Insets(5);
       
        borderPane.setTop(filterField);
        BorderPane.setMargin(filterField, insets);
        
        borderPane.setCenter(moleculeIndexTable);
        
        return borderPane;
	}
	
	public void showProperties() {
		if (!rootPane.getItems().contains(moleculePropertiesPane.getNode())) {
			rootPane.getItems().add(moleculePropertiesPane.getNode());
			rootPane.setDividerPosition(1, propertiesDividerPostion);
		}
	}
	
	public void hideProperties() {
		if (rootPane.getItems().contains(moleculePropertiesPane.getNode())) {
			propertiesDividerPostion = rootPane.getDividerPositions()[1];
			rootPane.getItems().remove(moleculePropertiesPane.getNode());
		}
	}
	
	public void saveCurrentRecord() {
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
    public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive) {
    	super.onInitializeMoleculeArchiveEvent(archive);
    	
    	moleculeCenterPane.fireEvent(new InitializeMoleculeArchiveEvent(archive));
    	moleculePropertiesPane.fireEvent(new InitializeMoleculeArchiveEvent(archive));
    	onMoleculeArchiveUnlockEvent();
    }
    
    @Override
	public ArrayList<Menu> getMenus() {
		return null;
	}
    
    @Override
    public void setMarsBdvFrames(MarsBdvFrame[] marsBdvFrames) {
    	this.marsBdvFrames = marsBdvFrames;
    }

	@Override
	protected void createIOMaps() {
		
		setJsonField("searchField", 
			jGenerator -> jGenerator.writeStringField("searchField", filterField.getText()),
			jParser -> filterField.setText(jParser.getText()));
		
		setJsonField("moleculeSelectionUID", 
			jGenerator -> jGenerator.writeStringField("moleculeSelectionUID", molecule.getUID()),
			jParser -> {
		        String moleculeSelectionUID = jParser.getText();
		    	for (int index = 0; index < filteredData.size(); index++) {
		    		if (filteredData.get(index).getUID().equals(moleculeSelectionUID)) {
		    			moleculeIndexTable.getSelectionModel().select(index);
		    			moleculeIndexTable.scrollTo(index);
		    		}
		    	}
			});
		
		setJsonField("centerPane", 
			jGenerator -> {
				jGenerator.writeFieldName("centerPane");
				if (moleculeCenterPane instanceof JsonConvertibleRecord)
				((JsonConvertibleRecord) moleculeCenterPane).toJSON(jGenerator);
			}, 
			jParser -> {
				if (moleculeCenterPane instanceof JsonConvertibleRecord)
					((JsonConvertibleRecord) moleculeCenterPane).fromJSON(jParser);
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
			
		setJsonField("MoleculeSelectionUID", null,
			jParser -> {
		        String moleculeSelectionUID = jParser.getText();
		    	for (int index = 0; index < filteredData.size(); index++) {
		    		if (filteredData.get(index).getUID().equals(moleculeSelectionUID)) {
		    			moleculeIndexTable.getSelectionModel().select(index);
		    			moleculeIndexTable.scrollTo(index);
		    		}
		    	}
			});
		
		setJsonField("CenterPane", null, 
			jParser -> {
				if (moleculeCenterPane instanceof JsonConvertibleRecord)
					((JsonConvertibleRecord) moleculeCenterPane).fromJSON(jParser);
		 	});
	}
	
	public abstract C createMoleculeCenterPane(final Context context);
	
	public abstract O createMoleculePropertiesPane(final Context context);
	
	protected class MoleculeIndexRow {
    	private int index;
    	
    	MoleculeIndexRow(int index) {
    		this.index = index;
    	}
    	
    	boolean contains(String str) {
    		if (Integer.toString(getIndex()).contains(str)) {
                return true;
            } else if (getUID().contains(str)) {
                return true;
            } else if (getTags().contains(str)) {
            	return true;
            } else if (getImageMetaDataUID().contains(str)) {
            	return true;
            } else {
            	return false;
            }
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
    		return archive.getMetadataUIDforMolecule(archive.getUIDAtIndex(index));
    	}
    }

	@Override
	public void onMoleculeArchiveLockEvent() {
		saveCurrentRecord();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onMoleculeArchiveUnlockEvent() {
    	moleculeIndexTable.getSelectionModel().selectedItemProperty().removeListener(moleculeIndexTableListener);
    	String currentUID = "";
    	if (molecule != null)
    		currentUID = molecule.getUID();
		moleculeRowList.clear();
		if (archive.getNumberOfMolecules() > 0) {
	    	for (int index = 0; index < archive.getNumberOfMolecules(); index++) {
	    		MoleculeIndexRow row = new MoleculeIndexRow(index);
	        	moleculeRowList.add(row);
	        }
	    	
	    	//Manually update filter in case a script changed the tags
	    	filteredData.setPredicate(molIndexRow -> {
	    		String newValue = filterField.getText();
	    		
                // If filter text is empty, display everything.
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
            	String[] searchList = newValue.split(",");
            	for (String str : searchList) {
            		if (!molIndexRow.contains(str.trim()))
            			return false;
            	}
                return true;
            });
	    	
	    	int newIndex = 0;
	    	for (int index = 0; index < filteredData.size(); index++) {
	    		if (filteredData.get(index).getUID().equals(currentUID))
	    			newIndex = index;
	    	}

	    	if (filteredData.size() > 0) {
	    		moleculeIndexTable.getSelectionModel().select(newIndex);
	    		molecule = (M) archive.get(moleculeIndexTable.getSelectionModel().getSelectedItem().getUID());
	    		moleculeCenterPane.fireEvent(new MoleculeArchiveUnlockEvent(archive));
		    	moleculeCenterPane.fireEvent(new MoleculeSelectionChangedEvent(molecule));
		    	moleculePropertiesPane.fireEvent(new MoleculeSelectionChangedEvent(molecule));
	    	}
    	}
		
		Platform.runLater(() -> {
			moleculeIndexTable.requestFocus();
		});
		moleculeIndexTable.getSelectionModel().selectedItemProperty().addListener(moleculeIndexTableListener);
	}

	@Override
	public void onMoleculeArchiveSavingEvent() {
		//The archive is always locked when saving and that saves the current record...
		//We don't need to do it twice.
		//saveCurrentRecord();
	}
	
	@Override
	public String getName() {
		return "MoleculesTab";
	}
}
