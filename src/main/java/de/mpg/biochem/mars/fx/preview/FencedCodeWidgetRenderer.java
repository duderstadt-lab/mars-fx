package de.mpg.biochem.mars.fx.preview;

import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.data.DataHolder;

import de.mpg.biochem.mars.fx.editor.DocumentEditor;

public class FencedCodeWidgetRenderer implements NodeRenderer {
	private DocumentEditor documentEditor;
	
    public FencedCodeWidgetRenderer(DataHolder options, DocumentEditor documentEditor) {
    	this.documentEditor = documentEditor;
    }

    @Override
    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        Set<NodeRenderingHandler<?>> set = new HashSet<>();
        set.add(new NodeRenderingHandler<>(FencedCodeBlock.class, this::render));
        return set;
    }

    private void render(FencedCodeBlock node, NodeRendererContext context, HtmlWriter html) {
    	// test the node to see if it needs overriding
        if (node.getInfo().equals("python-image-widget")) {
        	String script = node.getContentChars().normalizeEOL();
        	if (documentEditor.getDocument().getMediaIDs().contains(script)) {
        		
        		System.out.println("rendering... ");
        		
	        	html.attr("src", documentEditor.getDocument().getMedia(script))
	        	.withAttr()
	            .tag("img", true);
        	}
	        	
        } else if (node.getInfo().equals("python-string-widget")) {
        	
        } else if (node.getInfo().equals("groovy-string-widget")) {	
        	
        } else {
            context.delegateRender();
        }
    }
}