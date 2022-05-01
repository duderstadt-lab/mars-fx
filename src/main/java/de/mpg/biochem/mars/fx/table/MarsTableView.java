/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2022 Karl Duderstadt
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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.scijava.table.DoubleColumn;

import de.mpg.biochem.mars.table.MarsTable;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class MarsTableView extends TableView<MarsTableView.MarsTableRowObject> {

	private MarsTable table;

	public MarsTableView(MarsTable table) {
		this.table = table;
		buildTableView();
	}

	private void buildTableView() {
		// first add index
		TableColumn<MarsTableView.MarsTableRowObject, Object> rowIndexCol =
			new TableColumn<>("Index");
		rowIndexCol.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param
			.getValue().getRowNumber()));
		rowIndexCol.setSortable(false);
		getColumns().add(rowIndexCol);

		this.setItems(FXCollections.observableList(new MarsTableRowList()));

		// add columns
		for (int col = 0; col < table.getColumnCount(); col++) {
			String header = table.get(col).getHeader();
			// final int finalIdx = col+1;
			TableColumn<MarsTableView.MarsTableRowObject, Object> column =
				new TableColumn<>(header);

			if (table.get(col) instanceof DoubleColumn) column.setCellValueFactory(
				row -> new ReadOnlyObjectWrapper<>(row.getValue().getValue(header)));
			else column.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row
				.getValue().getStringValue(header)));

			column.setSortable(false);
			getColumns().add(column);
		}
	}

	class MarsTableRowObject {

		private final int row;

		MarsTableRowObject(final int row) {
			this.row = row;
		}

		double getValue(String header) {
			return table.getValue(header, row);
		}

		String getStringValue(String header) {
			return table.getStringValue(header, row);
		}

		int getRowNumber() {
			return row;
		}
	}

	class MarsTableRowList implements List<MarsTableRowObject> {

		public MarsTableRowList() {}

		@Override
		public int size() {
			return table.getRowCount();
		}

		@Override
		public boolean isEmpty() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean contains(Object o) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Iterator<MarsTableRowObject> iterator() {
			return null;
		}

		@Override
		public Object[] toArray() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> T[] toArray(T[] a) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean add(MarsTableRowObject e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean remove(Object o) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean addAll(Collection<? extends MarsTableRowObject> c) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean addAll(int index,
			Collection<? extends MarsTableRowObject> c)
		{
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void clear() {
			// TODO Auto-generated method stub

		}

		@Override
		public MarsTableRowObject get(int index) {
			return new MarsTableRowObject(index);
		}

		@Override
		public MarsTableRowObject set(int index, MarsTableRowObject element) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void add(int index, MarsTableRowObject element) {
			// TODO Auto-generated method stub

		}

		@Override
		public MarsTableRowObject remove(int index) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int indexOf(Object o) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int lastIndexOf(Object o) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public ListIterator<MarsTableRowObject> listIterator() {
			return null;
		}

		@Override
		public ListIterator<MarsTableRowObject> listIterator(int index) {
			return new MarsTableRowObjectListItr(index);
		}

		@Override
		public List<MarsTableRowObject> subList(int fromIndex, int toIndex) {
			return null;
		}
	}

	class MarsTableRowObjectListItr implements ListIterator<MarsTableRowObject> {

		private int index;

		MarsTableRowObjectListItr(int index) {
			this.index = index;
		}

		public boolean hasNext() {
			return index < table.getRowCount();
		}

		public MarsTableRowObject next() {
			MarsTableRowObject obj = new MarsTableRowObject(index);
			index++;
			return obj;
		}

		@Override
		public boolean hasPrevious() {
			return index > 0;
		}

		@Override
		public MarsTableRowObject previous() {
			return new MarsTableRowObject(index - 1);
		}

		@Override
		public int nextIndex() {
			return index + 1;
		}

		@Override
		public int previousIndex() {
			return index - 1;
		}

		@Override
		public void remove() {
			// TODO Auto-generated method stub

		}

		@Override
		public void set(MarsTableRowObject e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void add(MarsTableRowObject e) {
			// TODO Auto-generated method stub

		}
	}

}
