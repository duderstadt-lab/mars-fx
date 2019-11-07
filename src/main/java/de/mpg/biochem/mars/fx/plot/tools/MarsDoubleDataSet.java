package de.mpg.biochem.mars.fx.plot.tools;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.DoubleDataSet;
import javafx.scene.paint.Color;

public class MarsDoubleDataSet extends DoubleDataSet {
	
	private Color color;
	private double width;

	public MarsDoubleDataSet(String name, Color color, double width) {
		super(name);
		this.color = color;
		this.width = width;
	}

    public MarsDoubleDataSet(final String name, final int initalSize, Color color, double width) {
        super(name, initalSize);
        this.color = color;
        this.width = width;
    }

    public MarsDoubleDataSet(final DataSet another, Color color, double width) {
        super(another); 
        this.color = color;
        this.width = width;
    }

    public MarsDoubleDataSet(final String name, final double[] xValues, final double[] yValues, final int initalSize,
            final boolean deepCopy, Color color, double width) {
    	super(name, xValues, yValues, initalSize, deepCopy);
    	this.color = color;
    	this.width = width;
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
}
