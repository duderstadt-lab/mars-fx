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

public class TracksCard extends JPanel {

	private final JTextField radiusField;
	private final JCheckBox showCircle, showTrack, showLabel, rainbowColor, showAll;
	
	private final JComboBox<String> tColumn, xColumn, yColumn;
	
	public TracksCard(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive) {
		setLayout(new GridLayout(0, 2));
		
		Set<String> columnNames = archive.properties().getColumnSet();
		
		add(new JLabel("T"));
		tColumn = new JComboBox<>(columnNames.stream().sorted().collect(toList()).toArray(new String[0]));
		tColumn.setSelectedItem("T");
		add(tColumn);
		
		add(new JLabel("X"));
		xColumn = new JComboBox<>(columnNames.stream().sorted().collect(toList()).toArray(new String[0]));
		xColumn.setSelectedItem("x");
		add(xColumn);
		
		add(new JLabel("Y"));
		yColumn = new JComboBox<>(columnNames.stream().sorted().collect(toList()).toArray(new String[0]));
		yColumn.setSelectedItem("y");
		add(yColumn);
		
		showTrack = new JCheckBox("track", false);
		add(showTrack);
		showLabel = new JCheckBox("label", false);
		add(showLabel);
		showCircle = new JCheckBox("circle", false);
		add(showCircle);
		showAll = new JCheckBox("all", false);
		add(showAll);
		rainbowColor = new JCheckBox("rainbow", false);
		add(rainbowColor);
		add(new JPanel());
		
		add(new JLabel("Radius"));
		
		radiusField = new JTextField(6);
		radiusField.setText("5");
		Dimension dimScaleField = new Dimension(100, 20);
		radiusField.setMinimumSize(dimScaleField);
		
		add(radiusField);
	}
	
	public void fillMoleculeTrackOverlaySettings(MoleculeTrackOverlay moleculeTrackOverlay) {
		moleculeTrackOverlay.setXColumn(getXColumn());
		moleculeTrackOverlay.setYColumn(getYColumn());
		moleculeTrackOverlay.setTColumn(getTColumn());
		moleculeTrackOverlay.setCircleVisible(showCircle());
		moleculeTrackOverlay.setLabelVisible(showLabel());
		moleculeTrackOverlay.setTrackVisible(showTrack());
		moleculeTrackOverlay.setShowAll(showAll());
		moleculeTrackOverlay.setRainbowColor(rainbowColor());
		moleculeTrackOverlay.setRadius(getRadius());
	}
	
	public boolean showCircle() {
		return showCircle.isSelected();
	}
	
	public boolean rainbowColor() {
		return rainbowColor.isSelected();
	}
	
	public boolean showLabel() {
		return showLabel.isSelected();
	}
	
	public boolean showAll() {
		return showAll.isSelected();
	}
	
	public boolean showTrack() {
		return showTrack.isSelected();
	}
	
	public String getTColumn() {
		return (String) tColumn.getSelectedItem();
	}
	
	public String getXColumn() {
		return (String) xColumn.getSelectedItem();
	}
	
	public String getYColumn() {
		return (String) yColumn.getSelectedItem();
	}
	
	public double getRadius() {
		return Double.valueOf(radiusField.getText());
	}
}
