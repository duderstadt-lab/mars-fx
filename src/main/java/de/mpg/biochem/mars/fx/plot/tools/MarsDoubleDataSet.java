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
