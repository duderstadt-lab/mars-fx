package de.mpg.biochem.mars.fx.table;

import de.mpg.biochem.mars.table.MARSResultsTable;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class MARSTableView extends TableView<ObservableList<Object>> {
	private MARSResultsTable table;
	
	public MARSTableView(MARSResultsTable table) {
		this.table = table;
		buildTableView();
	}
	
	private void buildTableView() {
		//first add index
        TableColumn<ObservableList<Object>, Object> rowIndexCol = new TableColumn<>("Index");
        rowIndexCol.setCellValueFactory(param ->
                new ReadOnlyObjectWrapper<>(param.getValue().get(0))
        );
        rowIndexCol.setSortable(false);
        getColumns().add(rowIndexCol);

        // add columns
        for (int col = 0; col < table.getColumnCount(); col++) {
            final int finalIdx = col+1;
            TableColumn<ObservableList<Object>, Object> column = new TableColumn<>(
                    table.get(col).getHeader()
            );
            column.setCellValueFactory(param ->
                    new ReadOnlyObjectWrapper<>(param.getValue().get(finalIdx))
            );
            column.setSortable(false);
            getColumns().add(column);
        }

        // add data
        for (int row = 0; row < table.getRowCount(); row++) {
            getItems().add(
        		FXCollections.observableArrayList(
                        table.getRowAsList(row)
                )
            );
        }
	}
	
	public void reloadData() {
		getItems().clear();
		// add data
        for (int row = 0; row < table.getRowCount(); row++) {
            getItems().add(row,
        		FXCollections.observableArrayList(
                        table.getRowAsList(row)
                )
            );
        }
	}
}
