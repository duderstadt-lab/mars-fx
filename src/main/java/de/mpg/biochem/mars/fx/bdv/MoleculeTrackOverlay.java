package de.mpg.biochem.mars.fx.bdv;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Optional;

import bdv.util.BdvOverlay;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.table.MarsTableRow;
import net.imglib2.realtransform.AffineTransform2D;

public class MoleculeTrackOverlay extends BdvOverlay {

	private final String xLocation;
	private final String yLocation;
	private boolean useProperties;
	
	private double radius = 5;
	private Molecule molecule;
	
	public MoleculeTrackOverlay(final String xLocation, final String yLocation, final boolean useProperties) {
		this.xLocation = xLocation;
		this.yLocation = yLocation;
		this.useProperties = useProperties;
	}
	
	@Override
	protected void draw(Graphics2D g) {
		if (molecule != null) {
			if (useProperties && molecule.hasParameter(xLocation) && molecule.hasParameter(yLocation)) {
				double x = molecule.getParameter(xLocation);
				double y = molecule.getParameter(yLocation);
				if (!Double.isNaN(x) && !Double.isNaN(y))
					drawOval(g, x, y);
			} else if (molecule.getTable().hasColumn(xLocation) && molecule.getTable().hasColumn(yLocation) && molecule.getTable().hasColumn("T")) {
				Optional<MarsTableRow> currentRow = molecule.getTable().rows().filter(row -> row.getValue("T") == info.getTimePointIndex()).findFirst();
				if (currentRow.isPresent()) {
					double x = currentRow.get().getValue(xLocation);
					double y = currentRow.get().getValue(yLocation);
					if (!Double.isNaN(x) && !Double.isNaN(y))
						drawOval(g, x, y);
				}
			}
		}
	}
	
	private void drawOval(Graphics2D g, double x, double y) {
		g.setColor( Color.CYAN.darker() );
		g.setStroke( new BasicStroke( 2 ) );
		
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

		//final int tx = ( int ) ( viewerCoords[ 0 ] + arad + 5 );
		//final int ty = ( int ) viewerCoords[ 1 ];
		//g.drawString( molecule.getUID(), tx, ty );
	}

	public void setMolecule(Molecule molecule) {
		this.molecule = molecule;
	}
	
	public void setRadius(double radius) {
		this.radius = radius;
	}
}
