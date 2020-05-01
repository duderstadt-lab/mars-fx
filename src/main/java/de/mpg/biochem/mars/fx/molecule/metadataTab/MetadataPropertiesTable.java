/*******************************************************************************
 * Copyright (C) 2019, Duderstadt Lab
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package de.mpg.biochem.mars.fx.molecule.metadataTab;

import org.controlsfx.control.textfield.CustomTextField;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.event.MetadataEvent;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.cell.TextFieldTableCell;

public class MetadataPropertiesTable implements MetadataSubPane {
    
	private MarsMetadata marsImageMetadata;
	
	private BorderPane rootPane;
	
    private CustomTextField addParameterField;
    private TableView<ParameterRow> parameterTable;
    private ObservableList<ParameterRow> parameterRowList = FXCollections.observableArrayList();

    public MetadataPropertiesTable() {        
        initialize();
    }

    private void initialize() {
    	parameterTable = new TableView<ParameterRow>();
    	addParameterField = new CustomTextField();
    	
    	TableColumn<ParameterRow, ParameterRow> deleteColumn = new TableColumn<>();
    	deleteColumn.setPrefWidth(30);
    	deleteColumn.setMinWidth(30);
    	deleteColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
    	deleteColumn.setCellFactory(param -> new TableCell<ParameterRow, ParameterRow>() {
            private final Button removeButton = new Button();

            @Override
            protected void updateItem(ParameterRow pRow, boolean empty) {
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
                	marsImageMetadata.removeParameter(pRow.getName());
        			loadData();
        		});
            }
        });
    	deleteColumn.setStyle( "-fx-alignment: CENTER;");
    	deleteColumn.setSortable(false);
        parameterTable.getColumns().add(deleteColumn);

        TableColumn<ParameterRow, String> ParameterColumn = new TableColumn<>("Parameter");
        ParameterColumn.setCellValueFactory(parameterRow ->
                new ReadOnlyObjectWrapper<>(parameterRow.getValue().getName())
        );
        ParameterColumn.setSortable(false);
        ParameterColumn.setPrefWidth(100);
        ParameterColumn.setMinWidth(100);
        ParameterColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
        parameterTable.getColumns().add(ParameterColumn);
        
        //TODO how to also allow editing of this parameter
        TableColumn<ParameterRow, String> valueColumn = new TableColumn<>("Value");
        valueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        valueColumn.setOnEditCommit(
                event -> event.getRowValue().setValue(event.getNewValue()));
        valueColumn.setCellValueFactory(parameterRow ->
                new ReadOnlyObjectWrapper<>(parameterRow.getValue().getValue())
        );
        valueColumn.setSortable(false);
        valueColumn.setPrefWidth(100);
        valueColumn.setMinWidth(100);
        valueColumn.setEditable(true);
        valueColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
        parameterTable.getColumns().add(valueColumn);
        
        parameterTable.setItems(parameterRowList);
        parameterTable.setEditable(true);

		Button addButton = new Button();
		addButton.setGraphic(FontAwesomeIconFactory.get().createIcon(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLUS, "1.0em"));
		addButton.setCenterShape(true);
		addButton.setStyle(
                "-fx-background-radius: 5em; " +
                "-fx-min-width: 18px; " +
                "-fx-min-height: 18px; " +
                "-fx-max-width: 18px; " +
                "-fx-max-height: 18px;"
        );
		addButton.setOnAction(e -> {
			if (!addParameterField.getText().equals(""))
				marsImageMetadata.setParameter(addParameterField.getText(), Double.NaN);
			loadData();
		});
		
        addParameterField.textProperty().addListener((observable, oldValue, newValue) -> {
        	if (addParameterField.getText().isEmpty()) {
        		addParameterField.setRight(new Label(""));
        	} else {
        		addParameterField.setRight(addButton);
        	}
        });
        addParameterField.setStyle(
                "-fx-background-radius: 2em; "
        );

        rootPane = new BorderPane();
        Insets insets = new Insets(5);
        
        rootPane.setCenter(parameterTable);
        
        rootPane.setBottom(addParameterField);
        BorderPane.setMargin(addParameterField, insets);
        
        getNode().addEventHandler(MetadataEvent.METADATA_EVENT, this);
    }
    
    public Node getNode() {
    	return rootPane;
    }
    
    public void loadData() {
    	parameterRowList.clear();

    	for (String parameter : marsImageMetadata.getParameters().keySet()) {
        	parameterRowList.add(new ParameterRow(parameter));
        }
	}
    
    private class ParameterRow {
    	private String parameter;
    	
    	ParameterRow(String parameter) {
    		this.parameter = parameter;
    	}
    	
    	public String getName() {
    		return parameter;
    	}
    	
    	public String getValue() {
    		return String.valueOf(marsImageMetadata.getParameter(parameter));
    	}
    	
    	public void setValue(String value) {
    		try {
    			double num = Double.valueOf(value);
    			marsImageMetadata.setParameter(getName(), num);
    		} catch (NumberFormatException e) {
    			//Do nothing for the moment...
    		}
    	}
    }

	@Override
	public void handle(MetadataEvent event) {
		event.invokeHandler(this);
		event.consume();
	}

	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}

	@Override
	public void onMetadataSelectionChangedEvent(MarsMetadata marsImageMetadata) {
		this.marsImageMetadata = marsImageMetadata;
    	loadData();
	}
}
