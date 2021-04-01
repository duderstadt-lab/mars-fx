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

import java.util.LinkedList;
import java.util.List;

/**
 * Copyright (c) 2016 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.plugins.AbstractDataFormattingPlugin;
import de.gsi.dataset.DataSet;
import de.mpg.biochem.mars.fx.plot.DatasetOptionsPane;
import de.mpg.biochem.mars.fx.plot.MarsPlotPlugin;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;

import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;

import javafx.util.Pair;

/**
 * A tool tip label appearing next to the mouse cursor when placed over a data point's symbol. If symbols are not
 * created/shown for given plot, the tool tip is shown for the closest data point that is within the
 * pickingDistanceProperty() from the mouse cursor.
 * <p>
 * CSS style class name: {@value #STYLE_CLASS_LABEL}
 *
 * @author Grzegorz Kruk TODO: extend so that label = new Label(); is a generic object and can also be overwritten with
 *         another implementation (&lt;-&gt; advanced interactor) additional add/remove listener are needed to
 *         edit/update the custom object based on DataPoint (for the time being private class)
 *         
 * @author Karl Duderstadt Added curve tracking based on DatasetOptionsPane and small bug fixes.
 */

public class MarsDataPointTracker extends AbstractDataFormattingPlugin implements MarsPlotPlugin {

    /**
     * Name of the CSS class of the tool tip label.
     */
    public static final String STYLE_CLASS_LABEL = "chart-datapoint-tooltip-label";

    /**
     * The default distance between the data point coordinates and mouse cursor that triggers showing the tool tip
     * label.
     */
    public static final int DEFAULT_PICKING_DISTANCE = 5;

    private static final int LABEL_X_OFFSET = 15;
    private static final int LABEL_Y_OFFSET = 5;

    private final Label label = new Label();
    
    private final Circle circle = new Circle();
    private DatasetOptionsPane datasetOptionsPane;

    private final EventHandler<MouseEvent> mouseMoveHandler = this::updateToolTip;

    /**
     * Creates a new instance of MarsDataPointTracker class with pickingDistanceProperty() picking distance
     * initialized to {@value #DEFAULT_PICKING_DISTANCE}.
     */
    public MarsDataPointTracker() {
        label.getStyleClass().add(MarsDataPointTracker.STYLE_CLASS_LABEL);
        registerInputEventHandler(MouseEvent.MOUSE_MOVED, mouseMoveHandler);
        circle.setRadius(5.0f);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.RED);
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
        	DataPoint nearestDataPoint = null;

        	for (final DataPoint dataPoint : findNeighborPoints(xyChart, xValue)) {
                    final double x = xyChart.getXAxis().getDisplayPosition(dataPoint.x);
                    final double y = xyChart.getYAxis().getDisplayPosition(dataPoint.y);
                    final Point2D displayPoint = new Point2D(x, y);
                    dataPoint.distanceFromMouse = displayPoint.distance(mouseLocation);
                    
                    if (displayPoint.distance(mouseLocation) <= 10 && (nearestDataPoint == null
                            || dataPoint.distanceFromMouse < nearestDataPoint.distanceFromMouse)) {
                        nearestDataPoint = dataPoint;
                    }
            }
            return nearestDataPoint;
        }

        return findNearestDataPoint(dataset, xValue);
    }
    
    private List<DataPoint> findNeighborPoints(final XYChart chart, final double searchedX) {
        final List<DataPoint> points = new LinkedList<>();
        for (final DataSet dataSet : chart.getAllDatasets()) {
            final Pair<DataPoint, DataPoint> neighborPoints = findNeighborPoints(dataSet, searchedX);
            if (neighborPoints.getKey() != null) {
                points.add(neighborPoints.getKey());
            }
            if (neighborPoints.getValue() != null) {
                points.add(neighborPoints.getValue());
            }
        }
        return points;
    }
    
    /**
     * Handles series that have data sorted or not sorted with respect to X coordinate.
     * 
     * @param dataSet data set
     * @param searchedX x coordinate
     * @return return neighouring data points
     */
    private Pair<DataPoint, DataPoint> findNeighborPoints(final DataSet dataSet, final double searchedX) {
        int prevIndex = -1;
        int nextIndex = -1;
        double prevX = Double.MIN_VALUE;
        double nextX = Double.MAX_VALUE;

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
        
        return new Pair<>(prevPoint, nextPoint);
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
        
        if (nextPoint == null || prevPoint == null)
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
        String label = dataSet.getDataLabel(index);
        if (label == null) {
            return getDefaultDataLabel(dataSet, index);
        }
        return label;
    }

    protected String getDefaultDataLabel(final DataSet dataSet, final int index) {
    	return String.format("%s", dataSet.getName());
    }

    private void updateLabel(final MouseEvent event, final Bounds plotAreaBounds, final DataPoint dataPoint) {
    	String dataPointLabel = null;
    	if (dataPoint.getLabel() != null)
    		dataPointLabel = dataPoint.getLabel();
    	if (dataPointLabel != null)
    		label.setText(dataPointLabel + "\n" + formatDataPoint(dataPoint));
    	else 
    		label.setText(formatDataPoint(dataPoint));
        final double width = label.prefWidth(-1);
        final double height = label.prefHeight(width);
        
        final XYChart xyChart = (XYChart) getChart();
        
        final double dataPointX = xyChart.getXAxis().getDisplayPosition(dataPoint.x);
        final double dataPointY = xyChart.getYAxis().getDisplayPosition(dataPoint.y);

        double xLocation = dataPointX + MarsDataPointTracker.LABEL_X_OFFSET;
        double yLocation = dataPointY - MarsDataPointTracker.LABEL_Y_OFFSET - height;
        
        circle.setCenterX(dataPointX);
	    circle.setCenterY(dataPointY);

        if (xLocation + width > plotAreaBounds.getMaxX()) {
            xLocation = dataPointX - MarsDataPointTracker.LABEL_X_OFFSET - width;
        }
        if (yLocation < plotAreaBounds.getMinY()) {
            yLocation = dataPointY + MarsDataPointTracker.LABEL_Y_OFFSET;
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
    @Override
    public void setDatasetOptionsPane(DatasetOptionsPane datasetOptionsPane) {
    	this.datasetOptionsPane = datasetOptionsPane;
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
