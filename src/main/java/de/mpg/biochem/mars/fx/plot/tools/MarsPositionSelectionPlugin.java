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
import de.gsi.dataset.DataSet;
import de.gsi.chart.plugins.ChartPlugin;
import de.gsi.chart.ui.HiddenSidesPane;
import de.mpg.biochem.mars.fx.plot.DatasetOptionsPane;
import de.mpg.biochem.mars.fx.plot.MarsPlotPlugin;
import de.mpg.biochem.mars.fx.plot.event.NewMetadataPositionEvent;
import de.mpg.biochem.mars.fx.plot.event.NewMetadataRegionEvent;
import de.mpg.biochem.mars.fx.plot.event.NewMoleculePositionEvent;
import de.mpg.biochem.mars.fx.plot.event.NewMoleculeRegionEvent;
import de.mpg.biochem.mars.fx.plot.tools.MarsDataPointTracker.DataPoint;
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

import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import de.gsi.chart.Chart;

import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;


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
    

    //Tooltip variables

    public static final String STYLE_CLASS_LABEL = "chart-datapoint-tooltip-label";

    private static final int LABEL_X_OFFSET = 15;
    private static final int LABEL_Y_OFFSET = 5;

    private final Label label = new Label();
    private final Circle circle = new Circle();

    private Point2D trackingDataPointScreen;
    private DataPoint positionPoint = null;
    private DataPoint currentTrackingDataPoint;

    private final EventHandler<MouseEvent> trackMouseMoveHandler = this::updateToolTip;

    /**
     * Default region mouse filter passing on left mouse button (only).
     */
    public final Predicate<MouseEvent> defaultPositionSelectionMouseFilter = event -> MouseEventsHelper.isOnlyPrimaryButtonDown(
            event) && MouseEventsHelper.modifierKeysUp(event) && isMouseEventWithinCanvas(event);

    private Predicate<MouseEvent> positionSelectionMouseFilter = defaultPositionSelectionMouseFilter;

    private ObservableList<Axis> omitAxisregion = FXCollections.observableArrayList();

    private final ObjectProperty<AxisMode> axisMode = new SimpleObjectProperty<AxisMode>(this, "axisMode",
            AxisMode.XY) {
        @Override
        protected void invalidated() {
            Objects.requireNonNull(get(), "The " + getName() + " must not be null");
        }
    };

    private final EventHandler<MouseEvent> positonSelectionHandler = event -> {
        if (getPositionSelectionMouseFilter() == null || getPositionSelectionMouseFilter().test(event)) {
            positionSelected(event);
            event.consume();
        }
    };
    
    private final EventHandler<KeyEvent> keyPressedHandler = event -> {
        if (event.getCode() == KeyCode.P && trackingDataPointScreen != null) {
        	positionSelected(trackingDataPointScreen.getX(), trackingDataPointScreen.getY());
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
        registerEventHandlers();
        
        label.getStyleClass().add(MarsDataPointTracker.STYLE_CLASS_LABEL);
        circle.setRadius(5.0f);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.RED);
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

    private boolean isMouseEventWithinCanvas(final MouseEvent mouseEvent) {	
    	//Now sure why but sometimes this is fired...
    	// and these are null.. For the moment we add this work around
    	if (getChart() == null)
    		return false;
    	else if (getChart().getCanvas() == null)
    		return false;
    	
    	//This will prevent creation of a new position during drag events.
    	//drag events always originate from the Scene...
    	if (!(mouseEvent.getSource() instanceof HiddenSidesPane)) {
    		return false;
    	}
    	
        final Canvas canvas = getChart().getCanvas();
        // listen to only events within the canvas
        final Point2D mouseLoc = new Point2D(mouseEvent.getScreenX(), mouseEvent.getScreenY());
        final Bounds screenBounds = canvas.localToScreen(canvas.getBoundsInLocal());
        return screenBounds.contains(mouseLoc);
    }

    private void registerEventHandlers() {
        registerInputEventHandler(MouseEvent.MOUSE_CLICKED, positonSelectionHandler);
        
        //Tracking
        registerInputEventHandler(MouseEvent.MOUSE_MOVED, trackMouseMoveHandler);

        //I will manually add this for the moment since the super class doesn't seem to handle
        //key listeners very well :(
        chartProperty().addListener((obs, oldChart, newChart) -> {
            if (oldChart != null) {
                if (oldChart.getPlotArea().getScene() != null) {
                	oldChart.getPlotArea().getScene().removeEventHandler(KeyEvent.KEY_PRESSED, keyPressedHandler);
                }
            }
            if (newChart != null) {
            	newChart.getPlotArea().getScene().addEventHandler(KeyEvent.KEY_PRESSED, keyPressedHandler);
            }
        });
    }

    private void positionSelected(final MouseEvent event) {
    	if (datasetOptionsPane.getTrackingSeries() == null || event == null)
    		return;

    	// pixel coordinates w.r.t. plot area
        final Point2D positionPlotCoordinate = getChart().toPlotArea(event.getX(), event.getY());
        
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
        
        event.consume();
    }
    
    private void positionSelected(final double x, final double y) {
    	if (datasetOptionsPane.getTrackingSeries() == null)
    		return;
    	
    	positionPoint = this.currentTrackingDataPoint;//new Point2D(event.getX(), event.getY());

    	// pixel coordinates w.r.t. plot area
        //final Point2D positionPlotCoordinate = getChart().toPlotArea(positionPoint.getX(), positionPoint.getY());
        
        //Axis axis = ((XYChart) getChart()).getXAxis();
        //double xPosition = axis.getValueForDisplay(positionPlotCoordinate.getX());
        if (positionPoint != null) {
	        MarsPosition poi = new MarsPosition("Position");
	        poi.setColumn(datasetOptionsPane.getTrackingSeries().getXColumn());
	        poi.setPosition(positionPoint.x);
	        
	        //We add the region to the metadata if those indicators are selected.
	        //Otherwise we add it for molecule
	        //If None is selected we add nothing.
	        if (datasetOptionsPane.isMetadataIndicators())
	        	getChart().fireEvent(new NewMetadataPositionEvent(poi));
	        else if (datasetOptionsPane.isMoleculeIndicators())
	        	getChart().fireEvent(new NewMoleculePositionEvent(poi));
        }
        positionPoint = null;
    }
    
    private DataPoint findDataPoint(final MouseEvent event, final Bounds plotAreaBounds) {
        if (!plotAreaBounds.contains(event.getX(), event.getY())) {
            return null;
        }

        final Point2D mouseLocation = getLocationInPlotArea(event);

        Chart chart = getChart();
        
        if (chart.getDatasets().size() == 0)
        	return null;
        
        return findNearestDataPointWithinPickingDistance(chart, mouseLocation);
    }

    private DataPoint findNearestDataPointWithinPickingDistance(final Chart chart, final Point2D mouseLocation) {
        if (!(chart instanceof XYChart)) {
            return null;
        }
        final XYChart xyChart = (XYChart) chart;

        final double xValue = xyChart.getXAxis().getValueForDisplay(mouseLocation.getX());

        DataSet dataset = null;
        if (this.datasetOptionsPane != null) {
        	String datasetName =  datasetOptionsPane.getTrackingSeries().getYColumn() + " vs " + datasetOptionsPane.getTrackingSeries().getXColumn();
        	for (DataSet dataS : xyChart.getDatasets())
        		if (dataS.getName().equals(datasetName))
        			dataset = dataS;
        } else {
        	return null;
        }

        return findNearestDataPoint(dataset, xValue);
    }
    
    /**
     * Handles series that have data sorted or not sorted with respect to X coordinate.
     * 
     * @param dataSet data set
     * @param searchedX x coordinate
     * @return return neighouring data points
     */
    private DataPoint findNearestDataPoint(final DataSet dataSet, final double searchedX) {
    	if (dataSet == null)
    		return null;
    	
        int prevIndex = -1;
        int nextIndex = -1;
        double prevX = Double.NEGATIVE_INFINITY;
        double nextX = Double.POSITIVE_INFINITY;

        final int nDataCount = dataSet.getDataCount(DataSet.DIM_X);
        for (int i = 0, size = nDataCount; i < size; i++) {
            final double currentX = dataSet.get(DataSet.DIM_X, i);

            if (currentX < searchedX) {
                if (prevX < currentX) {
                    prevIndex = i;
                    prevX = currentX;
                }
            } else if (nextX > currentX) {
                nextIndex = i;
                nextX = currentX;
            }
        }
        final DataPoint prevPoint = prevIndex == -1 ? null
                : new DataPoint(getChart(), dataSet.get(DataSet.DIM_X, prevIndex),
                        dataSet.get(DataSet.DIM_Y, prevIndex), getDataLabelSafe(dataSet, prevIndex));
        final DataPoint nextPoint = nextIndex == -1 || nextIndex == prevIndex ? null
                : new DataPoint(getChart(), dataSet.get(DataSet.DIM_X, nextIndex),
                        dataSet.get(DataSet.DIM_Y, nextIndex), getDataLabelSafe(dataSet, nextIndex));

        if (prevPoint == null || nextPoint == null)
         	return null;
        
        final double prevDistance = Math.abs(searchedX - prevPoint.x);
        final double nextDistance = Math.abs(searchedX - nextPoint.x);

        if (prevDistance < nextDistance)
        	return prevPoint;
        else 
        	return nextPoint;
    }

    private String formatDataPoint(final DataPoint dataPoint) {
        return String.format("x: %.3f\ny: %.3f", dataPoint.x, dataPoint.y);
    }

    protected String getDataLabelSafe(final DataSet dataSet, final int index) {
        String lable = dataSet.getDataLabel(index);
        if (lable == null) {
            return getDefaultDataLabel(dataSet, index);
        }
        return lable;
    }

    protected String getDefaultDataLabel(final DataSet dataSet, final int index) {
    	return String.format("%s", dataSet.getName());
    }

    private void updateLabel(final MouseEvent event, final Bounds plotAreaBounds, final DataPoint dataPoint) {
        label.setText(formatDataPoint(dataPoint));
        final double width = label.prefWidth(-1);
        final double height = label.prefHeight(width);
        
        final XYChart xyChart = (XYChart) getChart();
        
        final double dataPointX = xyChart.getXAxis().getDisplayPosition(dataPoint.x);
        final double dataPointY = xyChart.getYAxis().getDisplayPosition(dataPoint.y);

        double xLocation = dataPointX + MarsPositionSelectionPlugin.LABEL_X_OFFSET;
        double yLocation = dataPointY - MarsPositionSelectionPlugin.LABEL_Y_OFFSET - height;
        
        trackingDataPointScreen = new Point2D(dataPointX, dataPointY);
        currentTrackingDataPoint = dataPoint;
        
        circle.setCenterX(dataPointX);
	    circle.setCenterY(dataPointY);

        if (xLocation + width > plotAreaBounds.getMaxX()) {
            xLocation = dataPointX - MarsPositionSelectionPlugin.LABEL_X_OFFSET - width;
        }
        if (yLocation < plotAreaBounds.getMinY()) {
            yLocation = dataPointY + MarsPositionSelectionPlugin.LABEL_Y_OFFSET;
        }
        label.resizeRelocate(xLocation, yLocation, width, height);
    }

    private void updateToolTip(final MouseEvent event) {
        final Bounds plotAreaBounds = getChart().getPlotArea().getBoundsInLocal();
        final DataPoint dataPoint = findDataPoint(event, plotAreaBounds);

        if (dataPoint == null) {
            getChartChildren().remove(label);
            getChartChildren().remove(circle);
            return;
        }
        
        updateLabel(event, plotAreaBounds, dataPoint);
        if (!getChartChildren().contains(label)) {
            getChartChildren().add(label);
            label.requestLayout();
        }
        if (!getChartChildren().contains(circle)) {
            getChartChildren().add(circle);
        }
    }

    protected class DataPoint {

        protected final Chart chart;
        protected final double x;
        protected final double y;
        protected final String label;
        protected double distanceFromMouse;

        protected DataPoint(final Chart chart, final double x, final double y, final String label) {
            this.chart = chart;
            this.x = x;
            this.y = y;
            this.label = label;
        }

        public Chart getChart() {
            return chart;
        }

        public double getDistanceFromMouse() {
            return distanceFromMouse;
        }

        public String getLabel() {
            return label;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

    }

	@Override
	public void setDatasetOptionsPane(DatasetOptionsPane datasetOptionsPane) {
		this.datasetOptionsPane = datasetOptionsPane;
	}
}
