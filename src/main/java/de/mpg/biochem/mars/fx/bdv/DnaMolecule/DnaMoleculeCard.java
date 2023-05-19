/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2023 Karl Duderstadt
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

package de.mpg.biochem.mars.fx.bdv.DnaMolecule;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.mpg.biochem.mars.fx.bdv.MarsBdvCard;
import de.mpg.biochem.mars.fx.bdv.MarsBdvFrame;
import net.imagej.ops.Initializable;

import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;

import bdv.util.BdvOverlay;
import de.mpg.biochem.mars.fx.bdv.commands.MarsDNAFinderBdvCommand;
import de.mpg.biochem.mars.fx.bdv.commands.MarsDNAPeakTrackerBdvCommand;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.AbstractJsonConvertibleRecord;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;

@Plugin(type = MarsBdvCard.class, name = "DNA-Overlay")
public class DnaMoleculeCard extends AbstractJsonConvertibleRecord implements
	MarsBdvCard, SciJavaPlugin, Initializable
{

	private JTextField dnaThickness;

	private JPanel panel;

	private LineOverlay dnaMoleculeOverlay;
	private Molecule molecule;

	private boolean active = false;

	@Parameter
	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;
	
	@Parameter
	protected MarsBdvFrame marsBdvFrame;
	
	@Parameter
	protected ModuleService moduleService;
	
	@Parameter
	protected LogService logService;
	
	@Parameter
	protected Context context;

	@Override
	public void initialize() {
		panel = new JPanel();
		panel.setLayout(new GridLayout(0, 2));

		panel.add(new JLabel("Thickness"));
		dnaThickness = new JTextField(6);
		dnaThickness.setText("5");
		dnaThickness.addActionListener(e -> {
			int thickness = Integer.valueOf(dnaThickness.getText());
			if (dnaMoleculeOverlay != null && thickness > 0 && thickness < 100) dnaMoleculeOverlay.setThickness(thickness);
		});
		Dimension dimScaleField = new Dimension(100, 20);
		dnaThickness.setMinimumSize(dimScaleField);

		panel.add(dnaThickness);
		
		JButton dnaFinderButton = new JButton("Find DNA");
		dnaFinderButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				ExecutorService backgroundThread = Executors.newSingleThreadExecutor();
				backgroundThread.submit(() -> {
						MarsDNAFinderBdvCommand dnaFinderCommand = new MarsDNAFinderBdvCommand();
						dnaFinderCommand.setContext(context);
						
						for (Window window : Window.getWindows())
							if (window instanceof JDialog && ((JDialog) window).getTitle()
								.equals(dnaFinderCommand.getInfo().getLabel()) && ((JDialog) window).isVisible()) {
										((JDialog) window).toFront();
										((JDialog) window).repaint();
										return;
							}
						
						//We set these directly to avoid pre and post processors from running
						//we don't need that in this context
						dnaFinderCommand.setMarsBdvFrame(marsBdvFrame);
						dnaFinderCommand.setArchive(archive);
						try {
							moduleService.run(dnaFinderCommand, true).get();
						}
						catch (InterruptedException | ExecutionException exc) {
							exc.printStackTrace();
						}
				});
				backgroundThread.shutdown();
			}
		});
		panel.add(dnaFinderButton);
		
		JButton peakTrackerButton = new JButton("Add Track");
		peakTrackerButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				ExecutorService backgroundThread = Executors.newSingleThreadExecutor();
				backgroundThread.submit(() -> {
					MarsDNAPeakTrackerBdvCommand peakTrackerCommand = new MarsDNAPeakTrackerBdvCommand();
					peakTrackerCommand.setContext(context);
					
					for (Window window : Window.getWindows())
						if (window instanceof JDialog && ((JDialog) window).getTitle()
							.equals(peakTrackerCommand.getInfo().getLabel()) && ((JDialog) window).isVisible()) {
									((JDialog) window).toFront();
									((JDialog) window).repaint();
									return;
						}
					
					//We set these directly to avoid pre and post processors from running
					//we don't need that in this context
					peakTrackerCommand.setMarsBdvFrame(marsBdvFrame);
					peakTrackerCommand.setArchive(archive);
					try {
						moduleService.run(peakTrackerCommand, true).get();
					}
					catch (InterruptedException | ExecutionException exc) {
						exc.printStackTrace();
					}
				});
				backgroundThread.shutdown();
			}
		});
		panel.add(peakTrackerButton);

		JButton dnaDrawButton = new JButton("Draw DNA");
		dnaDrawButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ExecutorService backgroundThread = Executors.newSingleThreadExecutor();
				backgroundThread.submit(() -> {
					LineEditor lineEditor = new LineEditor(marsBdvFrame);
					lineEditor.setArchive(archive);
					lineEditor.install();
				});
				backgroundThread.shutdown();
			}
		});
		panel.add(dnaDrawButton);
	}

	@Override
	public JPanel getPanel() {
		return panel;
	}

	@Override
	protected void createIOMaps() {
		setJsonField("thickness", jGenerator -> {
			if (dnaThickness != null) jGenerator.writeStringField("thickness",
				dnaThickness.getText());
		}, jParser -> dnaThickness.setText(jParser.getText()));
	}

	@Override
	public void setMolecule(Molecule molecule) {
		this.molecule = molecule;
		if (molecule != null && dnaMoleculeOverlay != null) dnaMoleculeOverlay.setLine(molecule.getParameter("Dna_Top_X1"),
				molecule.getParameter("Dna_Top_Y1"),
				molecule.getParameter("Dna_Bottom_X2"),
				molecule.getParameter("Dna_Bottom_Y2"));
	}

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
		return "DNA-Overlay";
	}

	@Override
	public BdvOverlay getBdvOverlay() {
		if (molecule != null && dnaMoleculeOverlay == null) {
			dnaMoleculeOverlay = new LineOverlay(molecule.getParameter("Dna_Top_X1"),
					molecule.getParameter("Dna_Top_Y1"),
					molecule.getParameter("Dna_Bottom_X2"),
					molecule.getParameter("Dna_Bottom_Y2"));
			dnaMoleculeOverlay.setThickness(Integer.valueOf(dnaThickness.getText()));
		}

		return dnaMoleculeOverlay;
	}

	@Override
	public boolean isActive() {
		return active;
	}

	@Override
	public void setActive(boolean active) {
		this.active = active;
	}
}
