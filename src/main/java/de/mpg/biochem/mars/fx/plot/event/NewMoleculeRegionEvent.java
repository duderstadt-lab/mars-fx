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

package de.mpg.biochem.mars.fx.plot.event;

import de.mpg.biochem.mars.util.MarsRegion;
import javafx.event.EventType;

public class NewMoleculeRegionEvent extends PlotEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final MarsRegion roi;

	public static final EventType<PlotEvent> NEW_MOLECULE_REGION =
		new EventType<>(PLOT_EVENT, "NEW_MOLECULE_REGION");

	public NewMoleculeRegionEvent(MarsRegion roi) {
		super(NEW_MOLECULE_REGION);
		this.roi = roi;
	}

	public MarsRegion getRegion() {
		return roi;
	}

	@Override
	public void invokeHandler(PlotEventHandler handler) {}
}
