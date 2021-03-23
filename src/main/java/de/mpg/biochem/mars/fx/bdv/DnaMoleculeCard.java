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
package de.mpg.biochem.mars.fx.bdv;

import static java.util.stream.Collectors.toList;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;

import com.fasterxml.jackson.core.JsonParser;

import bdv.util.BdvOverlay;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.AbstractJsonConvertibleRecord;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.table.MarsTableRow;
import net.imagej.ops.Initializable;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.numeric.ARGBType;

@Plugin( type = MarsBdvCard.class, name = "DNA-Overlay" )
public class DnaMoleculeCard extends AbstractJsonConvertibleRecord implements MarsBdvCard, SciJavaPlugin, Initializable {

	private JTextField dnaThickness;
	private JCheckBox showDNA;
	
	private JPanel panel;
	
	private DnaMoleculeOverlay dnaMoleculeOverlay;
	private Molecule molecule;
	
	private boolean active = false;
	
	@Parameter
	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;
	
	@Override
	public void initialize() {
		panel = new JPanel();
		panel.setLayout(new GridLayout(0, 2));

		showDNA = new JCheckBox("show", false);
		panel.add(showDNA);
		panel.add(new JPanel());
		
		panel.add(new JLabel("thickness"));
		
		dnaThickness = new JTextField(6);
		dnaThickness.setText("5");
		Dimension dimScaleField = new Dimension(100, 20);
		dnaThickness.setMinimumSize(dimScaleField);
		
		panel.add(dnaThickness);
	}
	
	@Override
	public JPanel getPanel() {
		return panel;
	}

	@Override
	protected void createIOMaps() {
		
		setJsonField("show", jGenerator -> {
			if (showDNA != null) jGenerator.writeBooleanField("show", showDNA.isSelected());
		}, jParser -> showDNA.setSelected(jParser.getBooleanValue()));
		
		setJsonField("thickness", jGenerator -> {
			if (dnaThickness != null) jGenerator.writeStringField("thickness", dnaThickness.getText());
		}, jParser -> dnaThickness.setText(jParser.getText()));
	}
	
	public boolean showDNA() {
		return showDNA.isSelected();
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
		return "DNA-Overlay";
	}

	@Override
	public BdvOverlay getBdvOverlay() {
		if (dnaMoleculeOverlay == null)
			dnaMoleculeOverlay = new DnaMoleculeOverlay();
		
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
	
	public class DnaMoleculeOverlay extends BdvOverlay {
		
		public DnaMoleculeOverlay() { }
		
		@Override
		protected void draw(Graphics2D g) {
			if (showDNA.isSelected()) {
				AffineTransform2D transform = new AffineTransform2D();
				getCurrentTransform2D(transform);
				
				double x1 = molecule.getParameter("Dna_Top_x1"); 
				double y1 = molecule.getParameter("Dna_Top_y1");
				double x2 = molecule.getParameter("Dna_Bottom_x2"); 
				double y2 = molecule.getParameter("Dna_Bottom_y2");
					
				if (Double.isNaN(x1) || Double.isNaN(y1) || Double.isNaN(x2) || Double.isNaN(y2))
					return;
				
				final double[] globalCoords = new double[] { x1, y1 };
				final double[] viewerCoords = new double[ 2 ];
				transform.apply( globalCoords, viewerCoords );
				
				int xSource =  ( int ) Math.round( viewerCoords[ 0 ] );
				int ySource =  ( int ) Math.round( viewerCoords[ 1 ] );
				
				final double[] globalCoords2 = new double[] { x2, y2 };
				final double[] viewerCoords2 = new double[ 2 ];
				transform.apply( globalCoords2, viewerCoords2 );
				
				int xTarget =  ( int ) Math.round( viewerCoords2[ 0 ] );
				int yTarget =  ( int ) Math.round( viewerCoords2[ 1 ] );
				
				g.setColor(getColor());
				g.setStroke( new BasicStroke( Integer.valueOf(dnaThickness.getText()) ) );
				g.drawLine( xSource, ySource, xTarget, yTarget );
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
}
