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

package de.mpg.biochem.mars.fx.table;

import org.scijava.Context;
import org.scijava.Priority;
import org.scijava.display.Display;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UserInterface;
import org.scijava.ui.viewer.AbstractDisplayViewer;
import org.scijava.ui.viewer.DisplayViewer;

import de.mpg.biochem.mars.table.MarsTable;

@Plugin(type = DisplayViewer.class, priority = Priority.NORMAL)
public class MarsTableFxView extends AbstractDisplayViewer<MarsTable> implements
	DisplayViewer<MarsTable>
{

	@Parameter
	private Context context;

	// This method is called to create and display a window
	// here we override it to make sure that calls like uiService.show( .. for
	// MarsTable
	// will use this method automatically..
	@Override
	public void view(final UserInterface ui, final Display<?> d) {
		MarsTable results = (MarsTable) d.get(0);

		// We also create a new window since we assume it is a new table...
		MarsTableFxFrame marsTableFxFrame = new MarsTableFxFrame(results.getName(),
			results, context);
		marsTableFxFrame.init();
	}

	@Override
	public boolean canView(final Display<?> d) {
		if (d instanceof MarsTableFxDisplay) {
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public MarsTableFxDisplay getDisplay() {
		return (MarsTableFxDisplay) super.getDisplay();
	}

	@Override
	public boolean isCompatible(UserInterface arg0) {
		// Needs to be updated if all contexts are to be enabled beyond ImageJ
		return true;
	}
}
