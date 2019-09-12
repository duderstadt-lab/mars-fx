package de.mpg.biochem.mars.fx.molecule.moleculesTab;
import org.controlsfx.control.textfield.CustomTextField;

import com.jfoenix.controls.JFXColorPicker;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.event.MoleculeSelectionChangedEvent;
import javafx.scene.paint.Color;
import de.mpg.biochem.mars.fx.plot.PlotSeries;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.PositionOfInterest;
import de.mpg.biochem.mars.molecule.RegionOfInterest;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.cell.TextFieldTableCell;

public class RegionOfInterestTable implements MoleculeSubPane {
    
	private Molecule molecule;
	
	private BorderPane rootPane;
	
    private CustomTextField addRegionNameField;
    private TableView<RegionOfInterest> regionTable;
    private ObservableList<RegionOfInterest> regionRowList = FXCollections.observableArrayList();

    public RegionOfInterestTable() {        
        initialize();
    }

    private void initialize() {
    	regionTable = new TableView<RegionOfInterest>();
    	addRegionNameField = new CustomTextField();
    	
    	TableColumn<RegionOfInterest, RegionOfInterest> deleteColumn = new TableColumn<>();
    	deleteColumn.setPrefWidth(30);
    	deleteColumn.setMinWidth(30);
    	deleteColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
    	deleteColumn.setCellFactory(param -> new TableCell<RegionOfInterest, RegionOfInterest>() {
            private final Button removeButton = new Button();

            @Override
            protected void updateItem(RegionOfInterest pRow, boolean empty) {
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
        			molecule.removeRegion(pRow.getName());
        			loadData();
        		});
            }
        });
    	deleteColumn.setStyle( "-fx-alignment: CENTER;");
    	deleteColumn.setSortable(false);
        regionTable.getColumns().add(deleteColumn);

        TableColumn<RegionOfInterest, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(regionOfInterest ->
        	new ReadOnlyObjectWrapper<>(regionOfInterest.getValue().getName())
        );
        nameColumn.setSortable(false);
        nameColumn.setPrefWidth(100);
        nameColumn.setMinWidth(100);
        nameColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
        regionTable.getColumns().add(nameColumn);
        
        TableColumn<RegionOfInterest, ComboBox<String>> columnColumn = new TableColumn<>("Column");
        columnColumn.setMinWidth(100);
        columnColumn.setCellValueFactory(cellData -> {
        	ComboBox<String> columns = new ComboBox<String>();
        	columns.getItems().addAll(molecule.getDataTable().getColumnHeadings());
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
        
        TableColumn<RegionOfInterest, String> startColumn = new TableColumn<>("Start");
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
        
        TableColumn<RegionOfInterest, String> endColumn = new TableColumn<>("End");
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
        
        TableColumn<RegionOfInterest, JFXColorPicker> colorColumn = new TableColumn<>("Color");
        colorColumn.setCellValueFactory(cellData -> {
        	JFXColorPicker colorPicker = new JFXColorPicker();
        	String color = cellData.getValue().getColor();
        	if (color != null)
        		colorPicker.setValue(Color.web(color));
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
        
        regionTable.setItems(regionRowList);
        regionTable.setEditable(true);

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
			if (!addRegionNameField.getText().equals("")) {
				RegionOfInterest regionOfInterest = new RegionOfInterest(addRegionNameField.getText());
				molecule.putRegion(regionOfInterest);
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
        
        getNode().addEventHandler(MoleculeEvent.MOLECULE_EVENT, this);
    }
    
    public Node getNode() {
    	return rootPane;
    }
    
    public void loadData() {
    	regionRowList.clear();

    	for (String name : molecule.getRegionNames()) {
        	regionRowList.add(molecule.getRegion(name));
        }
	}
    
    @Override
    public void handle(MoleculeEvent event) {
        event.invokeHandler(this);
        event.consume();
    }

	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}

	@Override
	public void onMoleculeSelectionChangedEvent(Molecule molecule) {
		this.molecule = molecule;
    	loadData();
	}
}
