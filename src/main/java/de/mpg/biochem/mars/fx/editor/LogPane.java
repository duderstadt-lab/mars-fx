/*
 * Copyright (c) 2015 Karl Tauber <karl at jformdesigner dot com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.mpg.biochem.mars.fx.editor;

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyCombination.*;
import static org.fxmisc.wellbehaved.event.EventPattern.keyPressed;
import static org.fxmisc.wellbehaved.event.InputMap.*;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.parser.Parser;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.Caret.CaretVisibility;
import org.fxmisc.richtext.CaretNode;
import org.fxmisc.richtext.CharacterHit;
import org.fxmisc.undo.UndoManager;
import org.fxmisc.wellbehaved.event.Nodes;

import de.mpg.biochem.mars.fx.controls.BottomSlidePane;
import de.mpg.biochem.mars.fx.editor.FindReplacePane.HitsChangeListener;
import de.mpg.biochem.mars.fx.editor.MarkdownSyntaxHighlighter.ExtraStyledRanges;
import de.mpg.biochem.mars.fx.options.MarkdownExtensions;
import de.mpg.biochem.mars.fx.options.Options;

public class LogPane
{
	private final BorderPane borderPane;
	private final MarkdownTextArea textArea;

	private final FindReplacePane findReplacePane;
	private final HitsChangeListener findHitsChangeListener;
	private Parser parser;
	private String lineSeparator = getLineSeparatorOrDefault();

	public LogPane() {
		textArea = new MarkdownTextArea();
		textArea.setStyle("-fx-font-family: 'Courier'; -fx-font-size: 10pt;");
		textArea.setEditable(false);
		//textArea.setWrapText(true);
		textArea.setUseInitialStyleForInsertion(true);
		textArea.getStyleClass().add("markdown-editor");
		textArea.getStylesheets().add("de/mpg/biochem/mars/fx/editor/MarkdownEditor.css");
		textArea.getStylesheets().add("de/mpg/biochem/mars/fx/prism.css");

		textArea.textProperty().addListener((observable, oldText, newText) -> {
			textChanged(newText);
		});

		// create scroll pane
		VirtualizedScrollPane<MarkdownTextArea> scrollPane = new VirtualizedScrollPane<>(textArea);

		// create border pane
		borderPane = new BorderPane();
		borderPane.setCenter(scrollPane);

		// initialize properties
		markdownText.set("");
		markdownAST.set(parseMarkdown(""));

		// find/replace
		findReplacePane = new FindReplacePane(textArea);
		findHitsChangeListener = this::findHitsChanged;
		findReplacePane.addListener(findHitsChangeListener);
		findReplacePane.closable(false);
		
		borderPane.setTop(findReplacePane.getNode());

		// workaround a problem with wrong selection after undo:
		//   after undo the selection is 0-0, anchor is 0, but caret position is correct
		//   --> set selection to caret position
		textArea.selectionProperty().addListener((observable,oldSelection,newSelection) -> {
			// use runLater because the wrong selection temporary occurs while edition
			Platform.runLater(() -> {
				IndexRange selection = textArea.getSelection();
				int caretPosition = textArea.getCaretPosition();
				if (selection.getStart() == 0 && selection.getEnd() == 0 && textArea.getAnchor() == 0 && caretPosition > 0)
					textArea.selectRange(caretPosition, caretPosition);
			});
		});
	}

	public javafx.scene.Node getNode() {
		return borderPane;
	}

	public boolean isReadOnly() {
		return textArea.isDisable();
	}

	public void setReadOnly(boolean readOnly) {
		textArea.setDisable(readOnly);
	}

	public BooleanProperty readOnlyProperty() {
		return textArea.disableProperty();
	}

	public UndoManager<?> getUndoManager() {
		return textArea.getUndoManager();
	}

	public void requestFocus() {
		Platform.runLater(() -> {
			if (textArea.getScene() != null)
				textArea.requestFocus();
			else {
				// text area still does not have a scene
				// --> use listener on scene to make sure that text area receives focus
				ChangeListener<Scene> l = new ChangeListener<Scene>() {
					@Override
					public void changed(ObservableValue<? extends Scene> observable, Scene oldValue, Scene newValue) {
						textArea.sceneProperty().removeListener(this);
						textArea.requestFocus();
					}
				};
				textArea.sceneProperty().addListener(l);
			}
		});
	}

	private String getLineSeparatorOrDefault() {
		String lineSeparator = Options.getLineSeparator();
		return (lineSeparator != null) ? lineSeparator : System.getProperty( "line.separator", "\n" );
	}

	private String determineLineSeparator(String str) {
		int strLength = str.length();
		for (int i = 0; i < strLength; i++) {
			char ch = str.charAt(i);
			if (ch == '\n')
				return (i > 0 && str.charAt(i - 1) == '\r') ? "\r\n" : "\n";
		}
		return getLineSeparatorOrDefault();
	}

	// 'markdown' property
	public String getMarkdown() {
		String markdown = textArea.getText();
		if (!lineSeparator.equals("\n"))
			markdown = markdown.replace("\n", lineSeparator);
		return markdown;
	}
	public void setMarkdown(String markdown) {
		// remember old selection range
		IndexRange oldSelection = textArea.getSelection();

		// replace text
		lineSeparator = determineLineSeparator(markdown);
		//textArea.replaceText(markdown);
		textArea.replaceText(0, textArea.getLength(), markdown);

		// restore old selection range
        int newLength = textArea.getLength();
        textArea.selectRange(Math.min(oldSelection.getStart(), newLength), Math.min(oldSelection.getEnd(), newLength));
	}
	public ObservableValue<String> markdownProperty() { return textArea.textProperty(); }

	// 'markdownText' property
	private final ReadOnlyStringWrapper markdownText = new ReadOnlyStringWrapper();
	public String getMarkdownText() { return markdownText.get(); }
	public ReadOnlyStringProperty markdownTextProperty() { return markdownText.getReadOnlyProperty(); }

	// 'markdownAST' property
	private final ReadOnlyObjectWrapper<Node> markdownAST = new ReadOnlyObjectWrapper<>();
	public Node getMarkdownAST() { return markdownAST.get(); }
	public ReadOnlyObjectProperty<Node> markdownASTProperty() { return markdownAST.getReadOnlyProperty(); }

	// 'selection' property
	public ObservableValue<IndexRange> selectionProperty() { return textArea.selectionProperty(); }

	// 'scrollY' property
	public double getScrollY() { return textArea.scrollY.getValue(); }
	public ObservableValue<Double> scrollYProperty() { return textArea.scrollY; }

	// 'path' property
	private final ObjectProperty<Path> path = new SimpleObjectProperty<>();
	public Path getPath() { return path.get(); }
	public void setPath(Path path) { this.path.set(path); }
	public ObjectProperty<Path> pathProperty() { return path; }

	Path getParentPath() {
		Path path = getPath();
		return (path != null) ? path.getParent() : null;
	}

	private void textChanged(String newText) {
		if (borderPane.getBottom() != null) {
			findReplacePane.removeListener(findHitsChangeListener);
			findReplacePane.textChanged();
			findReplacePane.addListener(findHitsChangeListener);
		}

		if (isReadOnly())
			newText = "";

		Node astRoot = parseMarkdown(newText);

		applyHighlighting(astRoot);

		markdownText.set(newText);
		markdownAST.set(astRoot);
	}

	private void findHitsChanged() {
		applyHighlighting(markdownAST.get());
	}

	Node parseMarkdown(String text) {
		if (parser == null) {
			parser = Parser.builder().build();
		}
		return parser.parse(text);
	}

	private void applyHighlighting(Node astRoot) {
		List<ExtraStyledRanges> extraStyledRanges = findReplacePane.hasHits()
			? Arrays.asList(
				new ExtraStyledRanges("hit", findReplacePane.getHits()),
				new ExtraStyledRanges("hit-active", Arrays.asList(findReplacePane.getActiveHit())))
			: null;

		MarkdownSyntaxHighlighter.highlight(textArea, astRoot, extraStyledRanges);
	}

	public void selectAll() {
		textArea.selectAll();
	}

	public void selectRange(int anchor, int caretPosition) {
		SmartEdit.selectRange(textArea, anchor, caretPosition);
	}

	//---- find/replace -------------------------------------------------------

	public void find(boolean replace) {
		//if (borderPane.getBottom() == null)
		//	borderPane.setBottom(findReplacePane.getNode());

		findReplacePane.show(replace, true);
	}

	public void findNextPrevious(boolean next) {
		if (borderPane.getBottom() == null) {
			// show pane
			find(false);
			return;
		}

		if (next)
			findReplacePane.findNext();
		else
			findReplacePane.findPrevious();
	}
}
