/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2026 Karl Duderstadt
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

package de.mpg.biochem.mars.fx.molecule.moleculesTab.kymograph;

import de.mpg.biochem.mars.molecule.DnaMoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.SingleMoleculeArchive;

/**
 * Archive types supported by the kymograph tab. Transverse flow archives are
 * recognized by class name because mars-transverseflow depends on mars-fx.
 */
public enum KymographArchiveType {
		DNA("DnaArchiveMoleculeRegionBuilder"), SINGLE_MOLECULE(
			"SingleMoleculeArchiveRegionBuilder"), TRANSVERSE_FLOW(
				"TransverseFlowArchiveMoleculeRegionBuilder");

	public static final String KYMOGRAPH_PACKAGE = "de.mpg.biochem.mars.kymograph";

	private static final String TRANSVERSE_FLOW_ARCHIVE =
		"de.mpg.biochem.mars.transverseflow.TransverseFlowArchive";

	private final String regionBuilder;

	KymographArchiveType(String regionBuilder) {
		this.regionBuilder = regionBuilder;
	}

	/**
	 * Get the mars-kymograph class used to extract the region around a molecule
	 * for montages.
	 *
	 * @return The simple class name of the region builder.
	 */
	public String getRegionBuilder() {
		return regionBuilder;
	}

	/**
	 * Kymographs need a line along the molecule, which only DNA molecules
	 * have.
	 *
	 * @return True if kymographs can be created.
	 */
	public boolean supportsKymograph() {
		return this == DNA;
	}

	/**
	 * Whether the mars-kymograph classes needed for this archive type are
	 * installed.
	 *
	 * @return True if the classes are available.
	 */
	public boolean isAvailable() {
		return classExists(KYMOGRAPH_PACKAGE + ".ImageFormatter") && classExists(
			KYMOGRAPH_PACKAGE + "." + regionBuilder);
	}

	/**
	 * Get the type for an archive.
	 *
	 * @param archive The archive.
	 * @return The type or null if the archive is not supported.
	 */
	public static KymographArchiveType of(MoleculeArchive<?, ?, ?, ?> archive) {
		if (archive == null) return null;
		if (archive instanceof DnaMoleculeArchive) return DNA;
		if (archive instanceof SingleMoleculeArchive) return SINGLE_MOLECULE;
		for (Class<?> c = archive.getClass(); c != null; c = c.getSuperclass())
			if (c.getName().equals(TRANSVERSE_FLOW_ARCHIVE)) return TRANSVERSE_FLOW;
		return null;
	}

	private static boolean classExists(String name) {
		try {
			Class.forName(name, false, KymographArchiveType.class.getClassLoader());
			return true;
		}
		catch (ClassNotFoundException | LinkageError e) {
			return false;
		}
	}
}
