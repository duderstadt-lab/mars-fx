package de.mpg.biochem.mars.fx.bdv;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Optional;
import java.util.Random;

import bdv.util.BdvOverlay;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.table.MarsTableRow;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.numeric.ARGBType;

public class MoleculeLocationOverlay extends BdvOverlay {

	private String xLocation;
	private String yLocation;
	private boolean showCircle = false;
	private boolean useParameters = false;
	private boolean showLabel = false;
	private boolean showAll = false;
	private boolean rainbowColor = false;
	
	private double radius = 5;
	private Molecule selectedMolecule;
	
	private final MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;
	
	public MoleculeLocationOverlay(MarsBdvFrame<?> marsBdvFrame, MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive,
			final boolean useProperties, final boolean showLabel, final String xLocation, final String yLocation) {
		this.archive = archive;
		this.xLocation = xLocation;
		this.yLocation = yLocation;
		this.useParameters = useProperties;
		this.showLabel = showLabel;
	}
	
	@Override
	protected void draw(Graphics2D g) {
		if (!showCircle)
			return;
		
		if (showAll)
			archive.molecules().forEach(molecule -> drawMolecule(g, molecule));
		else
			drawMolecule(g, selectedMolecule);
	}
	
	private void drawMolecule(Graphics2D g, Molecule molecule) {
		if (molecule != null) {
			
			//Need to implement that here!!!! Use a static map!!
			
			Color color = (rainbowColor) ? marsBdvFrame.getMoleculeColor(molecule.getUID()) : getColor();
			g.setColor(color);
			g.setStroke( new BasicStroke( 2 ) );
			
			if (useParameters && molecule.hasParameter(xLocation) && molecule.hasParameter(yLocation)) {
				double x = molecule.getParameter(xLocation);
				double y = molecule.getParameter(yLocation);
				if (!Double.isNaN(x) && !Double.isNaN(y))
					drawOval(g, molecule, x, y);
			} else if (molecule.getTable().hasColumn(xLocation) && molecule.getTable().hasColumn(yLocation)) {
				double x = molecule.getTable().mean(xLocation);
				double y = molecule.getTable().mean(yLocation);
				if (!Double.isNaN(x) && !Double.isNaN(y))
					drawOval(g, molecule, x, y);
			}
		}
	}
	
	private void drawOval(Graphics2D g, Molecule molecule, double x, double y) {
		AffineTransform2D transform = new AffineTransform2D();
		getCurrentTransform2D(transform);

		final double vx = transform.get( 0, 0 );
		final double vy = transform.get( 1, 0 );
		final double transformScale = Math.sqrt( vx * vx + vy * vy );

		final double[] globalCoords = new double[] { x, y };
		final double[] viewerCoords = new double[ 2 ];
		transform.apply( globalCoords, viewerCoords );

		final double rad = radius * transformScale;

		final double arad = Math.sqrt( rad * rad );
		g.drawOval( ( int ) ( viewerCoords[ 0 ] - arad ), ( int ) ( viewerCoords[ 1 ] - arad ), ( int ) ( 2 * arad ), ( int ) ( 2 * arad ) );

		if (showLabel) {
			final int tx = ( int ) ( viewerCoords[ 0 ] + arad + 5 );
			final int ty = ( int ) viewerCoords[ 1 ];
			g.drawString( molecule.getUID().substring(0, 6), tx, ty );
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

	public void setMolecule(Molecule selectedMolecule) {
		this.selectedMolecule = selectedMolecule;
	}
	
	public void setRadius(double radius) {
		this.radius = radius;
	}
	
	public void useParameters(boolean useParameters) {
		this.useParameters = useParameters;
	}
	
	public void setXLocation(String xLocation) {
		this.xLocation = xLocation;
	}
	
	public void setYLocation(String yLocation) {
		this.yLocation = yLocation;
	}
	
	public void setLabelVisible(boolean showLabel) {
		this.showLabel = showLabel;
	}
	
	public void setShowAll(boolean showAll) {
		this.showAll = showAll;
	}
	
	public void setShowCircle(boolean showCircle) {
		this.showCircle = showCircle;
	}
	
	public void setRainbowColor(boolean rainbowColor) {
		this.rainbowColor = rainbowColor;
	}
}
