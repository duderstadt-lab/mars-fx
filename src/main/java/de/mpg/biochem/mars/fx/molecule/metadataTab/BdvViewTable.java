package de.mpg.biochem.mars.fx.molecule.metadataTab;
import java.io.File;
import java.util.stream.Collectors;

import org.controlsfx.control.textfield.CustomTextField;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.event.MetadataEvent;
import de.mpg.biochem.mars.fx.event.MetadataEventHandler;
import javafx.stage.FileChooser;
import de.mpg.biochem.mars.molecule.MarsBdvSource;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
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

import javafx.scene.layout.FlowPane;

public class BdvViewTable implements MetadataEventHandler {
    
	protected MarsImageMetadata marsImageMetadata;
	
	protected BorderPane rootPane;
	
    protected CustomTextField addBdvSourceNameField;
    protected TableView<MarsBdvSource> bdvTable;
    protected ObservableList<MarsBdvSource> bdvRowList = FXCollections.observableArrayList();

    public BdvViewTable() {        
    	bdvTable = new TableView<MarsBdvSource>();
    	addBdvSourceNameField = new CustomTextField();
    	
    	TableColumn<MarsBdvSource, MarsBdvSource> deleteColumn = new TableColumn<>();
    	deleteColumn.setPrefWidth(30);
    	deleteColumn.setMinWidth(30);
    	deleteColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
    	deleteColumn.setCellFactory(param -> new TableCell<MarsBdvSource, MarsBdvSource>() {
            private final Button removeButton = new Button();

            @Override
            protected void updateItem(MarsBdvSource pRow, boolean empty) {
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
        			marsImageMetadata.removeBdvSource(pRow.getName());
        			loadBdvSources();
        		});
            }
        });
    	deleteColumn.setStyle( "-fx-alignment: CENTER;");
    	deleteColumn.setSortable(false);
    	bdvTable.getColumns().add(deleteColumn);

        TableColumn<MarsBdvSource, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        nameColumn.setOnEditCommit(event -> { 
        	String newRegionName = event.getNewValue();
        	if (!marsImageMetadata.hasBdvSource(newRegionName)) {
        		MarsBdvSource bdvSource = event.getRowValue();
        		String oldName = bdvSource.getName();
        		marsImageMetadata.removeBdvSource(oldName);
        		
        		bdvSource.setName(newRegionName);
        		marsImageMetadata.putBdvSource(bdvSource);
        	} else {
        		((MarsBdvSource) event.getTableView().getItems()
        	            .get(event.getTablePosition().getRow())).setName(event.getOldValue());
        		bdvTable.refresh();
        	}
        });
        nameColumn.setCellValueFactory(bdvSource ->
                new ReadOnlyObjectWrapper<>(bdvSource.getValue().getName())
        );
        nameColumn.setSortable(false);
        nameColumn.setPrefWidth(100);
        nameColumn.setMinWidth(100);
        nameColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
        bdvTable.getColumns().add(nameColumn);
        
        TableColumn<MarsBdvSource, String> m00Column = buildAffineColumn("m00", 0, 0);
        bdvTable.getColumns().add(m00Column);
        TableColumn<MarsBdvSource, String> m01Column = buildAffineColumn("m01", 0, 1);
        bdvTable.getColumns().add(m01Column);
        TableColumn<MarsBdvSource, String> m02Column = buildAffineColumn("m02", 0, 3);
        bdvTable.getColumns().add(m02Column);
        TableColumn<MarsBdvSource, String> m10Column = buildAffineColumn("m10", 1, 0);
        bdvTable.getColumns().add(m10Column);
        TableColumn<MarsBdvSource, String> m11Column = buildAffineColumn("m11", 1, 1);
        bdvTable.getColumns().add(m11Column);
        TableColumn<MarsBdvSource, String> m12Column = buildAffineColumn("m12", 1, 3);
        bdvTable.getColumns().add(m12Column);
        
        TableColumn<MarsBdvSource, String> xDriftColumn = buildEntryFieldColumn("xDriftColumn");
        xDriftColumn.setOnEditCommit(event -> event.getRowValue().setXDriftColumn(event.getNewValue()));
        xDriftColumn.setCellValueFactory(bdvSource ->
                new ReadOnlyObjectWrapper<>(String.valueOf(bdvSource.getValue().getXDriftColumn()))
        );
        bdvTable.getColumns().add(xDriftColumn);
        
        TableColumn<MarsBdvSource, String> yDriftColumn = buildEntryFieldColumn("yDriftColumn");
        yDriftColumn.setOnEditCommit(event -> event.getRowValue().setYDriftColumn(event.getNewValue()));
        yDriftColumn.setCellValueFactory(bdvSource ->
                new ReadOnlyObjectWrapper<>(String.valueOf(bdvSource.getValue().getYDriftColumn()))
        );
        bdvTable.getColumns().add(yDriftColumn);
        
        TableColumn<MarsBdvSource, String> xmlPathColumn = buildEntryFieldColumn("file path (xml)");
        xmlPathColumn.setOnEditCommit(event -> event.getRowValue().setPathToXml(event.getNewValue()));
        xmlPathColumn.setCellValueFactory(bdvSource ->
                new ReadOnlyObjectWrapper<>(String.valueOf(bdvSource.getValue().getPathToXml()))
        );
        bdvTable.getColumns().add(xmlPathColumn);
        
        bdvTable.setItems(bdvRowList);
        bdvTable.setEditable(true);

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
			if (!addBdvSourceNameField.getText().equals("") && !marsImageMetadata.hasBdvSource(addBdvSourceNameField.getText())) {
				MarsBdvSource bdvSource = new MarsBdvSource(addBdvSourceNameField.getText());
				
				FileChooser fileChooser = new FileChooser();
				fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

				File pathToXml = fileChooser.showSaveDialog(getNode().getScene().getWindow());
				
				if (pathToXml != null) {
					bdvSource.setPathToXml(pathToXml.getAbsolutePath());
					marsImageMetadata.putBdvSource(bdvSource);
					loadBdvSources();
				}
			}
		});
		
		addBdvSourceNameField.textProperty().addListener((observable, oldValue, newValue) -> {
        	if (addBdvSourceNameField.getText().isEmpty()) {
        		addBdvSourceNameField.setRight(new Label(""));
        	} else {
        		addBdvSourceNameField.setRight(addButton);
        	}
        });
		addBdvSourceNameField.setStyle(
                "-fx-background-radius: 2em; "
        );

        rootPane = new BorderPane();
        Insets insets = new Insets(5);
        
        rootPane.setCenter(bdvTable);
        
        //FlowPane flowPane = new FlowPane();
        //addBdvSourceNameField.setPrefWidth(200);
        //flowPane.getChildren().add(addBdvSourceNameField);
        rootPane.setBottom(addBdvSourceNameField);
        BorderPane.setMargin(addBdvSourceNameField, insets);
        
        getNode().addEventHandler(MetadataEvent.METADATA_EVENT, this);
    }
    
    protected TableColumn<MarsBdvSource, String> buildAffineColumn(String name, int rowIndex, int columnIndex) {
    	TableColumn<MarsBdvSource, String> column = buildEntryFieldColumn(name);
    	column.setOnEditCommit(event -> { 
        	try {
    			double num = Double.valueOf(event.getNewValue());
    			event.getRowValue().getAffineTransform3D().set(num, rowIndex, columnIndex);
    		} catch (NumberFormatException e) {
    			//Do nothing for the moment...
    		}
        });
    	column.setCellValueFactory(bdvSource ->
                new ReadOnlyObjectWrapper<>(String.valueOf(bdvSource.getValue().getAffineTransform3D().get(rowIndex, columnIndex)))
        );
    	return column;
    }
    
    protected TableColumn<MarsBdvSource, String> buildEntryFieldColumn(String name) {
    	TableColumn<MarsBdvSource, String> column = new TableColumn<>(name);
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setSortable(false);
        column.setPrefWidth(100);
        column.setMinWidth(100);
        column.setEditable(true);
        column.setStyle( "-fx-alignment: CENTER-LEFT;");
        return column;
    }
    
    public Node getNode() {
    	return rootPane;
    }
    
    public void loadBdvSources() {
    	bdvRowList.setAll(marsImageMetadata.getBdvSourceNames().stream().map(name -> marsImageMetadata.getBdvSource(name)).collect(Collectors.toList()));
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
	public void onMetadataSelectionChangedEvent(MarsImageMetadata marsImageMetadata) {
		this.marsImageMetadata = marsImageMetadata;
		loadBdvSources();
	}
}
