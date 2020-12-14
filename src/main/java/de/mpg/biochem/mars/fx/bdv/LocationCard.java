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
	private final JCheckBox showLocation, showLabel, roverSync, rainbowColor, showAll;
	
	private final JComboBox<String> locationSource, xLocation, yLocation;
	
	public LocationCard(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive) {
		setLayout(new GridLayout(0, 2));
		
		Set<String> columnNames = archive.properties().getColumnSet();
		Set<String> parameterNames = archive.properties().getParameterSet();
		
		add(new JLabel("Source"));
		locationSource = new JComboBox<>(new String[] {"Table", "Parameters"});
		add(locationSource);
		add(new JLabel("X"));
		xLocation = new JComboBox<>(columnNames.stream().sorted().collect(toList()).toArray(new String[0]));
		xLocation.setSelectedItem("x");
		add(xLocation);
		add(new JLabel("Y"));
		yLocation = new JComboBox<>(columnNames.stream().sorted().collect(toList()).toArray(new String[0]));
		yLocation.setSelectedItem("y");
		add(yLocation);
		
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
		
		showLocation = new JCheckBox("circle", false);
		add(showLocation);
		showLabel = new JCheckBox("label", false);
		add(showLabel);
		showAll = new JCheckBox("all", false);
		add(showAll);
		roverSync = new JCheckBox("rover sync", true);
		add(roverSync);
		rainbowColor = new JCheckBox("rainbow", false);
		add(rainbowColor);
		add(new JPanel());
		
		add(new JLabel("Radius"));
		
		radiusField = new JTextField(6);
		radiusField.setText("5");
		Dimension dimScaleField = new Dimension(100, 20);
		radiusField.setMinimumSize(dimScaleField);
		
		add(radiusField);
		add(new JLabel("Scale factor"));
		
		magnificationField = new JTextField(6);
		magnificationField.setText("10");
		magnificationField.setMinimumSize(dimScaleField);
		
		add(magnificationField);
	}
	
	public boolean showLocationOverlay() {
		return showLocation.isSelected();
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
