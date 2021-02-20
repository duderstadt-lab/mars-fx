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
package de.mpg.biochem.mars.fx.object;

import org.scijava.Priority;
import org.scijava.display.AbstractDisplay;
import org.scijava.display.Display;
import org.scijava.plugin.Plugin;

import de.mpg.biochem.mars.molecule.*;
import de.mpg.biochem.mars.object.ObjectArchive;

/**
 * Display for {@link DnaMoleculeArchive}. This ensures that uiService.show() for a DnaMoleculeArchive will automatically be detected and 
 * call the view method in MoleculeArchiveView to make our custom window with custom menus.
 * 
 * @author Karl Duderstadt
 */
@Plugin(type = Display.class, priority = Priority.HIGH)
public class ObjectArchiveFxDisplay extends AbstractDisplay<ObjectArchive> implements Display<ObjectArchive> {
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ObjectArchiveFxDisplay() {
		super((Class) ObjectArchive.class);
	}

	// -- Display methods --

	@Override
	public boolean canDisplay(final Class<?> c) {
		if (c.equals(ObjectArchive.class)) {
			return true;
		} else { 
			return super.canDisplay(c);
		}
	}
	
	@Override
	public boolean isDisplaying(final Object o) {
		return false;
	}
}
