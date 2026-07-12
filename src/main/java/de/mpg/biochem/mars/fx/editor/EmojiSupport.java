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

import org.kordamp.ikonli.fontawesome.FontAwesome;

import de.mpg.biochem.mars.fx.util.ActionUtils;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

/**
 * Wires emoji support (toolbar picker button + {@code :}-autocomplete) into a
 * {@link MarkdownEditorPane}. Single shared implementation used by the
 * Molecule Archive comments editor ({@code CommentsTab}/{@code DocumentEditor})
 * and every {@link MarkdownNotesPane} (Dataset Explorer, Molecule/Metadata
 * general tabs).
 */
public final class EmojiSupport {

	private EmojiSupport() {}

	/**
	 * Creates the toolbar button that opens the {@link EmojiPicker} and inserts
	 * the chosen shortcode at the caret. Callers add the returned button into
	 * their own toolbar/HBox alongside the other formatting buttons.
	 */
	public static Button createToolbarButton(MarkdownEditorPane editor) {
		Button button = new Button();
		button.setGraphic(ActionUtils.icon(FontAwesome.SMILE_O, "1.2em"));
		button.setTooltip(new Tooltip("Insert emoji"));
		button.setFocusTraversable(false);

		EmojiPicker picker = new EmojiPicker(entry -> {
			editor.getSmartEdit().insertText(entry.getInsertText());
			editor.requestFocus();
		});
		button.setOnAction(e -> picker.show(button));
		return button;
	}

	/** Installs the {@code :}-trigger autocomplete popup on this editor's text area. */
	public static void installAutocomplete(MarkdownEditorPane editor) {
		new EmojiAutocomplete(editor);
	}
}
