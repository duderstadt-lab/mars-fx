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

        	if(fencedCodeBlockNode.getInfo().equals("python-image-widget")) {
        		String script = fencedCodeBlockNode.getContentChars().normalizeEOL();
        		
        		Map<String, Object> inputs = new HashMap<String, Object>();
        		
        		inputs.put("scijavaContext", documentEditor.getContext());
        		inputs.put("archive", documentEditor.getArchive());
        		
        		PythonImageMarkdownWidget pythonImageMarkdownWidget = new PythonImageMarkdownWidget(documentEditor.getContext(), "Conda Python 3");
        		
        		Map<String, Object> outputs = pythonImageMarkdownWidget.runScript(inputs, script);
        		
        		String key = DocumentEditor.MARKDOWN_WIDGET_MEDIA_KEY_PREFIX + fencedCodeBlockNode.getInfo() + ":" + script;
        		documentEditor.getDocument().putMedia(key, (String) outputs.get("imgsrc"));
        	}        	
        }
    }
}