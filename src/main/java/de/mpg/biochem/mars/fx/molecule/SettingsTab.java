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
import java.util.HashMap;
import java.util.Map;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;

import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.control.textfield.CustomTextField;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.jfoenix.controls.JFXColorPicker;
import com.jfoenix.controls.JFXToggleButton;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.util.HotKeyEntry;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.JsonConvertibleRecord;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.molecule.MoleculeArchiveWindow;
import de.mpg.biochem.mars.util.MarsPosition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener.Change;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCombination.ModifierValue;
import javafx.scene.input.KeyEvent;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;

import java.util.stream.Collectors;

import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import org.scijava.Context;
import org.scijava.plugin.Parameter;
import org.scijava.prefs.PrefService;

public class SettingsTab extends AbstractMoleculeArchiveTab implements MoleculeArchiveTab {
	
    protected TableView<HotKeyEntry> hotKeyTable;
    protected ObservableList<HotKeyEntry> hotKeyRowList = FXCollections.observableArrayList();
    
    @Parameter
    private PrefService prefService;
	
	private VBox rootPane;
	
	public SettingsTab(final Context context) {
		super(context);
		
		setIcon(FontAwesomeIconFactory.get().createIcon(COG, "1.083em"));
		
		rootPane = new VBox();
			
		Text moleculesHeading = new Text("Tag shortcuts");
		moleculesHeading.setFont(Font.font("Helvetica", FontWeight.NORMAL, 20));
		
		Text hotKeySettingsDescription = new Text("Combine shift, control, option (alt), or command with\n"
				+ "other keys to create tag shortcuts.");
		hotKeySettingsDescription.setFont(Font.font("Helvetica", FontWeight.NORMAL, 14));
		
		rootPane.getChildren().add(moleculesHeading);
		VBox.setMargin(moleculesHeading, new Insets(15, 15, 15, 15));
		
		rootPane.getChildren().add(hotKeySettingsDescription);
		VBox.setMargin(hotKeySettingsDescription, new Insets(5, 50, 5, 50));
		
		rootPane.getChildren().add(buildHotKeyTable());
		
		Text bdvHeading = new Text("Bdv options");
		bdvHeading.setFont(Font.font("Helvetica", FontWeight.NORMAL, 20));
		
		rootPane.getChildren().add(bdvHeading);
		VBox.setMargin(bdvHeading, new Insets(15, 15, 15, 15));
		
		GridPane gridpane = new GridPane();
		
		Label volatileLabel = new Label("Use N5 volatile view");
		gridpane.add(volatileLabel, 0, 5);
		GridPane.setMargin(volatileLabel, new Insets(5, 5, 5, 5));
		
		ToggleSwitch volatileSwitch = new ToggleSwitch();
		gridpane.add(volatileSwitch, 1, 5);
		volatileSwitch.setSelected(prefService.getBoolean(SettingsTab.class, "useN5VolatileViews", true));
		volatileSwitch.selectedProperty().addListener((t, o, n) -> {
			prefService.remove(SettingsTab.class, "useN5VolatileViews");
			prefService.put(SettingsTab.class, "useN5VolatileViews", n);
		});
		GridPane.setMargin(volatileSwitch, new Insets(5, 5, 5, 5));
		
		rootPane.getChildren().add(gridpane);
		VBox.setMargin(gridpane, new Insets(15, 15, 15, 15));
		
		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, this);
		
		getTab().setContent(rootPane);
	}
	
	protected BorderPane buildHotKeyTable() {
		hotKeyTable = new TableView<HotKeyEntry>();
		hotKeyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    	
    	TableColumn<HotKeyEntry, HotKeyEntry> deleteColumn = new TableColumn<>();
    	deleteColumn.setMaxWidth(30);
    	deleteColumn.setMinWidth(30);
    	deleteColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
    	deleteColumn.setCellFactory(param -> new TableCell<HotKeyEntry, HotKeyEntry>() {
            private final Button removeButton = new Button();

            @Override
            protected void updateItem(HotKeyEntry pRow, boolean empty) {
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
                	hotKeyRowList.remove(pRow);
        		});
            }
        });
    	deleteColumn.setStyle( "-fx-alignment: CENTER;");
    	deleteColumn.setSortable(false);
    	hotKeyTable.getColumns().add(deleteColumn);

        TableColumn<HotKeyEntry, KeyCombination> shortcutColumn = new TableColumn<>("Shortcut");
        shortcutColumn.setCellFactory(column -> new KeyEditField());
        shortcutColumn.setOnEditCommit(event -> { 
        	event.getRowValue().setShortcut(event.getNewValue());
        	//String newShortcut = event.getNewValue();
        	//if (hotKeyRowList.stream().filter(row -> row.getShortcut().equals(newShortcut)).findFirst().isPresent()) {
        	//	((HotKeyEntry) event.getTableView().getItems().get(event.getTablePosition().getRow())).setShortcut(event.getOldValue());
        	//	hotKeyTable.refresh();
        	//}
        });
        shortcutColumn.setCellValueFactory(hotKeyEntry ->
                new ReadOnlyObjectWrapper<>(hotKeyEntry.getValue().getShortcut())
        );

        shortcutColumn.setSortable(false);
        //shortcutColumn.setPrefWidth(150);
        //shortcutColumn.setMinWidth(100);
        shortcutColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
        hotKeyTable.getColumns().add(shortcutColumn);
        
        TableColumn<HotKeyEntry, String> tagColumn = new TableColumn<>("Tag");
        tagColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        tagColumn.setOnEditCommit(event -> { 
    		event.getRowValue().setTag(event.getNewValue());
        });
        tagColumn.setCellValueFactory(hotKey ->
                new ReadOnlyObjectWrapper<>(String.valueOf(hotKey.getValue().getTag()))
        );
        tagColumn.setSortable(false);
        //tagColumn.setPrefWidth(150);
        //tagColumn.setMinWidth(150);
        tagColumn.setEditable(true);
        tagColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
        hotKeyTable.getColumns().add(tagColumn);
        
        hotKeyTable.setItems(hotKeyRowList);
        hotKeyTable.setEditable(true);

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
			//if (!addHotKeyField.getText().equals("") && !hotKeyRowList.stream().filter(row -> row.getShortcut().equals(addHotKeyField.getText())).findFirst().isPresent()) {
			HotKeyEntry newHotKey = new HotKeyEntry(KeyCombination.valueOf("Ctrl+T"), "Tag");
			hotKeyRowList.add(newHotKey);
			//}
		});
		/*
		addHotKeyField.textProperty().addListener((observable, oldValue, newValue) -> {
        	if (addHotKeyField.getText().isEmpty()) {
        		addHotKeyField.setRight(new Label(""));
        	} else {
        		addHotKeyField.setRight(addButton);
        	}
        });
		addHotKeyField.setStyle(
                "-fx-background-radius: 2em; "
        );
		*/
		
        BorderPane hotKeyPane = new BorderPane();
        //hotKeyPane.setMinWidth(600);
        //hotKeyPane.setMinHeight(350);
        //hotKeyPane.setMaxWidth(600);
        hotKeyPane.setMaxHeight(350);
        
        Insets insets = new Insets(5, 50, 5, 50);
        
        hotKeyPane.setCenter(hotKeyTable);
        BorderPane.setMargin(hotKeyTable, insets);
        
        hotKeyPane.setBottom(addButton);
        BorderPane.setMargin(addButton, insets);
        
        return hotKeyPane;
	}
	
	protected void importHotKeys() {
		if (prefService.getMap(SettingsTab.class, "tagHotKeyList") != null) {
			HashMap<String, String> tagHotKeyList = (HashMap<String, String>)prefService.getMap(SettingsTab.class, "tagHotKeyList");
			
			for (String shortcut : tagHotKeyList.keySet()) {
				hotKeyRowList.add(new HotKeyEntry(KeyCombination.valueOf(shortcut), tagHotKeyList.get(shortcut)));
			}
		}
	}
	
	public void save() {
		Map<String, String> tagHotKeyList = hotKeyRowList.stream().collect(
				Collectors.toMap(HotKeyEntry::getShortcutString, HotKeyEntry::getTag));
		
		prefService.remove(SettingsTab.class, "tagHotKeyList");
		prefService.put(SettingsTab.class, "tagHotKeyList", tagHotKeyList);
	}
	
	public Node getNode() {
		return this.rootPane;
	}
	
	public ArrayList<Menu> getMenus() {
		return new ArrayList<Menu>();
	}

	@Override
    public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive) {
    	super.onInitializeMoleculeArchiveEvent(archive);
    	importHotKeys();
	}
	
	public ObservableList<HotKeyEntry> getHotKeyList() {
		return hotKeyRowList;
	}

	@Override
	protected void createIOMaps() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public String getName() {
		return "SettingsTab";
	}
	
	private static class KeyEditField extends TableCell<HotKeyEntry, KeyCombination> {

	    private TextField field;

	    @Override
	    public void startEdit() {
	      super.startEdit();
	      if (field == null) {
	        field = createField();
	      }
	      field.setText(getString());
	      setText(null);
	      setGraphic(field);
	      field.requestFocus();
	    }

	    @Override
	    public void cancelEdit() {
	      super.cancelEdit();
	      setText(getString());
	      setGraphic(null);
	    }

	    @Override
	    protected void updateItem(KeyCombination item, boolean empty) {
	      super.updateItem(item, empty);
	      
	      if (empty) {
	        setText(null);
	        setGraphic(null);
	      } else {
	        if (isEditing()) {
	          setText(null);
	          setGraphic(field);
	        } else {
	          setText(getString());
	          setGraphic(null);
	        }
	      }
	    }

	    private TextField createField() {
	      TextField field = new TextField(getString());
	      field.setEditable(false);
	      field.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
	        if (e.getCode() == KeyCode.ENTER) {
	          commit(field.getText());
	        } else if (e.getCode() == KeyCode.ESCAPE) {
	          cancelEdit();
	        } else if (e.getCode() == KeyCode.BACK_SPACE) {
	          field.setText(null);
	        } else {
	          field.setText(convert(e).toString());
	        }
	        e.consume();
	      });
	      field.focusedProperty().addListener((obs, wasfocused, focused) -> {
		      if(!focused) 
		    	  commit(field.getText());
		  });
	      return field;
	    }
	    
	    public void commit(String text) {
	      try {
	        commitEdit(text.equals("") ? KeyCombination.NO_MATCH : KeyCombination.valueOf(text));
	      } catch (Exception ee) {
	        cancelEdit();
	      }
	    }

	    private String convert(KeyEvent e) {
	    	try {
		    	KeyCodeCombination key1 = new KeyCodeCombination(e.getCode(),
		              e.isShiftDown() ? ModifierValue.DOWN : ModifierValue.UP,
		              e.isControlDown() || !(e.isAltDown() || e.isShiftDown() || e.isMetaDown()) ? ModifierValue.DOWN : ModifierValue.UP,
		              e.isAltDown() ? ModifierValue.DOWN : ModifierValue.UP,
		              e.isMetaDown() ? ModifierValue.DOWN : ModifierValue.UP,
		              ModifierValue.UP);
		    	
		    	return key1.toString();
	    	} catch (Exception exception) {
	    		KeyCodeCombination key = new KeyCodeCombination(KeyCode.A,
		                e.isShiftDown() ? ModifierValue.DOWN : ModifierValue.UP,
		                e.isControlDown() ? ModifierValue.DOWN : ModifierValue.UP,
		                e.isAltDown() ? ModifierValue.DOWN : ModifierValue.UP,
		                e.isMetaDown() ? ModifierValue.DOWN : ModifierValue.UP,
		                ModifierValue.UP);

		            String name = key.getName();
		           
		            return name.substring(0, name.length() - key.getCode().getName().length());
	    	}
	    }

	    private String getString() {
	      return getItem() == null ? "" : getItem().toString();
	    }
	}
}
