package de.mpg.biochem.mars.fx.bdv;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Optional;

import bdv.util.BdvOverlay;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.table.MarsTableRow;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.numeric.ARGBType;

public class MoleculeTrackOverlay extends BdvOverlay {

	private String tColumn;
	private String xColumn;
	private String yColumn;
	private boolean showLabel = false;
	private boolean showCircle = true;
	private boolean showTrack = false;
	private boolean showAll = false;
	private boolean rainbowColor = false;
	
	private double radius = 5;
	private Molecule selectedMolecule;
	private MarsBdvFrame<?> marsBdvFrame;
	
	private final MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;
	
	public MoleculeTrackOverlay(final MarsBdvFrame<?> marsBdvFrame, final MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive, 
			 final String xColumn, final String yColumn, final String tColumn) {
		this.archive = archive;
		this.xColumn = xColumn;
		this.yColumn = yColumn;
		this.tColumn = tColumn;
		
		this.marsBdvFrame = marsBdvFrame;
	}
	
	@Override
	protected void draw(Graphics2D g) {
		if (!showCircle && !showTrack && !showLabel)
			return;
		
		if (showAll)
			archive.molecules().forEach(molecule -> drawMolecule(g, molecule));
		else
			drawMolecule(g, selectedMolecule);
	}
	
	private void drawMolecule(Graphics2D g, Molecule molecule) {
		if (molecule != null) {
			if (molecule.getTable().hasColumn(xColumn) && molecule.getTable().hasColumn(yColumn) && molecule.getTable().hasColumn(tColumn)) {
				Color color = getColor();
				g.setColor(color);
				g.setStroke( new BasicStroke( 2 ) );
				
				if (showCircle) {
					Optional<MarsTableRow> currentRow = molecule.getTable().rows().filter(row -> row.getValue(tColumn) == info.getTimePointIndex()).findFirst();
					if (currentRow.isPresent()) {
						double x = currentRow.get().getValue(xColumn);
						double y = currentRow.get().getValue(yColumn);
						if (!Double.isNaN(x) && !Double.isNaN(y))
							drawOval(g, x, y);
					}
				}
				
				if (showTrack)
					drawTrack(g, molecule);
				
				if (showLabel)
					drawLabel(g, molecule, molecule.getTable().mean(xColumn), molecule.getTable().mean(yColumn));
					
			}
		}
	}
	
	private void drawOval(Graphics2D g, double x, double y) {
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
	}
	
	private void drawTrack(Graphics2D g, Molecule molecule) {
		AffineTransform2D transform = new AffineTransform2D();
		getCurrentTransform2D(transform);
		
		if (molecule.getTable().getRowCount() < 2)
			return;
		
		boolean sourceInitialized = false;
		int xSource = 0; 
		int ySource = 0;
		for (int row = 0; row < molecule.getTable().getRowCount(); row++) {
			double x = molecule.getTable().getValue(xColumn, row);
			double y = molecule.getTable().getValue(yColumn, row);
			
			if (Double.isNaN(x) || Double.isNaN(y))
				continue;
			
			final double[] globalCoords = new double[] { x, y };
			final double[] viewerCoords = new double[ 2 ];
			transform.apply( globalCoords, viewerCoords );
			
			int xTarget =  ( int ) Math.round( viewerCoords[ 0 ] );
			int yTarget =  ( int ) Math.round( viewerCoords[ 1 ] );
			
			if (sourceInitialized)
				g.drawLine( xSource, ySource, xTarget, yTarget );
			
			xSource = xTarget;
			ySource = yTarget;
			sourceInitialized = true;
		}
	}
	
	private void drawLabel(Graphics2D g, Molecule molecule, double x, double y) {
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
		final int tx = ( int ) ( viewerCoords[ 0 ] + arad + 5 );
		final int ty = ( int ) viewerCoords[ 1 ];
		g.drawString( molecule.getUID().substring(0, 6), tx, ty );
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
	
	public void setTColumn(String tColumn) {
		this.tColumn = tColumn;
	}
	
	public void setXColumn(String xColumn) {
		this.xColumn = xColumn;
	}
	
	public void setYColumn(String yColumn) {
		this.yColumn = yColumn;
	}
	
	public void setCircleVisible(boolean showCircle) {
		this.showCircle = showCircle;
	}
	
	public void setTrackVisible(boolean showTrack) {
		this.showTrack = showTrack;
	}
	
	public void setLabelVisible(boolean showLabel) {
		this.showLabel = showLabel;
	}
	
	public void setShowAll(boolean showAll) {
		this.showAll = showAll;
	}
	
	public void setRainbowColor(boolean rainbowColor) {
		this.rainbowColor = rainbowColor;
	}
}
