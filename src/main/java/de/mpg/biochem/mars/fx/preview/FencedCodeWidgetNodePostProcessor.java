package de.mpg.biochem.mars.fx.preview;

import com.vladsch.flexmark.parser.block.NodePostProcessor;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeTracker;
import com.vladsch.flexmark.util.data.DataHolder;
import org.jetbrains.annotations.NotNull;
import com.vladsch.flexmark.ast.FencedCodeBlock;

public class FencedCodeWidgetNodePostProcessor extends NodePostProcessor {
    public FencedCodeWidgetNodePostProcessor(DataHolder options) {
    }

    @Override
    public void process(@NotNull NodeTracker state, @NotNull Node node) {
        if (node instanceof FencedCodeBlock) {
        	if(((FencedCodeBlock) node).getInfo().equals("python-image-widget")) {
        		System.out.println("FencedCodeWidgetNodePostProcessor ");
        		
        		String script = ((FencedCodeBlock) node).getContentChars().normalizeEOL();
        		
        		System.out.println("script " + script);
        	}
        	
        }
    }
}