package de.mpg.biochem.mars.fx.plot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import cern.extjfx.chart.plugins.AbstractDataFormattingPlugin;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.util.Pair;

import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;

/**
 * A tool tip label appearing next to the mouse cursor when placed over a data point's symbol. If symbols are not
 * created/shown for given plot, the tool tip is shown for the closest data point that is within the
 * {@link #pickingDistanceProperty()} from the mouse cursor.
 * <p>
 * CSS style class name: {@value #STYLE_CLASS_LABEL}
 *
 * @param <X> type of X values
 * @param <Y> type of Y values
 */
public class MarsDataPointTooltip<X, Y> extends AbstractDataFormattingPlugin<X, Y> {
    /**
     * Name of the CSS class of the tool tip label.
     */
    public static final String STYLE_CLASS_LABEL = "chart-datapoint-tooltip-label";

    /**
     * The default distance between the data point coordinates and mouse cursor that triggers showing the tool tip
     * label.
     */
    public static final int DEFAULT_PICKING_DISTANCE = 5;

    private static final int LABEL_X_OFFSET = 5;
    private static final int LABEL_Y_OFFSET = 5;

    private final Label label = new Label();
    
    private final Circle circle = new Circle();

    /**
     * Creates a new instance of DataPointTooltip class with {{@link #pickingDistanceProperty() picking distance}
     * initialized to {@value #DEFAULT_PICKING_DISTANCE}.
     */
    public MarsDataPointTooltip() {
        label.getStyleClass().add(STYLE_CLASS_LABEL);
        circle.setRadius(5.0f);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.RED);
        registerMouseEventHandler(MouseEvent.MOUSE_MOVED, mouseMoveHandler);
    }

    /**
     * Creates a new instance of DataPointTooltip class.
     *
     * @param pickingDistance the initial value for the {@link #pickingDistanceProperty() pickingDistance} property
     */
    public MarsDataPointTooltip(double pickingDistance) {
        this();
        setPickingDistance(pickingDistance);
    }

    private final DoubleProperty pickingDistance = new SimpleDoubleProperty(this, "pickingDistance",
            DEFAULT_PICKING_DISTANCE) {
        @Override
        protected void invalidated() {
            if (get() <= 0) {
                throw new IllegalArgumentException("The " + getName() + " must be a positive value");
            }
        }
    };

    /**
     * Distance of the mouse cursor from the data point (expressed in display units) that should trigger showing the
     * tool tip. By default initialized to {@value #DEFAULT_PICKING_DISTANCE}.
     *
     * @return the picking distance property
     */
    public final DoubleProperty pickingDistanceProperty() {
        return pickingDistance;
    }

    /**
     * Returns the value of the {@link #pickingDistanceProperty()}.
     *
     * @return the current picking distance
     */
    public final double getPickingDistance() {
        return pickingDistanceProperty().get();
    }

    /**
     * Sets the value of {@link #pickingDistanceProperty()}.
     *
     * @param distance the new picking distance
     */
    public final void setPickingDistance(double distance) {
        pickingDistanceProperty().set(distance);
    }

    private final EventHandler<MouseEvent> mouseMoveHandler = (MouseEvent event) -> {
        updateToolTip(event);
    };

    private void updateToolTip(MouseEvent event) {
        Bounds plotAreaBounds = getChartPane().getPlotAreaBounds();
        DataPoint dataPoint = findDataPoint(event, plotAreaBounds);

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

    private DataPoint findDataPoint(MouseEvent event, Bounds plotAreaBounds) {
        if (!plotAreaBounds.contains(event.getX(), event.getY())) {
            return null;
        }

        Point2D mouseLocation = getLocationInPlotArea(event);
        DataPoint nearestDataPoint = null;

        List<XYChart<X, Y>> charts = new ArrayList<>(getCharts());
        // Search points from top charts to bottom so that symbols drawn on top have precedence
        Collections.reverse(charts);
        for (XYChart<X, Y> chart : charts) {
            DataPoint point = findNearestDataPointWithinPickingDistance(chart, mouseLocation);
            if (nearestDataPoint == null
                    || (point != null && point.distanceFromMouse < nearestDataPoint.distanceFromMouse)) {
                nearestDataPoint = point;
            }
        }
        return nearestDataPoint;
    }

    private DataPoint findNearestDataPointWithinPickingDistance(XYChart<X, Y> chart, Point2D mouseLocation) {
        DataPoint nearestDataPoint = null;

        X xValue = toDataPoint(chart.getYAxis(), mouseLocation).getXValue();
        for (DataPoint dataPoint : findPointsToCheck(chart, xValue)) {
            Node node = dataPoint.data.getNode();
            if (node != null && node.isVisible()) {
                if (node.getBoundsInParent().contains(mouseLocation)) {
                    dataPoint.distanceFromMouse = 0;
                    return dataPoint;
                }
            } else {
                Point2D displayPoint = toDisplayPoint(chart.getYAxis(), dataPoint.data);
                
                dataPoint.distanceFromMouse = displayPoint.distance(mouseLocation);
                //if (displayPoint.distance(mouseLocation) <= getPickingDistance() && (nearestDataPoint == null || dataPoint.distanceFromMouse < nearestDataPoint.distanceFromMouse)) {
                if (nearestDataPoint == null || dataPoint.distanceFromMouse < nearestDataPoint.distanceFromMouse) {
                  nearestDataPoint = dataPoint;
                }
            }
        }
        return nearestDataPoint;
    }

    private List<DataPoint> findPointsToCheck(XYChart<X, Y> chart, X xValue) {
        if (xValue instanceof Number) {
            return findNeighborPoints(chart, ((Number) xValue).doubleValue());
        }
        List<DataPoint> points = new LinkedList<>();
        for (Series<X, Y> series : chart.getData()) {
            for (Data<X, Y> data : series.getData()) {
                points.add(new DataPoint(series, data));
            }
        }
        return points;
    }

    private List<DataPoint> findNeighborPoints(XYChart<X, Y> chart, double searchedX) {
        List<DataPoint> points = new LinkedList<>();
        for (Series<X, Y> series : chart.getData()) {
            Pair<DataPoint, DataPoint> neighborPoints = findNeighborPoints(series, searchedX);
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
     */
    private Pair<DataPoint, DataPoint> findNeighborPoints(Series<X, Y> series, double searchedX) {
        List<Data<? extends Number, Y>> seriesData = castXToNumber(series);

        int prevIndex = -1;
        int nextIndex = -1;
        double prevX = Double.MIN_VALUE;
        double nextX = Double.MAX_VALUE;

        for (int i = 0, size = seriesData.size(); i < size; i++) {
            double currentX = seriesData.get(i).getXValue().doubleValue();

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
        DataPoint prevPoint = (prevIndex == -1) ? null : new DataPoint(series, series.getData().get(prevIndex));
        DataPoint nextPoint = (nextIndex == -1 || nextIndex == prevIndex) ? null
                : new DataPoint(series, series.getData().get(nextIndex));

        return new Pair<>(prevPoint, nextPoint);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private List<Data<? extends Number, Y>> castXToNumber(Series series) {
        return series.getData();
    }

    private void updateLabel(MouseEvent event, Bounds plotAreaBounds, DataPoint dataPoint) {
    	label.setText(formatDataPoint(dataPoint));

        Point2D point = getLocationInScene(dataPoint.data);
        double pointX = point.getX();
        double pointY = point.getY();
        
        if (plotAreaBounds.contains(point)) {
	        circle.setCenterX(pointX);
	        circle.setCenterY(pointY);
	
	        double width = label.prefWidth(-1);
	        double height = label.prefHeight(width);
	
	        double xLocation = pointX + LABEL_X_OFFSET;
	        double yLocation = pointY - LABEL_Y_OFFSET - height;
	
	        if (xLocation + width > plotAreaBounds.getMaxX()) {
	            xLocation = pointX - LABEL_X_OFFSET - width;
	        }
	        if (yLocation < plotAreaBounds.getMinY()) {
	            yLocation = pointY + LABEL_Y_OFFSET;
	        }
	        label.resizeRelocate(xLocation, yLocation, width, height);
        }
    }
    
    private Point2D getLocationInScene(Data<X, Y> pointInPlotArea) {
    	double displayX = getChartPane().getChart().getXAxis().getDisplayPosition(pointInPlotArea.getXValue());
    	double displayY = getChartPane().getChart().getYAxis().getDisplayPosition(pointInPlotArea.getYValue());
    	Point2D transformedPoint = new Point2D(displayX, displayY);
    	//Have to correct for offset in BorderPane - Northern Pane items.
    	double transX = getChartPane().getChart().getXAxis().localToScene(transformedPoint).getX() - getChartPane().getChart().localToScene(new Point2D(0, 0)).getX();
    	double transY = getChartPane().getChart().getYAxis().localToScene(transformedPoint).getY() - getChartPane().getChart().localToScene(new Point2D(0, 0)).getY();

        return new Point2D(transX, transY);
    }

    private String formatDataPoint(DataPoint dataPoint) {
        return formatData(dataPoint.series.getChart().getYAxis(), dataPoint.data);
    }

    private class DataPoint {
        final Series<X, Y> series;
        final Data<X, Y> data;
        double distanceFromMouse;

        DataPoint(Series<X, Y> series, Data<X, Y> data) {
            this.series = series;
            this.data = data;
        }
    }
}

