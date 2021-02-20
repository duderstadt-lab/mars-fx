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
package de.mpg.biochem.mars.fx.table;

import de.mpg.biochem.mars.table.MarsTable;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class MarsTableView extends TableView<ObservableList<Object>> {
	private MarsTable table;
	
	public MarsTableView(MarsTable table) {
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
