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

package de.mpg.biochem.mars.fx.editor;

import java.util.List;
import java.util.Optional;

import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;
import javafx.stage.Window;

/**
 * Inline {@code :}-triggered emoji autocomplete for a {@link MarkdownEditorPane}.
 * Typing {@code :mic} opens a caret-anchored popup filtered by {@link EmojiData};
 * accepting a suggestion replaces the partial shortcode with the full
 * {@code :microscope: }. Uses the same {@link EmojiData} table as
 * {@link EmojiPicker}, so what autocompletes here is exactly what the picker
 * offers and what the preview can render.
 */
class EmojiAutocomplete {

	private static final int MAX_SUGGESTIONS = 30;
	private static final int MAX_QUERY_LENGTH = 40;

	private final MarkdownEditorPane editor;
	private final MarkdownTextArea textArea;
	private final Popup popup = new Popup();
	private final ListView<EmojiEntry> listView = new ListView<>();

	private int triggerColonOffset = -1;
	private boolean updatingProgrammatically;

	EmojiAutocomplete(MarkdownEditorPane editor) {
		this.editor = editor;
		this.textArea = editor.getTextArea();

		listView.setPrefWidth(220);
		listView.setPrefHeight(180);
		listView.setCellFactory(lv -> new ListCell<>() {

			private final Label label = new Label();

			@Override
			protected void updateItem(EmojiEntry entry, boolean empty) {
				super.updateItem(entry, empty);
				if (empty || entry == null) setGraphic(null);
				else {
					label.setText(entry.getUnicode() + "  :" + entry.getShortcode() +
						":");
					setGraphic(label);
				}
			}
		});
		listView.setOnMouseClicked(e -> accept());

		popup.getContent().add(listView);
		popup.setAutoHide(true);
		popup.setOnAutoHide(e -> triggerColonOffset = -1);

		textArea.caretPositionProperty().addListener((ob, o, n) -> {
			if (!updatingProgrammatically) updateTrigger();
		});

		// While a Popup is showing, JavaFX's PopupWindow.PopupEventRedirector
		// intercepts EVERY key event delivered to the owner window and redirects it
		// into the popup's OWN scene (to popup.getScene().getFocusOwner(), or the
		// scene itself if nothing inside has focus) -- then consumes the original
		// event so it never reaches the owner scene's normal dispatch at all. That
		// happens regardless of autoHide/focus settings; it's how ComboBox-style
		// popups capture arrow keys/Enter for their own navigation. So a filter on
		// textArea (the owner scene) never sees Up/Down/Enter/Escape while this
		// popup is showing -- it has to live in the popup's own scene instead.
		popup.getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
	}

	private void onKeyPressed(KeyEvent e) {
		if (triggerColonOffset < 0) return;

		switch (e.getCode()) {
			case DOWN:
				moveSelection(1);
				e.consume();
				break;
			case UP:
				moveSelection(-1);
				e.consume();
				break;
			case ENTER:
			case TAB:
				accept();
				e.consume();
				break;
			case ESCAPE:
				hide();
				e.consume();
				break;
			default:
				// let it fall through to normal typing; the caret listener will
				// recompute the trigger and refresh (or hide) the popup
				break;
		}
	}

	private void moveSelection(int delta) {
		int size = listView.getItems().size();
		if (size == 0) return;
		int current = listView.getSelectionModel().getSelectedIndex();
		int next = (current < 0) ? 0 : Math.floorMod(current + delta, size);
		listView.getSelectionModel().select(next);
		listView.scrollTo(next);
	}

	private void accept() {
		EmojiEntry entry = listView.getSelectionModel().getSelectedItem();
		if (entry == null || triggerColonOffset < 0) {
			hide();
			return;
		}

		int caret = textArea.getCaretPosition();
		updatingProgrammatically = true;
		try {
			editor.getSmartEdit().replaceTextRange(triggerColonOffset, caret, entry
				.getInsertText() + " ");
		}
		finally {
			updatingProgrammatically = false;
		}
		hide();
	}

	private void hide() {
		triggerColonOffset = -1;
		popup.hide();
	}

	/**
	 * Scans backwards from the caret on the current line for an unclosed
	 * {@code :shortcode} trigger and shows/updates/hides the popup accordingly.
	 */
	private void updateTrigger() {
		int paragraph = textArea.getCurrentParagraph();
		String line = textArea.getText(paragraph);
		int col = textArea.getCaretColumn();

		int colonCol = -1;
		int limit = Math.max(0, col - MAX_QUERY_LENGTH);
		for (int i = col - 1; i >= limit; i--) {
			char c = line.charAt(i);
			if (c == ':') {
				colonCol = i;
				break;
			}
			if (!isShortcodeChar(c)) break;
		}

		if (colonCol < 0) {
			hide();
			return;
		}

		// The colon just found might be the *closing* colon of a shortcode the user
		// just finished typing out by hand (":circus_tent:"), not the opening colon
		// of a new one -- caret sits right after it either way. Without this check,
		// finishing a shortcode re-triggers the popup with an empty query, and a
		// following Enter (meant to just start a new line) silently inserts whatever
		// happens to be first in that unrelated list. Only treat it as a fresh
		// trigger if it is NOT immediately preceded by a matching valid shortcode.
		if (isClosingColonOfCompleteShortcode(line, colonCol)) {
			hide();
			return;
		}

		String query = line.substring(colonCol + 1, col);
		List<EmojiEntry> matches = EmojiData.getInstance().searchPrefix(query,
			MAX_SUGGESTIONS);
		if (matches.isEmpty()) {
			hide();
			return;
		}

		triggerColonOffset = textArea.getAbsolutePosition(paragraph, colonCol);
		listView.setItems(javafx.collections.FXCollections.observableArrayList(
			matches));
		listView.getSelectionModel().select(0);

		// reposition on every keystroke, not just the first: the caret moves right
		// as the user keeps typing the shortcode query
		showPopupAtCaret();
	}

	private static boolean isShortcodeChar(char c) {
		return Character.isLetterOrDigit(c) || c == '_' || c == '+' || c == '-';
	}

	private static boolean isClosingColonOfCompleteShortcode(String line,
		int colonCol)
	{
		int openCol = -1;
		int limit = Math.max(0, colonCol - MAX_QUERY_LENGTH);
		for (int i = colonCol - 1; i >= limit; i--) {
			char c = line.charAt(i);
			if (c == ':') {
				openCol = i;
				break;
			}
			if (!isShortcodeChar(c)) break;
		}
		if (openCol < 0) return false;

		String candidate = line.substring(openCol + 1, colonCol);
		return !candidate.isEmpty() && EmojiData.getInstance().lookup(
			candidate) != null;
	}

	private void showPopupAtCaret() {
		// getCaretBounds() is already in screen coordinates (see Caret.caretBoundsProperty
		// javadoc) despite the name -- do NOT run it through localToScreen() again, that
		// double-applies the window offset and sends the popup miles off to one side.
		Optional<Bounds> bounds = textArea.getCaretBounds();
		if (bounds.isEmpty() || textArea.getScene() == null) return;
		Bounds screen = bounds.get();

		if (popup.isShowing()) {
			popup.setX(screen.getMinX());
			popup.setY(screen.getMaxY());
		}
		else {
			Window window = textArea.getScene().getWindow();
			popup.show(window, screen.getMinX(), screen.getMaxY());
		}
	}
}
