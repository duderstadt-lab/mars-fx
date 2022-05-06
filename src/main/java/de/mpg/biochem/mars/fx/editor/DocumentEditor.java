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

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.fxmisc.undo.UndoManager;
import org.scijava.Context;
import org.scijava.plugin.Parameter;

import com.vladsch.flexmark.parser.Parser;

import de.mpg.biochem.mars.fx.dialogs.RoverConfirmationDialog;
import de.mpg.biochem.mars.fx.event.RunMoleculeArchiveTaskEvent;
import de.mpg.biochem.mars.fx.molecule.CommentsTab;
import de.mpg.biochem.mars.fx.options.MarkdownExtensions;
import de.mpg.biochem.mars.fx.options.Options;
import de.mpg.biochem.mars.fx.preview.FencedCodeWidgetNodePostProcessorFactory;
import de.mpg.biochem.mars.fx.preview.MarkdownPreviewPane;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.util.MarsDocument;
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
import javafx.scene.control.ButtonType;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

/**
 * Editor for MoleculeArchive comments Original author - Karl Tauber from
 * markdownwriterfx Modifications by Karl Duderstadt adapted for MoleculeArchive
 * comments editing.
 */
public class DocumentEditor extends AnchorPane {

	private final CommentsTab commentsTab;
	private final Tab tab = new Tab();
	private SplitPane splitPane;
	private MarkdownEditorPane markdownEditorPane;
	private MarkdownPreviewPane markdownPreviewPane;
	private MarsDocument document;
	private Parser widgetParser;
	private Set<String> activeMediaIDs = new HashSet<String>();

	@Parameter
	protected Context context;

	public static final String MARKDOWN_WIDGET_MEDIA_KEY_PREFIX =
		"MARKDOWN_WIDGET_MEDIA_KEY:";

	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;

	public DocumentEditor(final Context context,
		MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive,
		CommentsTab commentsTab, String name)
	{
		context.inject(this);
		this.commentsTab = commentsTab;

		if (!name.equals("Comments")) {
			Label label = new Label(name);
			tab.setGraphic(label);

			TextField textField = new TextField();
			label.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2) {
					textField.setText(getDocument().getName());
					textField.setPrefWidth(label.getWidth() + label.getWidth() / 2);
					tab.setGraphic(textField);
					textField.selectAll();
					textField.requestFocus();
				}
			});

			textField.setOnAction(event -> {
				if (!textField.getText().equals(getDocument().getName()) && !archive
					.properties().getDocumentNames().contains(textField.getText()))
				{
					String newName = textField.getText();
					MarsDocument document = getDocument();
					archive.properties().removeDocument(document.getName());
					document.setName(newName);
					archive.properties().putDocument(document);
					label.setText(newName);
				}
				tab.setGraphic(label);
			});

			textField.focusedProperty().addListener((observable, oldValue,
				newValue) -> {
				if (!newValue) {
					if (!textField.getText().equals(getDocument().getName()) && !archive
						.properties().getDocumentNames().contains(textField.getText()))
					{
						String newName = textField.getText();
						MarsDocument document = getDocument();
						archive.properties().removeDocument(document.getName());
						document.setName(newName);
						archive.properties().putDocument(document);
						label.setText(newName);
					}
					tab.setGraphic(label);
				}
			});

			tab.setOnCloseRequest(e -> {
				RoverConfirmationDialog alert = new RoverConfirmationDialog(commentsTab
					.getNode().getScene().getWindow(), "Are you sure you want to close " +
						this.document.getName() + "?");

				Optional<ButtonType> result = alert.showAndWait();
				if (result.get() != ButtonType.OK) {
					e.consume();
				}
				else close();
			});
		}
		else tab.setText(name);

		this.archive = archive;

		if (archive.properties().getDocumentNames().contains(name)) this.document =
			archive.properties().getDocument(name);
		else {
			this.document = new MarsDocument(name, "");
			archive.properties().putDocument(document);
		}

		// avoid that this is GCed
		tab.setUserData(this);

		@SuppressWarnings("rawtypes")
		ChangeListener previewVisibleListener = (observable, oldValue,
			newValue) -> updateEditAndPreview();
		ChangeListener editModeListener = (observable, oldValue,
			newValue) -> updateEditAndPreview();

		tab.setOnSelectionChanged(e -> {
			if (tab.isSelected()) {
				Platform.runLater(() -> activated());

				Options.markdownRendererProperty().addListener(previewVisibleListener);
				commentsTab.previewVisible.addListener(previewVisibleListener);
				commentsTab.editMode.addListener(editModeListener);

				// mainWindow.stageFocusedProperty.addListener(stageFocusedListener);
			}
			else {
				Platform.runLater(() -> deactivated());

				Options.markdownRendererProperty().removeListener(
					previewVisibleListener);
				commentsTab.previewVisible.removeListener(previewVisibleListener);
				commentsTab.editMode.removeListener(editModeListener);

				// mainWindow.stageFocusedProperty.removeListener(stageFocusedListener);
			}
		});
	}

	public Node getNode() {
		return splitPane;
	}

	public MarsDocument getDocument() {
		return document;
	}

	public void close() {
		archive.properties().removeDocument(document.getName());
	}

	public Context getContext() {
		return context;
	}

	public
		MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>>
		getArchive()
	{
		return archive;
	}

	public void renderWidgets() {
		if (widgetParser == null) {
			widgetParser = Parser.builder().extensions(MarkdownExtensions
				.getFlexmarkExtensions(Options.getMarkdownRenderer()))
				.postProcessorFactory(new FencedCodeWidgetNodePostProcessorFactory(
					this)).build();
		}

		getNode().fireEvent(new RunMoleculeArchiveTaskEvent(archive, () -> {
			clearWidgetMedia();
			widgetParser.parse(markdownEditorPane.getMarkdown());
			markdownEditorPane.textChanged();
		}, "Rendering widgets..."));
	}

	public void clearWidgetMedia() {
		Set<String> oldKeys = new HashSet<String>();
		for (String key : document.getMediaIDs())
			if (key.startsWith(MARKDOWN_WIDGET_MEDIA_KEY_PREFIX)) oldKeys.add(key);

		for (String oldKey : oldKeys)
			document.removeMedia(oldKey);
	}

	public void addActiveMediaID(String activeMediaID) {
		this.activeMediaIDs.add(activeMediaID);
	}

	public void removeAllActiveMediaIDs() {
		activeMediaIDs.clear();
	}

	public void clearUnusedMedia() {
		// Clean-up by removing all media not currently in use from the document
		// media store..
		for (String mediaID : document.getMediaIDs())
			if (!activeMediaIDs.contains(mediaID) && !mediaID.startsWith(
				MARKDOWN_WIDGET_MEDIA_KEY_PREFIX)) document.removeMedia(mediaID);
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
	private final ObjectProperty<MarkdownEditorPane> editor =
		new SimpleObjectProperty<>();

	public ReadOnlyObjectProperty<MarkdownEditorPane> editorProperty() {
		return editor;
	}

	// 'path' property
	private final ObjectProperty<Path> path = new SimpleObjectProperty<>();

	Path getPath() {
		return path.get();
	}

	void setPath(Path path) {
		this.path.set(path);
	}

	ObjectProperty<Path> pathProperty() {
		return path;
	}

	// 'readOnly' property
	private final ReadOnlyBooleanWrapper readOnly = new ReadOnlyBooleanWrapper();

	boolean isReadOnly() {
		return readOnly.get();
	}

	ReadOnlyBooleanProperty readOnlyProperty() {
		return readOnly.getReadOnlyProperty();
	}

	// 'modified' property
	private final ReadOnlyBooleanWrapper modified = new ReadOnlyBooleanWrapper();

	boolean isModified() {
		return modified.get();
	}

	ReadOnlyBooleanProperty modifiedProperty() {
		return modified.getReadOnlyProperty();
	}

	// 'canUndo' property
	private final BooleanProperty canUndo = new SimpleBooleanProperty();

	BooleanProperty canUndoProperty() {
		return canUndo;
	}

	// 'canRedo' property
	private final BooleanProperty canRedo = new SimpleBooleanProperty();

	BooleanProperty canRedoProperty() {
		return canRedo;
	}

	private void activated() {
		if (tab.getTabPane() == null || !tab.isSelected()) return; // tab is already
																																// closed or no
																																// longer active

		if (tab.getContent() != null) {
			markdownEditorPane.setVisible(true);
			markdownEditorPane.requestFocus();
			updateEditAndPreview();
			return;
		}

		// load file and create UI when the tab becomes visible the first time

		markdownEditorPane = new MarkdownEditorPane(this);
		markdownPreviewPane = new MarkdownPreviewPane(this);

		// markdownEditorPane.getUndoManager().mark();

		// clear undo history after first load
		markdownEditorPane.getUndoManager().forgetHistory();

		// bind preview to editor
		markdownPreviewPane.markdownTextProperty().bind(markdownEditorPane
			.markdownTextProperty());
		markdownPreviewPane.markdownASTProperty().bind(markdownEditorPane
			.markdownASTProperty());
		markdownPreviewPane.editorSelectionProperty().bind(markdownEditorPane
			.selectionProperty());
		markdownPreviewPane.scrollYProperty().bind(markdownEditorPane
			.scrollYProperty());

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

		// We unbind selections until edit mode is activated.
		markdownPreviewPane.editorSelectionProperty().unbind();
		markdownPreviewPane.editorSelectionProperty().set(new IndexRange(-1, -1));

		markdownEditorPane.setMarkdown(document.getContent());
	}

	private boolean updateEditAndPreviewPending;

	public void updateEditAndPreview() {
		if (markdownPreviewPane == null || markdownPreviewPane == null) return;

		// avoid too many (and useless) runLater() invocations
		if (updateEditAndPreviewPending) return;

		updateEditAndPreviewPending = true;

		Platform.runLater(() -> {
			updateEditAndPreviewPending = false;
			ObservableList<Node> splitItems = splitPane.getItems();
			if (commentsTab.editMode.get()) showEditor();
			else if (splitItems.contains(markdownEditorPane.getNode())) {
				splitItems.remove(markdownEditorPane.getNode());
				markdownPreviewPane.editorSelectionProperty().unbind();
				markdownPreviewPane.editorSelectionProperty().set(new IndexRange(-1,
					-1));
				commentsTab.previewVisible.set(true);
			}

			if (commentsTab.previewVisible.get()) showPreview();
			else if (splitItems.contains(markdownPreviewPane.getNode())) {
				splitItems.remove(markdownPreviewPane.getNode());
			}
		});
	}

	private void deactivated() {
		if (markdownEditorPane == null) return;

		markdownEditorPane.setVisible(false);
		document.setContent(markdownEditorPane.getMarkdown());
	}

	public void requestFocus() {
		if (markdownEditorPane != null) markdownEditorPane.requestFocus();
	}

	public void createPrintJob() {
		markdownPreviewPane.createPrintJob();
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
			markdownPreviewPane.editorSelectionProperty().bind(markdownEditorPane
				.selectionProperty());
		}
	}

	public void save() {
		if (archive != null && markdownEditorPane != null) {
			document.setContent(markdownEditorPane.getMarkdown());
			markdownEditorPane.getUndoManager().mark();
		}
	}
}
