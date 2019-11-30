package de.mpg.biochem.mars.fx.plot.tools;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet2D;
import de.gsi.dataset.spi.DoubleDataSet;
import javafx.scene.paint.Color;

public class MarsDoubleDataSet extends DoubleDataSet {
	
	private Color color;
	private double width;
	private String lineStyle;

	public MarsDoubleDataSet(String name, Color color, double width, String lineStyle) {
		super(name);
		this.color = color;
		this.width = width;
		this.lineStyle = lineStyle;
	}

    public MarsDoubleDataSet(final String name, final int initalSize, Color color, double width, String lineStyle) {
        super(name, initalSize);
        this.color = color;
        this.width = width;
        this.lineStyle = lineStyle;
    }

    public MarsDoubleDataSet(final DataSet2D another, Color color, double width, String lineStyle) {
        super(another); 
        this.color = color;
        this.width = width;
        this.lineStyle = lineStyle;
    }

    public MarsDoubleDataSet(final String name, final double[] xValues, final double[] yValues, final int initalSize,
            final boolean deepCopy, Color color, double width, String lineStyle) {
    	super(name, xValues, yValues, initalSize, deepCopy);
    	this.color = color;
    	this.width = width;
    	this.lineStyle = lineStyle;
    }
	
    public void setColor(Color color) {
    	this.color = color;
    }
    
    public Color getColor() {
    	return color;
    }
    
    public void setWidth(double width) {
    	this.width = width;
    }
    
    public double getWidth() {
    	return width;
    }
    
    public void setLineStyle(String lineStyle) {
    	this.lineStyle = lineStyle;
    }
    
    public String getLineStyle() {
    	return lineStyle;
    }
}
