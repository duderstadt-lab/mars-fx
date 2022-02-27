package de.mpg.biochem.mars.fx.preview;

import org.jetbrains.annotations.NotNull;

import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.util.data.DataHolder;

import de.mpg.biochem.mars.util.MarsDocument;

public class MarsEmbbedImageRendererFactory implements NodeRendererFactory {
	private MarsDocument document;
	
	public MarsEmbbedImageRendererFactory(MarsDocument document) {
		this.document = document;
	}
	
    @NotNull
    @Override
    public NodeRenderer apply(@NotNull DataHolder options) {
        return new MarsEmbbededImageRenderer(options, document);
    }
}