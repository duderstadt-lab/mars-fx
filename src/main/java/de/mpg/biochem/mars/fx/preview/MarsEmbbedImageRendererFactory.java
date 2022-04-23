
package de.mpg.biochem.mars.fx.preview;

import org.jetbrains.annotations.NotNull;

import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.util.data.DataHolder;

import de.mpg.biochem.mars.fx.editor.DocumentEditor;

public class MarsEmbbedImageRendererFactory implements NodeRendererFactory {

	private DocumentEditor documentEditor;

	public MarsEmbbedImageRendererFactory(DocumentEditor documentEditor) {
		this.documentEditor = documentEditor;
	}

	@NotNull
	@Override
	public NodeRenderer apply(@NotNull DataHolder options) {
		return new MarsEmbbededImageRenderer(options, documentEditor);
	}
}
