package de.mpg.biochem.mars.fx.plot.tools;

import de.gsi.dataset.spi.DoubleDataSet;
import javafx.scene.paint.Color;

public class RegionDataSet extends DoubleDataSet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private double start, end;
	private double labelVerticalPosition;
	private double labelHorizontalPosition;
	private boolean xAxis = true;
	private Color color;
	
	public RegionDataSet(String name, Color color, double start, double end, double labelVerticalPosition, double labelHorizontalPosition) {
		super(name);
		this.color = color;
		this.start = start;
		this.end = end;
		this.labelVerticalPosition = labelVerticalPosition;
		this.labelHorizontalPosition = labelHorizontalPosition;
		
		setStyle("Region");
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
	
	public double getStart() {
		return start;
	}
	
	public double getEnd() {
		return end;
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
