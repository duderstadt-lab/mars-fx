package de.mpg.biochem.mars.gui.view;

import org.markdownwriterfx.util.Action;
import org.markdownwriterfx.util.ActionUtils;
import org.markdownwriterfx.util.Utils;

import de.jensd.fx.glyphs.GlyphIcons;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

import java.text.MessageFormat;
import java.util.function.Function;
import java.util.prefs.Preferences;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.markdownwriterfx.*;
import org.markdownwriterfx.editor.SmartEdit;
import org.markdownwriterfx.options.MarkdownExtensionsPane;
import org.markdownwriterfx.editor.MarkdownEditorPane;
import org.markdownwriterfx.options.Options;
import org.markdownwriterfx.options.Options.RendererType;
import org.markdownwriterfx.preview.MarkdownPreviewPane;
import org.markdownwriterfx.util.Utils;

public class MACommentsController extends BorderPane implements MAPaneController {
	private Scene scene;
	private Node extensionsButton;
    final BooleanProperty stageFocusedProperty = new SimpleBooleanProperty();
    final BooleanProperty editModeActive = new SimpleBooleanProperty(false);

    private CommentEditor commentEditor;
    
	MoleculeArchive archive;
	
	public MACommentsController(MoleculeArchive archive) {
		this.archive = archive;
		initialize();
	}
	
    public void initialize() {
    	Options.load(getOptions());
    	
    	//Build markdown gui
		getStyleClass().add("main");
		setPrefSize(800, 800);
		commentEditor = new CommentEditor(archive);
    	setCenter(commentEditor);
		
		setTop(createToolBar());
    	
		scene = new Scene(this);
		scene.getStylesheets().add("org/markdownwriterfx/MarkdownWriter.css");
		
		Utils.fixSpaceAfterDeadKey(scene);
		
		//Platform.runLater(() -> stageFocusedProperty.bind(scene.getWindow().focusedProperty()));
    }
    
	static private Preferences getPrefsRoot() {
		return Preferences.userRoot().node("markdownwriterfx");
	}

	static Preferences getOptions() {
		return getPrefsRoot().node("options");
	}
    
    private Node createToolBar() {
    	Action editModeAction = new Action("Edit", "Shortcut+E", PENCIL,
				null, null, editModeActive);
    	
		// Edit actions
		Action editUndoAction = new Action(Messages.get("MainWindow.editUndoAction"), "Shortcut+Z", UNDO,
				e -> commentEditor.getEditor().undo(), editModeActive);
		Action editRedoAction = new Action(Messages.get("MainWindow.editRedoAction"), "Shortcut+Y", REPEAT,
				e -> commentEditor.getEditor().redo(), editModeActive);
		Action editCutAction = new Action(Messages.get("MainWindow.editCutAction"), "Shortcut+X", CUT,
				e -> commentEditor.getEditor().cut(), editModeActive);
		Action editCopyAction = new Action(Messages.get("MainWindow.editCopyAction"), "Shortcut+C", COPY,
				e -> commentEditor.getEditor().copy(), editModeActive);
		Action editPasteAction = new Action(Messages.get("MainWindow.editPasteAction"), "Shortcut+V", PASTE,
				e -> commentEditor.getEditor().paste(), editModeActive);
		Action editSelectAllAction = new Action(Messages.get("MainWindow.editSelectAllAction"), "Shortcut+A", null,
				e -> commentEditor.getEditor().selectAll(), editModeActive);
		Action editFindAction = new Action(Messages.get("MainWindow.editFindAction"), "Shortcut+F", SEARCH,
				e -> commentEditor.getEditor().find(false), editModeActive);
		Action editReplaceAction = new Action(Messages.get("MainWindow.editReplaceAction"), "Shortcut+H", RETWEET,
				e -> commentEditor.getEditor().find(true), editModeActive);
		Action editFindNextAction = new Action(Messages.get("MainWindow.editFindNextAction"), "F3", null,
				e -> commentEditor.getEditor().findNextPrevious(true), editModeActive);
		Action editFindPreviousAction = new Action(Messages.get("MainWindow.editFindPreviousAction"), "Shift+F3", null,
				e -> commentEditor.getEditor().findNextPrevious(false), editModeActive);

		Action editFormatAllAction = new Action(Messages.get("MainWindow.editFormatAll"), "Shortcut+Shift+F", null,
				e -> commentEditor.getEditor().getSmartEdit().format(false, null), editModeActive);
		Action editFormatSelectionAction = new Action(Messages.get("MainWindow.editFormatSelection"), "Shortcut+Shift+Alt+F", null,
				e -> commentEditor.getEditor().getSmartEdit().format(true, null), editModeActive);
		
		// View actions
		Action viewPreviewAction = new Action(Messages.get("MainWindow.viewPreviewAction"), null, EYE,
				null, null, commentEditor.previewVisible);
		Action viewShowLineNoAction = new Action(Messages.get("MainWindow.viewShowLineNoAction"), null, null,
				null, null, Options.showLineNoProperty());
		Action viewShowWhitespaceAction = new Action(Messages.get("MainWindow.viewShowWhitespaceAction"), "Alt+W", null,
				null, null, Options.showWhitespaceProperty());
		Action viewShowImagesEmbeddedAction = new Action(Messages.get("MainWindow.viewShowImagesEmbeddedAction"), "Alt+I", null,
				null, null, Options.showImagesEmbeddedProperty());

		// Insert actions
		Action insertBoldAction = new Action(Messages.get("MainWindow.insertBoldAction"), "Shortcut+B", BOLD,
				e -> commentEditor.getEditor().getSmartEdit().insertBold(Messages.get("MainWindow.insertBoldText")), editModeActive);
		Action insertItalicAction = new Action(Messages.get("MainWindow.insertItalicAction"), "Shortcut+I", ITALIC,
				e -> commentEditor.getEditor().getSmartEdit().insertItalic(Messages.get("MainWindow.insertItalicText")), editModeActive);
		Action insertStrikethroughAction = new Action(Messages.get("MainWindow.insertStrikethroughAction"), "Shortcut+T", STRIKETHROUGH,
				e -> commentEditor.getEditor().getSmartEdit().insertStrikethrough(Messages.get("MainWindow.insertStrikethroughText")), editModeActive);
		Action insertCodeAction = new Action(Messages.get("MainWindow.insertCodeAction"), "Shortcut+K", CODE,
				e -> commentEditor.getEditor().getSmartEdit().insertInlineCode(Messages.get("MainWindow.insertCodeText")), editModeActive);

		Action insertLinkAction = new Action(Messages.get("MainWindow.insertLinkAction"), "Shortcut+L", LINK,
				e -> commentEditor.getEditor().getSmartEdit().insertLink(), editModeActive);
		Action insertImageAction = new Action(Messages.get("MainWindow.insertImageAction"), "Shortcut+G", PICTURE_ALT,
				e -> commentEditor.getEditor().getSmartEdit().insertImage(), editModeActive);

		Action insertUnorderedListAction = new Action(Messages.get("MainWindow.insertUnorderedListAction"), "Shortcut+U", LIST_UL,
				e -> commentEditor.getEditor().getSmartEdit().insertUnorderedList(), editModeActive);
		Action insertOrderedListAction = new Action(Messages.get("MainWindow.insertOrderedListAction"), "Shortcut+Shift+U", LIST_OL,
				e -> commentEditor.getEditor().getSmartEdit().surroundSelection("\n\n1. ", ""), editModeActive);
		Action insertBlockquoteAction = new Action(Messages.get("MainWindow.insertBlockquoteAction"), "Ctrl+Q", QUOTE_LEFT, // not Shortcut+Q because of conflict on Mac
				e -> commentEditor.getEditor().getSmartEdit().surroundSelection("\n\n> ", ""), editModeActive);
		Action insertFencedCodeBlockAction = new Action(Messages.get("MainWindow.insertFencedCodeBlockAction"), "Shortcut+Shift+K", FILE_CODE_ALT,
				e -> commentEditor.getEditor().getSmartEdit().surroundSelection("\n\n```\n", "\n```\n\n", Messages.get("MainWindow.insertFencedCodeBlockText")), editModeActive);

		Action insertHeader1Action = new Action(Messages.get("MainWindow.insertHeader1Action"), "Shortcut+1", HEADER,
				e -> commentEditor.getEditor().getSmartEdit().insertHeading(1, Messages.get("MainWindow.insertHeader1Text")), editModeActive);
		Action insertHeader2Action = new Action(Messages.get("MainWindow.insertHeader2Action"), "Shortcut+2", HEADER,
				e -> commentEditor.getEditor().getSmartEdit().insertHeading(2, Messages.get("MainWindow.insertHeader2Text")), editModeActive);
		Action insertHeader3Action = new Action(Messages.get("MainWindow.insertHeader3Action"), "Shortcut+3", HEADER,
				e -> commentEditor.getEditor().getSmartEdit().insertHeading(3, Messages.get("MainWindow.insertHeader3Text")), editModeActive);
		Action insertHeader4Action = new Action(Messages.get("MainWindow.insertHeader4Action"), "Shortcut+4", HEADER,
				e -> commentEditor.getEditor().getSmartEdit().insertHeading(4, Messages.get("MainWindow.insertHeader4Text")), editModeActive);
		Action insertHeader5Action = new Action(Messages.get("MainWindow.insertHeader5Action"), "Shortcut+5", HEADER,
				e -> commentEditor.getEditor().getSmartEdit().insertHeading(5, Messages.get("MainWindow.insertHeader5Text")), editModeActive);
		Action insertHeader6Action = new Action(Messages.get("MainWindow.insertHeader6Action"), "Shortcut+6", HEADER,
				e -> commentEditor.getEditor().getSmartEdit().insertHeading(6, Messages.get("MainWindow.insertHeader6Text")), editModeActive);

		//Action insertHorizontalRuleAction = new Action(Messages.get("MainWindow.insertHorizontalRuleAction"), null, null,
		//		e -> getActiveSmartEdit().surroundSelection("\n\n---\n\n", ""),
		//		activeFileEditorIsNull);

		// Tools actions
		//Action toolsOptionsAction = new Action(Messages.get("MainWindow.toolsOptionsAction"), "Shortcut+,", null, e -> toolsOptions());

		//---- ToolBar ----

		ToolBar toolBar = ActionUtils.createToolBar(
				editUndoAction,
				editRedoAction,
				null,
				new Action(insertBoldAction, createActiveEditBooleanProperty(SmartEdit::boldProperty)),
				new Action(insertItalicAction, createActiveEditBooleanProperty(SmartEdit::italicProperty)),
				new Action(insertCodeAction, createActiveEditBooleanProperty(SmartEdit::codeProperty)),
				null,
				new Action(insertLinkAction, createActiveEditBooleanProperty(SmartEdit::linkProperty)),
				new Action(insertImageAction, createActiveEditBooleanProperty(SmartEdit::imageProperty)),
				null,
				new Action(insertUnorderedListAction, createActiveEditBooleanProperty(SmartEdit::unorderedListProperty)),
				new Action(insertOrderedListAction, createActiveEditBooleanProperty(SmartEdit::orderedListProperty)),
				new Action(insertBlockquoteAction, createActiveEditBooleanProperty(SmartEdit::blockquoteProperty)),
				new Action(insertFencedCodeBlockAction, createActiveEditBooleanProperty(SmartEdit::fencedCodeProperty)),
				null,
				new Action(insertHeader1Action, createActiveEditBooleanProperty(SmartEdit::headerProperty)));
		
		toolBar.getItems().add(0, new Separator());
		Node editModeButton = ActionUtils.createToolBarButton(editModeAction);
		toolBar.getItems().add(0, editModeButton);

		// horizontal spacer
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		toolBar.getItems().add(spacer);

		// preview renderer type choice box
		ChoiceBox<RendererType> previewRenderer = new ChoiceBox<>();
		previewRenderer.setFocusTraversable(false);
		previewRenderer.getItems().addAll(RendererType.values());
		previewRenderer.getSelectionModel().select(Options.getMarkdownRenderer());
		previewRenderer.getSelectionModel().selectedItemProperty().addListener((ob, o, n) -> {
			Options.setMarkdownRenderer(n);
		});
		Options.markdownRendererProperty().addListener((ob, o, n) -> {
			previewRenderer.getSelectionModel().select(n);
		});
		
		//For the moment we just leave the CommonMark Editor
		//toolBar.getItems().add(previewRenderer);

		// markdown extensions popover
		String title = Messages.get("MainWindow.MarkdownExtensions");
		extensionsButton = ActionUtils.createToolBarButton(
				new Action(title, null, COG, e -> {
					PopOver popOver = new PopOver();
					popOver.setTitle(title);
					popOver.setHeaderAlwaysVisible(true);
					popOver.setArrowLocation(ArrowLocation.TOP_CENTER);
					popOver.setContentNode(new MarkdownExtensionsPane(true));
					popOver.show(extensionsButton);
				}));
		toolBar.getItems().add(extensionsButton);
		toolBar.getItems().add(new Separator());

		// preview actions
		Node previewButton = ActionUtils.createToolBarButton(viewPreviewAction);
		toolBar.getItems().add(previewButton);

		return toolBar;
	}
    
	/**
	 * Creates a boolean property that is bound to another boolean value
	 * of the active editor's SmartEdit.
	 */
	private BooleanProperty createActiveEditBooleanProperty(Function<SmartEdit, ObservableBooleanValue> func) {
		BooleanProperty b = new SimpleBooleanProperty() {
			@Override
			public void set(boolean newValue) {
				// invoked when the user invokes an action
				// do not try to change SmartEdit properties because this
				// would throw a "bound value cannot be set" exception
			}
		};
		
		b.unbind();
		
		b.bind(func.apply(commentEditor.getEditor().getSmartEdit()));
		
		return b;
	}
	
	Alert createAlert(AlertType alertType, String title,
			String contentTextFormat, Object... contentTextArgs)
		{
			Alert alert = new Alert(alertType);
			alert.setTitle(title);
			alert.setHeaderText(null);
			alert.setContentText(MessageFormat.format(contentTextFormat, contentTextArgs));
			alert.initOwner(getScene().getWindow());
			return alert;
		}

	@Override
	public void setArchive(MoleculeArchive archive) {
		this.archive = archive;
	}
}
