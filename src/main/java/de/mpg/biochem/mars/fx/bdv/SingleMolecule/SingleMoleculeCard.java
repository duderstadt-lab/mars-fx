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
package de.mpg.biochem.mars.fx.bdv.SingleMolecule;

import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import net.imagej.ops.Initializable;

import org.scijava.Context;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;

import bdv.util.BdvOverlay;
import de.mpg.biochem.mars.fx.bdv.MarsBdvCard;
import de.mpg.biochem.mars.fx.bdv.MarsBdvFrame;
import de.mpg.biochem.mars.fx.bdv.commands.MarsPeakTrackerBdvCommand;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.AbstractJsonConvertibleRecord;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;

/**
 * BDV card for single-molecule archives with a button that opens the
 * {@link MarsPeakTrackerBdvCommand} to find and track peaks in the record being
 * viewed. Tracked molecules are shown with the Location card's options.
 */
@Plugin(type = MarsBdvCard.class, name = "Peak Tracker")
public class SingleMoleculeCard extends AbstractJsonConvertibleRecord implements
	MarsBdvCard, SciJavaPlugin, Initializable
{

	private JPanel panel;
	private BdvOverlay overlay;
	private boolean active = false;

	@Parameter
	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;

	@Parameter
	protected MarsBdvFrame marsBdvFrame;

	@Parameter
	protected Context context;

	@Parameter
	protected ModuleService moduleService;

	@Override
	public void initialize() {
		panel = new JPanel();
		panel.setLayout(new GridLayout(0, 1));

		JButton trackButton = new JButton("Track Peaks");
		trackButton.addActionListener(e -> openPeakTracker());
		panel.add(trackButton);
	}

	private void openPeakTracker() {
		ExecutorService backgroundThread = Executors.newSingleThreadExecutor();
		backgroundThread.submit(() -> {
			MarsPeakTrackerBdvCommand peakTracker = new MarsPeakTrackerBdvCommand();
			peakTracker.setContext(context);

			// Bring an open tracker to the front instead of opening a second one.
			for (Window window : Window.getWindows())
				if (window instanceof JDialog && ((JDialog) window).getTitle().equals(
					peakTracker.getInfo().getLabel()) && window.isVisible())
				{
					window.toFront();
					window.repaint();
					return;
				}

			// Set directly to avoid pre and post processors, as the other cards do.
			peakTracker.setMarsBdvFrame(marsBdvFrame);
			peakTracker.setArchive(archive);
			try {
				moduleService.run(peakTracker, true).get();
			}
			catch (InterruptedException | ExecutionException exc) {
				exc.printStackTrace();
			}
		});
		backgroundThread.shutdown();
	}

	@Override
	public void setMolecule(Molecule molecule) {}

	@Override
	public void setArchive(
		MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive)
	{
		this.archive = archive;
	}

	@Override
	public void setBdvFrame(MarsBdvFrame marsBdvFrame) {
		this.marsBdvFrame = marsBdvFrame;
	}

	@Override
	public String getName() {
		return "Peak Tracker";
	}

	/** Nothing to draw: the frame adds every card's overlay, so this one is empty. */
	@Override
	public BdvOverlay getBdvOverlay() {
		if (overlay == null) overlay = new BdvOverlay() {

			@Override
			protected void draw(Graphics2D g) {}
		};
		return overlay;
	}

	@Override
	public boolean isActive() {
		return active;
	}

	@Override
	public void setActive(boolean active) {
		this.active = active;
	}

	@Override
	public JPanel getPanel() {
		return panel;
	}

	@Override
	protected void createIOMaps() {
		// No settings to save.
	}
}
