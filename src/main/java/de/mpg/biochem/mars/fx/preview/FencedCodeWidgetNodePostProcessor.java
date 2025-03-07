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

import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.parser.block.NodePostProcessor;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeTracker;
import com.vladsch.flexmark.util.data.DataHolder;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import de.mpg.biochem.mars.fx.editor.DocumentEditor;

public class FencedCodeWidgetNodePostProcessor extends NodePostProcessor {

	private DocumentEditor documentEditor;

	public FencedCodeWidgetNodePostProcessor(DataHolder options,
		DocumentEditor documentEditor)
	{
		this.documentEditor = documentEditor;
	}

	@Override
	public void process(@NotNull NodeTracker state, @NotNull Node node) {
		if (node instanceof FencedCodeBlock) {
			FencedCodeBlock fencedCodeBlockNode = (FencedCodeBlock) node;

			if (fencedCodeBlockNode.getInfo().equals("python-image-widget"))
				processFencedCodeBlockWidget(fencedCodeBlockNode, "Python (PyImageJ)",
					"imgsrc");
			else if (fencedCodeBlockNode.getInfo().equals("python-html-widget"))
				processFencedCodeBlockWidget(fencedCodeBlockNode, "Python (PyImageJ)",
					"html");
			else if (fencedCodeBlockNode.getInfo().equals("groovy-image-widget"))
				processFencedCodeBlockWidget(fencedCodeBlockNode, "Groovy", "imgsrc");
			else if (fencedCodeBlockNode.getInfo().equals("groovy-html-widget"))
				processFencedCodeBlockWidget(fencedCodeBlockNode, "Groovy", "html");
		}
	}

	private void processFencedCodeBlockWidget(FencedCodeBlock fencedCodeBlockNode,
		String language, String outputVariableName)
	{
		String script = fencedCodeBlockNode.getContentChars().normalizeEOL();

		Map<String, Object> inputs = new HashMap<String, Object>();

		inputs.put("scijavaContext", documentEditor.getContext());
		inputs.put("archive", documentEditor.getArchive());

		FencedCodeBlockMarkdownWidget fencedCodeBlockMarkdownWidget =
			new FencedCodeBlockMarkdownWidget(documentEditor.getContext(),
				documentEditor.getArchive(), language);

		Map<String, Object> outputs = fencedCodeBlockMarkdownWidget.runScript(
			inputs, script);

		String key = DocumentEditor.MARKDOWN_WIDGET_MEDIA_KEY_PREFIX +
			fencedCodeBlockNode.getInfo() + ":" + script;

		if (outputs.containsKey(
			FencedCodeBlockMarkdownWidget.MARKDOWN_WIDGET_ERROR_KEY_PREFIX))
		{
			documentEditor.getDocument().putMedia(key, (String) outputs.get(
				FencedCodeBlockMarkdownWidget.MARKDOWN_WIDGET_ERROR_KEY_PREFIX));
		}
		else documentEditor.getDocument().putMedia(key, (String) outputs.get(
			outputVariableName));
	}
}
