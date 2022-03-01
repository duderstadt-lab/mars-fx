package de.mpg.biochem.mars.fx.preview;

import org.jetbrains.annotations.NotNull;

import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.parser.block.NodePostProcessor;
import com.vladsch.flexmark.parser.block.NodePostProcessorFactory;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.DataHolder;

import de.mpg.biochem.mars.fx.editor.DocumentEditor;

public class FencedCodeWidgetNodePostProcessorFactory extends NodePostProcessorFactory {
	private DocumentEditor documentEditor;
	
	public FencedCodeWidgetNodePostProcessorFactory(DocumentEditor documentEditor) {
		super(false);
		this.documentEditor = documentEditor;
		addNodes(FencedCodeBlock.class);
	}
	
    public FencedCodeWidgetNodePostProcessorFactory(DataHolder options) {
        super(false);

        addNodes(FencedCodeBlock.class);
    }

    @NotNull
    @Override
    public NodePostProcessor apply(@NotNull Document document) {
        return new FencedCodeWidgetNodePostProcessor(document);
    }
}