package de.mpg.biochem.mars.fx.molecule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
import de.mpg.biochem.mars.molecule.JsonConvertibleRecord;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.molecule.MoleculeArchiveWindow;
import de.mpg.biochem.mars.util.PositionOfInterest;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener.Change;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;

import java.util.stream.Collectors;

import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import org.scijava.plugin.Parameter;
import org.scijava.prefs.PrefService;

public class SettingsTab extends AbstractMoleculeArchiveTab implements MoleculeArchiveTab {
	
	private JFXToggleButton smileEncodingButton;
	
	protected CustomTextField addHotKeyField;
    protected TableView<HotKeyEntry> hotKeyTable;
    protected ObservableList<HotKeyEntry> hotKeyRowList = FXCollections.observableArrayList();
    
    @Parameter
    private PrefService prefService;
	
	private VBox rootPane;
	
	public SettingsTab(PrefService prefService) {
		super();
		this.prefService = prefService;
		
		setIcon(FontAwesomeIconFactory.get().createIcon(COG, "1.3em"));
		
		//smileEncodingButton = new JFXToggleButton();
		rootPane = new VBox();
			
		Text moleculesHeading = new Text("Molecules");
		moleculesHeading.setFont(Font.font("Helvetica", FontWeight.NORMAL, 20));
		
		rootPane.getChildren().add(moleculesHeading);
		VBox.setMargin(moleculesHeading, new Insets(15, 15, 15, 15));
		
		rootPane.getChildren().add(buildHotKeyTable());
		
		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, this);
		
		setContent(rootPane);
	}
	
	protected BorderPane buildHotKeyTable() {
		hotKeyTable = new TableView<HotKeyEntry>();
		addHotKeyField = new CustomTextField();
    	
    	TableColumn<HotKeyEntry, HotKeyEntry> deleteColumn = new TableColumn<>();
    	deleteColumn.setPrefWidth(30);
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

        TableColumn<HotKeyEntry, String> shortcutColumn = new TableColumn<>("Shortcut");
        shortcutColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        shortcutColumn.setOnEditCommit(event -> { 
        	String newShortcut = event.getNewValue();
        	if (hotKeyRowList.stream().filter(row -> row.getShortcut().equals(newShortcut)).findFirst().isPresent()) {
        		((HotKeyEntry) event.getTableView().getItems().get(event.getTablePosition().getRow())).setShortcut(event.getOldValue());
        		hotKeyTable.refresh();
        	}
        	
    		//HotKeyEntry hotkey = event.getRowValue();
    		//String oldShortcut = hotkey.getShortcut();
    		//String tag = hotkey.getTag();
    		
    		//NOT SURE WHAT TO DO HERE... won't it just allow editing...
    		
    		//Maybe do nothing...

        });
        shortcutColumn.setCellValueFactory(regionOfInterest ->
                new ReadOnlyObjectWrapper<>(regionOfInterest.getValue().getShortcut())
        );
        shortcutColumn.setSortable(false);
        shortcutColumn.setPrefWidth(100);
        shortcutColumn.setMinWidth(100);
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
        tagColumn.setPrefWidth(100);
        tagColumn.setMinWidth(100);
        tagColumn.setEditable(true);
        tagColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
        hotKeyTable.getColumns().add(tagColumn);
        
        hotKeyTable.setItems(hotKeyRowList);
        hotKeyTable.setEditable(true);

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
			if (!addHotKeyField.getText().equals("") && !hotKeyRowList.stream().filter(row -> row.getShortcut().equals(addHotKeyField.getText())).findFirst().isPresent()) {
				HotKeyEntry newHotKey = new HotKeyEntry(addHotKeyField.getText());
				hotKeyRowList.add(newHotKey);
			}
		});
		
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

        BorderPane hotKeyPane = new BorderPane();
        hotKeyPane.setMinWidth(350);
        hotKeyPane.setMinHeight(300);
        hotKeyPane.setMaxWidth(350);
        hotKeyPane.setMaxHeight(300);
        
        Insets insets = new Insets(5, 50, 5, 50);
        
        hotKeyPane.setCenter(hotKeyTable);
        BorderPane.setMargin(hotKeyTable, insets);
        
        hotKeyPane.setBottom(addHotKeyField);
        BorderPane.setMargin(addHotKeyField, insets);
        
        return hotKeyPane;
	}
	
	protected void importHotKeys() {
		if (prefService.getMap(SettingsTab.class, "tagHotKeyList") != null) {
			HashMap<String, String> tagHotKeyList = (HashMap<String, String>)prefService.getMap(SettingsTab.class, "tagHotKeyList");
			
			for (String shortcut : tagHotKeyList.keySet()) {
				hotKeyRowList.add(new HotKeyEntry(shortcut, tagHotKeyList.get(shortcut)));
			}
		}
	}
	
	public void save() {
		Map<String, String> tagHotKeyList = hotKeyRowList.stream().collect(
				Collectors.toMap(HotKeyEntry::getShortcut, HotKeyEntry::getTag));
		
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
    public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive) {
    	super.onInitializeMoleculeArchiveEvent(archive);
    	importHotKeys();
    	
		//smileEncodingButton.setSelected(archive.isSMILEOutputEncoding());
	}
	
	public ObservableList<HotKeyEntry> getHotKeyList() {
		return hotKeyRowList;
	}
}
