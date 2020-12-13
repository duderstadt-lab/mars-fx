package de.mpg.biochem.mars.fx.bdv;

import static java.util.stream.Collectors.toList;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;

public class LocationCard extends JPanel {

	private final JTextField magnificationField, radiusField;
	private final JCheckBox showLocation, showLabel, roverSync;
	
	private final JComboBox<String> locationSource, xLocation, yLocation;
	
	public LocationCard(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive) {
		setLayout(new GridLayout(4, 1));
		
		Set<String> columnNames = archive.properties().getColumnSet();
		Set<String> parameterNames = archive.properties().getParameterSet();
		
		JPanel p1 = new JPanel();
		p1.add(new JLabel("Source"));
		locationSource = new JComboBox<>(new String[] {"Table", "Parameters"});
		p1.add(locationSource);
		p1.add(new JLabel(" X "));
		xLocation = new JComboBox<>(columnNames.stream().sorted().collect(toList()).toArray(new String[0]));
		xLocation.setSelectedItem("x");
		p1.add(xLocation);
		p1.add(new JLabel(" Y "));
		yLocation = new JComboBox<>(columnNames.stream().sorted().collect(toList()).toArray(new String[0]));
		yLocation.setSelectedItem("y");
		p1.add(yLocation);
		
		locationSource.addActionListener(new ActionListener( ) {
		      public void actionPerformed(ActionEvent e) {
		        String sourceName = (String)locationSource.getSelectedItem();
		        
		        xLocation.removeAllItems();
		    	yLocation.removeAllItems();  
		        
				if (sourceName.equals("Parameters")) {
					for (String item : parameterNames.stream().sorted().collect(toList()))
						xLocation.addItem(item);
			    	
					for (String item : parameterNames.stream().sorted().collect(toList()))
						yLocation.addItem(item);
				} else {
					for (String item : columnNames.stream().sorted().collect(toList()))
						xLocation.addItem(item);
					xLocation.setSelectedItem("x");
			    	
					for (String item : columnNames.stream().sorted().collect(toList()))
						yLocation.addItem(item);
					yLocation.setSelectedItem("y");
				}
			}
		});
		
		JPanel p2 = new JPanel();
		showLocation = new JCheckBox("show", true);
		p2.add(showLocation);
		showLabel = new JCheckBox("label", true);
		p2.add(showLabel);
		roverSync = new JCheckBox("Rover sync", true);
		p2.add(roverSync);
		
		JPanel p3 = new JPanel();
		p3.add(new JLabel("Radius "));
		
		radiusField = new JTextField(6);
		radiusField.setText("5");
		Dimension dimScaleField = new Dimension(100, 20);
		radiusField.setMinimumSize(dimScaleField);
		
		p3.add(radiusField);
		
		p3.add(new JLabel("Zoom "));
		
		magnificationField = new JTextField(6);
		magnificationField.setText("10");
		magnificationField.setMinimumSize(dimScaleField);
		
		p3.add(magnificationField);
		
		add(p1);
		add(p2);
		add(p3);
	}
	
	public boolean showLocationOverlay() {
		return showLocation.isSelected();
	}
	
	public boolean showLabel() {
		return showLabel.isSelected();
	}
	
	public boolean roverSync() {
		return roverSync.isSelected();
	}
	
	public String getXLocation() {
		return (String) xLocation.getSelectedItem();
	}
	
	public String getYLocation() {
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
}
