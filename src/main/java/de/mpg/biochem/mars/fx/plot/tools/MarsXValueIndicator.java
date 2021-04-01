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

import de.gsi.chart.axes.Axis;
import de.gsi.chart.plugins.XValueIndicator;
import de.mpg.biochem.mars.fx.plot.DatasetOptionsPane;
import de.mpg.biochem.mars.fx.plot.MarsPlotPlugin;
import de.mpg.biochem.mars.fx.plot.event.NewMetadataRegionEvent;
import de.mpg.biochem.mars.fx.plot.event.NewMoleculeRegionEvent;
import de.mpg.biochem.mars.fx.plot.event.UpdateMetadataPositionEvent;
import de.mpg.biochem.mars.fx.plot.event.UpdateMoleculePositionEvent;
import de.mpg.biochem.mars.util.MarsPosition;
import de.mpg.biochem.mars.util.MarsRegion;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;

/**
 * ValueIndicator subclass with added MarsRecord update on drag functionality.
 *
 * @author Karl Duderstadt
 */

public class MarsXValueIndicator extends XValueIndicator implements MarsPlotPlugin {
	
    private DatasetOptionsPane datasetOptionsPane;

    /**
     * Creates a new instance of the indicator.
     *
     * @param axis the axis this indicator is associated with
     * @param value a X value to be indicated
     * @param datasetOptionsPane Reference to the datasetOptionsPane.
     */
    public MarsXValueIndicator(final Axis axis, final double value, DatasetOptionsPane datasetOptionsPane) {
        this(axis, value, null, datasetOptionsPane);
    }

    /**
     * Creates a new instance of the indicator.
     *
     * @param axis the axis this indicator is associated with
     * @param value a X value to be indicated
     * @param text the text to be shown by the label. Value of {@link #textProperty()}.
     * @param datasetOptionsPane Reference to the datasetOptionsPane.
     */
    public MarsXValueIndicator(final Axis axis, final double value, final String text, DatasetOptionsPane datasetOptionsPane) {
        super(axis, value, text);
        
        this.datasetOptionsPane = datasetOptionsPane;
     
        pickLine.setOnMouseClicked(this::handleMouseClickedEvent);
        triangle.setOnMouseClicked(this::handleMouseClickedEvent);
        label.setOnMouseClicked(this::handleMouseClickedEvent);
        
        pickLine.setOnMouseReleased(this::handleMouseReleaseEvent);
        triangle.setOnMouseReleased(this::handleMouseReleaseEvent);
        label.setOnMouseReleased(this::handleMouseReleaseEvent);
    }
    
    protected void handleMouseClickedEvent(final MouseEvent mouseEvent) {
        //Need to make sure to consume mouse click events if it is just a reposition event
        //Otherwise a new position is added each time, a position is moved
    	//System.out.println("Consuming click event");
        mouseEvent.consume();
    }
    
    protected void handleMouseReleaseEvent(final MouseEvent mouseEvent) {
        MarsPosition poi = new MarsPosition(getText());
        poi.setPosition(valueProperty().doubleValue());
        
        if (datasetOptionsPane.isMetadataIndicators())
        	getChart().fireEvent(new UpdateMetadataPositionEvent(poi));
        else if (datasetOptionsPane.isMoleculeIndicators())
        	getChart().fireEvent(new UpdateMoleculePositionEvent(poi));
    	
        mouseEvent.consume();
    }
    
	@Override
	public void setDatasetOptionsPane(DatasetOptionsPane datasetOptionsPane) {
		this.datasetOptionsPane = datasetOptionsPane;
	}
}
