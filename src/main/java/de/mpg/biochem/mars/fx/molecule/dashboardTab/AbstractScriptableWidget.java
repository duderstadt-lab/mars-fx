package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.BOOK;
import static de.jensd.fx.glyphs.octicons.OctIcon.CODE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javafx.application.Platform;

import org.apache.commons.io.IOUtils;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleService;
import org.scijava.script.ScriptInfo;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;

import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.fx.syntaxhighlighter.JavaSyntaxHighlighter;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;

import javafx.scene.layout.HBox;
import javafx.scene.control.Toggle;

import net.imagej.ops.Initializable;
import org.scijava.plugin.Parameter;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.reactfx.Subscription;

public abstract class AbstractScriptableWidget extends AbstractDashboardWidget implements Initializable {
	
	@Parameter
	protected ScriptService scriptService;
	
	@Parameter
	protected ModuleService moduleService;
	
	@Parameter
	protected LogService log;
	
	@Parameter
	protected Context context;
	
	protected ScriptLanguage lang;
	protected RadioButton radioButtonGroovy, radioButtonPython;
	protected ToggleGroup languageGroup;
	protected CodeArea codeArea;
	protected Subscription cleanupWhenNoLongerNeedIt;
	protected InlineCssTextArea logArea;
	protected Writer writer;
	
	@Override
	public void initialize() {
		super.initialize();
		
		lang = scriptService.getLanguageByName("Groovy");
		
		//Script Pane
        Tab scriptTab = new Tab();
        scriptTab.setGraphic(OctIconFactory.get().createIcon(CODE, "1.0em"));
		
        codeArea = new CodeArea();

        // add line numbers to the left of area
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        
        codeArea.getStylesheets().add("de/mpg/biochem/mars/fx/syntaxhighlighter/java-keywords.css");

        // recompute the syntax highlighting 500 ms after user stops editing area
        cleanupWhenNoLongerNeedIt = codeArea

                // plain changes = ignore style changes that are emitted when syntax highlighting is reapplied
                // multi plain changes = save computation by not rerunning the code multiple times
                //   when making multiple changes (e.g. renaming a method at multiple parts in file)
                .multiPlainChanges()

                // do not emit an event until 500 ms have passed since the last emission of previous stream
                .successionEnds(Duration.ofMillis(500))

                // run the following code block when previous stream emits an event
                .subscribe(ignore -> codeArea.setStyleSpans(0, JavaSyntaxHighlighter.computeHighlighting(codeArea.getText())));

        // when no longer need syntax highlighting and wish to clean up memory leaks
        // run: `cleanupWhenNoLongerNeedIt.unsubscribe();`


        // auto-indent: insert previous line's indents on enter
        final Pattern whiteSpace = Pattern.compile( "^\\s+" );
        codeArea.addEventHandler( KeyEvent.KEY_PRESSED, KE ->
        {
            if ( KE.getCode() == KeyCode.ENTER ) {
            	int caretPosition = codeArea.getCaretPosition();
            	int currentParagraph = codeArea.getCurrentParagraph();
                Matcher m0 = whiteSpace.matcher( codeArea.getParagraph( currentParagraph-1 ).getSegments().get( 0 ) );
                if ( m0.find() ) Platform.runLater( () -> codeArea.insertText( caretPosition, m0.group() ) );
            }
        });
    
        BorderPane scriptBorder = new BorderPane();
        scriptBorder.setCenter(new VirtualizedScrollPane<>(codeArea));
        
        languageGroup = new ToggleGroup();
        
        radioButtonGroovy = new RadioButton("Groovy");
        radioButtonGroovy.setToggleGroup(languageGroup);
        
        radioButtonPython = new RadioButton("Python");
        radioButtonPython.setToggleGroup(languageGroup);
        
        radioButtonGroovy.setSelected(true);
        
        languageGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            @Override
            public void changed(ObservableValue<? extends Toggle> ov, Toggle oldToggle, Toggle newToggle) {
            	if (newToggle == radioButtonGroovy)
            		lang = scriptService.getLanguageByName("Groovy");
            	else if (newToggle == radioButtonPython)
            		lang = scriptService.getLanguageByName("Python");
            }
        });
        
        HBox hbox = new HBox(radioButtonGroovy, radioButtonPython);
        hbox.setSpacing(5);
        hbox.setPadding(new Insets(5, 5, 5, 5));
        scriptBorder.setPadding(new Insets(5, 5, 5, 5));
        scriptBorder.setPrefSize(100, 100);
        scriptBorder.setTop(hbox);
        
        scriptTab.setContent(scriptBorder);
        getTabPane().getTabs().add(scriptTab);
        
        logArea = new InlineCssTextArea("");
        logArea.setEditable(false);
  
        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(new VirtualizedScrollPane<>(logArea));
        borderPane.setPrefSize(100, 100);
        borderPane.setPadding(new Insets(5, 5, 5, 5));
        
        Tab logTab = new Tab();
        logTab.setContent(borderPane);
        logTab.setGraphic(OctIconFactory.get().createIcon(BOOK, "1.0em"));
        getTabPane().getTabs().add(logTab);

	}
	
	protected Map<String, Object> runScript() {
		writeToLog(new java.util.Date() + " - Running script... ");
		
		Reader reader = new StringReader(codeArea.getText());
		
		String scriptName = "script";
		if (radioButtonGroovy.isSelected()) {
			scriptName += ".groovy";
		} else if (radioButtonPython.isSelected()) {
			scriptName += ".py";
		}
		
		ScriptInfo scriptInfo = new ScriptInfo(context, scriptName, reader);
		scriptInfo.setLanguage(lang);
			
		ScriptModule module = null;
		try {
			module = scriptInfo.createModule();
			context.inject(module);
		} catch (ModuleException e) {
			return null;
		}
		
		Console console = new Console(logArea);
        PrintStream ps = new PrintStream(console, true);
        
		try {
			writer = new OutputStreamWriter(ps,"UTF-8");
			
			module.setOutputWriter(writer);
			module.setErrorWriter(writer);
		} catch (UnsupportedEncodingException e1) {
			return null;
		}
		
		module.setInput("archive", archive);
		
		try {
			moduleService.run(module, false).get();
		} catch (InterruptedException e) {
			return null;
		} catch (ExecutionException e) {
			return null;
		}
		
		return module.getOutputs();
	}
	
	public void loadScript(String name) throws IOException {
		//Load example script
    	InputStream is = this.getClass().getResourceAsStream(name);
    	final String scriptExample = IOUtils.toString(is, "UTF-8");
		is.close();
		codeArea.replaceText(0, 0, scriptExample);
	}
	
	@Override
	public void close() {
		super.close();
		cleanupWhenNoLongerNeedIt.unsubscribe();
	}
	
	protected void writeToLog(String message) {
		Platform.runLater( () -> logArea.appendText(message + "\n") );
	}
	
	class Console extends OutputStream {

        private InlineCssTextArea logarea;

        public Console(InlineCssTextArea logarea) {
        	this.logarea = logarea;
        }

        @Override
        public void write(int i) throws IOException {
        	Platform.runLater( () -> logarea.appendText(String.valueOf((char) i)) );
        }
    }
}
