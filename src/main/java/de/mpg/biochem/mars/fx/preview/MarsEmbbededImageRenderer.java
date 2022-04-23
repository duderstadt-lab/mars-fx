package de.mpg.biochem.mars.fx.preview;

import java.util.HashSet;
import java.util.Set;

import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import de.mpg.biochem.mars.fx.editor.DocumentEditor;

public class MarsEmbbededImageRenderer implements NodeRenderer {
	private DocumentEditor documentEditor;
	
    public MarsEmbbededImageRenderer(DataHolder options, DocumentEditor documentEditor) {
    	this.documentEditor = documentEditor;
    }

    @Override
    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        Set<NodeRenderingHandler<?>> set = new HashSet<>();
        set.add(new NodeRenderingHandler<>(Image.class, this::render));
        return set;
    }

    private void render(Image node, NodeRendererContext context, HtmlWriter html) {
    	String mediaData = node.getUrl().toString();
    	if (documentEditor != null && documentEditor.getDocument().getMediaIDs().contains(mediaData)) {
    		documentEditor.addActiveMediaID(mediaData);
    		mediaData = documentEditor.getDocument().getMedia(mediaData);
    		node.setUrl(BasedSequence.of(mediaData));
    	}
    	context.delegateRender();
    }
}
