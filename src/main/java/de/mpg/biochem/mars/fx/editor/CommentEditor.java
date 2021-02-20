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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Menu;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.AnchorPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Text;
import org.fxmisc.undo.UndoManager;

import de.mpg.biochem.mars.fx.options.Options;
import de.mpg.biochem.mars.fx.preview.MarkdownPreviewPane;
import de.mpg.biochem.mars.fx.preview.MarkdownPreviewPane.Type;
import de.mpg.biochem.mars.fx.util.PrefsBooleanProperty;
import de.mpg.biochem.mars.molecule.MoleculeArchive;

/**
 * Editor for MoleculeArchive comments
 *
 * Original author - Karl Tauber from markdownwriterfx
 * Modifications by Karl Duderstadt adapted for MoleculeArchive comments editing.
 */
public class CommentEditor extends AnchorPane {
	private SplitPane splitPane;
	private MarkdownEditorPane markdownEditorPane;
	private MarkdownPreviewPane markdownPreviewPane;

	final PrefsBooleanProperty editMode = new PrefsBooleanProperty(false);
	
	final PrefsBooleanProperty previewVisible = new PrefsBooleanProperty(true);
	final PrefsBooleanProperty htmlSourceVisible = new PrefsBooleanProperty();
	final PrefsBooleanProperty markdownAstVisible = new PrefsBooleanProperty();
	final PrefsBooleanProperty externalVisible = new PrefsBooleanProperty();

	public CommentEditor() {
		@SuppressWarnings("rawtypes")
		ChangeListener previewTypeListener = (observable, oldValue, newValue) -> updatePreviewType();
		
		Options.markdownRendererProperty().addListener(previewTypeListener);
		previewVisible.addListener(previewTypeListener);
		
		ChangeListener editModeListener = (observable, oldValue, newValue) -> updateEditMode();
		editMode.addListener(editModeListener);
		
		initialize();
	}

	MarkdownEditorPane getEditor() {
		return markdownEditorPane;
	}

	// 'editor' property
	private final ObjectProperty<MarkdownEditorPane> editor = new SimpleObjectProperty<>();
	ReadOnlyObjectProperty<MarkdownEditorPane> editorProperty() { return editor; }

	// 'path' property
	private final ObjectProperty<Path> path = new SimpleObjectProperty<>();
	Path getPath() { return path.get(); }
	void setPath(Path path) { this.path.set(path); }
	ObjectProperty<Path> pathProperty() { return path; }

	// 'readOnly' property
	private final ReadOnlyBooleanWrapper readOnly = new ReadOnlyBooleanWrapper();
	boolean isReadOnly() { return readOnly.get(); }
	ReadOnlyBooleanProperty readOnlyProperty() { return readOnly.getReadOnlyProperty(); }

	// 'modified' property
	private final ReadOnlyBooleanWrapper modified = new ReadOnlyBooleanWrapper();
	boolean isModified() { return modified.get(); }
	ReadOnlyBooleanProperty modifiedProperty() { return modified.getReadOnlyProperty(); }

	// 'canUndo' property
	private final BooleanProperty canUndo = new SimpleBooleanProperty();
	BooleanProperty canUndoProperty() { return canUndo; }

	// 'canRedo' property
	private final BooleanProperty canRedo = new SimpleBooleanProperty();
	BooleanProperty canRedoProperty() { return canRedo; }

	private boolean updatePreviewTypePending;
	private void updatePreviewType() {
		if (markdownPreviewPane == null)
			return;

		// avoid too many (and useless) runLater() invocations
		if (updatePreviewTypePending)
			return;
		updatePreviewTypePending = true;

		Platform.runLater(() -> {
			updatePreviewTypePending = false;

			MarkdownPreviewPane.Type previewType = getPreviewType();

			markdownPreviewPane.setRendererType(Options.getMarkdownRenderer());
			markdownPreviewPane.setType(previewType);

			// add/remove previewPane from splitPane
			ObservableList<Node> splitItems = splitPane.getItems();
			Node previewPane = markdownPreviewPane.getNode();
			if (previewType != Type.None) {
				if (!splitItems.contains(previewPane))
					splitItems.add(previewPane);
			} else
				splitItems.remove(previewPane);
		});
	}
	
	private boolean updateEditModePending;
	private void updateEditMode() {
		if (markdownPreviewPane == null)
			return;

		// avoid too many (and useless) runLater() invocations
		if (updateEditModePending)
			return;
		updateEditModePending = true;
		
		Platform.runLater(() -> {
			updateEditModePending = false;
			
			MarkdownPreviewPane.Type previewType = getPreviewType();

			markdownPreviewPane.setRendererType(Options.getMarkdownRenderer());
			markdownPreviewPane.setType(previewType);

			// add/remove editopPane from splitPane
			ObservableList<Node> splitItems = splitPane.getItems();
			Node previewPane = markdownPreviewPane.getNode();
			if (!splitItems.contains(markdownEditorPane.getNode())) {
				showEditor();				
			} else {
				showPreview();
			}
		});
	}
	
	public void showPreview() {
		ObservableList<Node> splitItems = splitPane.getItems();
		Node previewPane = markdownPreviewPane.getNode();
		if (!splitItems.contains(previewPane)) {
			previewVisible.set(true);
			System.out.println("adding Preview");
			splitItems.add(previewPane);
		}
		if (splitItems.contains(markdownEditorPane.getNode())) {
			splitItems.remove(markdownEditorPane.getNode());
			markdownPreviewPane.editorSelectionProperty().unbind();
			markdownPreviewPane.editorSelectionProperty().set(new IndexRange(-1,-1));
		}
	}
	
	public void showEditor() {
		ObservableList<Node> splitItems = splitPane.getItems();
		if (!splitItems.contains(markdownEditorPane.getNode())) {
			splitItems.add(0, markdownEditorPane.getNode());
			markdownPreviewPane.editorSelectionProperty().bind(markdownEditorPane.selectionProperty());				
		}
	}

	private MarkdownPreviewPane.Type getPreviewType() {
		MarkdownPreviewPane.Type previewType = Type.None;
		if (previewVisible.get())
			previewType = MarkdownPreviewPane.Type.Web;
		else 
			previewType = Type.None;
		return previewType;
	}

	private void initialize() {
		markdownEditorPane = new MarkdownEditorPane();
		markdownPreviewPane = new MarkdownPreviewPane();

		markdownEditorPane.pathProperty().bind(path);

		// clear undo history after first load
		markdownEditorPane.getUndoManager().forgetHistory();

		// bind preview to editor
		markdownPreviewPane.pathProperty().bind(pathProperty());
		markdownPreviewPane.markdownTextProperty().bind(markdownEditorPane.markdownTextProperty());
		markdownPreviewPane.markdownASTProperty().bind(markdownEditorPane.markdownASTProperty());
		markdownPreviewPane.editorSelectionProperty().bind(markdownEditorPane.selectionProperty());
		markdownPreviewPane.scrollYProperty().bind(markdownEditorPane.scrollYProperty());
		
		// bind properties
		readOnly.bind(markdownEditorPane.readOnlyProperty());

		// bind the editor undo manager to the properties
		UndoManager<?> undoManager = markdownEditorPane.getUndoManager();
		modified.bind(Bindings.not(undoManager.atMarkedPositionProperty()));
		canUndo.bind(undoManager.undoAvailableProperty());
		canRedo.bind(undoManager.redoAvailableProperty());

		//splitPane = new SplitPane(markdownEditorPane.getNode());
		if (getPreviewType() != MarkdownPreviewPane.Type.None)
			splitPane = new SplitPane(markdownPreviewPane.getNode());
		
		getChildren().add(splitPane);
        AnchorPane.setTopAnchor(splitPane, 0.0);
        AnchorPane.setBottomAnchor(splitPane, 0.0);
        AnchorPane.setRightAnchor(splitPane, 0.0);
        AnchorPane.setLeftAnchor(splitPane, 0.0);

		updatePreviewType();
		markdownEditorPane.requestFocus();

		// update 'editor' property
		editor.set(markdownEditorPane);
		
		//We unbind selections until edit mode is activated.
		markdownPreviewPane.editorSelectionProperty().unbind();
		markdownPreviewPane.editorSelectionProperty().set(new IndexRange(-1,-1));
	}

	public void setComments(String comments) {
		markdownEditorPane.setMarkdown(comments);
		markdownEditorPane.getUndoManager().mark();
	}

	public String getComments() {
		String comments = markdownEditorPane.getMarkdown();
		markdownEditorPane.getUndoManager().mark();
		return comments;
	}
}

