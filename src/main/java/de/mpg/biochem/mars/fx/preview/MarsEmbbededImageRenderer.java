package de.mpg.biochem.mars.fx.preview;

import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.data.DataHolder;

import de.mpg.biochem.mars.util.MarsDocument;
import com.vladsch.flexmark.util.sequence.BasedSequence;

public class MarsEmbbededImageRenderer implements NodeRenderer {
	private MarsDocument document;
	
    public MarsEmbbededImageRenderer(DataHolder options, MarsDocument document) {
    	this.document = document;
    }

    @Override
    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        Set<NodeRenderingHandler<?>> set = new HashSet<>();
        set.add(new NodeRenderingHandler<>(Image.class, this::render));
        return set;
    }

    private void render(Image node, NodeRendererContext context, HtmlWriter html) {
    	String mediaData = node.getUrl().toString();
    	if (document != null && document.getMediaIDs().contains(mediaData)) {
    		System.out.println("inserting image data");
    		mediaData = document.getMedia(mediaData);
    		node.setUrl(BasedSequence.of(mediaData));
    	}
    	
    	context.delegateRender();
    	
    	//Record Active media list somehow... So non-active can be removed... Add to MarsDocment ???

    }
}
