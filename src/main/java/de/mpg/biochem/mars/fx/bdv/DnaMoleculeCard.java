package de.mpg.biochem.mars.fx.bdv;

import static java.util.stream.Collectors.toList;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import bdv.util.BdvOverlay;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.table.MarsTableRow;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.numeric.ARGBType;

public class DnaMoleculeCard extends JPanel implements MarsBdvCard {

	private final JTextField dnaThickness;
	private final JCheckBox showDNA;
	
	private DnaMoleculeOverlay dnaMoleculeOverlay;
	private MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;
	private Molecule molecule;
	
	private boolean active = false;
	
	public DnaMoleculeCard(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive) {
		this.archive = archive;
		setLayout(new GridLayout(0, 2));

		showDNA = new JCheckBox("show", false);
		add(showDNA);
		add(new JPanel());
		
		add(new JLabel("Thickness"));
		
		dnaThickness = new JTextField(6);
		dnaThickness.setText("5");
		Dimension dimScaleField = new Dimension(100, 20);
		dnaThickness.setMinimumSize(dimScaleField);
		
		add(dnaThickness);
	}
	
	public boolean showDNA() {
		return showDNA.isSelected();
	}
	
	@Override
	public void setMolecule(Molecule molecule) {
		this.molecule = molecule;
	}

	@Override
	public String getCardName() {
		return "DNA";
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
