/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2025 Karl Duderstadt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package de.mpg.biochem.mars.fx.dashboard;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.BOOK;
import static de.jensd.fx.glyphs.octicons.OctIcon.CODE;

import java.io.ByteArrayInputStream;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.imagej.ops.Initializable;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.script.ScriptInfo;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;

import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.fx.editor.MarsScriptEditor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;

public abstract class AbstractScriptableWidget extends AbstractDashboardWidget
	implements Initializable
{

	@Parameter
	protected ScriptService scriptService;

	@Parameter
	protected ModuleService moduleService;

	@Parameter
	protected MarsDashboardWidgetService marsDashboardWidgetService;

	@Parameter
	protected LogService log;

	@Parameter
	protected Context context;

	protected ScriptLanguage lang;
	protected Label langLabel;
	protected ToggleGroup languageGroup;
	protected MarsScriptEditor codeArea;
	protected InlineCssTextArea logArea;

	// Used for Python (scyjava) widgets
	protected String imgsrc;

	@Override
	public void initialize() {
		super.initialize();

		String languageName = marsDashboardWidgetService
			.getDefaultScriptingLanguage();

		if (languageName.equals("Groovy")) {
			lang = scriptService.getLanguages().stream().filter(l -> l
				.getLanguageName().equals("Groovy")).findFirst().get();
		}
		else if (languageName.equals("Python (Jython)")) {
			if (scriptService.getLanguages().stream().filter(l -> l
				.getLanguageName().equals("Python")).findFirst().isPresent())
				lang = scriptService.getLanguages().stream().filter(l -> l
					.getLanguageName().equals("Python")).findFirst().get();
			else if (scriptService.getLanguages().stream().filter(l -> l
				.getLanguageName().equals("Python (Jython)")).findFirst().isPresent())
				lang = scriptService.getLanguages().stream().filter(l -> l
					.getLanguageName().equals("Python (Jython)")).findFirst().get();
		}
		else if (languageName.equals("Python (scyjava)")) {
			lang = scriptService.getLanguages().stream().filter(l -> l
				.getLanguageName().equals("Python (scyjava)")).findFirst().get();
		}

		// Script Pane
		Tab scriptTab = new Tab();
		scriptTab.setGraphic(OctIconFactory.get().createIcon(CODE, "1.0em"));

		codeArea = new MarsScriptEditor();

		// auto-indent: insert previous line's indents on enter
		final Pattern whiteSpace = Pattern.compile("^\\s+");
		codeArea.addEventHandler(KeyEvent.KEY_PRESSED, KE -> {
			if (KE.getCode() == KeyCode.ENTER) {
				int caretPosition = codeArea.getCaretPosition();
				int currentParagraph = codeArea.getCurrentParagraph();
				Matcher m0 = whiteSpace.matcher(codeArea.getParagraph(currentParagraph -
					1).getSegments().get(0));
				if (m0.find()) Platform.runLater(() -> codeArea.insertText(
					caretPosition, m0.group()));
			}
		});

		BorderPane scriptBorder = new BorderPane();
		scriptBorder.setCenter(new VirtualizedScrollPane<>(codeArea));

		langLabel = new Label(lang.getLanguageName());
		langLabel.setPadding(new Insets(5, 5, 5, 5));
		scriptBorder.setTop(langLabel);

		scriptTab.setContent(scriptBorder);
		getTabPane().getTabs().add(scriptTab);

		logArea = new InlineCssTextArea("");
		logArea.getStyleClass().add("log-area");
		logArea.getStyleClass().add("markdown-editor");
		logArea.setEditable(false);
		//logArea.setStyle("-fx-font-family: \"monospace\"; -fx-font-size: 10pt;");

		BorderPane borderPane = new BorderPane();
		borderPane.setCenter(new VirtualizedScrollPane<>(logArea));
		borderPane.setPrefSize(100, 100);
		borderPane.setPadding(new Insets(5, 5, 5, 5));

		Tab logTab = new Tab();
		logTab.setContent(borderPane);
		logTab.setGraphic(OctIconFactory.get().createIcon(BOOK, "1.0em"));
		getTabPane().getTabs().add(logTab);

		if (lang.getLanguageName().equals("Python (scyjava)")) loadImage();
	}

	protected Map<String, Object> runScript() {
		writeToLog(new java.util.Date() + " - Running script... ");

		Reader reader = new StringReader(codeArea.getText());

		String scriptName = "script";
		if (lang.getLanguageName().equals("Groovy")) {
			scriptName += ".groovy";
		}
		else if (lang.getLanguageName().equals("Python") || 
				     lang.getLanguageName().equals("Python (Jython)") ||
				     lang.getLanguageName().equals("Python (scyjava)"))
		{
			scriptName += ".py";
		}

		ScriptInfo scriptInfo = new ScriptInfo(context, scriptName, reader);
		scriptInfo.setLanguage(lang);

		ScriptModule module = null;
		try {
			module = scriptInfo.createModule();
			context.inject(module);
		}
		catch (ModuleException e) {
			return null;
		}

		// Can these move into the initialize block...

		OutputConsole outputConsole = new OutputConsole(logArea);
		PrintStream outputPS = new PrintStream(outputConsole, true);

		ErrorConsole errorConsole = new ErrorConsole(logArea);
		PrintStream errorPS = new PrintStream(errorConsole, true);

		try {
			Writer outputWriter = new OutputStreamWriter(outputPS, "UTF-8");
			module.setOutputWriter(outputWriter);

			Writer errorWriter = new OutputStreamWriter(errorPS, "UTF-8");
			module.setErrorWriter(errorWriter);

		}
		catch (UnsupportedEncodingException e1) {
			outputPS.close();
			errorPS.close();
			return null;
		}

		setScriptInputs(module);

		try {
			moduleService.run(module, false).get();
		}
		catch (InterruptedException e) {
			return null;
		}
		catch (ExecutionException e) {
			return null;
		}

		if (errorConsole.errorsFound()) return null;

		outputPS.close();
		errorPS.close();

		return module.getOutputs();
	}

	protected abstract void setScriptInputs(ScriptModule module);

	protected void loadScript(String name) throws IOException {
		loadScript(name, null);
	}

	protected void loadScript(String name, String inputParameters)
		throws IOException
	{
		if (lang.getLanguageName().equals("Groovy")) {
			name += ".groovy";
		}
		else if (lang.getLanguageName().equals("Python") || lang.getLanguageName().equals("Python (Jython)")) {
			name += ".py";
		}
		else if (lang.getLanguageName().equals("Python (scyjava)")) {
			name += "_scyjava.py";
		}
		InputStream is = de.mpg.biochem.mars.fx.dashboard.MarsDashboardWidget.class
			.getResourceAsStream(name);
		String scriptTemplate = (inputParameters != null) ? inputParameters +
			IOUtils.toString(is, "UTF-8") : IOUtils.toString(is, "UTF-8");
		is.close();
		codeArea.replaceText(scriptTemplate);
	}

	private void updateImageViewSize(ImageView imageView, double HtoWratio) {
		double rootWidth = rootPane.getWidth();
		double rootHeight = rootPane.getHeight();

		if (rootWidth * HtoWratio < rootHeight) {
			imageView.setFitWidth(rootWidth);
		}
		else {
			imageView.setFitHeight(rootHeight);
		}
	}

	protected void loadImage() {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				try {
					if (imgsrc == null) return;
					String base64string = imgsrc.substring(22);

					byte[] imageByte = Base64.decodeBase64(base64string);
					ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
					Image image = new Image(bis);
					bis.close();
					ImageView imageView = new ImageView();
					imageView.setImage(image);

					rootPane.widthProperty().addListener((obs, oldVal, newVal) -> {
						updateImageViewSize(imageView, image.getHeight() / image
							.getWidth());
					});
					rootPane.heightProperty().addListener((obs, oldVal, newVal) -> {
						updateImageViewSize(imageView, image.getHeight() / image
							.getWidth());
					});

					imageView.setPreserveRatio(true);

					BorderPane borderPane = new BorderPane();
					setContent(borderPane);
					borderPane.setCenter(imageView);

					updateImageViewSize(imageView, image.getHeight() / image.getWidth());
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	protected void createIOMaps() {
		super.createIOMaps();

		setJsonField("Language", jGenerator -> {
			jGenerator.writeStringField("Language", lang.getLanguageName());
		}, jParser -> {
			String language = jParser.getText();
			langLabel.setText(language);
			lang = scriptService.getLanguageByName(language);
		});

		setJsonField("Script", jGenerator -> {
			jGenerator.writeStringField("Script", codeArea.getText());
		}, jParser -> {
			codeArea.replaceText(jParser.getText());
		});

		setJsonField("imgsrc", jGenerator -> {
			if (imgsrc != null) jGenerator.writeStringField("imgsrc", imgsrc);
		}, jParser -> imgsrc = jParser.getText());
	}

	@Override
	public void close() {
		super.close();
		codeArea.cleanup();
	}

	protected void writeToLog(String message) {
		Platform.runLater(() -> logArea.appendText(message + "\n"));
	}

	class OutputConsole extends OutputStream {

		private InlineCssTextArea logarea;

		public OutputConsole(InlineCssTextArea logarea) {
			this.logarea = logarea;
		}

		@Override
		public void write(int i) throws IOException {
			Platform.runLater(() -> logarea.appendText(String.valueOf((char) i)));
		}
	}

	class ErrorConsole extends OutputStream {

		private InlineCssTextArea logarea;
		private boolean errorsFound = false;

		public ErrorConsole(InlineCssTextArea logarea) {
			this.logarea = logarea;
		}

		@Override
		public void write(int i) throws IOException {
			errorsFound = true;
			Platform.runLater(() -> logarea.appendText(String.valueOf((char) i)));
		}

		public boolean errorsFound() {
			return errorsFound;
		}
	}
}
