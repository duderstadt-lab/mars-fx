package de.mpg.biochem.mars.fx.preview;

import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.parser.block.NodePostProcessor;
import com.vladsch.flexmark.parser.block.NodePostProcessorFactory;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeTracker;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.misc.CharPredicate;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Image;

public class FencedCodeWidgetNodePostProcessor extends NodePostProcessor {
    public FencedCodeWidgetNodePostProcessor(DataHolder options) {
    }

    @Override
    public void process(@NotNull NodeTracker state, @NotNull Node node) {
        if (node instanceof FencedCodeBlock) {
        	System.out.println("Found a fenced code block " + ((FencedCodeBlock) node).getInfo());
        	
        	Node previous = node.getPrevious();
        	
        	Image imageNode = new Image();
        	
        	imageNode.takeChildren(node);
            node.unlink();
            state.nodeRemoved(node);
            previous.insertAfter(imageNode);
            //state.nodeAddedWithChildren(imageNode);
            //previous.setChars(chars.subSequence(0, chars.length() - mediaLink.getPrefix().length()));
            //if (previous.getChars().length() == 0) {
            //    previous.unlink();
            //    state.nodeRemoved(previous);
            //}

            if (previous instanceof Text) {
                BasedSequence chars = previous.getChars();
                if (chars.isContinuedBy(node.getChars())) {
                	/*
                    AbstractMediaLink mediaLink;
                    if (chars.endsWith(AudioLink.PREFIX) && !isEscaped(chars, AudioLink.PREFIX)) { // AudioLink
                        mediaLink = new AudioLink((Link) node);
                    } else if (chars.endsWith(EmbedLink.PREFIX) && !isEscaped(chars, EmbedLink.PREFIX)) { // EmbedLink
                        mediaLink = new EmbedLink((Link) node);
                    } else if (chars.endsWith(PictureLink.PREFIX) && !isEscaped(chars, PictureLink.PREFIX)) { // PictureLink
                        mediaLink = new PictureLink((Link) node);
                    } else if (chars.endsWith(VideoLink.PREFIX) && !isEscaped(chars, VideoLink.PREFIX)) { // VideoLink
                        mediaLink = new VideoLink((Link) node);
                    } else { // None of the Above, abort postprocess
                        return;
                    }

                    mediaLink.takeChildren(node);
                    node.unlink();
                    state.nodeRemoved(node);
                    previous.insertAfter(mediaLink);
                    state.nodeAddedWithChildren(mediaLink);
                    previous.setChars(chars.subSequence(0, chars.length() - mediaLink.getPrefix().length()));
                    if (previous.getChars().length() == 0) {
                        previous.unlink();
                        state.nodeRemoved(previous);
                    }
                    */
                }
            }
        }
    }

    private boolean isEscaped(BasedSequence chars, String prefix) {
        int backslashCount = chars.subSequence(0, chars.length() - prefix.length()).countTrailing(CharPredicate.BACKSLASH);
        return (backslashCount & 1) != 0;
    }
}