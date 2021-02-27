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
import java.util.stream.Collectors;

import org.controlsfx.control.textfield.CustomTextField;

import com.jfoenix.controls.JFXColorPicker;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.event.DefaultMoleculeArchiveEventHandler;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.event.MoleculeSelectionChangedEvent;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
import de.mpg.biochem.mars.fx.event.MoleculeIndicatorChangedEvent;
import javafx.scene.paint.Color;
import de.mpg.biochem.mars.fx.plot.PlotSeries;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.AbstractJsonConvertibleRecord;
import de.mpg.biochem.mars.molecule.MarsRecord;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.util.MarsPosition;
import de.mpg.biochem.mars.util.MarsRegion;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.cell.TextFieldTableCell;

public abstract class AbstractRegionOfInterestTable {
    
	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;
	protected MarsRecord record;
	
	protected BorderPane rootPane;
	
    protected CustomTextField addRegionNameField;
    protected TableView<MarsRegion> regionTable;
    protected ObservableList<MarsRegion> regionRowList = FXCollections.observableArrayList();

    public AbstractRegionOfInterestTable() {        
    	regionTable = new TableView<MarsRegion>();
    	addRegionNameField = new CustomTextField();
    	
    	TableColumn<MarsRegion, MarsRegion> deleteColumn = new TableColumn<>();
    	deleteColumn.setPrefWidth(30);
    	deleteColumn.setMinWidth(30);
    	deleteColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
    	deleteColumn.setCellFactory(param -> new TableCell<MarsRegion, MarsRegion>() {
            private final Button removeButton = new Button();

            @Override
            protected void updateItem(MarsRegion pRow, boolean empty) {
                super.updateItem(pRow, empty);

                if (pRow == null) {
                    setGraphic(null);
                    return;
                }
                
                removeButton.setGraphic(FontAwesomeIconFactory.get().createIcon(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.MINUS, "1.0em"));
        		removeButton.setCenterShape(true);
        		removeButton.setStyle(
                        "-fx-background-radius: 5em; " +
                        "-fx-min-width: 18px; " +
                        "-fx-min-height: 18px; " +
                        "-fx-max-width: 18px; " +
                        "-fx-max-height: 18px;"
                );
        		
                setGraphic(removeButton);
                removeButton.setOnAction(e -> {
        			record.removeRegion(pRow.getName());
        			loadData();
        			fireIndicatorChangedEvent();
        		});
            }
        });
    	deleteColumn.setStyle( "-fx-alignment: CENTER;");
    	deleteColumn.setSortable(false);
        regionTable.getColumns().add(deleteColumn);

        TableColumn<MarsRegion, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        nameColumn.setOnEditCommit(event -> { 
        	String newRegionName = event.getNewValue();
        	if (!record.hasRegion(newRegionName)) {
        		MarsRegion roi = event.getRowValue();
        		String oldName = roi.getName();
        		record.removeRegion(oldName);
        		
        		roi.setName(newRegionName);
        		record.putRegion(roi);
        	} else {
        		((MarsRegion) event.getTableView().getItems()
        	            .get(event.getTablePosition().getRow())).setName(event.getOldValue());
        		regionTable.refresh();
        	}
        });
        nameColumn.setCellValueFactory(regionOfInterest ->
                new ReadOnlyObjectWrapper<>(regionOfInterest.getValue().getName())
        );
        nameColumn.setSortable(false);
        nameColumn.setPrefWidth(100);
        nameColumn.setMinWidth(100);
        nameColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
        regionTable.getColumns().add(nameColumn);
        
        TableColumn<MarsRegion, ComboBox<String>> columnColumn = new TableColumn<>("Column");
        columnColumn.setMinWidth(100);
        columnColumn.setCellValueFactory(cellData -> {
        	ComboBox<String> columns = new ComboBox<String>();
        	columns.getItems().addAll(archive.properties().getColumnSet());
            columns.getSelectionModel().select(cellData.getValue().getColumn());
            
            columns.getSelectionModel().selectedItemProperty().addListener(
                    (observable, oldColumn, newColumn) -> {
                        if (newColumn != null) {
                        	cellData.getValue().setColumn(newColumn);
                        }
                });
            
        	return new ReadOnlyObjectWrapper<>(columns);
        });
        columnColumn.setStyle("-fx-alignment: CENTER;");
        columnColumn.setSortable(false);
        regionTable.getColumns().add(columnColumn);
        
        TableColumn<MarsRegion, String> startColumn = new TableColumn<>("Start");
        startColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        startColumn.setOnEditCommit(event -> { 
        	try {
    			double num = Double.valueOf(event.getNewValue());
    			event.getRowValue().setStart(num);
    		} catch (NumberFormatException e) {
    			//Do nothing for the moment...
    		}
        });
        startColumn.setCellValueFactory(regionOfInterest ->
                new ReadOnlyObjectWrapper<>(String.valueOf(regionOfInterest.getValue().getStart()))
        );
        startColumn.setSortable(false);
        startColumn.setPrefWidth(100);
        startColumn.setMinWidth(100);
        startColumn.setEditable(true);
        startColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
        regionTable.getColumns().add(startColumn);
        
        TableColumn<MarsRegion, String> endColumn = new TableColumn<>("End");
        endColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        endColumn.setOnEditCommit(event -> { 
        	try {
    			double num = Double.valueOf(event.getNewValue());
    			event.getRowValue().setEnd(num);
    		} catch (NumberFormatException e) {
    			//Do nothing for the moment...
    		}
        });
        endColumn.setCellValueFactory(regionOfInterest ->
                new ReadOnlyObjectWrapper<>(String.valueOf(regionOfInterest.getValue().getEnd()))
        );
        endColumn.setSortable(false);
        endColumn.setPrefWidth(100);
        endColumn.setMinWidth(100);
        endColumn.setEditable(true);
        endColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
        regionTable.getColumns().add(endColumn);
        
        TableColumn<MarsRegion, JFXColorPicker> colorColumn = new TableColumn<>("Color");
        colorColumn.setCellValueFactory(cellData -> {
        	JFXColorPicker colorPicker = new JFXColorPicker();
        	Color color = Color.web("rgba(50,50,50,0.2)");
        	try {
        		color = Color.web(cellData.getValue().getColor());
        	} catch (NullPointerException e1) {
        		//e1.printStackTrace();
        		
        	} catch (IllegalArgumentException e2) {
        		//e2.printStackTrace();
        	}
        	colorPicker.setValue(color);
        	colorPicker.setOnAction(action -> {
			     Color c = colorPicker.getValue();
			     cellData.getValue().setColor(c.toString());
			 });
        	
        	return new ReadOnlyObjectWrapper<>(colorPicker);
        });
        colorColumn.setMinWidth(50);
        colorColumn.setSortable(false);
        colorColumn.setStyle("-fx-alignment: CENTER;");
        regionTable.getColumns().add(colorColumn);
        
        TableColumn<MarsRegion, String> opacityColumn = new TableColumn<>("Opacity");
        opacityColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        opacityColumn.setOnEditCommit(event -> { 
        	try {
    			double num = Double.valueOf(event.getNewValue());
    			event.getRowValue().setOpacity(num);
    		} catch (NumberFormatException e) {
    			//Do nothing for the moment...
    		}
        });
        opacityColumn.setCellValueFactory(regionOfInterest ->
                new ReadOnlyObjectWrapper<>(String.valueOf(regionOfInterest.getValue().getOpacity()))
        );
        opacityColumn.setSortable(false);
        opacityColumn.setPrefWidth(100);
        opacityColumn.setMinWidth(100);
        opacityColumn.setEditable(true);
        opacityColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
        regionTable.getColumns().add(opacityColumn);
        
        regionTable.setItems(regionRowList);
        regionTable.setEditable(true);

		Button addButton = new Button();
		addButton.setGraphic(FontAwesomeIconFactory.get().createIcon(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLUS, "1.0em"));
		addButton.setCenterShape(true);
		addButton.setCursor(Cursor.DEFAULT);
		addButton.setStyle(
                "-fx-background-radius: 5em; " +
                "-fx-min-width: 18px; " +
                "-fx-min-height: 18px; " +
                "-fx-max-width: 18px; " +
                "-fx-max-height: 18px;"
        );
		addButton.setOnAction(e -> {
			if (!addRegionNameField.getText().equals("")) {
				MarsRegion regionOfInterest = new MarsRegion(addRegionNameField.getText());
				record.putRegion(regionOfInterest);
				loadData();
			}
		});
		
		addRegionNameField.textProperty().addListener((observable, oldValue, newValue) -> {
        	if (addRegionNameField.getText().isEmpty()) {
        		addRegionNameField.setRight(new Label(""));
        	} else {
        		addRegionNameField.setRight(addButton);
        	}
        });
		addRegionNameField.setStyle(
                "-fx-background-radius: 2em; "
        );

        rootPane = new BorderPane();
        Insets insets = new Insets(5);
        
        rootPane.setCenter(regionTable);
        
        rootPane.setBottom(addRegionNameField);
        BorderPane.setMargin(addRegionNameField, insets);
        
        getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, new DefaultMoleculeArchiveEventHandler() {
        	@Override
        	public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> newArchive) {
        		archive = newArchive;
        	}
        });
        
        addEventHandlers();
    }
    
    public Node getNode() {
    	return rootPane;
    }
    
    public void loadData() {
    	regionRowList.setAll(record.getRegionNames().stream().map(name -> record.getRegion(name)).collect(Collectors.toList()));
	}
    
    protected abstract void fireIndicatorChangedEvent();
    
    protected abstract void addEventHandlers();
}
