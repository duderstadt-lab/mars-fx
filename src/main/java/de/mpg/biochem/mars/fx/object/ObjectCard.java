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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.Random;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;

import com.fasterxml.jackson.core.JsonParser;

import bdv.util.BdvOverlay;
import de.mpg.biochem.mars.fx.bdv.MarsBdvCard;
import de.mpg.biochem.mars.fx.molecule.dashboardTab.MoleculeArchiveDashboardWidget;
import de.mpg.biochem.mars.image.PeakShape;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.AbstractJsonConvertibleRecord;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.object.MartianObject;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import net.imagej.ops.Initializable;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.numeric.ARGBType;

@Plugin( type = MarsBdvCard.class, name = "Object-Overlay" )
public class ObjectCard extends AbstractJsonConvertibleRecord implements MarsBdvCard, SciJavaPlugin, Initializable {

	private JTextField outlineThickness;
	private JCheckBox showObject;
	
	private JPanel panel;
	
	private ObjectOverlay objectOverlay;
	private Molecule molecule;
	
	private boolean active = false;
	
	public Random ran = new Random();
	
	@Parameter
	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;
	
	@Override
	public void initialize() {
		panel = new JPanel();
		panel.setLayout(new GridLayout(0, 2));

		showObject = new JCheckBox("show", false);
		panel.add(showObject);
		panel.add(new JPanel());
		
		panel.add(new JLabel("thickness"));
		
		outlineThickness = new JTextField(6);
		outlineThickness.setText("5");
		Dimension dimScaleField = new Dimension(100, 20);
		outlineThickness.setMinimumSize(dimScaleField);
		
		panel.add(outlineThickness);
	}
	
	public boolean showObject() {
		return showObject.isSelected();
	}
	
	@Override
	public void setMolecule(Molecule molecule) {
		this.molecule = molecule;
	}
	
	@Override
	public void setArchive(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive) {
		this.archive = archive;
	}

	@Override
	public String getName() {
		return "Object-Overlay";
	}

	@Override
	public BdvOverlay getBdvOverlay() {
		if (objectOverlay == null)
			objectOverlay = new ObjectOverlay();
		
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
		
		public ObjectOverlay() { }
		
		@Override
		protected void draw(Graphics2D g) {
			if (showObject.isSelected() && ((MartianObject) molecule).hasShape(info.getTimePointIndex())) {
				AffineTransform2D transform = new AffineTransform2D();
				getCurrentTransform2D(transform);
				
				g.setColor(getColor());
				g.setStroke( new BasicStroke( Integer.valueOf(outlineThickness.getText()) ) );
				
				PeakShape shape = ((MartianObject) molecule).getShape(info.getTimePointIndex());
				
				boolean sourceInitialized = false;
				int xSource = 0; 
				int ySource = 0;
				int x1 = 0; 
				int y1 = 0;
				for (int pIndex = 0; pIndex < shape.x.length; pIndex++) {
					double x = shape.x[pIndex];
					double y = shape.y[pIndex];
					
					if (Double.isNaN(x) || Double.isNaN(y))
						continue;
					
					final double[] globalCoords = new double[] { x, y };
					final double[] viewerCoords = new double[ 2 ];
					transform.apply( globalCoords, viewerCoords );
					
					int xTarget =  ( int ) Math.round( viewerCoords[ 0 ] );
					int yTarget =  ( int ) Math.round( viewerCoords[ 1 ] );
					
					if (sourceInitialized)
						g.drawLine( xSource, ySource, xTarget, yTarget );
					else {
						x1 = xTarget;
						y1 = yTarget;
					}
					
					xSource = xTarget;
					ySource = yTarget;
					sourceInitialized = true;
				}
				
				if (x1 != xSource || y1 != ySource)
					g.drawLine( xSource, ySource, x1, y1 );
			}
		}
		
		private Color getColor()
		{
			int alpha = (int) info.getDisplayRangeMax();
			
			if (alpha > 255 || alpha < 0)
				alpha = 255;

			final int r = ARGBType.red( info.getColor().get() );
			final int g = ARGBType.green( info.getColor().get() );
			final int b = ARGBType.blue( info.getColor().get() );
			return new Color( r , g, b, alpha );
		}
	}

	@Override
	public JPanel getPanel() {
		return panel;
	}

	@Override
	protected void createIOMaps() {
		
		setJsonField("show", jGenerator -> {
			if (showObject != null) jGenerator.writeBooleanField("show", showObject.isSelected());
		}, jParser -> showObject.setSelected(jParser.getBooleanValue()));
		
		setJsonField("thickness", jGenerator -> {
			if (outlineThickness != null) jGenerator.writeStringField("thickness", outlineThickness.getText());
		}, jParser -> outlineThickness.setText(jParser.getText()));
	}
}
