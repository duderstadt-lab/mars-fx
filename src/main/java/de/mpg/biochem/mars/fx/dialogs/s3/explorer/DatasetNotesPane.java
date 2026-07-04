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
package de.mpg.biochem.mars.fx.dialogs.s3.explorer;

import java.util.function.Consumer;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.mpg.biochem.mars.fx.editor.MarkdownEditorPane;
import de.mpg.biochem.mars.fx.options.Options;
import de.mpg.biochem.mars.fx.webview.MarkdownPreviewPane;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * Standalone markdown notes pane for the Dataset Explorer.
 *
 * <p>Reuses mars-fx's {@link MarkdownEditorPane} and {@link MarkdownPreviewPane}
 * but <b>without any archive coupling</b>: both are built with their no-arg
 * constructors, so nothing here touches a {@code MoleculeArchive}, the document
 * media store, or the SciJava context. The only editor feature this gives up is
 * drag-and-drop image embedding (which needs the archive media store); plain
 * text drops, links, and all formatting still work.
 *
 * <p>A single pencil {@link ToggleButton} switches between:
 * <ul>
 *   <li><b>view mode</b> (pencil off): the {@link MarkdownPreviewPane} shows the
 *       rendered markdown; the formatting buttons are hidden.</li>
 *   <li><b>edit mode</b> (pencil on): the {@link MarkdownEditorPane} is shown and
 *       the formatting toolbar (bold, italic, strikethrough, inline code,
 *       H1/H2/H3, list, link) appears.</li>
 * </ul>
 *
 * <p>Content is plain markdown via {@link #getMarkdown()} / {@link #setMarkdown}.
 * The explorer stores that string in {@code DatasetEntry} and persists it in the
 * userdata JSON. A change callback lets the window autosave on edits.
 */
public class DatasetNotesPane extends BorderPane {

    private final MarkdownEditorPane editorPane;
    private final MarkdownPreviewPane previewPane;

    private final ToggleButton editToggle;
    private final HBox formatBar;      // formatting buttons; visible only in edit mode
    private final HBox toolBar;        // whole top bar (pencil + format buttons)
    private final StackPane centerStack;

    private Consumer<String> onMarkdownChanged;
    private boolean suppressChangeEvents = false;

    public DatasetNotesPane() {
        getStyleClass().add("dataset-notes-pane");

        // --- Editor (archive-free: no-arg constructor) ---
        editorPane = new MarkdownEditorPane();

        // --- Preview (archive-free: no-arg constructor) ---
        previewPane = new MarkdownPreviewPane();

        // Set a non-null empty selection BEFORE binding text/AST. Binding those
        // triggers MarkdownPreviewPane.update(), which calls WebViewPreview
        // .highlightNodesAt() and dereferences the selection — a null there NPEs.
        // DocumentEditor sets (-1,-1) for the same reason. We have no selection
        // sync (single pane), so we set it once and never bind it.
        previewPane.editorSelectionProperty().set(new IndexRange(-1, -1));

        // Bind preview to the editor's parsed text/AST, as DocumentEditor does.
        // We deliberately DO NOT bind scrollYProperty: driving scroll against the
        // WebView before preview.js loads throws "Can't find variable: preview",
        // and we have no scroll-sync to preserve.
        previewPane.markdownTextProperty().bind(editorPane.markdownTextProperty());
        previewPane.markdownASTProperty().bind(editorPane.markdownASTProperty());

        previewPane.setRendererType(Options.getMarkdownRenderer());

        // Start with explicit empty content so the very first preview render has a
        // real (empty) document rather than null. The editor already initializes
        // its markdownText to "", but we set it defensively.
        editorPane.setMarkdown("");

        // Defer setType(Web) — which initializes the WebView and internally calls
        // scrollY() — until this pane is actually attached to a live scene. Doing
        // it in the constructor drives the WebView before preview.js has loaded,
        // producing the "Can't find variable: preview" ReferenceError. Once shown,
        // the WebView loads cleanly.
        final boolean[] previewStarted = {false};
        sceneProperty().addListener((o, oldScene, newScene) -> {
            if (newScene != null && !previewStarted[0]) {
                previewStarted[0] = true;
                previewPane.setType(MarkdownPreviewPane.Type.Web);
            }
        });

        // Fire the change callback whenever the markdown text changes.
        editorPane.markdownTextProperty().addListener((o, a, b) -> {
            if (!suppressChangeEvents && onMarkdownChanged != null)
                onMarkdownChanged.accept(getMarkdown());
        });

        // --- Toolbar ---
        editToggle = new ToggleButton();
        editToggle.setGraphic(icon(FontAwesomeIcon.PENCIL));
        editToggle.setTooltip(new Tooltip("Edit notes"));
        editToggle.selectedProperty().addListener((o, was, on) -> setEditMode(on));

        formatBar = buildFormatBar();
        formatBar.setVisible(false);
        formatBar.setManaged(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolBar = new HBox(6, editToggle, formatBar);
        toolBar.setAlignment(Pos.CENTER_LEFT);
        toolBar.getStyleClass().add("dataset-notes-toolbar");
        toolBar.setStyle("-fx-padding: 4 8 4 8;");
        setTop(toolBar);

        // --- Center: swap editor/preview node depending on mode ---
        centerStack = new StackPane(previewPane.getNode());
        setCenter(centerStack);

        // start in view mode
        setEditMode(false);
    }

    private HBox buildFormatBar() {
        // Undo / redo (operate on the editor's undo manager).
        Button undo = formatButton(FontAwesomeIcon.UNDO, "Undo",
                () -> editorPane.undo());
        Button redo = formatButton(FontAwesomeIcon.REPEAT, "Redo",
                () -> editorPane.redo());

        Button bold = formatButton(FontAwesomeIcon.BOLD, "Bold",
                () -> editorPane.getSmartEdit().insertBold("bold"));
        Button italic = formatButton(FontAwesomeIcon.ITALIC, "Italic",
                () -> editorPane.getSmartEdit().insertItalic("italic"));
        Button strike = formatButton(FontAwesomeIcon.STRIKETHROUGH, "Strikethrough",
                () -> editorPane.getSmartEdit().insertStrikethrough("strikethrough"));
        Button code = formatButton(FontAwesomeIcon.CODE, "Inline code",
                () -> editorPane.getSmartEdit().insertInlineCode("code"));

        Button h1 = textButton("H1", "Heading 1",
                () -> editorPane.getSmartEdit().insertHeading(1, "heading"));
        Button h2 = textButton("H2", "Heading 2",
                () -> editorPane.getSmartEdit().insertHeading(2, "heading"));
        Button h3 = textButton("H3", "Heading 3",
                () -> editorPane.getSmartEdit().insertHeading(3, "heading"));

        // Lists / quote: insert markers explicitly via surroundSelection rather than
        // SmartEdit.insertUnorderedList(), which reads Options.getBulletListMarker()
        // — that returns null outside an archive/Options context and inserts the
        // literal text "null". Explicit markers avoid that entirely.
        Button bullet = formatButton(FontAwesomeIcon.LIST_UL, "Bulleted list",
                () -> editorPane.getSmartEdit().surroundSelection("\n\n- ", ""));
        Button numbered = formatButton(FontAwesomeIcon.LIST_OL, "Numbered list",
                () -> editorPane.getSmartEdit().surroundSelection("\n\n1. ", ""));
        Button quote = formatButton(FontAwesomeIcon.QUOTE_LEFT, "Block quote",
                () -> editorPane.getSmartEdit().surroundSelection("\n\n> ", ""));

        Button link = formatButton(FontAwesomeIcon.LINK, "Insert link",
                () -> editorPane.getSmartEdit().insertLink());

        HBox bar = new HBox(2, undo, redo, sep(), bold, italic, strike, code,
                sep(), h1, h2, h3, sep(), bullet, numbered, quote, sep(), link);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    // ---- edit/view switching -------------------------------------------

    /** Enter (true) or leave (false) edit mode. Idempotent. */
    public void setEditMode(boolean edit) {
        if (editToggle.isSelected() != edit) editToggle.setSelected(edit);

        formatBar.setVisible(edit);
        formatBar.setManaged(edit);

        if (edit) {
            centerStack.getChildren().setAll(editorPane.getNode());
            editorPane.requestFocus();
        } else {
            // leaving edit mode — the preview is bound to the editor's text, so it
            // already reflects the latest content; just swap the visible node.
            centerStack.getChildren().setAll(previewPane.getNode());
        }
    }

    public boolean isEditMode() {
        return editToggle.isSelected();
    }

    /**
     * Force the preview WebView to re-render. WebViewPreview picks its stylesheet
     * (markdownpad-github.css vs -dark.css) from MarsThemeManager.isDarkTheme()
     * only when it rebuilds its HTML in update(). It has no listener for theme
     * changes, so after the app theme switches we must nudge it here, otherwise
     * the preview keeps showing the previous theme's CSS until the text changes.
     */
    public void refreshPreview() {
        if (previewPane != null)
            previewPane.setType(MarkdownPreviewPane.Type.Web);
    }

    // ---- content -------------------------------------------------------

    public String getMarkdown() {
        return editorPane.getMarkdown();
    }

    /**
     * Replace the notes content. Does not fire the change callback (so loading a
     * dataset's saved notes isn't mistaken for a user edit), and resets undo
     * history so the newly-loaded content isn't undoable back to the previous
     * dataset's text.
     */
    public void setMarkdown(String markdown) {
        suppressChangeEvents = true;
        try {
            editorPane.setMarkdown(markdown == null ? "" : markdown);
            editorPane.getUndoManager().forgetHistory();
            editorPane.getUndoManager().mark();
        } finally {
            suppressChangeEvents = false;
        }
    }

    /** Called by the window when notes should be saved (e.g. on dataset switch). */
    public void setOnMarkdownChanged(Consumer<String> callback) {
        this.onMarkdownChanged = callback;
    }

    public MarkdownEditorPane getEditorPane() {
        return editorPane;
    }

    public MarkdownPreviewPane getPreviewPane() {
        return previewPane;
    }

    // ---- button helpers ------------------------------------------------

    private Button formatButton(FontAwesomeIcon glyph, String tip, Runnable action) {
        Button b = new Button();
        b.setGraphic(icon(glyph));
        b.setTooltip(new Tooltip(tip));
        b.getStyleClass().add("dataset-notes-format-button");
        b.setOnAction(e -> {
            action.run();
            editorPane.requestFocus();
        });
        return b;
    }

    private Button textButton(String label, String tip, Runnable action) {
        Button b = new Button(label);
        b.setTooltip(new Tooltip(tip));
        b.getStyleClass().add("dataset-notes-format-button");
        b.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        b.setOnAction(e -> {
            action.run();
            editorPane.requestFocus();
        });
        return b;
    }

    private Region sep() {
        Region r = new Region();
        r.setMinWidth(8);
        return r;
    }

    // Single place that builds FontAwesome glyphs — change this one method when
    // migrating to Ikonli.
    private static FontAwesomeIconView icon(FontAwesomeIcon glyph) {
        FontAwesomeIconView view = new FontAwesomeIconView(glyph);
        view.setGlyphSize(14);
        return view;
    }
}