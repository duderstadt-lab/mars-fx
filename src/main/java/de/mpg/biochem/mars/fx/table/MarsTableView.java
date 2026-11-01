/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2026 Karl Duderstadt
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

import java.util.AbstractList;
import java.util.Locale;
import java.util.prefs.Preferences;

import org.scijava.table.DoubleColumn;

import de.mpg.biochem.mars.table.MarsTable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.NestedTableColumnHeader;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * Read-only view of a {@link MarsTable}. Double values are rounded for
 * display only; copying cells to the clipboard always uses full precision.
 */
public class MarsTableView extends TableView<MarsTableView.MarsTableRowObject> {

	/** Decimal places setting that shows every digit of a double value. */
	public static final int FULL_PRECISION = -1;

	private static final int DEFAULT_DECIMAL_PLACES = 2;
	private static final int[] DECIMAL_PLACES_CHOICES = { 0, 1, 2, 3, 4, 6,
		FULL_PRECISION };
	private static final String DECIMAL_PLACES_KEY = "decimalPlaces";

	private static final KeyCombination COPY = new KeyCodeCombination(KeyCode.C,
		KeyCombination.SHORTCUT_DOWN);
	private static final KeyCombination COPY_WITH_HEADERS =
		new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN,
			KeyCombination.SHIFT_DOWN);

	// Shared by all tables and remembered between sessions.
	private static final IntegerProperty decimalPlaces =
		new SimpleIntegerProperty(readDecimalPlaces());

	static {
		decimalPlaces.addListener((ob, o, n) -> {
			try {
				Preferences.userNodeForPackage(MarsTableView.class).putInt(
					DECIMAL_PLACES_KEY, n.intValue());
			}
			catch (Exception e) {
				// The setting is then only kept for this session.
			}
		});
	}

	private MarsTable table;

	// Held in a field so the weak listener lives as long as this view.
	private final ChangeListener<Number> decimalPlacesListener = (ob, o,
		n) -> refresh();

	private int anchorRow = -1;
	private int anchorColumn = -1;

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
		rowIndexCol.setCellFactory(c -> new IndexCell());
		rowIndexCol.setSortable(false);
		rowIndexCol.setReorderable(false);
		getColumns().add(rowIndexCol);

		this.setItems(FXCollections.observableList(new MarsTableRowList()));

		// add columns
		for (int col = 0; col < table.getColumnCount(); col++) {
			String header = table.get(col).getHeader();
			TableColumn<MarsTableView.MarsTableRowObject, Object> column =
				new TableColumn<>(header);

			if (table.get(col) instanceof DoubleColumn) {
				column.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row
					.getValue().getValue(header)));
				column.setCellFactory(c -> new DoubleCell());
			}
			else column.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row
				.getValue().getStringValue(header)));

			column.setSortable(false);
			column.setReorderable(false);
			getColumns().add(column);
		}

		getSelectionModel().setCellSelectionEnabled(true);
		getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// Clicking a column header selects the whole column.
		addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
			if (e.getButton() != MouseButton.PRIMARY || !e.isStillSincePress())
				return;
			TableColumnHeader header = findColumnHeader(e.getPickResult()
				.getIntersectedNode());
			if (header == null || header instanceof NestedTableColumnHeader ||
				header.getTableColumn() == null) return;
			requestFocus();
			selectColumns(getColumns().indexOf(header.getTableColumn()), e
				.isShiftDown(), e.isShortcutDown());
		});

		addEventHandler(KeyEvent.KEY_PRESSED, e -> {
			if (COPY.match(e)) {
				copySelection(false);
				e.consume();
			}
			else if (COPY_WITH_HEADERS.match(e)) {
				copySelection(true);
				e.consume();
			}
		});

		setContextMenu(buildContextMenu());

		decimalPlaces.addListener(new WeakChangeListener<>(decimalPlacesListener));
	}

	private ContextMenu buildContextMenu() {
		// No accelerators here: Control registers context menu accelerators with
		// the whole scene, which would clash between tables. Keys are handled
		// above instead.
		MenuItem copy = new MenuItem("Copy");
		copy.setOnAction(e -> copySelection(false));

		MenuItem copyWithHeaders = new MenuItem("Copy with headers");
		copyWithHeaders.setOnAction(e -> copySelection(true));

		MenuItem selectAll = new MenuItem("Select all");
		selectAll.setOnAction(e -> getSelectionModel().selectAll());

		Menu decimalPlacesMenu = new Menu("Decimal places");
		ToggleGroup group = new ToggleGroup();
		for (int places : DECIMAL_PLACES_CHOICES) {
			RadioMenuItem item = new RadioMenuItem(places == FULL_PRECISION ? "Full"
				: String.valueOf(places));
			item.setUserData(places);
			item.setToggleGroup(group);
			item.setOnAction(e -> decimalPlaces.set(places));
			decimalPlacesMenu.getItems().add(item);
		}

		ContextMenu contextMenu = new ContextMenu(copy, copyWithHeaders,
			selectAll, new SeparatorMenuItem(), decimalPlacesMenu);
		// Another table may have changed the shared setting.
		contextMenu.setOnShowing(e -> group.getToggles().forEach(t -> t
			.setSelected(t.getUserData().equals(decimalPlaces.get()))));
		return contextMenu;
	}

	private TableColumnHeader findColumnHeader(Node node) {
		while (node != null && node != this) {
			if (node instanceof TableColumnHeader) return (TableColumnHeader) node;
			node = node.getParent();
		}
		return null;
	}

	private void selectRows(int row, boolean extend, boolean add) {
		int from = row, to = row;
		if (extend && anchorRow >= 0) {
			from = Math.min(anchorRow, row);
			to = Math.max(anchorRow, row);
		}
		else anchorRow = row;

		if (!add) getSelectionModel().clearSelection();
		getSelectionModel().selectRange(from, getColumns().get(0), to,
			getColumns().get(getColumns().size() - 1));
	}

	private void selectColumns(int column, boolean extend, boolean add) {
		int rows = getItems().size();
		if (rows == 0 || column < 0) return;

		int from = column, to = column;
		if (extend && anchorColumn >= 0) {
			from = Math.min(anchorColumn, column);
			to = Math.max(anchorColumn, column);
		}
		else anchorColumn = column;

		if (!add) getSelectionModel().clearSelection();
		getSelectionModel().selectRange(0, getColumns().get(from), rows - 1,
			getColumns().get(to));
	}

	/**
	 * Copies the selected cells as tab-separated text with full precision.
	 * Unselected cells inside the rectangle around the selection are left
	 * empty so the values keep their positions when pasted.
	 */
	private void copySelection(boolean withHeaders) {
		if (getSelectionModel().getSelectedCells().isEmpty()) return;

		int minRow = Integer.MAX_VALUE, maxRow = -1;
		int minCol = Integer.MAX_VALUE, maxCol = -1;
		for (TablePosition<?, ?> pos : getSelectionModel().getSelectedCells()) {
			minRow = Math.min(minRow, pos.getRow());
			maxRow = Math.max(maxRow, pos.getRow());
			minCol = Math.min(minCol, pos.getColumn());
			maxCol = Math.max(maxCol, pos.getColumn());
		}
		if (minRow < 0 || minCol < 0) return;

		boolean[][] selected = new boolean[maxRow - minRow + 1][maxCol - minCol +
			1];
		for (TablePosition<?, ?> pos : getSelectionModel().getSelectedCells())
			selected[pos.getRow() - minRow][pos.getColumn() - minCol] = true;

		StringBuilder sb = new StringBuilder();
		if (withHeaders) {
			for (int col = minCol; col <= maxCol; col++) {
				if (col > minCol) sb.append('\t');
				sb.append(getVisibleLeafColumn(col).getText());
			}
			sb.append('\n');
		}
		for (int row = minRow; row <= maxRow; row++) {
			for (int col = minCol; col <= maxCol; col++) {
				if (col > minCol) sb.append('\t');
				if (selected[row - minRow][col - minCol]) sb.append(copyText(row,
					getVisibleLeafColumn(col)));
			}
			sb.append('\n');
		}

		ClipboardContent content = new ClipboardContent();
		content.putString(sb.toString());
		Clipboard.getSystemClipboard().setContent(content);
	}

	private String copyText(int row, TableColumn<MarsTableRowObject, ?> column) {
		Object value = column.getCellData(row);
		if (value instanceof Double) return fullPrecision((Double) value);
		return value == null ? "" : value.toString();
	}

	/**
	 * Formats a double for display. Whole numbers have no decimals, very small
	 * and very large magnitudes use scientific notation so they never round to
	 * zero, and everything else is rounded to the given decimal places.
	 */
	public static String format(double value, int decimalPlaces) {
		if (Double.isNaN(value)) return "NaN";
		if (Double.isInfinite(value)) return value > 0 ? "∞" : "-∞";
		if (decimalPlaces == FULL_PRECISION || isWhole(value)) return fullPrecision(
			value);

		double magnitude = Math.abs(value);
		if (magnitude < Math.pow(10, -decimalPlaces) || magnitude >= 1e6)
			return String.format(Locale.US, "%." + decimalPlaces + "E", value);
		return String.format(Locale.US, "%." + decimalPlaces + "f", value);
	}

	/** Shortest text that reads back as exactly the same double. */
	public static String fullPrecision(double value) {
		if (isWhole(value)) return Long.toString((long) value);
		return Double.toString(value);
	}

	private static boolean isWhole(double value) {
		return value == Math.rint(value) && Math.abs(value) < 1e15;
	}

	public static int getDecimalPlaces() {
		return decimalPlaces.get();
	}

	public static void setDecimalPlaces(int places) {
		decimalPlaces.set(places);
	}

	private static int readDecimalPlaces() {
		try {
			return Preferences.userNodeForPackage(MarsTableView.class).getInt(
				DECIMAL_PLACES_KEY, DEFAULT_DECIMAL_PLACES);
		}
		catch (Exception e) {
			return DEFAULT_DECIMAL_PLACES;
		}
	}

	private class DoubleCell extends TableCell<MarsTableRowObject, Object> {

		private final Tooltip tooltip = new Tooltip();

		DoubleCell() {
			setStyle("-fx-alignment: CENTER-RIGHT;");
		}

		@Override
		protected void updateItem(Object item, boolean empty) {
			super.updateItem(item, empty);
			if (empty || !(item instanceof Double)) {
				setText(null);
				setTooltip(null);
				return;
			}
			double value = (Double) item;
			String text = format(value, decimalPlaces.get());
			String full = fullPrecision(value);
			setText(text);
			if (text.equals(full)) setTooltip(null);
			else {
				tooltip.setText(full);
				setTooltip(tooltip);
			}
		}
	}

	/** Clicking an index cell selects the whole row. */
	private class IndexCell extends TableCell<MarsTableRowObject, Object> {

		IndexCell() {
			addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
				if (isEmpty() || e.getButton() != MouseButton.PRIMARY) return;
				requestFocus();
				selectRows(getIndex(), e.isShiftDown(), e.isShortcutDown());
				e.consume();
			});
		}

		@Override
		protected void updateItem(Object item, boolean empty) {
			super.updateItem(item, empty);
			setText(empty || item == null ? null : item.toString());
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

		@Override
		public boolean equals(Object o) {
			return o instanceof MarsTableRowObject &&
				((MarsTableRowObject) o).row == row;
		}

		@Override
		public int hashCode() {
			return Integer.hashCode(row);
		}
	}

	/** Rows are created on demand so large tables need no copies. */
	class MarsTableRowList extends AbstractList<MarsTableRowObject> {

		@Override
		public MarsTableRowObject get(int index) {
			if (index < 0 || index >= size()) throw new IndexOutOfBoundsException(
				index);
			return new MarsTableRowObject(index);
		}

		@Override
		public int size() {
			return table.getRowCount();
		}

		@Override
		public int indexOf(Object o) {
			if (!(o instanceof MarsTableRowObject)) return -1;
			int row = ((MarsTableRowObject) o).row;
			return row < size() ? row : -1;
		}

		@Override
		public int lastIndexOf(Object o) {
			return indexOf(o);
		}

		@Override
		public boolean contains(Object o) {
			return indexOf(o) >= 0;
		}
	}

}
