package de.mpg.biochem.mars.fx.plot.tools;

import de.gsi.dataset.spi.DoubleDataSet;
import javafx.scene.paint.Color;

public class PositionDataSet extends DoubleDataSet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private double position;
	private double labelVerticalPosition;
	private double labelHorizontalPosition;
	private boolean xAxis = true;
	private Color color;
	
	public PositionDataSet(String name, Color color, double position, double labelVerticalPosition, double labelHorizontalPosition) {
		super(name);
		this.color = color;
		this.position = position;
		this.labelVerticalPosition = labelVerticalPosition;
		this.labelHorizontalPosition = labelHorizontalPosition;
		
		setStyle("Position");
	}
	
	public void setLabelVerticalPosition(double labelVerticalPosition) {
		this.labelVerticalPosition = labelVerticalPosition;
	}
	
	public double getLabelVerticalPosition() {
		return labelVerticalPosition;
	}
	
	public void setLabelHorizontalPosition(double labelHorizontalPosition) {
		this.labelHorizontalPosition = labelHorizontalPosition;
	}
	
	public double getLabelHorizontalPosition() {
		return labelHorizontalPosition;
	}
	
	public void setColor(Color color) {
		this.color = color;
	}
	
	public Color getColor() {
		return color;
	}
	
	public double getPosition() {
		return position;
	}
	
	public void setXAxis() {
		xAxis = true;
	}
	
	public void setYAxis() {
		xAxis = false;
	}
	
	public boolean isXAxis() {
		return xAxis;
	}
	
	public boolean isYAxis() {
		return !xAxis;
	}
}
