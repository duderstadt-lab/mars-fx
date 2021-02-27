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
import org.controlsfx.control.textfield.CustomTextField;

import com.jfoenix.controls.JFXCheckBox;
import javafx.scene.control.TextField;
import javafx.scene.control.ContentDisplay;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.molecule.MarsRecord;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

public abstract class AbstractParametersTable {
    
	protected MarsRecord record;
	protected BorderPane rootPane;
	
	protected CustomTextField addParameterField;
	protected Button typeButton;
	protected int buttonType = 0;
    
	protected TableView<ParameterRow> parameterTable;
	protected ObservableList<ParameterRow> parameterRowList = FXCollections.observableArrayList();

    public AbstractParametersTable() {        
    	parameterTable = new TableView<ParameterRow>();
    	addParameterField = new CustomTextField();
    	
    	TableColumn<ParameterRow, ParameterRow> typeColumn = new TableColumn<>();
    	typeColumn.setPrefWidth(40);
    	typeColumn.setMinWidth(40);
    	typeColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
    	typeColumn.setCellFactory(param -> new TableCell<ParameterRow, ParameterRow>() {
            private final Label label = new Label();

            @Override
            protected void updateItem(ParameterRow pRow, boolean empty) {
                super.updateItem(pRow, empty);

                if (pRow == null) {
                    setGraphic(null);
                    return;
                }
                
                label.setCenterShape(true);
                setGraphic(label);
                
                switch (pRow.getType()) {
					case 0:
						label.setText("123");
						label.setGraphic(null);
						break;
					case 1:
						label.setText("Aa");
						label.setGraphic(null);
						break;
					case 2:
						label.setText("");
						label.setGraphic(FontAwesomeIconFactory.get().createIcon(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.SQUARE_ALT, "1.1em"));
						break;
				}       		
            }
        });
    	typeColumn.setStyle( "-fx-alignment: CENTER;");
    	typeColumn.setSortable(false);
        parameterTable.getColumns().add(typeColumn);
    	
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
        			record.removeParameter(pRow.getName());
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
        
        TableColumn<ParameterRow, String> valueColumn = new TableColumn<>("Value");
        valueColumn.setCellValueFactory(param -> {
        	if (param.getValue().getType() == 2)
        		return new ReadOnlyObjectWrapper<>("");
        	else
        		return new ReadOnlyObjectWrapper<>(param.getValue().getValue());
        });
        
        valueColumn.setCellFactory(param -> new TableCell<ParameterRow, String>() {
            private JFXCheckBox checkbox;
            private TextField textField;
            
            @Override
            public void commitEdit(String value) {
            	super.commitEdit(value);            	
            	getTableView().getItems().get(getIndex()).setValue(value);
            	loadData();
            }
            
            @Override
            public void startEdit() {
                super.startEdit();
                if (textField == null) {
                    createTextField();
                }
                
                setGraphic(textField);
                textField.setText(getItem());
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                textField.selectAll();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();

                setText(String.valueOf(getItem()));
                setContentDisplay(ContentDisplay.TEXT_ONLY);
            }

            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);

                if (empty || value == null) {
                	setText(null);
                    setGraphic(null);
                    return;
                } else {
                	ParameterRow row = getTableView().getItems().get(getIndex());
                	
                	if (isEditing()) {
                        if (textField != null) {
                            textField.setText(getItem());
                        }
                        setGraphic(textField);
                        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    } else if (row.getType() == 2) {
                    	if (checkbox == null)
                    		checkbox = new JFXCheckBox();
                    	checkbox.setCheckedColor(Color.valueOf("black"));
                		checkbox.setCenterShape(true);
                		checkbox.setSelected(record.getBooleanParameter(row.getName()));
		                checkbox.setOnAction(e -> {
		        			record.setParameter(row.getName(), checkbox.isSelected());
		        		});
                		setStyle( "-fx-alignment: CENTER-LEFT;");
		                setGraphic(checkbox);
		                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		                setEditable(false);
                	} else {
                		setEditable(true);
                		setStyle( "-fx-alignment: CENTER-LEFT;");
                		setText(value);
                		setContentDisplay(ContentDisplay.TEXT_ONLY);
                		setGraphic(null);
                		return;
                	}
                }
            }
            
            private void createTextField() {
                textField = new TextField(getItem());
                textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
                textField.setOnKeyPressed(t -> {
                    if (t.getCode() == KeyCode.ENTER) {
                        commitEdit(textField.getText());
                    } else if (t.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                    }
                });
            }
        });
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
		addButton.setCursor(Cursor.DEFAULT);
		addButton.setStyle(
                "-fx-background-radius: 5em; " +
                "-fx-min-width: 18px; " +
                "-fx-min-height: 18px; " +
                "-fx-max-width: 18px; " +
                "-fx-max-height: 18px;"
        );
		addButton.setOnAction(e -> {
			switch (buttonType) {
				case 0:
		    		record.setParameter(addParameterField.getText(), Double.NaN);
		    		break;
				case 1:
					record.setParameter(addParameterField.getText(), "");
					break;
				case 2:
					record.setParameter(addParameterField.getText(), false);
					break;
			}
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
        
        
        typeButton = new Button();
        typeButton.setText("123");
        typeButton.setCenterShape(true);
        typeButton.setStyle(
                "-fx-background-radius: 2em; " +
                "-fx-min-width: 60px; " +
                "-fx-min-height: 30px; " +
                "-fx-max-width: 60px; " +
                "-fx-max-height: 30px;"
        );
        typeButton.setOnAction(e -> {
        	buttonType++;
        	if (buttonType > 2)
        		buttonType = 0;
        	
			switch (buttonType) {
				case 0:
					typeButton.setText("123");
					typeButton.setGraphic(null);
					break;
				case 1:
					typeButton.setText("Aa");
					typeButton.setGraphic(null);
					break;
				case 2:
					typeButton.setText("");
					typeButton.setGraphic(FontAwesomeIconFactory.get().createIcon(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.SQUARE_ALT, "1.1em"));
					break;
			}
		});
        
        BorderPane inputPane = new BorderPane();
        inputPane.setLeft(typeButton);
        inputPane.setCenter(addParameterField);

        rootPane = new BorderPane();
        rootPane.setCenter(parameterTable);
        rootPane.setBottom(inputPane);
        
        //Insets(double top, double right, double bottom, double left)
        BorderPane.setMargin(addParameterField, new Insets(5));
        BorderPane.setMargin(typeButton, new Insets(5));
    }
    
    public Node getNode() {
    	return rootPane;
    }
    
    public void loadData() {
    	parameterRowList.clear();

    	for (String parameter : record.getParameters().keySet()) {
    		if (record.getParameters().get(parameter) instanceof Double) {
    			parameterRowList.add(new ParameterRow(parameter, 0));
    		} else if (record.getParameters().get(parameter) instanceof String) {
    			parameterRowList.add(new ParameterRow(parameter, 1));
    		} else if (record.getParameters().get(parameter) instanceof Boolean) {
    			parameterRowList.add(new ParameterRow(parameter, 2));
    		}
        }
	}
    
    protected class ParameterRow {
    	private String parameter;
    	private int type;
    	
    	ParameterRow(String parameter, int type) {
    		this.parameter = parameter;
    		this.type = type;
    	}
    	
    	public String getName() {
    		return parameter;
    	}
    	
    	public int getType() {
    		return type;
    	}
    	
    	public String getValue() {
    		switch (type) {
				case 0:
					return String.valueOf(record.getParameter(parameter));
				case 1:
					return record.getStringParameter(parameter);
				case 2:
					return String.valueOf(record.getBooleanParameter(parameter));
				default:
					return null;
    		}
    	}
    	
    	public void setValue(String value) {
    		switch (type) {
				case 0:
					try {
		    			double num = Double.valueOf(value);
		    			record.setParameter(getName(), num);
		    		} catch (NumberFormatException e) {
		    			record.setParameter(getName(), Double.NaN);
		    		}
					break;
				case 1:
					record.setParameter(getName(), value);
					break;
				case 2:
					record.setParameter(getName(), Boolean.valueOf(value));
					break;
			}
    	}
    	
    }
}
