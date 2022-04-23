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

package de.mpg.biochem.mars.fx.util;

import javafx.event.Event;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

public class EditCell<S, T> extends TableCell<S, T> {

	private final TextField textField = new TextField();

	// Converter for converting the text in the text field to the user type, and
	// vice-versa:
	private final StringConverter<T> converter;

	public EditCell(StringConverter<T> converter) {
		this.converter = converter;

		itemProperty().addListener((obx, oldItem, newItem) -> {
			if (newItem == null) {
				setText(null);
			}
			else {
				setText(converter.toString(newItem));
			}
		});
		setGraphic(textField);
		setContentDisplay(ContentDisplay.TEXT_ONLY);

		textField.setOnAction(evt -> {
			commitEdit(this.converter.fromString(textField.getText()));
		});
		textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
			if (!isNowFocused) {
				commitEdit(this.converter.fromString(textField.getText()));
			}
		});
		textField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.ESCAPE) {
				textField.setText(converter.toString(getItem()));
				cancelEdit();
				event.consume();
			}
			else if (event.getCode() == KeyCode.RIGHT) {
				getTableView().getSelectionModel().selectRightCell();
				event.consume();
			}
			else if (event.getCode() == KeyCode.LEFT) {
				getTableView().getSelectionModel().selectLeftCell();
				event.consume();
			}
			else if (event.getCode() == KeyCode.UP) {
				getTableView().getSelectionModel().selectAboveCell();
				event.consume();
			}
			else if (event.getCode() == KeyCode.DOWN) {
				getTableView().getSelectionModel().selectBelowCell();
				event.consume();
			}
		});
	}

	/**
	 * Convenience converter that does nothing (converts Strings to themselves and
	 * vice-versa...).
	 */
	public static final StringConverter<String> IDENTITY_CONVERTER =
		new StringConverter<String>()
		{

			@Override
			public String toString(String object) {
				return object;
			}

			@Override
			public String fromString(String string) {
				return string;
			}

		};

	public static <S> EditCell<S, String> createStringEditCell() {
		return new EditCell<S, String>(IDENTITY_CONVERTER);
	}

	// set the text of the text field and display the graphic
	@Override
	public void startEdit() {
		super.startEdit();
		textField.setText(converter.toString(getItem()));
		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		textField.requestFocus();
	}

	// revert to text display
	@Override
	public void cancelEdit() {
		super.cancelEdit();
		setContentDisplay(ContentDisplay.TEXT_ONLY);
	}

	// commits the edit. Update property if possible and revert to text display
	@Override
	public void commitEdit(T item) {

		// This block is necessary to support commit on losing focus, because the
		// baked-in mechanism
		// sets our editing state to false before we can intercept the loss of
		// focus.
		// The default commitEdit(...) method simply bails if we are not editing...
		if (!isEditing() && !item.equals(getItem())) {
			TableView<S> table = getTableView();
			if (table != null) {
				TableColumn<S, T> column = getTableColumn();
				CellEditEvent<S, T> event = new CellEditEvent<>(table,
					new TablePosition<S, T>(table, getIndex(), column), TableColumn
						.editCommitEvent(), item);
				Event.fireEvent(column, event);
			}
		}

		super.commitEdit(item);

		setContentDisplay(ContentDisplay.TEXT_ONLY);
	}

}
