package de.mpg.biochem.mars.fx.preview;

import java.io.StringReader;
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

public class PythonImageMarkdownWidget {

	@Parameter
	protected ScriptService scriptService;

	@Parameter
	protected ModuleService moduleService;

	@Parameter
	protected Context context;

	protected ScriptLanguage lang;

	public PythonImageMarkdownWidget(final Context context, String language) {
		context.inject(this);
		lang = scriptService.getLanguageByName(language);
	}

	protected Map<String, Object> runScript(Map<String, Object> inputs, String script) {
		
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

		/*
		OutputConsole outputConsole = new OutputConsole(logArea);
		PrintStream outputPS = new PrintStream(outputConsole, true);

		ErrorConsole errorConsole = new ErrorConsole(logArea);
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
*/
		
		try {
			moduleService.run(module, false).get();
		} catch (InterruptedException e) {
			return null;
		} catch (ExecutionException e) {
			return null;
		}

		//if (errorConsole.errorsFound())
		//	return null;

		//outputPS.close();
		//errorPS.close();

		return module.getOutputs();
	}
}
