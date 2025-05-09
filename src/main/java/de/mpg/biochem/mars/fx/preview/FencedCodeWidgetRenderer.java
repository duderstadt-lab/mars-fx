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

package de.mpg.biochem.mars.fx.preview;

import java.util.HashSet;
import java.util.Set;

import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import de.mpg.biochem.mars.fx.editor.DocumentEditor;

public class FencedCodeWidgetRenderer implements NodeRenderer {

	private DocumentEditor documentEditor;

	public FencedCodeWidgetRenderer(DataHolder options,
		DocumentEditor documentEditor)
	{
		this.documentEditor = documentEditor;
	}

	@Override
	public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
		Set<NodeRenderingHandler<?>> set = new HashSet<>();
		set.add(new NodeRenderingHandler<>(FencedCodeBlock.class, this::render));
		return set;
	}

	private void render(FencedCodeBlock node, NodeRendererContext context,
		HtmlWriter html)
	{

		// Should we check if node.getInfo() matches something first?

		String key = DocumentEditor.MARKDOWN_WIDGET_MEDIA_KEY_PREFIX + node
			.getInfo() + ":" + node.getContentChars().normalizeEOL();

		if (documentEditor.getDocument().getMediaIDs().contains(key)) {
			int startOffset = node.getStartOffset();
			int endOffset = node.getEndOffset();
			String content = documentEditor.getDocument().getMedia(key);

			if (content.startsWith(
				FencedCodeBlockMarkdownWidget.MARKDOWN_WIDGET_ERROR_KEY_PREFIX))
			{
				BasedSequence errorMessage = BasedSequence.of(content.substring(
					FencedCodeBlockMarkdownWidget.MARKDOWN_WIDGET_ERROR_KEY_PREFIX
						.length()));
				html.line();
				html.attr("data-pos", startOffset + ":" + endOffset).withAttr().tag(
					"pre").openPre();
				html.attr("data-pos", startOffset + ":" + endOffset).srcPosWithEOL(
					errorMessage).withAttr().tag("code");
				html.text(errorMessage.normalizeEOL());
				html.tag("/code");
				html.tag("/pre").closePre();
				return;
			}
			else if (node.getInfo().equals("python-image-widget") || node.getInfo()
				.equals("groovy-image-widget"))
			{
				html.attr("src", documentEditor.getDocument().getMedia(key)).withAttr()
					.tag("img", true);
				return;
			}
			else if (node.getInfo().equals("python-images-widget") || node.getInfo()
					.equals("groovy-images-widget"))
			{
				String[] imageData = convertCommaStringToArray(documentEditor.getDocument().getMedia(key));
                for (String imageDatum : imageData) html.attr("src", imageDatum).withAttr().tag("img", true);
				return;
			}
			else if (node.getInfo().equals("python-html-widget") || node.getInfo()
				.equals("groovy-html-widget"))
			{
				html.line();
				html.raw(documentEditor.getDocument().getMedia(key));
				return;
			}
		}
		context.delegateRender();
	}

	public static String[] convertCommaStringToArray(String commaString) {
		if (commaString == null || commaString.isEmpty()) {
			return new String[0];
		}

		// Split the string at commas followed by optional whitespace
		String[] array = commaString.split("\\s+");

		return array;
	}
}
