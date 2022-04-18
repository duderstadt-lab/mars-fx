package de.mpg.biochem.mars.fx.preview;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.ast.BlockContent;

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
    	
    	//Should we check if node.getInfo() matches something first?
    	
    	String key = DocumentEditor.MARKDOWN_WIDGET_MEDIA_KEY_PREFIX + node.getInfo() + ":" + node.getContentChars().normalizeEOL();
		
		if (documentEditor.getDocument().getMediaIDs().contains(key)) {
			int startOffset = node.getStartOffset();
    		int endOffset = node.getEndOffset();
    		String content = documentEditor.getDocument().getMedia(key);
    		
    		if (content.startsWith(FencedCodeBlockMarkdownWidget.MARKDOWN_WIDGET_ERROR_KEY_PREFIX)) {
    			BasedSequence errorMessage = BasedSequence.of(content.substring(FencedCodeBlockMarkdownWidget.MARKDOWN_WIDGET_ERROR_KEY_PREFIX.length()));
	    		html.line();
	            html.attr("data-pos", startOffset + ":" + endOffset).withAttr().tag("pre").openPre();
	            html.attr("data-pos", startOffset + ":" + endOffset).srcPosWithEOL(errorMessage).withAttr().tag("code");
	            html.text(errorMessage.normalizeEOL());
	            html.tag("/code");
	            html.tag("/pre").closePre();
	            return;
			} else if (node.getInfo().equals("python-image-widget") || node.getInfo().equals("groovy-markdown-widget")) {
	        	html.attr("src", documentEditor.getDocument().getMedia(key))
	        	.withAttr()
	            .tag("img", true);
	        	return;
	        } else if (node.getInfo().equals("python-html-widget") || node.getInfo().equals("groovy-html-widget")) {
	        	html.line();
	            html.raw(documentEditor.getDocument().getMedia(key));
	            return;
	        }
		}
        context.delegateRender();
    }
}