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
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
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
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Callback;
import javafx.scene.control.cell.TextFieldTableCell;

public abstract class AbstractPositionOfInterestTable {
    
	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;
	protected MarsRecord record;
	
	protected BorderPane rootPane;
	
    protected CustomTextField addPositionField;
    protected TableView<MarsPosition> positionTable;
    protected ObservableList<MarsPosition> positionRowList = FXCollections.observableArrayList();

    public AbstractPositionOfInterestTable() {        
    	positionTable = new TableView<MarsPosition>();
    	addPositionField = new CustomTextField();
    	
    	TableColumn<MarsPosition, MarsPosition> deleteColumn = new TableColumn<>();
    	deleteColumn.setPrefWidth(30);
    	deleteColumn.setMinWidth(30);
    	deleteColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
    	deleteColumn.setCellFactory(param -> new TableCell<MarsPosition, MarsPosition>() {
            private final Button removeButton = new Button();

            @Override
            protected void updateItem(MarsPosition pRow, boolean empty) {
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
                	record.removePosition(pRow.getName());
        			loadData();
        			fireIndicatorChangedEvent();
        		});
            }
        });
    	deleteColumn.setStyle( "-fx-alignment: CENTER;");
    	deleteColumn.setSortable(false);
        positionTable.getColumns().add(deleteColumn);

        TableColumn<MarsPosition, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        nameColumn.setOnEditCommit(event -> { 
        	String newPositionName = event.getNewValue();
        	if (!record.hasPosition(newPositionName)) {
        		MarsPosition poi = event.getRowValue();
        		String oldName = poi.getName();
        		record.removePosition(oldName);
        		
        		poi.setName(newPositionName);
        		record.putPosition(poi);
        		fireIndicatorChangedEvent();
        	} else {
        		((MarsPosition) event.getTableView().getItems()
        	            .get(event.getTablePosition().getRow())).setName(event.getOldValue());
        		positionTable.refresh();
        	}
        });
        nameColumn.setCellValueFactory(regionOfInterest ->
                new ReadOnlyObjectWrapper<>(regionOfInterest.getValue().getName())
        );
        nameColumn.setSortable(false);
        nameColumn.setPrefWidth(100);
        nameColumn.setMinWidth(100);
        nameColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
        positionTable.getColumns().add(nameColumn);

        TableColumn<MarsPosition, ComboBox<String>> columnColumn = new TableColumn<>("Column");
        columnColumn.setMinWidth(100);
        columnColumn.setCellValueFactory(cellData -> {
        	ComboBox<String> columns = new ComboBox<String>();
        	columns.getItems().addAll(archive.properties().getColumnSet());
            columns.getSelectionModel().select(cellData.getValue().getColumn());
            
            columns.getSelectionModel().selectedItemProperty().addListener(
                    (observable, oldColumn, newColumn) -> {
                        if (newColumn != null) {
                        	cellData.getValue().setColumn(newColumn);
                        	fireIndicatorChangedEvent();
                        }
                });
            
        	return new ReadOnlyObjectWrapper<>(columns);
        });
        columnColumn.setStyle("-fx-alignment: CENTER;");
        columnColumn.setSortable(false);
        positionTable.getColumns().add(columnColumn);
        
        TableColumn<MarsPosition, String> positionColumn = new TableColumn<>("Position");
        positionColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        positionColumn.setOnEditCommit(event -> { 
        	try {
    			double num = Double.valueOf(event.getNewValue());
    			event.getRowValue().setPosition(num);
    			fireIndicatorChangedEvent();
    		} catch (NumberFormatException e) {
    			//Do nothing for the moment...
    		}
        });
        positionColumn.setCellValueFactory(positionOfInterest ->
                new ReadOnlyObjectWrapper<>(String.valueOf(positionOfInterest.getValue().getPosition()))
        );
        positionColumn.setSortable(false);
        positionColumn.setPrefWidth(100);
        positionColumn.setMinWidth(100);
        positionColumn.setEditable(true);
        positionColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
        positionTable.getColumns().add(positionColumn);
        
        TableColumn<MarsPosition, JFXColorPicker> colorColumn = new TableColumn<>("Color");
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
			     fireIndicatorChangedEvent();
			 });
        	
        	return new ReadOnlyObjectWrapper<>(colorPicker);
        });
        colorColumn.setMinWidth(50);
        colorColumn.setSortable(false);
        colorColumn.setStyle("-fx-alignment: CENTER;");
        positionTable.getColumns().add(colorColumn);
        
        TableColumn<MarsPosition, String> strokeColumn = new TableColumn<>("Stroke");
        strokeColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        strokeColumn.setOnEditCommit(event -> { 
        	try {
    			double num = Double.valueOf(event.getNewValue());
    			event.getRowValue().setStroke(num);
    			fireIndicatorChangedEvent();
    		} catch (NumberFormatException e) {
    			//Do nothing for the moment...
    		}
        });
        strokeColumn.setCellValueFactory(positionOfInterest ->
                new ReadOnlyObjectWrapper<>(String.valueOf(positionOfInterest.getValue().getStroke()))
        );
        strokeColumn.setSortable(false);
        strokeColumn.setPrefWidth(100);
        strokeColumn.setMinWidth(100);
        strokeColumn.setEditable(true);
        strokeColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
        positionTable.getColumns().add(strokeColumn);
        
        positionTable.setItems(positionRowList);
        positionTable.setEditable(true);

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
			if (!addPositionField.getText().equals("")) {
				MarsPosition positionOfInterest = new MarsPosition(addPositionField.getText());
				record.putPosition(positionOfInterest);
				loadData();
			}
		});
		
		addPositionField.textProperty().addListener((observable, oldValue, newValue) -> {
        	if (addPositionField.getText().isEmpty()) {
        		addPositionField.setRight(new Label(""));
        	} else {
        		addPositionField.setRight(addButton);
        	}
        });
		addPositionField.setStyle(
                "-fx-background-radius: 2em; "
        );

        rootPane = new BorderPane();
        Insets insets = new Insets(5);
        
        rootPane.setCenter(positionTable);
        
        rootPane.setBottom(addPositionField);
        BorderPane.setMargin(addPositionField, insets);
        
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
    	positionRowList.setAll(record.getPositionNames().stream().map(name -> record.getPosition(name)).collect(Collectors.toList()));
	}
    
    protected abstract void fireIndicatorChangedEvent();
    
    protected abstract void addEventHandlers();
}
