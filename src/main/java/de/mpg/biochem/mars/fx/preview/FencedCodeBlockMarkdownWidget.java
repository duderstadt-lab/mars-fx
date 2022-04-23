package de.mpg.biochem.mars.fx.preview;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.scijava.Context;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.script.ScriptInfo;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;

import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.molecule.MoleculeArchiveWindow;
import javafx.application.Platform;

public class FencedCodeBlockMarkdownWidget {

	@Parameter
	protected ScriptService scriptService;

	@Parameter
	protected ModuleService moduleService;

	@Parameter
	protected Context context;
	
	public static final String MARKDOWN_WIDGET_ERROR_KEY_PREFIX = "MARKDOWN_WIDGET_ERROR:";
	
	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;

	protected ScriptLanguage lang;

	public FencedCodeBlockMarkdownWidget(final Context context, MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive, String language) {
		this.archive = archive;
		context.inject(this);
		lang = scriptService.getLanguageByName(language);
	}

	protected Map<String, Object> runScript(Map<String, Object> inputs, String script) {
		archive.getWindow().logln("Running fenced code block markdown widget...");
		
		String scriptName = "script";
		if (lang.getLanguageName().equals("Groovy")) {
			scriptName += ".groovy";
		} else if (lang.getLanguageName().equals("Python")) {
			scriptName += ".py";
		}

		ScriptInfo scriptInfo = new ScriptInfo(context, scriptName, new StringReader(script));
		scriptInfo.setLanguage(lang);
		
		ScriptModule module = null;
		try {
			module = scriptInfo.createModule();
			context.inject(module);
		} catch (ModuleException e) {
			return null;
		}
		
		module.setInputs(inputs);
		
		StringBuffer std_sb = new StringBuffer();
		StringBuffer error_sb = new StringBuffer();
		
		OutputConsole outputConsole = new OutputConsole(archive.getWindow(), std_sb);
		PrintStream outputPS = new PrintStream(outputConsole, true);

		ErrorConsole errorConsole = new ErrorConsole(archive.getWindow(), error_sb);
		PrintStream errorPS = new PrintStream(errorConsole, true);

		try {
			Writer outputWriter = new OutputStreamWriter(outputPS, "UTF-8");
			module.setOutputWriter(outputWriter);

			Writer errorWriter = new OutputStreamWriter(errorPS, "UTF-8");
			module.setErrorWriter(errorWriter);

		} catch (UnsupportedEncodingException e1) {
			outputPS.close();
			errorPS.close();
			return null;
		}

		try {
			moduleService.run(module, false).get();
		} catch (InterruptedException e) {
			return null;
		} catch (ExecutionException e) {
			return null;
		}
		
		if (errorConsole.errorsFound()) {
			Map<String, Object> errorOutput = new HashMap<String, Object>();
			errorOutput.put(MARKDOWN_WIDGET_ERROR_KEY_PREFIX, MARKDOWN_WIDGET_ERROR_KEY_PREFIX + std_sb.toString() + error_sb.toString());
			return errorOutput;
		}

		outputPS.close();
		errorPS.close();

		return module.getOutputs();
	}
	
	class OutputConsole extends OutputStream {

		private MoleculeArchiveWindow lockScreen;
		private StringBuffer sb;

		public OutputConsole(MoleculeArchiveWindow lockScreen, StringBuffer sb) {
			this.lockScreen = lockScreen;
			this.sb = sb;
		}

		@Override
		public void write(int i) throws IOException {
			sb.append((char) i);
			Platform.runLater(() -> {
				lockScreen.log(String.valueOf((char) i));
			});
		}
	}

	class ErrorConsole extends OutputStream {

		private MoleculeArchiveWindow lockScreen;
		private boolean errorsFound = false;
		private StringBuffer sb;

		public ErrorConsole(MoleculeArchiveWindow lockScreen, StringBuffer sb) {
			this.lockScreen = lockScreen;
			this.sb = sb;
		}

		@Override
		public void write(int i) throws IOException {
			errorsFound = true;
			sb.append((char) i);
			Platform.runLater(() -> {
				lockScreen.log(String.valueOf((char) i));
			});
		}
		
		public boolean errorsFound() {
			return errorsFound;
		}
	}
}
