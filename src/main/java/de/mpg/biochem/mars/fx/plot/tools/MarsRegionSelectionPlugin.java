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
import de.gsi.dataset.DataSet;
import de.mpg.biochem.mars.fx.plot.DatasetOptionsPane;
import de.mpg.biochem.mars.fx.plot.MarsPlotPlugin;
import de.mpg.biochem.mars.fx.plot.event.NewMetadataRegionEvent;
import de.mpg.biochem.mars.fx.plot.event.NewMoleculeRegionEvent;
import de.mpg.biochem.mars.fx.plot.tools.MarsDataPointTracker.DataPoint;
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
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;

import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;

/**
 * Region along X or Y axis.
 * 
 * Region selection start - triggered on {@link MouseEvent#MOUSE_PRESSED MOUSE_PRESSED}
 * event that is accepted by getregionInMouseFilter() region-in filter. It
 * shows a rectangle determining the region once mouse button is
 * released.
 * 
 * @author Karl Duderstadt
 */
public class MarsRegionSelectionPlugin extends ChartPlugin implements MarsPlotPlugin {
    /**
     * Name of the CCS class of the region rectangle.
     */
    public static final String STYLE_CLASS_REGION_RECT = "chart-zoom-rect";
    private static final int REGION_RECT_MIN_SIZE = 5;
    //private static final int FONT_SIZE = 20;
    
    private DatasetOptionsPane datasetOptionsPane;
    
    //Tooltip variables
    
    public static final String STYLE_CLASS_LABEL = "chart-datapoint-tooltip-label";

    private static final int LABEL_X_OFFSET = 15;
    private static final int LABEL_Y_OFFSET = 5;

    private final Label label = new Label();
    private final Circle circle = new Circle();
    
    private Point2D trackingDataPointScreen;
    private DataPoint currentTrackingDataPoint;
    private DataPoint trackingDataPointStart, trackingDataPointEnd;

    private final EventHandler<MouseEvent> trackMouseMoveHandler = this::updateToolTip;

    /**
     * Default region mouse filter passing on left mouse button (only).
     */
    public final Predicate<MouseEvent> defaultRegionSelectionMouseFilter = event -> MouseEventsHelper.isOnlyPrimaryButtonDown(
            event) && MouseEventsHelper.modifierKeysUp(event) && isMouseEventWithinCanvas(event);

    private Predicate<MouseEvent> regionSelectionMouseFilter = defaultRegionSelectionMouseFilter;

    private final Rectangle regionRectangle = new Rectangle();
    private Point2D regionStartPoint = null;
    private Point2D regionEndPoint = null;
    private ObservableList<Axis> omitAxisregion = FXCollections.observableArrayList();

    private final ObjectProperty<AxisMode> axisMode = new SimpleObjectProperty<AxisMode>(this, "axisMode",
            AxisMode.XY) {
        @Override
        protected void invalidated() {
            Objects.requireNonNull(get(), "The " + getName() + " must not be null");
        }
    };

    private Cursor originalCursor;

    private final ObjectProperty<Cursor> dragCursor = new SimpleObjectProperty<>(this, "dragCursor");

    private final EventHandler<MouseEvent> regionSelectionStartHandler = event -> {
        if (getRegionSelectionMouseFilter() == null || getRegionSelectionMouseFilter().test(event) && getChart().getDatasets().size() > 0) {
            regionSelectionStarted(event);
            event.consume();
        }
    };

    private final EventHandler<MouseEvent> regionSelectionDragHandler = event -> {
        if (regionSelectionOngoing()) {
            regionSelectionDragged(event);
            event.consume();
        }
    };

    private final EventHandler<MouseEvent> regionSelectionEndHandler = event -> {
        if (regionSelectionOngoing()) {
            regionSelectionEnded();
            event.consume();
        }
    };
    
    private final EventHandler<KeyEvent> keyPressedHandler = event -> {
            if (event.getCode() == KeyCode.P && trackingDataPointScreen != null) {
            	if (regionSelectionOngoing()) {
            		regionSelectionEnded(trackingDataPointScreen.getX(), trackingDataPointScreen.getY());
            	} else {
            		regionSelectionStarted(trackingDataPointScreen.getX(), trackingDataPointScreen.getY());
            	}
                event.consume();
            }
    };
    
    public MarsRegionSelectionPlugin() {
        this(AxisMode.X);
    }

    /**
     * Creates a new instance of region selector with animation disabled.
     *
     * @param axisMode
     *            initial value of {@link #axisModeProperty() regionMode} property
     */
    public MarsRegionSelectionPlugin(final AxisMode axisMode) {
        super();
        setAxisMode(axisMode);
        setDragCursor(Cursor.CROSSHAIR);

        regionRectangle.setManaged(false);
        regionRectangle.getStyleClass().add(STYLE_CLASS_REGION_RECT);
        getChartChildren().add(regionRectangle);
        registerEventHandlers();
        
        label.getStyleClass().add(MarsDataPointTracker.STYLE_CLASS_LABEL);
        circle.setRadius(5.0f);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.RED);
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
     * Mouse cursor to be used during drag operation.
     *
     * @return the mouse cursor property
     */
    public final ObjectProperty<Cursor> dragCursorProperty() {
        return dragCursor;
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
     * Returns the value of the {@link #dragCursorProperty()}
     *
     * @return the current cursor
     */
    public final Cursor getDragCursor() {
        return dragCursorProperty().get();
    }

    /**
     * Returns region-in mouse event filter.
     *
     * @return region-in mouse event filter
     */
    public Predicate<MouseEvent> getRegionSelectionMouseFilter() {
        return regionSelectionMouseFilter;
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
     * Sets value of the {@link #dragCursorProperty()}.
     *
     * @param cursor
     *            the cursor to be used by the plugin
     */
    public final void setDragCursor(final Cursor cursor) {
        dragCursorProperty().set(cursor);
    }

    /**
     * Sets filter on MouseEvent#DRAG_DETECTED DRAG_DETECTED events that
     * should start region-in operation.
     *
     * @param regionSelectionMouseFilter
     *            the filter to accept region-in mouse event. If {@code null} then
     *            any DRAG_DETECTED event will start region-in operation. By
     *            default it's set to defaultregionInMouseFilter.
     */
    public void setRegionSelectionMouseFilter(final Predicate<MouseEvent> regionSelectionMouseFilter) {
        this.regionSelectionMouseFilter = regionSelectionMouseFilter;
    }

    private void installCursor() {
        final Region chart = getChart();
        originalCursor = chart.getCursor();
        if (getDragCursor() != null) {
            chart.setCursor(getDragCursor());
        }
    }

    private boolean isMouseEventWithinCanvas(final MouseEvent mouseEvent) {
        final Canvas canvas = getChart().getCanvas();
        // listen to only events within the canvas
        final Point2D mouseLoc = new Point2D(mouseEvent.getScreenX(), mouseEvent.getScreenY());
        final Bounds screenBounds = canvas.localToScreen(canvas.getBoundsInLocal());
        return screenBounds.contains(mouseLoc);
    }

    private void registerEventHandlers() {
        registerInputEventHandler(MouseEvent.MOUSE_PRESSED, regionSelectionStartHandler);
        registerInputEventHandler(MouseEvent.MOUSE_DRAGGED, regionSelectionDragHandler);
        registerInputEventHandler(MouseEvent.MOUSE_RELEASED, regionSelectionEndHandler);
        
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

    private void uninstallCursor() {
        getChart().setCursor(originalCursor);
    }

    private void regionSelectionDragged(final MouseEvent event) {
        final Bounds plotAreaBounds = getChart().getPlotArea().getBoundsInLocal();
        regionEndPoint = limitToPlotArea(event, plotAreaBounds);

        double regionRectX = plotAreaBounds.getMinX();
        double regionRectY = plotAreaBounds.getMinY();
        double regionRectWidth = plotAreaBounds.getWidth();
        double regionRectHeight = plotAreaBounds.getHeight();

        if (getAxisMode().allowsX()) {
            regionRectX = Math.min(regionStartPoint.getX(), regionEndPoint.getX());
            regionRectWidth = Math.abs(regionEndPoint.getX() - regionStartPoint.getX());
        }
        if (getAxisMode().allowsY()) {
            regionRectY = Math.min(regionStartPoint.getY(), regionEndPoint.getY());
            regionRectHeight = Math.abs(regionEndPoint.getY() - regionStartPoint.getY());
        }
        regionRectangle.setX(regionRectX);
        regionRectangle.setY(regionRectY);
        regionRectangle.setWidth(regionRectWidth);
        regionRectangle.setHeight(regionRectHeight);
    }
    
    private void regionSelectionEnded() {
        regionRectangle.setVisible(false);
        
        if (regionRectangle.getWidth() > REGION_RECT_MIN_SIZE) {
            final double minX = regionRectangle.getX();
            final double minY = regionRectangle.getY() + regionRectangle.getHeight();
            final double maxX = regionRectangle.getX() + regionRectangle.getWidth();
            final double maxY = regionRectangle.getY();

            // pixel coordinates w.r.t. plot area
            final Point2D minPlotCoordinate = getChart().toPlotArea(minX, minY);
            final Point2D maxPlotCoordinate = getChart().toPlotArea(maxX, maxY);
            
            Axis axis = ((XYChart) getChart()).getXAxis();
            double dataMin = axis.getValueForDisplay(minPlotCoordinate.getX());
            double dataMax = axis.getValueForDisplay(maxPlotCoordinate.getX());
            
            MarsRegion roi = new MarsRegion("Region");
            roi.setColumn(datasetOptionsPane.getTrackingSeries().getXColumn());
            roi.setStart(dataMin);
            roi.setEnd(dataMax);
            
            //We add the region to the metadata if those indicators are selected.
            //Otherwise we add it for molecule
            //If None is selected we add nothing.
            if (datasetOptionsPane.isMetadataIndicators())
            	getChart().fireEvent(new NewMetadataRegionEvent(roi));
            else if (datasetOptionsPane.isMoleculeIndicators())
            	getChart().fireEvent(new NewMoleculeRegionEvent(roi));
            
        }
        trackingDataPointStart = trackingDataPointEnd = null;
        regionStartPoint = regionEndPoint = null;
        uninstallCursor();
    }

    private void regionSelectionEnded(final double xEnd, final double yEnd) {
    	if (datasetOptionsPane.getTrackingSeries() == null)
    		return;
    	
    	trackingDataPointEnd = this.currentTrackingDataPoint;
    	
        regionRectangle.setVisible(false);
        double width = Math.abs(xEnd - regionStartPoint.getX());

        if (width > REGION_RECT_MIN_SIZE && trackingDataPointStart != null && trackingDataPointEnd != null) {
            MarsRegion roi = new MarsRegion("Region");
            roi.setColumn(datasetOptionsPane.getTrackingSeries().getXColumn());
            roi.setStart(trackingDataPointStart.x);
            roi.setEnd(trackingDataPointEnd.x);

            //We add the region to the metadata if those indicators are selected.
            //Otherwise we add it for molecule
            //If None is selected we add nothing.
            if (datasetOptionsPane.isMetadataIndicators())
            	getChart().fireEvent(new NewMetadataRegionEvent(roi));
            else if (datasetOptionsPane.isMoleculeIndicators())
            	getChart().fireEvent(new NewMoleculeRegionEvent(roi));
        }
        trackingDataPointStart = trackingDataPointEnd = null;
        regionStartPoint = regionEndPoint = null;
        uninstallCursor();
    }

    private void regionSelectionStarted(final MouseEvent event) {
        regionStartPoint = new Point2D(event.getX(), event.getY());

        regionRectangle.setX(regionStartPoint.getX());
        regionRectangle.setY(regionStartPoint.getY());
        regionRectangle.setWidth(0);
        regionRectangle.setHeight(0);
        regionRectangle.setVisible(true);
        installCursor();
    }
    
    private void regionSelectionStarted(double x, double y) {
        regionStartPoint = new Point2D(x, y);
        
        trackingDataPointStart = this.currentTrackingDataPoint;

        regionRectangle.setX(regionStartPoint.getX());
        regionRectangle.setY(regionStartPoint.getY());
        regionRectangle.setWidth(0);
        regionRectangle.setHeight(0);
        regionRectangle.setVisible(true);
        installCursor();
    }

    private boolean regionSelectionOngoing() {
        return regionStartPoint != null;
    }

	@Override
	public void setDatasetOptionsPane(DatasetOptionsPane datasetOptionsPane) {
		this.datasetOptionsPane = datasetOptionsPane;
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
    	return String.format("x: %.6f\ny: %.6f", dataPoint.x, dataPoint.y);
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

        double xLocation = dataPointX + MarsRegionSelectionPlugin.LABEL_X_OFFSET;
        double yLocation = dataPointY - MarsRegionSelectionPlugin.LABEL_Y_OFFSET - height;
        
        trackingDataPointScreen = new Point2D(dataPointX, dataPointY);
        currentTrackingDataPoint = dataPoint;
        
        circle.setCenterX(dataPointX);
	    circle.setCenterY(dataPointY);

        if (xLocation + width > plotAreaBounds.getMaxX()) {
            xLocation = dataPointX - MarsRegionSelectionPlugin.LABEL_X_OFFSET - width;
        }
        if (yLocation < plotAreaBounds.getMinY()) {
            yLocation = dataPointY + MarsRegionSelectionPlugin.LABEL_Y_OFFSET;
        }
        label.resizeRelocate(xLocation, yLocation, width, height);
    }
    /*
    private Point2D getNearestDataPoint(final MouseEvent event) {
    	final Bounds plotAreaBounds = getChart().getPlotArea().getBoundsInLocal();

        if (!plotAreaBounds.contains(event.getX(), event.getY())) {
            return null;
        }

        final Point2D mouseLocation = getLocationInPlotArea(event);

        final XYChart xyChart = (XYChart) getChart();
        final DataPoint dataPoint = findNearestDataPointWithinPickingDistance(xyChart, mouseLocation);

        final double dataPointX = xyChart.getXAxis().getDisplayPosition(dataPoint.x);
        final double dataPointY = xyChart.getYAxis().getDisplayPosition(dataPoint.y);
        
        return new Point2D(dataPointX, dataPointY);
    }
    */
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

        if (regionSelectionOngoing()) {
        	regionSelectionDragged(event);
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
}
