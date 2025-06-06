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

package de.mpg.biochem.mars.fx.molecule;

import org.scijava.Context;

import de.mpg.biochem.mars.fx.molecule.moleculesTab.DefaultMoleculeCenterPane;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.DefaultMoleculePropertiesPane;
import de.mpg.biochem.mars.molecule.Molecule;

public class DefaultMoleculesTab extends
	AbstractMoleculesTab<Molecule, DefaultMoleculeCenterPane, DefaultMoleculePropertiesPane>
{

	public DefaultMoleculesTab(final Context context) {
		super(context);
	}

	@Override
	public DefaultMoleculeCenterPane createMoleculeCenterPane(
		final Context context)
	{
		return new DefaultMoleculeCenterPane(context);
	}

	@Override
	public DefaultMoleculePropertiesPane createMoleculePropertiesPane(
		final Context context)
	{
		return new DefaultMoleculePropertiesPane(context);
	}
}
