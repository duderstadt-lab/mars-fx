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

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.apache.commons.io.IOUtils;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleService;
import org.scijava.script.ScriptInfo;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;

import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.fx.event.RefreshMetadataEvent;
import de.mpg.biochem.mars.fx.event.RefreshMoleculeEvent;
import de.mpg.biochem.mars.fx.event.RefreshMoleculePropertiesEvent;
import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.fx.util.HotKeyEntry;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Insets;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;

import javafx.scene.layout.HBox;
import javafx.scene.control.Toggle;

import net.imagej.ops.Initializable;
import org.scijava.plugin.Parameter;

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
	protected TextArea textarea, scriptTextArea;
	//protected LanguageSettableEditorPane editorpane;
	protected RadioButton radioButtonGroovy, radioButtonPython;
	protected ToggleGroup languageGroup;
	
	//protected RTextScrollPane scroll;
	
	@Override
	public void initialize() {
		super.initialize();
		
		lang = scriptService.getLanguageByName("Groovy");
		
		//Script Pane
        Tab scriptTab = new Tab();
        scriptTab.setGraphic(OctIconFactory.get().createIcon(CODE, "1.0em"));
		
        //SwingNode swingNode = new SwingNode();
        //createSwingContent(swingNode);
    
        BorderPane scriptBorder = new BorderPane();
        scriptTextArea = new TextArea();
        scriptBorder.setCenter(scriptTextArea);
        
        //scriptBorder.setCenter(swingNode);
        
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
            	/*SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                    	editorpane.setLanguage(lang);
                    }
            	});*/
            }
        });
        
        HBox hbox = new HBox(radioButtonGroovy, radioButtonPython);
        hbox.setSpacing(5);
        hbox.setPadding(new Insets(5, 5, 5, 5));
        scriptBorder.setPadding(new Insets(5, 5, 5, 5));
        scriptBorder.setPrefSize(250, 250);
        scriptBorder.setTop(hbox);
        
        scriptTab.setContent(scriptBorder);
        getTabPane().getTabs().add(scriptTab);
        
        textarea = new TextArea();
        textarea.setEditable(false);
  
        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(textarea);
        borderPane.setPrefSize(250, 250);
        borderPane.setPadding(new Insets(5, 5, 5, 5));
        
        Tab logTab = new Tab();
        logTab.setContent(borderPane);
        logTab.setGraphic(OctIconFactory.get().createIcon(BOOK, "1.0em"));
        getTabPane().getTabs().add(logTab);
        /*
        tabs.getSelectionModel().selectedItemProperty().addListener(
    		new ChangeListener<Tab>() {
    			@Override
    			public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
    				if (newValue == scriptTab) 
    					SwingUtilities.invokeLater(new Runnable() {
    	                    @Override
    	                    public void run() {
    	                    	swingNode.autosize();
    	                    }
    	            	});
    			}
    		});
    		*/
	}
	/*
	private void createSwingContent(final SwingNode swingNode) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
            	editorpane = new LanguageSettableEditorPane();
        		context.inject(editorpane);
            	editorpane.setLanguage(lang);
            	scroll = editorpane.wrappedInScrollbars();
                swingNode.setContent(scroll);
            }
        });
        
    }
	*/
	@SuppressWarnings("resource")
	protected Map<String, Object> runScript() {
		//Reader reader = new StringReader(editorpane.getText());
		Reader reader = new StringReader(scriptTextArea.getText());
		
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
			log.error(e);
			return null;
		}
		
		Console console = new Console(textarea);
        PrintStream ps = new PrintStream(console, true);
        
        Writer writer;
		try {
			writer = new OutputStreamWriter(ps,"UTF-8");
			
			module.setOutputWriter(writer);
			module.setErrorWriter(writer);
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			log.error(e1);
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
		scriptTextArea.setText(scriptExample);
		/*
		SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
            	editorpane.setText(scriptExample);
            }
        });
        */
	}
	
	public static class Console extends OutputStream {

        private TextArea output;

        public Console(TextArea ta) {
            this.output = ta;
        }

        @Override
        public void write(int i) throws IOException {
        	Platform.runLater( () -> output.appendText(String.valueOf((char) i)) );
            
        }
    }
}
