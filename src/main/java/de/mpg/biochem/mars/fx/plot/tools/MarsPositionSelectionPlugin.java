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

import java.util.Objects;
import java.util.function.Predicate;

import de.gsi.chart.plugins.*;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.plugins.ChartPlugin;
import de.gsi.chart.ui.HiddenSidesPane;
import de.mpg.biochem.mars.fx.plot.DatasetOptionsPane;
import de.mpg.biochem.mars.fx.plot.MarsPlotPlugin;
import de.mpg.biochem.mars.fx.plot.event.NewMetadataPositionEvent;
import de.mpg.biochem.mars.fx.plot.event.NewMetadataRegionEvent;
import de.mpg.biochem.mars.fx.plot.event.NewMoleculePositionEvent;
import de.mpg.biochem.mars.fx.plot.event.NewMoleculeRegionEvent;
import de.mpg.biochem.mars.util.MarsPosition;
import de.mpg.biochem.mars.util.MarsRegion;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import de.gsi.chart.XYChart;

/**
 * Position along X or Y axis.
 * 
 * Position selection - triggered on {@link MouseEvent#MOUSE_PRESSED MOUSE_PRESSED}
 * event that is accepted by getPositionInMouseFilter() position-in filter. 
 * released.
 * 
 * @author Karl Duderstadt
 */
public class MarsPositionSelectionPlugin extends ChartPlugin implements MarsPlotPlugin {
    /**
     * Name of the CCS class of the region rectangle.
     */
    //private static final int FONT_SIZE = 20;
    
    private DatasetOptionsPane datasetOptionsPane;

    /**
     * Default region mouse filter passing on left mouse button (only).
     */
    public final Predicate<MouseEvent> defaultPositionSelectionMouseFilter = event -> MouseEventsHelper.isOnlyPrimaryButtonDown(
            event) && MouseEventsHelper.modifierKeysUp(event) && isMouseEventWithinCanvas(event);

    private Predicate<MouseEvent> positionSelectionMouseFilter = defaultPositionSelectionMouseFilter;

    private Point2D positionPoint = null;
    private ObservableList<Axis> omitAxisregion = FXCollections.observableArrayList();

    private final ObjectProperty<AxisMode> axisMode = new SimpleObjectProperty<AxisMode>(this, "axisMode",
            AxisMode.XY) {
        @Override
        protected void invalidated() {
            Objects.requireNonNull(get(), "The " + getName() + " must not be null");
        }
    };

    private Cursor originalCursor;

    private final EventHandler<MouseEvent> positonSelectionHandler = event -> {
        if (getPositionSelectionMouseFilter() == null || getPositionSelectionMouseFilter().test(event)) {
            positionSelected(event);
            event.consume();
        }
    };

    public MarsPositionSelectionPlugin() {
        this(AxisMode.X);
    }

    /**
     * Creates a new instance of region selector with animation disabled.
     *
     * @param axisMode
     *            initial value of {@link #axisModeProperty() regionMode} property
     */
    public MarsPositionSelectionPlugin(final AxisMode axisMode) {
        super();
        setAxisMode(axisMode);
        //setDragCursor(Cursor.CROSSHAIR);
        registerMouseHandlers();
    }

    /**
     * limits the mouse event position to the min/max range of the canavs (N.B.
     * event can occur to be negative/larger/outside than the canvas) This is to
     * avoid regioning outside the visible canvas range
     *
     * @param event
     *            the mouse event
     * @param plotBounds
     *            of the canvas
     * @return the clipped mouse location
     */
    private static Point2D limitToPlotArea(final MouseEvent event, final Bounds plotBounds) {
        final double limitedX = Math.max(Math.min(event.getX() - plotBounds.getMinX(), plotBounds.getMaxX()),
                plotBounds.getMinX());
        final double limitedY = Math.max(Math.min(event.getY() - plotBounds.getMinY(), plotBounds.getMaxY()),
                plotBounds.getMinY());
        return new Point2D(limitedX, limitedY);
    }

    /**
     * The mode defining axis along which the region can be performed. By default
     * initialised to {@link AxisMode#XY}.
     *
     * @return the axis mode property
     */
    public final ObjectProperty<AxisMode> axisModeProperty() {
        return axisMode;
    }

    /**
     * Returns the value of the {@link #axisModeProperty()}.
     *
     * @return current mode
     */
    public final AxisMode getAxisMode() {
        return axisModeProperty().get();
    }

    /**
     * Returns region-in mouse event filter.
     *
     * @return region-in mouse event filter
     */
    public Predicate<MouseEvent> getPositionSelectionMouseFilter() {
        return positionSelectionMouseFilter;
    }

    /**
     * @return list of axes that shall be ignored when performing region-in or outs
     */
    public final ObservableList<Axis> omitAxisregionList() {
        return omitAxisregion;
    }

    /**
     * Sets the value of the {@link #axisModeProperty()}.
     *
     * @param mode
     *            the mode to be used
     */
    public final void setAxisMode(final AxisMode mode) {
        axisModeProperty().set(mode);
    }

    /**
     * Sets filter on {@link MouseEvent#DRAG_DETECTED DRAG_DETECTED} events that
     * should start region-in operation.
     *
     * @param positionSelectionMouseFilter
     *            the filter to accept region-in mouse event. If {@code null} then
     *            any DRAG_DETECTED event will start region-in operation. By
     *            default it's set to defaultregionInMouseFilter.
     */
    public void setPositionSelectionMouseFilter(final Predicate<MouseEvent> positionSelectionMouseFilter) {
        this.positionSelectionMouseFilter = positionSelectionMouseFilter;
    }

    private void installCursor() {
        final Region chart = getChart();
        originalCursor = chart.getCursor();
        chart.setCursor(Cursor.CROSSHAIR);
    }

    private boolean isMouseEventWithinCanvas(final MouseEvent mouseEvent) {	
    	//System.out.println("source: " + mouseEvent.getSource());
    	//Now sure why but sometimes this is fired...
    	// and these are null.. For the moment we add this work around
    	if (getChart() == null)
    		return true;
    	else if (getChart().getCanvas() == null)
    		return true;
    	
    	//This will prevent creation of a new position during drag events.
    	//drag events always originate from the Scene...
    	if (!(mouseEvent.getSource() instanceof HiddenSidesPane)) {
    		//System.out.println("Event not from HiddenSidesPane");
    		return false;
    	}
    	
        final Canvas canvas = getChart().getCanvas();
        // listen to only events within the canvas
        final Point2D mouseLoc = new Point2D(mouseEvent.getScreenX(), mouseEvent.getScreenY());
        final Bounds screenBounds = canvas.localToScreen(canvas.getBoundsInLocal());
        return screenBounds.contains(mouseLoc);
    }

    private void registerMouseHandlers() {
        registerInputEventHandler(MouseEvent.MOUSE_CLICKED, positonSelectionHandler);
    }

    private void uninstallCursor() {
        getChart().setCursor(originalCursor);
    }

    private void positionSelected(final MouseEvent event) {
    	if (datasetOptionsPane.getTrackingSeries() == null)
    		return;
    	
    	positionPoint = new Point2D(event.getX(), event.getY());
    	
    	if (getChart() == null)
    		System.out.println("chart is null");
    	
    	if (positionPoint == null)
    		System.out.println("positionPoint is null");

    	// pixel coordinates w.r.t. plot area
        final Point2D positionPlotCoordinate = getChart().toPlotArea(positionPoint.getX(), positionPoint.getY());
        
        Axis axis = ((XYChart) getChart()).getXAxis();
        double xPosition = axis.getValueForDisplay(positionPlotCoordinate.getX());
        
        MarsPosition poi = new MarsPosition("Position");
        poi.setColumn(datasetOptionsPane.getTrackingSeries().getXColumn());
        poi.setPosition(xPosition);
        
        //We add the region to the metadata if those indicators are selected.
        //Otherwise we add it for molecule
        //If None is selected we add nothing.
        if (datasetOptionsPane.isMetadataIndicators())
        	getChart().fireEvent(new NewMetadataPositionEvent(poi));
        else if (datasetOptionsPane.isMoleculeIndicators())
        	getChart().fireEvent(new NewMoleculePositionEvent(poi));
    	
        positionPoint = null;
        //installCursor();
        event.consume();
    }

	@Override
	public void setDatasetOptionsPane(DatasetOptionsPane datasetOptionsPane) {
		this.datasetOptionsPane = datasetOptionsPane;
	}
}
