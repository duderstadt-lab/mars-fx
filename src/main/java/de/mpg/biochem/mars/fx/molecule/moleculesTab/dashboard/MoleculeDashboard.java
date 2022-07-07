/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2022 Karl Duderstadt
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

package de.mpg.biochem.mars.fx.molecule.moleculesTab.dashboard;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import org.scijava.Context;

import de.mpg.biochem.mars.fx.dashboard.AbstractDashboard;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.util.MarsUtil;
import javafx.event.Event;
import javafx.event.EventHandler;

public class MoleculeDashboard<M extends Molecule> extends
	AbstractDashboard<MoleculeDashboardWidget> implements MoleculeSubPane
{

	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;
	protected M molecule;

	public MoleculeDashboard(final Context context) {
		super(context);

		getNode().addEventHandler(MoleculeEvent.MOLECULE_EVENT, this);
		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT,
			new EventHandler<MoleculeArchiveEvent>()
			{

				@Override
				public void handle(MoleculeArchiveEvent e) {
					if (e.getEventType().getName().equals(
						"INITIALIZE_MOLECULE_ARCHIVE"))
			{
						archive = e.getArchive();

						if (archive != null) discoverWidgets();
						if (archive == null) {
							widgets.stream().forEach(widget -> {
								widget.setArchive(null);
								widget.setMolecule(null);
								molecule = null;
							});
						}
						e.consume();
					}
				}
			});
	}

	@Override
	public MoleculeDashboardWidget createWidget(String widgetName) {
		MoleculeDashboardWidget widget =
			(MoleculeDashboardWidget) marsDashboardWidgetService.createWidget(
				widgetName);
		widget.setMolecule(molecule);
		widget.setArchive(archive);
		widget.setParent(this);
		widget.initialize();
		return widget;
	}

	@Override
	public ArrayList<String> getWidgetToolbarOrder() {
		return new ArrayList<String>(Arrays.asList("MoleculeCategoryChartWidget",
			"MoleculeHistogramWidget", "MoleculeXYChartWidget",
			"MoleculeBubbleChartWidget"));
	}

	@Override
	public void handle(MoleculeEvent event) {
		event.invokeHandler(this);
		event.consume();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onMoleculeSelectionChangedEvent(Molecule molecule) {
		this.molecule = (M) molecule;
		widgets.forEach(widget -> widget.setMolecule(molecule));
	}

	public Set<String> getWidgetNames() {
		return marsDashboardWidgetService.getWidgetNames(
			MoleculeDashboardWidget.class);
	}

	@Override
	public boolean importFromRoverFile(File roverFile) {
		try {
			InputStream inputStream = new BufferedInputStream(new FileInputStream(
				roverFile));
			JsonFactory jfactory = new JsonFactory();
			JsonParser jParser = jfactory.createParser(inputStream);
			MarsUtil.readJsonObject(jParser, this, "moleculesTab", "centerPane",
				"moleculeDashboard");
			
			jParser.close();
			inputStream.close();
		}
		catch (IOException e) {
			return false;
		}
		return true;
	}

	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}
}
