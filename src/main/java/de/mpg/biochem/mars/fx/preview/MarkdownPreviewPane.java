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

package de.mpg.biochem.mars.fx.preview;

import java.nio.file.Path;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.IndexRange;
import javafx.scene.layout.BorderPane;
import de.mpg.biochem.mars.fx.editor.DocumentEditor;
import de.mpg.biochem.mars.fx.options.Options;
import de.mpg.biochem.mars.fx.options.Options.RendererType;
import de.mpg.biochem.mars.fx.util.Range;
import de.mpg.biochem.mars.util.MarsDocument;

import com.vladsch.flexmark.util.ast.Node;

/**
 * Markdown preview pane.
 *
 * @author Karl Tauber
 */
public class MarkdownPreviewPane
{
	public enum Type { None, Web, Source, Ast, External };

	private final BorderPane pane = new BorderPane();
	private final WebViewPreview webViewPreview = new WebViewPreview();
	private final HtmlSourcePreview htmlSourcePreview = new HtmlSourcePreview();
	private final ASTPreview astPreview = new ASTPreview();
	private final ExternalPreview externalPreview = new ExternalPreview();
	private final PreviewContext previewContext;

	private RendererType activeRendererType;
	private Renderer activeRenderer;
	private Preview activePreview;
	
	private DocumentEditor documentEditor;

	interface Renderer {
		void update(String markdownText, Node astRoot, Path path);
		String getHtml(boolean source);
		String getHtml(boolean source, DocumentEditor documentEditor);
		String getAST();
		List<Range> findSequences(int startOffset, int endOffset);
	}

	interface Preview {
		javafx.scene.Node getNode();
		void update(PreviewContext context, Renderer renderer);
		void scrollY(PreviewContext context, double value);
		void editorSelectionChanged(PreviewContext context, IndexRange range);
	}

	interface PreviewContext {
		Renderer getRenderer();
		String getMarkdownText();
		Node getMarkdownAST();
		Path getPath();
		IndexRange getEditorSelection();
	}

	public MarkdownPreviewPane() {
		pane.getStyleClass().add("preview-pane");

		previewContext = new PreviewContext() {
			@Override public Renderer getRenderer() { return activeRenderer; }
			@Override public String getMarkdownText() { return markdownText.get(); }
			@Override public Node getMarkdownAST() { return markdownAST.get(); }
			@Override public Path getPath() { return path.get(); }
			@Override public IndexRange getEditorSelection() { return editorSelection.get(); }
		};

		path.addListener((observable, oldValue, newValue) -> update() );
		markdownText.addListener((observable, oldValue, newValue) -> update() );
		markdownAST.addListener((observable, oldValue, newValue) -> update() );
		scrollY.addListener((observable, oldValue, newValue) -> scrollY());
		editorSelection.addListener((observable, oldValue, newValue) -> editorSelectionChanged());

		Options.additionalCSSProperty().addListener((observable, oldValue, newValue) -> update() );
	}
	
	public MarkdownPreviewPane(DocumentEditor documentEditor) {
		this();
		this.documentEditor = documentEditor;
		if (documentEditor != null)
			webViewPreview.setDocumentEditor(documentEditor);
	}

	public static boolean hasExternalPreview() {
		return ExternalPreview.hasExternalPreview();
	}

	public javafx.scene.Node getNode() {
		return pane;
	}

	public void setRendererType(RendererType rendererType) {
		rendererType = RendererType.FlexMark;

		/*
		
		if (activeRendererType == rendererType)
			return;
		activeRendererType = rendererType;
		activePreview = null;
		
		*/

		activeRenderer = new FlexmarkPreviewRenderer();
	}

	public void setType(Type type) {
		/*
		Preview preview;
		switch (type) {
			case Web:		preview = webViewPreview; break;
			case Source:	preview = htmlSourcePreview; break;
			case Ast:		preview = astPreview; break;
			case External:	preview = externalPreview; break;
			default:		preview = null; break;
		}
		if (activePreview == preview)
			return;
		
		preview = webViewPreview;

		activePreview = preview;
		*/
		
		Preview preview = webViewPreview;
		
		activePreview = preview;
		
		pane.setCenter((preview != null) ? preview.getNode() : null);

		update();
		scrollY();
	}

	private boolean updateRunLaterPending;
	private void update() {
		if (activePreview == null)
			return;

		// avoid too many (and useless) runLater() invocations
		if (updateRunLaterPending)
			return;
		updateRunLaterPending = true;

		Platform.runLater(() -> {
			updateRunLaterPending = false;

			activeRenderer.update(markdownText.get(), markdownAST.get(), path.get());
			activePreview.update(previewContext, activeRenderer);
		});
	}

	private boolean scrollYrunLaterPending;
	private void scrollY() {
		if (activePreview == null)
			return;

		// avoid too many (and useless) runLater() invocations
		if (scrollYrunLaterPending)
			return;
		scrollYrunLaterPending = true;

		Platform.runLater(() -> {
			scrollYrunLaterPending = false;
			activePreview.scrollY(previewContext, scrollY.get());
		});
	}

	private boolean editorSelectionChangedRunLaterPending;
	private void editorSelectionChanged() {
		if (activePreview == null)
			return;

		// avoid too many (and useless) runLater() invocations
		if (editorSelectionChangedRunLaterPending)
			return;
		editorSelectionChangedRunLaterPending = true;

		Platform.runLater(() -> {
			editorSelectionChangedRunLaterPending = false;

			// use another runLater() to make sure that activePreview.editorSelectionChanged()
			// is invoked after activePreview.update(), so that it can work on already updated text
			Platform.runLater(() -> {
				activePreview.editorSelectionChanged(previewContext, editorSelection.get());
			});
		});
	}

	// 'path' property
	private final ObjectProperty<Path> path = new SimpleObjectProperty<>();
	public ObjectProperty<Path> pathProperty() { return path; }

	// 'markdownText' property
	private final SimpleStringProperty markdownText = new SimpleStringProperty();
	public SimpleStringProperty markdownTextProperty() { return markdownText; }

	// 'markdownAST' property
	private final ObjectProperty<Node> markdownAST = new SimpleObjectProperty<>();
	public ObjectProperty<Node> markdownASTProperty() { return markdownAST; }

	// 'scrollY' property
	private final DoubleProperty scrollY = new SimpleDoubleProperty();
	public DoubleProperty scrollYProperty() { return scrollY; }

	// 'editorSelection' property
	private final ObjectProperty<IndexRange> editorSelection = new SimpleObjectProperty<>();
	public ObjectProperty<IndexRange> editorSelectionProperty() { return editorSelection; }
}
