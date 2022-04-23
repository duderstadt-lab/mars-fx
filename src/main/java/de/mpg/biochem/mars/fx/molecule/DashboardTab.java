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

package de.mpg.biochem.mars.fx.molecule;

import java.io.IOException;
import java.util.ArrayList;

import org.scijava.Context;
import org.scijava.plugin.Parameter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.mpg.biochem.mars.fx.dashboard.MarsDashboardWidgetService;
import de.mpg.biochem.mars.fx.event.InitializeMoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.molecule.dashboardTab.MoleculeArchiveDashboard;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.layout.BorderPane;

public class DashboardTab extends AbstractMoleculeArchiveTab {

	private BorderPane borderPane;

	private MoleculeArchiveDashboard dashboardPane;

	@Parameter
	private MarsDashboardWidgetService marsDashboardWidgetService;

	public DashboardTab(final Context context) {
		super(context);

		setIcon(MaterialIconFactory.get().createIcon(
			de.jensd.fx.glyphs.materialicons.MaterialIcon.DASHBOARD, "1.083em"));

		dashboardPane = new MoleculeArchiveDashboard(context);

		borderPane = new BorderPane();
		borderPane.setCenter(dashboardPane.getNode());

		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT,
			this);

		getTab().setContent(borderPane);
	}

	@Override
	public void onInitializeMoleculeArchiveEvent(
		MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive)
	{
		this.archive = archive;
		dashboardPane.getNode().fireEvent(new InitializeMoleculeArchiveEvent(
			archive));
	}

	@Override
	public String getName() {
		return "dashboardTab";
	}

	@Override
	public ArrayList<Menu> getMenus() {
		return null;
	}

	@Override
	public Node getNode() {
		return borderPane;
	}

	@Override
	public void toJSON(JsonGenerator jGenerator) throws IOException {
		dashboardPane.toJSON(jGenerator);
	}

	@Override
	public void fromJSON(JsonParser jParser) throws IOException {
		dashboardPane.fromJSON(jParser);
	}

	@Override
	protected void createIOMaps() {
		// Not needed. All the action is happening inside the dashboardPane.
	}
}
