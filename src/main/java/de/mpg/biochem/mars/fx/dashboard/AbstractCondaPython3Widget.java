/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2021 Karl Duderstadt
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

import static de.jensd.fx.glyphs.octicons.OctIcon.ZAP;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.scijava.module.ModuleItem;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

import de.gsi.dataset.spi.DefaultErrorDataSet;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.util.MarsMath;
import javafx.application.Platform;
import javafx.scene.Node;

import org.scijava.convert.ConvertService;
import org.scijava.module.ModuleException;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.script.ScriptInfo;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptModule;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import net.imagej.ops.Initializable;

import org.apache.commons.codec.binary.Base64;

public abstract class AbstractCondaPython3Widget extends AbstractScriptableWidget
		implements MarsDashboardWidget, SciJavaPlugin, Initializable {

	protected String imgsrc;
	protected BorderPane borderPane;

	@Parameter
	private ConvertService conversionService;

	@Override
	public void initialize() {
		super.initialize();
		langLabel.setText("Conda Python 3");

		borderPane = new BorderPane();

		setContent(getIcon(), borderPane);

		rootPane.setMinSize(250, 250);
		rootPane.setMaxSize(250, 250);
		
		loadImage();
	}

	@Override
	protected Map<String, Object> runScript() {
		writeToLog(new java.util.Date() + " - Running script... ");

		Reader reader = new StringReader(codeArea.getText());

		String scriptName = "script.py";

		lang = scriptService.getLanguageByName("Conda Python 3");

		ScriptInfo scriptInfo = new ScriptInfo(context, scriptName, reader);
		scriptInfo.setLanguage(lang);

		ScriptModule module = null;
		try {
			module = scriptInfo.createModule();
			context.inject(module);
		} catch (ModuleException e) {
			return null;
		}

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

		setScriptInputs(module);

		try {
			moduleService.run(module, false).get();
		} catch (InterruptedException e) {
			return null;
		} catch (ExecutionException e) {
			return null;
		}

		if (errorConsole.errorsFound())
			return null;

		outputPS.close();
		errorPS.close();

		return module.getOutputs();
	}

	@Override
	public Node getIcon() {
		return (Node) OctIconFactory.get().createIcon(ZAP, "1.2em");
	}

	private void updateImageViewSize(ImageView imageView, double HtoWratio) {		 
		 //double rootWidth = rootPane.getWidth() - 30;
		 //double rootHeight = rootPane.getHeight() - 70;
		 double rootWidth = rootPane.getWidth();
		 double rootHeight = rootPane.getHeight();

		 if (rootWidth*HtoWratio < rootHeight) {
			 imageView.setFitWidth(rootWidth);
		 } else {
			 imageView.setFitHeight(rootHeight);
		 }
	}
	
	private void loadImage() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				try {
					if (imgsrc == null)
						return;
					String base64string = imgsrc.substring(22);
					
		            byte[] imageByte = Base64.decodeBase64(base64string);
		            ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
		            Image image = new Image(bis);
					bis.close();
		            ImageView imageView = new ImageView();
					imageView.setImage(image);
	
					rootPane.widthProperty().addListener((obs, oldVal, newVal) -> {
						updateImageViewSize(imageView, image.getHeight()/image.getWidth());		 
					});
					rootPane.heightProperty().addListener((obs, oldVal, newVal) -> {
						updateImageViewSize(imageView, image.getHeight()/image.getWidth());	
					});
	
					imageView.setPreserveRatio(true);
	
					borderPane.setCenter(imageView);
	
					updateImageViewSize(imageView, image.getHeight()/image.getWidth());	
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void run() {
		Map<String, Object> outputs = runScript();
		imgsrc = (String) outputs.get("imgsrc");
		loadImage();
	}
}
