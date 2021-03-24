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

@Plugin( type = MarsBdvCard.class, name = "Location" )
public class LocationCard extends AbstractJsonConvertibleRecord implements MarsBdvCard, SciJavaPlugin, Initializable {

	private JPanel panel;
	private JTextField magnificationField, radiusField;
	private JCheckBox showCircle, showTrack, followTrack, showLabel, roverSync, rainbowColor, showAll;
	
	private JComboBox<String> locationSource, tLocation, xLocation, yLocation;
	
	private MoleculeLocationOverlay moleculeLocationOverlay;
	private Molecule molecule;
	
	private boolean active = false;
	
	public static HashMap<String, Color> moleculeRainbowColors;
	public Random ran = new Random();
	
	@Parameter
	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;
	
	@Override
	public void initialize() {
		panel = new JPanel();
		panel.setLayout(new GridLayout(0, 2));

		Set<String> columnNames = archive.properties().getColumnSet();
		Set<String> parameterNames = archive.properties().getParameterSet();
		
		panel.add(new JLabel("Source"));
		locationSource = new JComboBox<>(new String[] {"Table", "Parameters"});
		panel.add(locationSource);
		panel.add(new JLabel("T"));
		tLocation = new JComboBox<>(columnNames.stream().sorted().collect(toList()).toArray(new String[0]));
		tLocation.setSelectedItem("T");
		panel.add(tLocation);
		panel.add(new JLabel("X"));
		xLocation = new JComboBox<>(columnNames.stream().sorted().collect(toList()).toArray(new String[0]));
		xLocation.setSelectedItem("x");
		panel.add(xLocation);
		panel.add(new JLabel("Y"));
		yLocation = new JComboBox<>(columnNames.stream().sorted().collect(toList()).toArray(new String[0]));
		yLocation.setSelectedItem("y");
		panel.add(yLocation);
		
		locationSource.addActionListener(new ActionListener( ) {
		      public void actionPerformed(ActionEvent e) {
		        String sourceName = (String)locationSource.getSelectedItem();
		        Set<String> sourceItemNames = (sourceName.equals("Table")) ? columnNames : parameterNames;
		        
		        tLocation.removeAllItems();
		        xLocation.removeAllItems();
		    	yLocation.removeAllItems();  
		        
		    	for (String item : sourceItemNames.stream().sorted().collect(toList())) {
					tLocation.addItem(item);
					xLocation.addItem(item);
					yLocation.addItem(item);
		    	}
						
				if (!sourceName.equals("Parameters")) {
					tLocation.setSelectedItem("T");
					xLocation.setSelectedItem("x");
					yLocation.setSelectedItem("y");
				}
			}
		});
		
		showCircle = new JCheckBox("circle", false);
		panel.add(showCircle);
		showLabel = new JCheckBox("label", false);
		panel.add(showLabel);
		showTrack = new JCheckBox("track", false);
		panel.add(showTrack);
		followTrack = new JCheckBox("follow", false);
		panel.add(followTrack);
		showAll = new JCheckBox("all", false);
		panel.add(showAll);
		roverSync = new JCheckBox("rover sync", false);
		panel.add(roverSync);
		rainbowColor = new JCheckBox("rainbow", false);
		panel.add(rainbowColor);
		panel.add(new JPanel());
		
		panel.add(new JLabel("Radius"));
		
		radiusField = new JTextField(6);
		radiusField.setText("5");
		Dimension dimScaleField = new Dimension(100, 20);
		radiusField.setMinimumSize(dimScaleField);
		
		panel.add(radiusField);
		panel.add(new JLabel("Scale factor"));
		
		magnificationField = new JTextField(6);
		magnificationField.setText("10");
		magnificationField.setMinimumSize(dimScaleField);
		
		panel.add(magnificationField);
	}
	
	public JPanel getPanel() {
		return panel;
	}
	
	public boolean showLocationOverlay() {
		if (showCircle.isSelected() || showTrack.isSelected() || followTrack.isSelected() || showLabel.isSelected())
			return true;
		else
			return false;
	}
	
	public boolean showCircle() {
		return showCircle.isSelected();
	}
	
	public boolean showLabel() {
		return showLabel.isSelected();
	}
	
	public boolean showAll() {
		return showAll.isSelected();
	}
	
	public boolean roverSync() {
		return roverSync.isSelected();
	}
	
	public boolean rainbowColor() {
		return rainbowColor.isSelected();
	}
	
	public boolean showTrack() {
		return showTrack.isSelected();
	}
	
	public boolean followTrack() {
		return followTrack.isSelected();
	}
	
	public String getTLocationSource() {
		return (String) tLocation.getSelectedItem();
	}
	
	public String getXLocationSource() {
		return (String) xLocation.getSelectedItem();
	}
	
	public String getYLocationSource() {
		return (String) yLocation.getSelectedItem();
	}
	
	public boolean useParameters() {
		return locationSource.getSelectedItem().equals("Parameters");
	}

	public double getMagnification() {
		return Double.valueOf(magnificationField.getText());
	}
	
	public double getRadius() {
		return Double.valueOf(radiusField.getText());
	}
	
	@Override
	protected void createIOMaps() {

		setJsonField("locationSource", jGenerator -> {
			if (locationSource != null) jGenerator.writeStringField("locationSource", (String) locationSource.getSelectedItem());
		}, jParser -> locationSource.setSelectedItem(jParser.getText()));
		
		setJsonField("tLocation", jGenerator -> {
			if (tLocation != null) jGenerator.writeStringField("tLocation", (String) tLocation.getSelectedItem());
		}, jParser -> tLocation.setSelectedItem(jParser.getText()));
		
		setJsonField("xLocation", jGenerator -> {
			if (xLocation != null) jGenerator.writeStringField("xLocation", (String) xLocation.getSelectedItem());
		}, jParser -> xLocation.setSelectedItem(jParser.getText()));
		
		setJsonField("yLocation", jGenerator -> {
			if (yLocation != null) jGenerator.writeStringField("yLocation", (String) yLocation.getSelectedItem());
		}, jParser -> yLocation.setSelectedItem(jParser.getText()));
		
		setJsonField("showCircle", jGenerator -> {
			if (showCircle != null) jGenerator.writeBooleanField("showCircle", showCircle.isSelected());
		}, jParser -> showCircle.setSelected(jParser.getBooleanValue()));
		
		setJsonField("showTrack", jGenerator -> {
			if (showTrack != null) jGenerator.writeBooleanField("showTrack", showTrack.isSelected());
		}, jParser -> showTrack.setSelected(jParser.getBooleanValue()));
		
		setJsonField("followTrack", jGenerator -> {
			if (followTrack != null) jGenerator.writeBooleanField("followTrack", followTrack.isSelected());
		}, jParser -> followTrack.setSelected(jParser.getBooleanValue()));
		
		setJsonField("showLabel", jGenerator -> {
			if (showLabel != null) jGenerator.writeBooleanField("showLabel", showLabel.isSelected());
		}, jParser -> showLabel.setSelected(jParser.getBooleanValue()));
		
		setJsonField("roverSync", jGenerator -> {
			if (roverSync != null) jGenerator.writeBooleanField("roverSync", roverSync.isSelected());
		}, jParser -> roverSync.setSelected(jParser.getBooleanValue()));
		
		setJsonField("rainbowColor", jGenerator -> {
			if (rainbowColor != null) jGenerator.writeBooleanField("rainbowColor", rainbowColor.isSelected());
		}, jParser -> rainbowColor.setSelected(jParser.getBooleanValue()));
		
		setJsonField("showAll", jGenerator -> {
			if (showAll != null) jGenerator.writeBooleanField("showAll", showAll.isSelected());
		}, jParser -> showAll.setSelected(jParser.getBooleanValue()));
		
		setJsonField("magnification", jGenerator -> {
			if (magnificationField != null) jGenerator.writeStringField("magnification", magnificationField.getText());
		}, jParser -> magnificationField.setText(jParser.getText()));
		
		setJsonField("radius", jGenerator -> {
			if (magnificationField != null) jGenerator.writeStringField("radius", radiusField.getText());
		}, jParser -> radiusField.setText(jParser.getText()));
		
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
		return "Location";
	}

	@Override
	public BdvOverlay getBdvOverlay() {
		if (moleculeLocationOverlay == null)
			moleculeLocationOverlay = new MoleculeLocationOverlay();
		
		return moleculeLocationOverlay;
	}

	@Override
	public boolean isActive() {
		return active;
	}

	@Override
	public void setActive(boolean active) {
		this.active = active;
	}
	
	public class MoleculeLocationOverlay extends BdvOverlay {
		
		public MoleculeLocationOverlay() {
		}
		
		@Override
		protected void draw(Graphics2D g) {
			if (!showCircle.isSelected())
				return;
			
			if (showAll.isSelected())
				archive.molecules().forEach(molecule -> drawMolecule(g, molecule));
			else
				drawMolecule(g, molecule);
		}
		
		private void drawMolecule(Graphics2D g, Molecule molecule) {
			if (molecule != null) {
				Color color = (rainbowColor()) ? getMoleculeColor(molecule.getUID()) : getColor();
				g.setColor(color);
				g.setStroke( new BasicStroke( 2 ) );
				
				double centerX = Double.NaN;
				double centerY = Double.NaN;
				
				if (useParameters() && molecule.hasParameter(getXLocationSource()) && molecule.hasParameter(getYLocationSource())) {
					centerX = molecule.getParameter(getXLocationSource());
					centerY = molecule.getParameter(getYLocationSource());
				} else if (molecule.getTable().hasColumn(getXLocationSource()) && molecule.getTable().hasColumn(getYLocationSource())) {
					centerX = molecule.getTable().mean(getXLocationSource());
					centerY = molecule.getTable().mean(getYLocationSource());
				} else
					return;
				
				if (Double.isNaN(centerX) || Double.isNaN(centerY))
					return;
				
				if (showTrack())
					drawTrack(g, molecule);
				
				if (showLabel())
					drawLabel(g, molecule.getUID().substring(0, 6), centerX, centerY);
				
				if (followTrack() && molecule.getTable().hasColumn(getXLocationSource()) && molecule.getTable().hasColumn(getYLocationSource()) && molecule.getTable().hasColumn(getTLocationSource())) {
					Optional<MarsTableRow> currentRow = molecule.getTable().rows().filter(row -> row.getValue(getTLocationSource()) == info.getTimePointIndex()).findFirst();
					if (currentRow.isPresent()) {
						double x = currentRow.get().getValue(getXLocationSource());
						double y = currentRow.get().getValue(getYLocationSource());
						if (!Double.isNaN(x) && !Double.isNaN(y))
							drawOval(g, x, y);
					}
				} else if (showCircle())
					drawOval(g, centerX, centerY);
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

			final double rad = getRadius() * transformScale;

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
				double x = molecule.getTable().getValue(getXLocationSource(), row);
				double y = molecule.getTable().getValue(getYLocationSource(), row);
				
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
		
		private void drawLabel(Graphics2D g, String uid, double x, double y) {
			AffineTransform2D transform = new AffineTransform2D();
			getCurrentTransform2D(transform);

			final double vx = transform.get( 0, 0 );
			final double vy = transform.get( 1, 0 );
			final double transformScale = Math.sqrt( vx * vx + vy * vy );

			final double[] globalCoords = new double[] { x, y };
			final double[] viewerCoords = new double[ 2 ];
			transform.apply( globalCoords, viewerCoords );

			final double rad = getRadius() * transformScale;

			final double arad = Math.sqrt( rad * rad );
			final int tx = ( int ) ( viewerCoords[ 0 ] + arad + 5 );
			final int ty = ( int ) viewerCoords[ 1 ];
			g.drawString( uid, tx, ty );
		}
		
		public synchronized Color getMoleculeColor(String UID) {
			if (moleculeRainbowColors == null) {
				moleculeRainbowColors = new HashMap<String, Color>();
				archive.molecules().forEach(m -> moleculeRainbowColors.put(m.getUID(), new Color(ran.nextFloat(), ran.nextFloat(), ran.nextFloat())));
			} else if (!moleculeRainbowColors.containsKey(UID)) {
				moleculeRainbowColors.put(UID, new Color(ran.nextFloat(), ran.nextFloat(), ran.nextFloat()));
			}
			
			return moleculeRainbowColors.get(UID);
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
