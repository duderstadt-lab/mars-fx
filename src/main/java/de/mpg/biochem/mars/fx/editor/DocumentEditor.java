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

import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.molecule.CommentsTab;
import de.mpg.biochem.mars.fx.options.Options;
import de.mpg.biochem.mars.fx.preview.MarkdownPreviewPane;
import de.mpg.biochem.mars.fx.preview.MarkdownPreviewPane.Type;
import de.mpg.biochem.mars.fx.util.PrefsBooleanProperty;
import de.mpg.biochem.mars.molecule.MoleculeArchive;

import javafx.event.Event;
import javafx.event.EventHandler;

import de.mpg.biochem.mars.fx.dashboard.AbstractDashboard;
import de.mpg.biochem.mars.fx.event.MetadataEvent;
import de.mpg.biochem.mars.fx.molecule.metadataTab.MetadataSubPane;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.util.DefaultJsonConverter;
import de.mpg.biochem.mars.util.MarsUtil;

/**
 * Editor for MoleculeArchive comments
 *
 * Original author - Karl Tauber from markdownwriterfx
 * Modifications by Karl Duderstadt adapted for MoleculeArchive comments editing.
 */
public class DocumentEditor extends AnchorPane {
	
	private final CommentsTab commentsTab;
	private final Tab tab = new Tab();
	private SplitPane splitPane;
	private MarkdownEditorPane markdownEditorPane;
	private MarkdownPreviewPane markdownPreviewPane;
	private String name;
	
	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;

	public DocumentEditor(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive, CommentsTab commentsTab, String name) {
		this.commentsTab = commentsTab;
		this.name = name;
		tab.setText(name);
		this.archive = archive;
		
		// avoid that this is GCed
		tab.setUserData(this);
		
		@SuppressWarnings("rawtypes")
		ChangeListener previewVisibleListener = (observable, oldValue, newValue) -> updateEditAndPreview();
		ChangeListener editModeListener = (observable, oldValue, newValue) -> updateEditAndPreview();
		//ChangeListener<Boolean> stageFocusedListener = (observable, oldValue, newValue) -> {
		//	if (newValue)
		//		load();
		//};
		
		tab.setOnSelectionChanged(e -> {
			if(tab.isSelected()) {
				Platform.runLater(() -> activated());

				Options.markdownRendererProperty().addListener(previewVisibleListener);
				commentsTab.previewVisible.addListener(previewVisibleListener);
				commentsTab.editMode.addListener(editModeListener);

				//mainWindow.stageFocusedProperty.addListener(stageFocusedListener);
			} else {
				Platform.runLater(() -> deactivated());

				Options.markdownRendererProperty().removeListener(previewVisibleListener);
				commentsTab.previewVisible.removeListener(previewVisibleListener);
				commentsTab.editMode.removeListener(editModeListener);

				//mainWindow.stageFocusedProperty.removeListener(stageFocusedListener);
			}
		});
	}
	
	public Node getNode() {
		return splitPane;
	}
	
	public void dispose() {
		// avoid memory leaks
		tab.setUserData(null);
		tab.setContent(null);
	}

	public Tab getTab() {
		return tab;
	}

	public MarkdownEditorPane getEditor() {
		return markdownEditorPane;
	}

	// 'editor' property
	private final ObjectProperty<MarkdownEditorPane> editor = new SimpleObjectProperty<>();
	public ReadOnlyObjectProperty<MarkdownEditorPane> editorProperty() { return editor; }

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
	
	private void activated() {
		if( tab.getTabPane() == null || !tab.isSelected())
			return; // tab is already closed or no longer active

		if (tab.getContent() != null) {
			markdownEditorPane.setVisible(true);
			markdownEditorPane.requestFocus();
			updateEditAndPreview();
			return;
		}

		// load file and create UI when the tab becomes visible the first time

		markdownEditorPane = new MarkdownEditorPane();
		markdownPreviewPane = new MarkdownPreviewPane();

		String markdown = archive.properties().getDocument(name);

		markdownEditorPane.setMarkdown(markdown);
		//markdownEditorPane.getUndoManager().mark();

		// clear undo history after first load
		markdownEditorPane.getUndoManager().forgetHistory();

		// bind preview to editor
		markdownPreviewPane.markdownTextProperty().bind(markdownEditorPane.markdownTextProperty());
		markdownPreviewPane.markdownASTProperty().bind(markdownEditorPane.markdownASTProperty());
		markdownPreviewPane.editorSelectionProperty().bind(markdownEditorPane.selectionProperty());
		markdownPreviewPane.scrollYProperty().bind(markdownEditorPane.scrollYProperty());

		markdownPreviewPane.setRendererType(Options.getMarkdownRenderer());
		markdownPreviewPane.setType(MarkdownPreviewPane.Type.Web);
		
		// bind properties
		readOnly.bind(markdownEditorPane.readOnlyProperty());

		// bind the editor undo manager to the properties
		UndoManager<?> undoManager = markdownEditorPane.getUndoManager();
		modified.bind(Bindings.not(undoManager.atMarkedPositionProperty()));
		canUndo.bind(undoManager.undoAvailableProperty());
		canRedo.bind(undoManager.redoAvailableProperty());

		splitPane = new SplitPane();
		splitPane.setStyle("-fx-border-color: lightgray");
		tab.setContent(splitPane);

		markdownEditorPane.setVisible(true);
		markdownEditorPane.requestFocus();

		// update 'editor' property
		editor.set(markdownEditorPane);
		
		updateEditAndPreview();
		
		//We unbind selections until edit mode is activated.
		//markdownPreviewPane.editorSelectionProperty().unbind();
		//markdownPreviewPane.editorSelectionProperty().set(new IndexRange(-1,-1));
	}
	
	private boolean updateEditAndPreviewPending;
	public void updateEditAndPreview() {
		if (markdownPreviewPane == null || markdownPreviewPane == null)
			return;
		
		// avoid too many (and useless) runLater() invocations
		if (updateEditAndPreviewPending)
			return;
		
		updateEditAndPreviewPending = true;

		Platform.runLater(() -> {
			updateEditAndPreviewPending = false;
			ObservableList<Node> splitItems = splitPane.getItems();	
			if (commentsTab.editMode.get()) 
				showEditor();
			else if (splitItems.contains(markdownEditorPane.getNode())) {
				splitItems.remove(markdownEditorPane.getNode());
				markdownPreviewPane.editorSelectionProperty().unbind();
				markdownPreviewPane.editorSelectionProperty().set(new IndexRange(-1,-1));
				commentsTab.previewVisible.set(true);
			}
				
			if (commentsTab.previewVisible.get())
				showPreview();
			else if (splitItems.contains(markdownPreviewPane.getNode())) {
				splitItems.remove(markdownPreviewPane.getNode());
			}		
		});
	}

	private void deactivated() {
		if (markdownEditorPane == null)
			return;

		markdownEditorPane.setVisible(false);
	}

	public void requestFocus() {
		if (markdownEditorPane != null)
			markdownEditorPane.requestFocus();
	}
	
	public void showPreview() {
		ObservableList<Node> splitItems = splitPane.getItems();
		Node previewPane = markdownPreviewPane.getNode();
		if (!splitItems.contains(previewPane)) {
			splitItems.add(previewPane);
		}
	}
	
	public void showEditor() {
		ObservableList<Node> splitItems = splitPane.getItems();
		if (!splitItems.contains(markdownEditorPane.getNode())) {
			splitItems.add(0, markdownEditorPane.getNode());
			markdownPreviewPane.editorSelectionProperty().bind(markdownEditorPane.selectionProperty());				
		}
	}
	
	public void save() {
		if (archive != null) {
			archive.properties().putDocument(name, markdownEditorPane.getMarkdown());
			markdownEditorPane.getUndoManager().mark();
		}
	}
}

