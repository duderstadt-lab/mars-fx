/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2024 Karl Duderstadt
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

package de.mpg.biochem.mars.fx.molecule;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.BOLD;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CODE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.COPY;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CUT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.EYE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.FILE_CODE_ALT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.HEADER;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ITALIC;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LIST_OL;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LIST_UL;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PASTE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PENCIL;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLUS;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PRINT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.QUOTE_LEFT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.REFRESH;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.REPEAT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.RETWEET;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.SAVE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.SEARCH;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.STRIKETHROUGH;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.UNDO;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.prefs.Preferences;

import org.scijava.Context;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.Messages;
import de.mpg.biochem.mars.fx.dialogs.DocumentTemplateSelectionDialog;
import de.mpg.biochem.mars.fx.dialogs.RoverErrorDialog;
import de.mpg.biochem.mars.fx.editor.DocumentEditor;
import de.mpg.biochem.mars.fx.editor.MarkdownEditorPane;
import de.mpg.biochem.mars.fx.editor.SmartEdit;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.options.Options;
import de.mpg.biochem.mars.fx.options.Options.RendererType;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.fx.util.PrefsBooleanProperty;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Menu;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

public class CommentsTab extends AbstractMoleculeArchiveTab {

	final BooleanProperty stageFocusedProperty = new SimpleBooleanProperty();

	private BorderPane borderPane;
	private final TabPane tabPane;
	private final ReadOnlyObjectWrapper<DocumentEditor> activeDocumentEditor =
		new ReadOnlyObjectWrapper<>();

	public final PrefsBooleanProperty editMode = new PrefsBooleanProperty(false);

	public final PrefsBooleanProperty previewVisible = new PrefsBooleanProperty(
		true);
	public final PrefsBooleanProperty htmlSourceVisible =
		new PrefsBooleanProperty();
	public final PrefsBooleanProperty markdownAstVisible =
		new PrefsBooleanProperty();
	public final PrefsBooleanProperty externalVisible =
		new PrefsBooleanProperty();

	private ArrayList<Menu> menus;

	private ToolBar nonEditToolBar;
	private ToolBar editToolBar;

	public CommentsTab(final Context context) {
		super(context);

		Region bookIcon = new Region();
		bookIcon.getStyleClass().add("bookIcon");
		setIcon(bookIcon, "comments");

		Options.load(getOptions());

		tabPane = new TabPane();
		tabPane.setFocusTraversable(false);
		tabPane.setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
		tabPane.setStyle("");
		tabPane.getStylesheets().clear();
		tabPane.getStylesheets().add("de/mpg/biochem/mars/fx/MarkdownWriter.css");

		borderPane = new BorderPane();
		borderPane.getStyleClass().add("main");
		borderPane.setPrefSize(800, 800);
		borderPane.setCenter(tabPane);
		initializeToolBars();
		borderPane.setTop(nonEditToolBar);

		// update activeDocumentEditor property
		tabPane.getSelectionModel().selectedItemProperty().addListener((observable,
			oldTab, newTab) -> {
			activeDocumentEditor.set((newTab != null) ? (DocumentEditor) newTab
				.getUserData() : null);
		});

		tabPane.getTabs().addListener((ListChangeListener<Tab>) c -> {
			while (c.next()) {
				if (c.wasRemoved()) {
					for (Tab tab : c.getRemoved()) {
						((DocumentEditor) tab.getUserData()).dispose();
					}
				}
			}
		});

		// Utils.fixSpaceAfterDeadKey(scene);

		// Platform.runLater(() ->
		// stageFocusedProperty.bind(scene.getWindow().focusedProperty()));

		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT,
			this);

		getTab().setContent(borderPane);
	}

	static private Preferences getPrefsRoot() {
		return Preferences.userRoot().node("markdownwriterfx");
	}

	static Preferences getOptions() {
		return getPrefsRoot().node("options");
	}

	private void initializeToolBars() {
		Action editModeAction = new Action("Edit", "Shortcut+E", PENCIL, null, null,
			editMode);
		Node editModeButton = ActionUtils.createToolBarButton(editModeAction);

		Action createPrintDocumentAction = new Action("Print or export PDF", null,
			PRINT, e -> getActiveDocumentEditor().createPrintJob());
		Node createPrintDocumentButton = ActionUtils.createToolBarButton(
			createPrintDocumentAction);

		nonEditToolBar = new ToolBar();
		nonEditToolBar.getItems().add(editModeButton);

		Region nonEditspacer = new Region();
		HBox.setHgrow(nonEditspacer, Priority.ALWAYS);
		nonEditToolBar.getItems().add(nonEditspacer);

		nonEditToolBar.getItems().add(createPrintDocumentButton);
		nonEditToolBar.getStylesheets().add(
			"de/mpg/biochem/mars/fx/MarkdownWriter.css");

		// Edit actions
		Action editUndoAction = new Action(Messages.get(
			"MainWindow.editUndoAction"), "Shortcut+Z", UNDO, e -> getActiveEditor()
				.undo());
		Action editRedoAction = new Action(Messages.get(
			"MainWindow.editRedoAction"), "Shortcut+Y", REPEAT, e -> getActiveEditor()
				.redo());
		Action editCutAction = new Action(Messages.get("MainWindow.editCutAction"),
			"Shortcut+X", CUT, e -> getActiveEditor().cut());
		Action editCopyAction = new Action(Messages.get(
			"MainWindow.editCopyAction"), "Shortcut+C", COPY, e -> getActiveEditor()
				.copy());
		Action editPasteAction = new Action(Messages.get(
			"MainWindow.editPasteAction"), "Shortcut+V", PASTE, e -> getActiveEditor()
				.paste());
		Action editSelectAllAction = new Action(Messages.get(
			"MainWindow.editSelectAllAction"), "Shortcut+A", null,
			e -> getActiveEditor().selectAll());
		Action editFindAction = new Action(Messages.get(
			"MainWindow.editFindAction"), "Shortcut+F", SEARCH, e -> getActiveEditor()
				.find(false));
		Action editReplaceAction = new Action(Messages.get(
			"MainWindow.editReplaceAction"), "Shortcut+H", RETWEET,
			e -> getActiveEditor().find(true));
		Action editFindNextAction = new Action(Messages.get(
			"MainWindow.editFindNextAction"), "F3", null, e -> getActiveEditor()
				.findNextPrevious(true));
		Action editFindPreviousAction = new Action(Messages.get(
			"MainWindow.editFindPreviousAction"), "Shift+F3", null,
			e -> getActiveEditor().findNextPrevious(false));

		Action editFormatAllAction = new Action(Messages.get(
			"MainWindow.editFormatAll"), "Shortcut+Shift+F", null,
			e -> getActiveSmartEdit().format(false, null));
		Action editFormatSelectionAction = new Action(Messages.get(
			"MainWindow.editFormatSelection"), "Shortcut+Shift+Alt+F", null,
			e -> getActiveSmartEdit().format(true, null));

		// View actions
		Action viewPreviewAction = new Action(Messages.get(
			"MainWindow.viewPreviewAction"), null, EYE, null, null, previewVisible);
		Action viewShowLineNoAction = new Action(Messages.get(
			"MainWindow.viewShowLineNoAction"), null, null, null, null, Options
				.showLineNoProperty());
		Action viewShowWhitespaceAction = new Action(Messages.get(
			"MainWindow.viewShowWhitespaceAction"), "Alt+W", null, null, null, Options
				.showWhitespaceProperty());

		// Insert actions
		Action insertBoldAction = new Action(Messages.get(
			"MainWindow.insertBoldAction"), "Shortcut+B", BOLD,
			e -> getActiveSmartEdit().insertBold(Messages.get(
				"MainWindow.insertBoldText")));
		Action insertItalicAction = new Action(Messages.get(
			"MainWindow.insertItalicAction"), "Shortcut+I", ITALIC,
			e -> getActiveSmartEdit().insertItalic(Messages.get(
				"MainWindow.insertItalicText")));
		Action insertStrikethroughAction = new Action(Messages.get(
			"MainWindow.insertStrikethroughAction"), "Shortcut+T", STRIKETHROUGH,
			e -> getActiveSmartEdit().insertStrikethrough(Messages.get(
				"MainWindow.insertStrikethroughText")));
		Action insertCodeAction = new Action(Messages.get(
			"MainWindow.insertCodeAction"), "Shortcut+K", CODE,
			e -> getActiveSmartEdit().insertInlineCode(Messages.get(
				"MainWindow.insertCodeText")));

		Action insertUnorderedListAction = new Action(Messages.get(
			"MainWindow.insertUnorderedListAction"), "Shortcut+U", LIST_UL,
			e -> getActiveSmartEdit().insertUnorderedList());
		Action insertOrderedListAction = new Action(Messages.get(
			"MainWindow.insertOrderedListAction"), "Shortcut+Shift+U", LIST_OL,
			e -> getActiveSmartEdit().surroundSelection("\n\n1. ", ""));
		Action insertBlockquoteAction = new Action(Messages.get(
			"MainWindow.insertBlockquoteAction"), "Ctrl+Q", QUOTE_LEFT, // not
																																	// Shortcut+Q
																																	// because of
																																	// conflict on
																																	// Mac
			e -> getActiveSmartEdit().surroundSelection("\n\n> ", ""));
		Action insertFencedCodeBlockAction = new Action(Messages.get(
			"MainWindow.insertFencedCodeBlockAction"), "Shortcut+Shift+K",
			FILE_CODE_ALT, e -> getActiveSmartEdit().surroundSelection("\n\n```\n",
				"\n```\n\n", Messages.get("MainWindow.insertFencedCodeBlockText")));

		Action insertHeader1Action = new Action(Messages.get(
			"MainWindow.insertHeader1Action"), "Shortcut+1", HEADER,
			e -> getActiveSmartEdit().insertHeading(1, Messages.get(
				"MainWindow.insertHeader1Text")));
		Action insertHeader2Action = new Action(Messages.get(
			"MainWindow.insertHeader2Action"), "Shortcut+2", HEADER,
			e -> getActiveSmartEdit().insertHeading(2, Messages.get(
				"MainWindow.insertHeader2Text")));
		Action insertHeader3Action = new Action(Messages.get(
			"MainWindow.insertHeader3Action"), "Shortcut+3", HEADER,
			e -> getActiveSmartEdit().insertHeading(3, Messages.get(
				"MainWindow.insertHeader3Text")));
		Action insertHeader4Action = new Action(Messages.get(
			"MainWindow.insertHeader4Action"), "Shortcut+4", HEADER,
			e -> getActiveSmartEdit().insertHeading(4, Messages.get(
				"MainWindow.insertHeader4Text")));
		Action insertHeader5Action = new Action(Messages.get(
			"MainWindow.insertHeader5Action"), "Shortcut+5", HEADER,
			e -> getActiveSmartEdit().insertHeading(5, Messages.get(
				"MainWindow.insertHeader5Text")));
		Action insertHeader6Action = new Action(Messages.get(
			"MainWindow.insertHeader6Action"), "Shortcut+6", HEADER,
			e -> getActiveSmartEdit().insertHeading(6, Messages.get(
				"MainWindow.insertHeader6Text")));

		Action insertHorizontalRuleAction = new Action(Messages.get(
			"MainWindow.insertHorizontalRuleAction"), null, null,
			e -> getActiveSmartEdit().surroundSelection("\n\n---\n\n", ""));

		Menu editMenu = ActionUtils.createMenu("Edit", editUndoAction,
			editRedoAction, null, editCutAction, editCopyAction, editPasteAction,
			editSelectAllAction, null, editFindAction, editReplaceAction, null,
			editFindNextAction, editFindPreviousAction, null, editFormatAllAction,
			editFormatSelectionAction);

		Menu viewMenu = ActionUtils.createMenu("View", viewPreviewAction, null,
			viewShowLineNoAction, viewShowWhitespaceAction);

		// ,viewShowImagesEmbeddedAction);

		Menu insertMenu = ActionUtils.createMenu("Insert", insertBoldAction,
			insertItalicAction, insertStrikethroughAction, insertCodeAction, null,
			// insertLinkAction,
			// insertImageAction,
			// null,
			insertUnorderedListAction, insertOrderedListAction,
			insertBlockquoteAction, insertFencedCodeBlockAction, null,
			insertHeader1Action, insertHeader2Action, insertHeader3Action,
			insertHeader4Action, insertHeader5Action, insertHeader6Action, null,
			insertHorizontalRuleAction);

		menus = new ArrayList<Menu>();

		menus.add(editMenu);
		menus.add(viewMenu);
		menus.add(insertMenu);

		// ---- ToolBar ----

		editToolBar = ActionUtils.createToolBar(editUndoAction, editRedoAction,
			null, new Action(insertBoldAction, createActiveEditBooleanProperty(
				SmartEdit::boldProperty)), new Action(insertItalicAction,
					createActiveEditBooleanProperty(SmartEdit::italicProperty)),
			new Action(insertCodeAction, createActiveEditBooleanProperty(
				SmartEdit::codeProperty)), new Action(insertUnorderedListAction,
					createActiveEditBooleanProperty(SmartEdit::unorderedListProperty)),
			new Action(insertOrderedListAction, createActiveEditBooleanProperty(
				SmartEdit::orderedListProperty)), new Action(insertBlockquoteAction,
					createActiveEditBooleanProperty(SmartEdit::blockquoteProperty)),
			new Action(insertFencedCodeBlockAction, createActiveEditBooleanProperty(
				SmartEdit::fencedCodeProperty)), new Action(insertHeader1Action,
					createActiveEditBooleanProperty(SmartEdit::headerProperty)), null);

		Action createNewDocumentAction = new Action("New", "Shortcut+N", PLUS,
			e -> {
				String reportTemplateDirectoryPath = prefService.get(CommentsTab.class,
					"reportTemplateDirectory");
				File reportTemplateDirectory = (reportTemplateDirectoryPath != null)
					? new File(reportTemplateDirectoryPath) : null;
				DocumentTemplateSelectionDialog documentTemplateSelectionDialog =
					new DocumentTemplateSelectionDialog(getNode().getScene().getWindow(),
						"Create report", reportTemplateDirectory);
				documentTemplateSelectionDialog.showAndWait().ifPresent(result -> {
					if (archive.properties().getDocumentNames().contains(result
						.getName()))
			{
						RoverErrorDialog alert = new RoverErrorDialog(getNode().getScene()
							.getWindow(), "Document name " + result.getName() +
								" is already in use. Please choose another.");
						alert.show();
						return;
					}

					DocumentEditor documentEditor = newEditor(result.getName());

					// Should a template be used?
					if (result.getTemplateFileName() != null && result
						.getSelectedDirectory() != null)
			{
						String path = result.getSelectedDirectory().getAbsolutePath() +
							"/" + result.getTemplateFileName();
						String content = "";
						try {
							byte[] encoded = Files.readAllBytes(Paths.get(path));
							content = new String(encoded, StandardCharsets.UTF_8);
						}
						catch (IOException ioexception) {
							RoverErrorDialog alert = new RoverErrorDialog(getNode().getScene()
								.getWindow(),
								"Unable to load the report template selected due to IOException.");
							alert.show();
						}
						documentEditor.getDocument().setContent(content);
					}

					if (result.getSelectedDirectory() != null) {
						prefService.remove(CommentsTab.class, "reportTemplateDirectory");
						prefService.put(CommentsTab.class, "reportTemplateDirectory", result
							.getSelectedDirectory().getAbsolutePath());
					}
				});
			});
		Node createNewDocumentButton = ActionUtils.createToolBarButton(
			createNewDocumentAction);
		editToolBar.getItems().add(createNewDocumentButton);

		Action saveDocumentTemplateAction = new Action("Save template", null, SAVE,
			e -> {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Save report template");
				String reportTemplateDirectoryPath = prefService.get(CommentsTab.class,
					"reportTemplateDirectory");
				File reportTemplateDirectory = (reportTemplateDirectoryPath != null)
					? new File(reportTemplateDirectoryPath) : null;
				if (reportTemplateDirectory != null) fileChooser.setInitialDirectory(
					reportTemplateDirectory);
				fileChooser.setInitialFileName(getActiveEditor().getDocumentEditor()
					.getDocument().getName() + ".md");

				File newFile = fileChooser.showSaveDialog(getNode().getScene()
					.getWindow());
				if (newFile != null) {
					try {
						Files.write(Paths.get(newFile.getAbsolutePath()), getActiveEditor()
							.getMarkdown().getBytes());
					}
					catch (IOException ioexception) {
						RoverErrorDialog alert = new RoverErrorDialog(getNode().getScene()
							.getWindow(),
							"Unable to write template file due to IOException.");
						alert.show();
					}
				}
			});
		Node saveDocumentTemplateButton = ActionUtils.createToolBarButton(
			saveDocumentTemplateAction);
		editToolBar.getItems().add(saveDocumentTemplateButton);

		editToolBar.getItems().add(0, new Separator());

		editToolBar.getStylesheets().add(
			"de/mpg/biochem/mars/fx/MarkdownWriter.css");

		// horizontal spacer
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		editToolBar.getItems().add(spacer);

		// preview renderer type choice box
		ChoiceBox<RendererType> previewRenderer = new ChoiceBox<>();
		previewRenderer.setFocusTraversable(false);
		previewRenderer.getItems().addAll(RendererType.values());
		previewRenderer.getSelectionModel().select(Options.getMarkdownRenderer());
		previewRenderer.getSelectionModel().selectedItemProperty().addListener((ob,
			o, n) -> {
			Options.setMarkdownRenderer(n);
		});
		Options.markdownRendererProperty().addListener((ob, o, n) -> {
			previewRenderer.getSelectionModel().select(n);
		});

		Action editModeAction2 = new Action("Edit", "Shortcut+E", PENCIL, null,
			null, editMode);
		Node editModeButton2 = ActionUtils.createToolBarButton(editModeAction2);

		editToolBar.getItems().add(0, editModeButton2);

		ButtonBase renderWidgetsButton = new Button();
		Text renderWidgetsIcon = FontAwesomeIconFactory.get().createIcon(REFRESH,
			"1.2em");
		renderWidgetsButton.setGraphic(renderWidgetsIcon);
		renderWidgetsButton.setTooltip(new Tooltip("Render Widgets"));
		renderWidgetsButton.setFocusTraversable(false);

		renderWidgetsButton.setOnAction(e -> getActiveDocumentEditor()
			.renderWidgets());

		editToolBar.getItems().add(renderWidgetsButton);

		// preview actions
		Node previewButton = ActionUtils.createToolBarButton(viewPreviewAction);
		editToolBar.getItems().add(previewButton);

		ChangeListener editModeListener = (observable, oldValue,
			newValue) -> updateToolsAndMenus();
		editMode.addListener(editModeListener);
	}

	private MarkdownEditorPane getActiveEditor() {
		return getActiveDocumentEditor().getEditor();
	}

	private SmartEdit getActiveSmartEdit() {
		return getActiveEditor().getSmartEdit();
	}

	/**
	 * Creates a boolean property that is bound to another boolean value of the
	 * active editor's SmartEdit.
	 */
	private BooleanProperty createActiveEditBooleanProperty(
		Function<SmartEdit, ObservableBooleanValue> func)
	{
		BooleanProperty b = new SimpleBooleanProperty() {

			@Override
			public void set(boolean newValue) {
				// invoked when the user invokes an action
				// do not try to change SmartEdit properties because this
				// would throw a "bound value cannot be set" exception
			}
		};

		ChangeListener<? super DocumentEditor> listener = (observable,
			oldDocumentEditor, newDocumentEditor) -> {
			b.unbind();
			if (newDocumentEditor != null) {
				if (newDocumentEditor.getEditor() != null) b.bind(func.apply(
					newDocumentEditor.getEditor().getSmartEdit()));
				else {
					newDocumentEditor.editorProperty().addListener((ob, o, n) -> {
						b.bind(func.apply(n.getSmartEdit()));
					});
				}
			}
			else b.set(false);
		};
		DocumentEditor documentEditor = getActiveDocumentEditor();
		listener.changed(null, null, documentEditor);
		activeDocumentEditorProperty().addListener(listener);
		return b;
	}

	// 'activeDocumentEditor' property
	DocumentEditor getActiveDocumentEditor() {
		return activeDocumentEditor.get();
	}

	ReadOnlyObjectProperty<DocumentEditor> activeDocumentEditorProperty() {
		return activeDocumentEditor.getReadOnlyProperty();
	}

	DocumentEditor newEditor(String name) {
		DocumentEditor documentEditor = new DocumentEditor(context, archive, this,
			name);
		Tab tab = documentEditor.getTab();
		tabPane.getTabs().add(tab);
		tabPane.getSelectionModel().select(tab);
		return documentEditor;
	}

	public ArrayList<Menu> getMenus() {
		return menus;
	}

	private boolean updateToolsAndMenus;

	private void updateToolsAndMenus() {
		// avoid too many (and useless) runLater() invocations
		if (updateToolsAndMenus) return;
		updateToolsAndMenus = true;

		Platform.runLater(() -> {
			updateToolsAndMenus = false;

			if (editMode.get()) {
				borderPane.setTop(editToolBar);
			}
			else {
				borderPane.setTop(nonEditToolBar);
			}
		});
	}

	@Override
	public void onInitializeMoleculeArchiveEvent(
		MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive)
	{
		super.onInitializeMoleculeArchiveEvent(archive);
		this.archive = archive;
		if (archive == null) {
			for (Tab tab : tabPane.getTabs())
				((DocumentEditor) tab.getUserData()).setArchive(null);
			return;
		}
		for (String name : archive.properties().getDocumentNames()) {
			DocumentEditor editor = newEditor(name);
			if (name.equals("Comments")) {
				editor.getTab().setClosable(false);
			}
		}
		tabPane.getSelectionModel().select(0);
	}

	public void setEditMode(boolean editmode) {
		editMode.set(editmode);
	}

	public Node getNode() {
		return tabPane;
	}

	@Override
	public void onMoleculeArchiveLockEvent() {
		for (Tab tab : tabPane.getTabs()) {
			((DocumentEditor) tab.getUserData()).clearUnusedMedia();
			((DocumentEditor) tab.getUserData()).save();
		}
	}

	public void saveComments() {
		for (Tab tab : tabPane.getTabs())
			((DocumentEditor) tab.getUserData()).save();
	}

	@Override
	public void onMoleculeArchiveSavingEvent() {}

	@Override
	protected void createIOMaps() {
		// Currently we are not saving any settings into the rover file. The
		// documents are saved back to the archive.
	}

	@Override
	public String getName() {
		return "commentsTab";
	}
}
