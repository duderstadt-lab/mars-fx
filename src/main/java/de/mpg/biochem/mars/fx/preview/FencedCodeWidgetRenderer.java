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

public class FencedCodeWidgetRenderer implements NodeRenderer {
    public FencedCodeWidgetRenderer(DataHolder options) {

    }

    @Override
    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        Set<NodeRenderingHandler<?>> set = new HashSet<>();
        set.add(new NodeRenderingHandler<>(FencedCodeBlock.class, this::render));
        return set;
    }

    private void render(FencedCodeBlock node, NodeRendererContext context, HtmlWriter html) {
    	// test the node to see if it needs overriding
        if (node.getInfo().equals("marspy")) {
            System.out.println("Found a marspy: " + html.toSequence());
            html.text("");
        	//html.attr("class", "my-info").withAttr().tag("p").line();
            //html.text(node.getContentChars().normalizeEOL());
            //html.closeTag("p").line();
        } else {
            context.delegateRender();
        }
    }

    public static class Factory implements NodeRendererFactory {
        @NotNull
        @Override
        public NodeRenderer apply(@NotNull DataHolder options) {
            return new FencedCodeWidgetRenderer(options);
        }
    }
}