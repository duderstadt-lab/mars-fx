/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2025 Karl Duderstadt
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
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import de.mpg.biochem.mars.fx.preview.MarkdownPreviewPane.PreviewContext;
import de.mpg.biochem.mars.fx.preview.MarkdownPreviewPane.Renderer;
import de.mpg.biochem.mars.fx.syntaxhighlighter.SyntaxHighlighter;
import de.mpg.biochem.mars.fx.util.Utils;
import javafx.scene.control.IndexRange;
import javafx.scene.control.ScrollBar;

/**
 * HTML source preview.
 *
 * @author Karl Tauber
 */
class HtmlSourcePreview implements MarkdownPreviewPane.Preview {

	private PreviewStyledTextArea textArea;
	private VirtualizedScrollPane<StyleClassedTextArea> scrollPane;
	private ScrollBar vScrollBar;

	HtmlSourcePreview() {}

	private void createNodes() {
		textArea = new PreviewStyledTextArea();
		textArea.setWrapText(true);
		textArea.getStylesheets().add("org/markdownwriterfx/prism.css");

		scrollPane = new VirtualizedScrollPane<>(textArea);
	}

	@Override
	public javafx.scene.Node getNode() {
		if (scrollPane == null) createNodes();
		return scrollPane;
	}

	@Override
	public void update(PreviewContext context, Renderer renderer) {
		String html = renderer.getHtml(true);
		textArea.replaceText(html, computeHighlighting(html));
	}

	@Override
	public void scrollY(PreviewContext context, double value) {
		if (vScrollBar == null) vScrollBar = Utils.findVScrollBar(scrollPane);
		if (vScrollBar == null) return;

		double maxValue = vScrollBar.maxProperty().get();
		vScrollBar.setValue(maxValue * value);
	}

	@Override
	public void editorSelectionChanged(PreviewContext context,
		IndexRange range)
	{}

	// ---- selection highlighting ---------------------------------------------

	private static final HashMap<String, Collection<String>> spanStyleCache =
		new HashMap<>();

	private static StyleSpans<Collection<String>> computeHighlighting(
		String text)
	{
		StyleSpansBuilder<Collection<String>> spansBuilder =
			new StyleSpansBuilder<>();
		SyntaxHighlighter.highlight(text, "html", (length, style) -> {
			spansBuilder.add(toSpanStyle(style), length);
		});
		return spansBuilder.create();
	}

	private static Collection<String> toSpanStyle(String style) {
		if (style == null) return Collections.emptyList();

		Collection<String> spanStyle = spanStyleCache.get(style);
		if (spanStyle == null) {
			spanStyle = Arrays.asList(style, "token");
			spanStyleCache.put(style, spanStyle);
		}
		return spanStyle;
	}

	@Override
	public void createPrintJob() {
		// TODO Auto-generated method stub

	}
}
