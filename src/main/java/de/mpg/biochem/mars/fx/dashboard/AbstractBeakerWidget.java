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

import static de.jensd.fx.glyphs.octicons.OctIcon.BEAKER;

import java.util.Map;

import net.imagej.ops.Initializable;

import org.scijava.plugin.SciJavaPlugin;

import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public abstract class AbstractBeakerWidget extends AbstractScriptableWidget
	implements MarsDashboardWidget, SciJavaPlugin, Initializable
{

	protected Node node;

	@Override
	public void initialize() {
		super.initialize();

		node = new BorderPane();

		setContent(getIcon(), node);

		rootPane.setMinSize(250, 250);
		rootPane.setMaxSize(250, 250);
	}

	@Override
	public Node getIcon() {
		return (Node) OctIconFactory.get().createIcon(BEAKER, "1.2em");
	}

	@Override
	public void run() {
		Map<String, Object> outputs = runScript();

		if (outputs == null) return;

		if (lang.getLanguageName().equals("Python (scyjava)")) {
			if (!outputs.containsKey("imgsrc")) {
				writeToLog("required output imgsrc is missing.");
				return;
			}

			imgsrc = (String) outputs.get("imgsrc");
			loadImage();
			return;
		}

		if (!outputs.containsKey("node")) {
			writeToLog("required output node is missing.");
			return;
		}

		node = (Node) outputs.get("node");

		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				setContent(node);
			}
		});
	}
}
