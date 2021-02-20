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

import static de.jensd.fx.glyphs.octicons.OctIcon.BEAKER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.gsi.dataset.spi.DefaultErrorDataSet;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.fx.molecule.dashboardTab.MoleculeArchiveDashboardWidget;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.application.Platform;
import javafx.scene.Node;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.script.ScriptModule;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import net.imagej.ops.Initializable;

public abstract class AbstractBeakerWidget extends AbstractScriptableWidget
		implements MarsDashboardWidget, SciJavaPlugin, Initializable {

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

		if (outputs == null)
			return;

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
