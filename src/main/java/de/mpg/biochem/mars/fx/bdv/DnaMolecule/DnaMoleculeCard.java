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

package de.mpg.biochem.mars.fx.bdv.DnaMolecule;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.*;

import de.mpg.biochem.mars.fx.bdv.MarsBdvCard;
import de.mpg.biochem.mars.fx.bdv.MarsBdvFrame;
import de.mpg.biochem.mars.fx.dialogs.RoverConfirmationDialog;
import de.mpg.biochem.mars.fx.dialogs.ShowVideoDialog;
import de.mpg.biochem.mars.fx.molecule.AbstractMoleculeArchiveFxFrame;
import de.mpg.biochem.mars.image.DNASegment;
import de.mpg.biochem.mars.table.MarsTable;
import de.mpg.biochem.mars.util.LogBuilder;
import de.mpg.biochem.mars.util.MarsMath;
import javafx.application.Platform;
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

import java.util.List;
import java.util.ArrayList;
import java.awt.event.ItemEvent;

@Plugin(type = MarsBdvCard.class, name = "DNA-Overlay")
public class DnaMoleculeCard extends AbstractJsonConvertibleRecord implements
	MarsBdvCard, SciJavaPlugin, Initializable
{

	private JTextField dnaThickness;

	private JCheckBox showAllDNAs;

	private JPanel panel;

	private LineOverlay dnaMoleculeOverlay;
	private Molecule molecule;

	private boolean active = false;

	@Parameter
	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;
	
	@Parameter
	protected MarsBdvFrame marsBdvFrame;

	protected LineEditor lineEditor;
	
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

		panel.add(new JLabel(""));
		showAllDNAs = new JCheckBox("Show all", false);
		panel.add(showAllDNAs);
		
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

		panel.add(new JLabel("Drawing tools"));
		panel.add(new JPanel());

		JToggleButton dnaDrawButton = new JToggleButton("Draw DNAs");
		dnaDrawButton.addItemListener((ItemEvent ev) -> {
				if(ev.getStateChange() == ItemEvent.SELECTED) {
					if (lineEditor == null) lineEditor = new LineEditor(marsBdvFrame);
					lineEditor.install();
				} else if(ev.getStateChange() == ItemEvent.DESELECTED) {
					final List<DNASegment> segments = lineEditor.getSegments();
					lineEditor.uninstall();
					if (segments.size() > 0)
						Platform.runLater(() -> {
							RoverConfirmationDialog addDNAMoleculesToArchive =
									new RoverConfirmationDialog(((AbstractMoleculeArchiveFxFrame) archive.getWindow()).getNode().getScene().getWindow(),
											"Create DnaMolecule records from drawn DNAs?", "Yes", "No");
							addDNAMoleculesToArchive.showAndWait().ifPresent(result -> {
								if (result.getButtonData().isDefaultButton())
									((AbstractMoleculeArchiveFxFrame) archive.getWindow())
											.runTask(() -> createDNAmoleculeRecords(segments),
													"Creating DnaMolecule records...");
							});
						});
				}
			});
		panel.add(dnaDrawButton);

		JButton removeLastDNA = new JButton("Remove last DNA");
		removeLastDNA.addActionListener((ActionEvent e) -> {
			if (lineEditor != null) {
				List<DNASegment> segments = lineEditor.getSegments();
				if (segments.size() > 0) {
					segments.remove(segments.size() - 1);
					marsBdvFrame.getBdvHandle().getViewerPanel().getDisplay().repaint();
				}
			}
		});
		panel.add(removeLastDNA);

		JButton clearDNAs = new JButton("Clear");
		clearDNAs.addActionListener((ActionEvent e) -> {
			if (lineEditor != null) {
				lineEditor.getSegments().clear();
				marsBdvFrame.getBdvHandle().getViewerPanel().getDisplay().repaint();
			}
		});
		panel.add(clearDNAs);
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

		setJsonField("showAll", jGenerator -> {
			if (showAllDNAs != null) jGenerator.writeBooleanField("showAll",
					showAllDNAs.isSelected());
		}, jParser -> showAllDNAs.setSelected(jParser.getBooleanValue()));
	}

	@Override
	public void setMolecule(Molecule molecule) {
		this.molecule = molecule;
		if (molecule != null && dnaMoleculeOverlay != null) {
			List<DNASegment> segments = new ArrayList<DNASegment>();
			if (showAllDNAs.isSelected() && archive != null) {
				archive.molecules().filter(m -> m.getMetadataUID().equals(molecule.getMetadataUID())).forEach(m ->
						segments.add(new DNASegment(m.getParameter("Dna_Top_X1"),
								m.getParameter("Dna_Top_Y1"),
								m.getParameter("Dna_Bottom_X2"),
								m.getParameter("Dna_Bottom_Y2"))));
			} else segments.add(new DNASegment(molecule.getParameter("Dna_Top_X1"),
					molecule.getParameter("Dna_Top_Y1"),
					molecule.getParameter("Dna_Bottom_X2"),
					molecule.getParameter("Dna_Bottom_Y2")));
			dnaMoleculeOverlay.setSegments(segments);
		}
	}

	protected void createDNAmoleculeRecords(List<DNASegment> segments) {
		if (archive != null) {
			archive.getWindow().lock();
			List<String> uids = new ArrayList<>();
			for (DNASegment segment : segments) {
				//Add an empty table...
				MarsTable table = new MarsTable("table");

				//Build molecule record with DNA location
				Molecule molecule = archive.createMolecule(MarsMath.getUUID58(), table);
				molecule.setMetadataUID(marsBdvFrame.getMetadataUID());
				molecule.setImage(archive.getMetadata(marsBdvFrame.getMetadataUID()).getImage(0).getImageID());
				molecule.setParameter("Dna_Top_X1", segment.getX1());
				molecule.setParameter("Dna_Top_Y1", segment.getY1());
				molecule.setParameter("Dna_Bottom_X2", segment.getX2());
				molecule.setParameter("Dna_Bottom_Y2", segment.getY2());

				molecule.addTag("Bdv Draw DNA");
				molecule.setNotes("DnaMolecule created on " + new java.util.Date() + " by the Bdv Draw DNA");
				//add to archive
				archive.put(molecule);
				//should add something to the archive log ... logService.info("Added DnaMolecule record " + molecule.getUID());
				uids.add(molecule.getUID());
			}

			LogBuilder builder = new LogBuilder();
			String log = LogBuilder.buildTitleBlock("Bdv Drawn DNA");

			String uidList = uids.get(0);
			for (int i = 1; i < uids.size(); i++)
				uidList = uidList + ", " + uids.get(i);

			builder.addParameter("Created DnaMolecules", uidList);
			builder.addParameter("Metadata UID", marsBdvFrame.getMetadataUID());
			log += builder.buildParameterList();
			log += "\n" + LogBuilder.endBlock();
			archive.getMetadata(marsBdvFrame.getMetadataUID()).logln(log);

			archive.getWindow().unlock();
			final String lastUID = uids.get(uids.size() - 1);
			Platform.runLater(() -> ((AbstractMoleculeArchiveFxFrame) archive.getWindow()).getMoleculesTab().setSelectedMolecule(lastUID));
			marsBdvFrame.setMolecule(archive.get(lastUID));
		}
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
		if (dnaMoleculeOverlay == null) {
			dnaMoleculeOverlay = new LineOverlay();
			dnaMoleculeOverlay.setThickness(Integer.valueOf(dnaThickness.getText()));
			if (molecule != null) {
				List<DNASegment> segments = new ArrayList<DNASegment>();
				if (showAllDNAs.isSelected() && archive != null) {
					archive.molecules().filter(m -> m.getMetadataUID().equals(molecule.getMetadataUID())).forEach(m ->
						segments.add(new DNASegment(m.getParameter("Dna_Top_X1"),
								m.getParameter("Dna_Top_Y1"),
								m.getParameter("Dna_Bottom_X2"),
								m.getParameter("Dna_Bottom_Y2"))));
				} else segments.add(new DNASegment(molecule.getParameter("Dna_Top_X1"),
						molecule.getParameter("Dna_Top_Y1"),
						molecule.getParameter("Dna_Bottom_X2"),
						molecule.getParameter("Dna_Bottom_Y2")));
				dnaMoleculeOverlay.setSegments(segments);
			}
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
