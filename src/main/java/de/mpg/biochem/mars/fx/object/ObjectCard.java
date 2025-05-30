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

package de.mpg.biochem.mars.fx.object;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.*;

import de.mpg.biochem.mars.fx.bdv.commands.MarsDNAFinderBdvCommand;
import de.mpg.biochem.mars.fx.bdv.commands.MarsObjectIntegrationBdvCommand;
import de.mpg.biochem.mars.fx.bdv.commands.MarsObjectTrackerBdvCommand;
import net.imagej.ops.Initializable;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.numeric.ARGBType;

import org.scijava.Context;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;

import bdv.util.BdvOverlay;
import de.mpg.biochem.mars.fx.bdv.MarsBdvCard;
import de.mpg.biochem.mars.fx.bdv.MarsBdvFrame;
import de.mpg.biochem.mars.image.PeakShape;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.AbstractJsonConvertibleRecord;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.object.MartianObject;

@Plugin(type = MarsBdvCard.class, name = "Object-Overlay")
public class ObjectCard extends AbstractJsonConvertibleRecord implements
	MarsBdvCard, SciJavaPlugin, Initializable
{

	private JTextField outlineThickness;
	private JCheckBox showObject, showAllObjects;

	private JPanel panel;

	private ObjectOverlay objectOverlay;
	private Molecule molecule;

	private boolean active = false;

	public Random ran = new Random();

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
		panel.setLayout(new GridLayout(0, 2));

		showObject = new JCheckBox("show", false);
		panel.add(showObject);
		showAllObjects = new JCheckBox("show all", false);
		panel.add(showAllObjects);
		panel.add(new JLabel("thickness"));

		outlineThickness = new JTextField(6);
		outlineThickness.setText("5");
		Dimension dimScaleField = new Dimension(100, 20);
		outlineThickness.setMinimumSize(dimScaleField);

		panel.add(outlineThickness);

		JButton objectFinderButton = new JButton("Find Objects");
		objectFinderButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				ExecutorService backgroundThread = Executors.newSingleThreadExecutor();
				backgroundThread.submit(() -> {
					MarsObjectTrackerBdvCommand objectFinderCommand = new MarsObjectTrackerBdvCommand();
					objectFinderCommand.setContext(context);

					for (Window window : Window.getWindows())
						if (window instanceof JDialog && ((JDialog) window).getTitle()
								.equals(objectFinderCommand.getInfo().getLabel()) && ((JDialog) window).isVisible()) {
							((JDialog) window).toFront();
							((JDialog) window).repaint();
							return;
						}

					//We set these directly to avoid pre and post processors from running
					//we don't need that in this context
					objectFinderCommand.setMarsBdvFrame(marsBdvFrame);
					objectFinderCommand.setArchive(archive);
					try {
						moduleService.run(objectFinderCommand, true).get();
					}
					catch (InterruptedException | ExecutionException exc) {
						exc.printStackTrace();
					}
				});
				backgroundThread.shutdown();
			}
		});
		panel.add(objectFinderButton);

		JButton objectIntegrationButton = new JButton("Integrate Objects");
		objectIntegrationButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				ExecutorService backgroundThread = Executors.newSingleThreadExecutor();
				backgroundThread.submit(() -> {
					MarsObjectIntegrationBdvCommand objectIntegrationCommand = new MarsObjectIntegrationBdvCommand();
					objectIntegrationCommand.setContext(context);

					for (Window window : Window.getWindows())
						if (window instanceof JDialog && ((JDialog) window).getTitle()
								.equals(objectIntegrationCommand.getInfo().getLabel()) && ((JDialog) window).isVisible()) {
							((JDialog) window).toFront();
							((JDialog) window).repaint();
							return;
						}

					//We set these directly to avoid pre and post processors from running
					//we don't need that in this context
					objectIntegrationCommand.setMarsBdvFrame(marsBdvFrame);
					objectIntegrationCommand.setArchive(archive);
					objectIntegrationCommand.setSelectedObjectUID(molecule.getUID());
					try {
						moduleService.run(objectIntegrationCommand, true).get();
					}
					catch (InterruptedException | ExecutionException exc) {
						exc.printStackTrace();
					}
				});
				backgroundThread.shutdown();
			}
		});
		panel.add(objectIntegrationButton);
	}

	public boolean showObject() {
		return showObject.isSelected();
	}

	public boolean showALlObjects() {
		return showAllObjects.isSelected();
	}

	@Override
	public void setMolecule(Molecule molecule) {
		this.molecule = molecule;
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
		return "Object-Overlay";
	}

	@Override
	public BdvOverlay getBdvOverlay() {
		if (objectOverlay == null) objectOverlay = new ObjectOverlay();

		return objectOverlay;
	}

	@Override
	public boolean isActive() {
		return active;
	}

	@Override
	public void setActive(boolean active) {
		this.active = active;
	}

	public class ObjectOverlay extends BdvOverlay {

		public ObjectOverlay() {}

		@Override
		protected void draw(Graphics2D g) {
			AffineTransform2D transform = new AffineTransform2D();
			getCurrentTransform2D(transform);

			g.setColor(getColor());
			g.setStroke(new BasicStroke(Integer.valueOf(outlineThickness
				.getText())));
			if (showAllObjects.isSelected() && archive != null) {
				archive.molecules().filter(m -> m.getMetadataUID().equals(molecule.getMetadataUID())).forEach(m -> {
					PeakShape shape = ((MartianObject) m).getShape(info
							.getTimePointIndex());
					if (shape != null) {
						boolean sourceInitialized = false;
						int xSource = 0;
						int ySource = 0;
						int x1 = 0;
						int y1 = 0;
						for (int pIndex = 0; pIndex < shape.x.length; pIndex++) {
							double x = shape.x[pIndex];
							double y = shape.y[pIndex];

							if (Double.isNaN(x) || Double.isNaN(y)) continue;

							final double[] globalCoords = new double[]{x, y};
							final double[] viewerCoords = new double[2];
							transform.apply(globalCoords, viewerCoords);

							int xTarget = (int) Math.round(viewerCoords[0]);
							int yTarget = (int) Math.round(viewerCoords[1]);

							if (sourceInitialized) g.drawLine(xSource, ySource, xTarget, yTarget);
							else {
								x1 = xTarget;
								y1 = yTarget;
							}

							xSource = xTarget;
							ySource = yTarget;
							sourceInitialized = true;
						}

						if (x1 != xSource || y1 != ySource) g.drawLine(xSource, ySource, x1,
								y1);
					}
				});
			} else if (showObject.isSelected()) {
				PeakShape shape = ((MartianObject) molecule).getShape(info
						.getTimePointIndex());

				if (shape != null) {
					boolean sourceInitialized = false;
					int xSource = 0;
					int ySource = 0;
					int x1 = 0;
					int y1 = 0;
					for (int pIndex = 0; pIndex < shape.x.length; pIndex++) {
						double x = shape.x[pIndex];
						double y = shape.y[pIndex];

						if (Double.isNaN(x) || Double.isNaN(y)) continue;

						final double[] globalCoords = new double[]{x, y};
						final double[] viewerCoords = new double[2];
						transform.apply(globalCoords, viewerCoords);

						int xTarget = (int) Math.round(viewerCoords[0]);
						int yTarget = (int) Math.round(viewerCoords[1]);

						if (sourceInitialized) g.drawLine(xSource, ySource, xTarget, yTarget);
						else {
							x1 = xTarget;
							y1 = yTarget;
						}

						xSource = xTarget;
						ySource = yTarget;
						sourceInitialized = true;
					}

					if (x1 != xSource || y1 != ySource) g.drawLine(xSource, ySource, x1,
							y1);
				}
			}
		}

		private Color getColor() {
			int alpha = (int) info.getDisplayRangeMax();

			if (alpha > 255 || alpha < 0) alpha = 255;

			final int r = ARGBType.red(info.getColor().get());
			final int g = ARGBType.green(info.getColor().get());
			final int b = ARGBType.blue(info.getColor().get());
			return new Color(r, g, b, alpha);
		}
	}

	@Override
	public JPanel getPanel() {
		return panel;
	}

	@Override
	protected void createIOMaps() {

		setJsonField("show", jGenerator -> {
			if (showObject != null) jGenerator.writeBooleanField("show", showObject
				.isSelected());
		}, jParser -> showObject.setSelected(jParser.getBooleanValue()));

		setJsonField("showAll", jGenerator -> {
			if (showAllObjects != null) jGenerator.writeBooleanField("showAll", showAllObjects
					.isSelected());
		}, jParser -> showAllObjects.setSelected(jParser.getBooleanValue()));

		setJsonField("thickness", jGenerator -> {
			if (outlineThickness != null) jGenerator.writeStringField("thickness",
				outlineThickness.getText());
		}, jParser -> outlineThickness.setText(jParser.getText()));
	}
}
