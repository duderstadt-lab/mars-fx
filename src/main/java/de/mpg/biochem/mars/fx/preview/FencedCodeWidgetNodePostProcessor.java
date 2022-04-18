package de.mpg.biochem.mars.fx.preview;

import com.vladsch.flexmark.parser.block.NodePostProcessor;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeTracker;
import com.vladsch.flexmark.util.data.DataHolder;

import de.mpg.biochem.mars.fx.editor.DocumentEditor;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import com.vladsch.flexmark.ast.FencedCodeBlock;

public class FencedCodeWidgetNodePostProcessor extends NodePostProcessor {
	private DocumentEditor documentEditor;
	
    public FencedCodeWidgetNodePostProcessor(DataHolder options, DocumentEditor documentEditor) {
		this.documentEditor = documentEditor;
	}

    @Override
    public void process(@NotNull NodeTracker state, @NotNull Node node) {
        if (node instanceof FencedCodeBlock) {
        	FencedCodeBlock fencedCodeBlockNode = (FencedCodeBlock) node;

        	if (fencedCodeBlockNode.getInfo().equals("python-image-widget")) 
        		processFencedCodeBlockWidget(fencedCodeBlockNode, "Conda Python 3", "imgsrc");
        	else if (fencedCodeBlockNode.getInfo().equals("python-html-widget")) 
        		processFencedCodeBlockWidget(fencedCodeBlockNode, "Conda Python 3", "html");
        	else if (fencedCodeBlockNode.getInfo().equals("groovy-image-widget")) 
        		processFencedCodeBlockWidget(fencedCodeBlockNode, "Groovy", "imgsrc");
        	else if (fencedCodeBlockNode.getInfo().equals("groovy-html-widget")) 
        		processFencedCodeBlockWidget(fencedCodeBlockNode, "Groovy", "html");
        }
    }
    
    private void processFencedCodeBlockWidget(FencedCodeBlock fencedCodeBlockNode, String language, String outputVariableName) {
    	String script = fencedCodeBlockNode.getContentChars().normalizeEOL();
		
		Map<String, Object> inputs = new HashMap<String, Object>();
		
		inputs.put("scijavaContext", documentEditor.getContext());
		inputs.put("archive", documentEditor.getArchive());
		
		FencedCodeBlockMarkdownWidget fencedCodeBlockMarkdownWidget = new FencedCodeBlockMarkdownWidget(documentEditor.getContext(), documentEditor.getArchive(), language);
		
		Map<String, Object> outputs = fencedCodeBlockMarkdownWidget.runScript(inputs, script);
		
		String key = DocumentEditor.MARKDOWN_WIDGET_MEDIA_KEY_PREFIX + fencedCodeBlockNode.getInfo() + ":" + script;
		
		if (outputs.containsKey(FencedCodeBlockMarkdownWidget.MARKDOWN_WIDGET_ERROR_KEY_PREFIX)) {
			documentEditor.getDocument().putMedia(key, (String) outputs.get(FencedCodeBlockMarkdownWidget.MARKDOWN_WIDGET_ERROR_KEY_PREFIX));
		} else
			documentEditor.getDocument().putMedia(key, (String) outputs.get(outputVariableName));
    }
}