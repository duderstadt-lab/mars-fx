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
package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.script.ScriptModule;

import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.util.MarsMath;
import de.mpg.biochem.mars.fx.dashboard.AbstractCondaPython3Widget;
import net.imagej.ops.Initializable;

@Plugin( type = MoleculeArchiveDashboardWidget.class, name = "CondaPython3Widget" )
public class MoleculeArchiveCondaPython3Widget extends AbstractCondaPython3Widget implements MoleculeArchiveDashboardWidget, SciJavaPlugin, Initializable {

	@Parameter
	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;

	@Override
	public void initialize() {
		super.initialize();

		if (file == null)
			file = new File(archive.getFile().getAbsolutePath() + ".rover.images/" + MarsMath.getUUID58() + ".png");

		try {
			InputStream is = de.mpg.biochem.mars.fx.dashboard.MarsDashboardWidget.class.getResourceAsStream("CondaPython3.py");
			String scriptTemplate = IOUtils.toString(is, "UTF-8");
			is.close();
			codeArea.replaceText(scriptTemplate);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void createIOMaps() {
		super.createIOMaps();

		setJsonField("imageFilename", 
			jGenerator -> jGenerator.writeStringField("imageFilename", file.getName()),
			jParser -> file = new File(archive.getFile().getAbsolutePath() + ".rover.images/" + jParser.getText()));
	}

	@Override
	protected void setScriptInputs(ScriptModule module) {
		module.setInput("scijavaContext", context);
		module.setInput("archive", archive);
		module.setInput("width", Float.valueOf((float)rootPane.getWidth()/72));
		module.setInput("height", Float.valueOf((float)(rootPane.getHeight() - 75)/72));
		module.setInput("path", file.getAbsolutePath());
		if (!file.getParentFile().exists())
			file.getParentFile().mkdir();
	}

	public void setArchive(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive) {
		this.archive = archive;
	}

	public MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> getArchive() {
		return archive;
	}

	@Override
	public String getName() {
		return "CondaPython3Widget";
	}
}