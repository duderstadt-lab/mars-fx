/**
 * Copyright (c) 2018 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package de.mpg.biochem.mars.fx.plot.tools;

import static cern.extjfx.chart.AxisMode.XY;

import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import java.util.Objects;
import java.util.function.Predicate;

import cern.extjfx.chart.AxisMode;
import cern.extjfx.chart.XYChartPlugin;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.RegionOfInterest;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 */
public class MarsRegionSelectionTool extends XYChartPlugin<Number, Number> {

    /**
     * Name of the CCS class of the selection rectangle.
     */
    public static final String STYLE_CLASS_SELECTION_RECT = "chart-zoom-rect";
    private static final int SELECTION_RECT_MIN_SIZE = 5;
    private static final Duration DEFAULT_SELECTION_DURATION = Duration.millis(500);
    
    private Molecule molecule;
    private String column;
    
    static boolean isOnlyPrimaryButtonDown(MouseEvent event) {
        return event.getButton() == PRIMARY && !event.isMiddleButtonDown() && !event.isSecondaryButtonDown();
    }

    static boolean isOnlySecondaryButtonDown(MouseEvent event) {
        return event.getButton() == SECONDARY && !event.isPrimaryButtonDown() && !event.isMiddleButtonDown();
    }

    static boolean isOnlyCtrlModifierDown(MouseEvent event) {
        return event.isControlDown() && !event.isAltDown() && !event.isMetaDown() && !event.isShiftDown();
    }

    static boolean modifierKeysUp(MouseEvent event) {
        return !event.isAltDown() && !event.isControlDown() && !event.isMetaDown() && !event.isShiftDown();
    }

    /**
     * Default selection-in mouse filter passing on left mouse button (only).
     */
    public static final Predicate<MouseEvent> DEFAULT_IN_SELECTION_MOUSE_FILTER = event -> 
    		isOnlyPrimaryButtonDown(event) && modifierKeysUp(event);

    private Predicate<MouseEvent> inSelectionMouseFilter = DEFAULT_IN_SELECTION_MOUSE_FILTER;

    private final Rectangle selectionRectangle = new Rectangle();
    private Point2D selectionStartPoint = null;
    private Point2D selectionEndPoint = null;

    public MarsRegionSelectionTool(AxisMode selectionMode, Molecule molecule) {
        setAxisMode(selectionMode);

        this.molecule = molecule;
        
        selectionRectangle.setManaged(false);
        selectionRectangle.getStyleClass().add(STYLE_CLASS_SELECTION_RECT);
        getChartChildren().add(selectionRectangle);
        registerMouseHandlers();
    }

    private void registerMouseHandlers() {
        registerMouseEventHandler(MouseEvent.MOUSE_MOVED, inSelectionDragHandler);
        registerMouseEventHandler(MouseEvent.MOUSE_CLICKED, regionSelectionHandler);
    }

    /**
     * Returns selection-in mouse event filter.
     *
     * @return selection-in mouse event filter
     * @see #setselectionInMouseFilter(Predicate)
     */
    public Predicate<MouseEvent> getInSelectionMouseFilter() {
        return inSelectionMouseFilter;
    }

    /**
     * Sets filter on {@link MouseEvent#DRAG_DETECTED DRAG_DETECTED} events that should start selection-in operation.
     *
     * @param selectionInMouseFilter the filter to accept selection-in mouse event. If {@code null} then any DRAG_DETECTED event
     *            will start selection-in operation. By default it's set to {@link #DEFAULT_selection_IN_MOUSE_FILTER}.
     * @see #getselectionInMouseFilter()
     */
    public void setInSelectionMouseFilter(Predicate<MouseEvent> selectionInMouseFilter) {
        this.inSelectionMouseFilter = selectionInMouseFilter;
    }

    private final ObjectProperty<AxisMode> axisMode = new SimpleObjectProperty<AxisMode>(this, "axisMode", XY) {
        @Override
        protected void invalidated() {
            Objects.requireNonNull(get(), "The " + getName() + " must not be null");
        }
    };

    /**
     * The mode defining axis along which the selection can be performed. By default initialized to {@link AxisMode#XY}.
     *
     * @return the axis mode property
     */
    public final ObjectProperty<AxisMode> axisModeProperty() {
        return axisMode;
    }

    /**
     * Sets the value of the {@link #axisModeProperty()}.
     *
     * @param mode the mode to be used
     */
    public final void setAxisMode(AxisMode mode) {
        axisModeProperty().set(mode);
    }

    /**
     * Returns the value of the {@link #axisModeProperty()}.
     *
     * @return current mode
     */
    public final AxisMode getAxisMode() {
        return axisModeProperty().get();
    }

    private final ObjectProperty<Duration> selectionDuration = new SimpleObjectProperty<Duration>(this, "selectionDuration",
    		DEFAULT_SELECTION_DURATION) {
        @Override
        protected void invalidated() {
            Objects.requireNonNull(get(), "The " + getName() + " must not be null");
        }
    };

    /**
     * Duration of the animated selection (in and out). Used only when {@link #animatedProperty()} is set to {@code true}. By
     * default initialized to 500ms.
     *
     * @return the selection duration property
     */
    public final ObjectProperty<Duration> selectionDurationProperty() {
        return selectionDuration;
    }

    /**
     * Sets the value of the {@link #selectionDurationProperty()}.
     *
     * @param duration duration of the selection
     */
    public final void setselectionDuration(Duration duration) {
        selectionDurationProperty().set(duration);
    }

    /**
     * Returns the value of the {@link #selectionDurationProperty()}.
     *
     * @return the current selection duration
     */
    public final Duration getselectionDuration() {
        return selectionDurationProperty().get();
    }

    private final EventHandler<MouseEvent> regionSelectionHandler = event -> {
        if (!selectionOngoing()) {
            inSelectionStarted(event);
        } else {
            selectionEnded();
        }
        event.consume();
    };

    private final EventHandler<MouseEvent> inSelectionDragHandler = event -> {
        if (selectionOngoing()) {
            inSelectionMoved(event);
            event.consume();
        }
    };

    private boolean selectionOngoing() {
        return selectionStartPoint != null;
    }
    
    private void inSelectionStarted(MouseEvent event) {
        selectionStartPoint = new Point2D(event.getX(), event.getY());
        selectionRectangle.setX(selectionStartPoint.getX());
        selectionRectangle.setY(selectionStartPoint.getY());
        selectionRectangle.setWidth(0);
        selectionRectangle.setHeight(0);
        selectionRectangle.setVisible(true);
    }

    private void inSelectionMoved(MouseEvent event) {
        Bounds plotAreaBounds = getChartPane().getPlotAreaBounds();
        selectionEndPoint = limitToPlotArea(event, plotAreaBounds);

        double selectionRectX = plotAreaBounds.getMinX();
        double selectionRectY = plotAreaBounds.getMinY();
        double selectionRectWidth = plotAreaBounds.getWidth();
        double selectionRectHeight = plotAreaBounds.getHeight();

        if (getAxisMode().allowsX()) {
            selectionRectX = Math.min(selectionStartPoint.getX(), selectionEndPoint.getX());
            selectionRectWidth = Math.abs(selectionEndPoint.getX() - selectionStartPoint.getX());
        }
        if (getAxisMode().allowsY()) {
            selectionRectY = Math.min(selectionStartPoint.getY(), selectionEndPoint.getY());
            selectionRectHeight = Math.abs(selectionEndPoint.getY() - selectionStartPoint.getY());
        }
        selectionRectangle.setX(selectionRectX);
        selectionRectangle.setY(selectionRectY);
        selectionRectangle.setWidth(selectionRectWidth);
        selectionRectangle.setHeight(selectionRectHeight);
    }

    private Point2D limitToPlotArea(MouseEvent event, Bounds plotBounds) {
        double limitedX = Math.max(Math.min(event.getX(), plotBounds.getMaxX()), plotBounds.getMinX());
        double limitedY = Math.max(Math.min(event.getY(), plotBounds.getMaxY()), plotBounds.getMinY());
        return new Point2D(limitedX, limitedY);
    }
    
    public void setColumn(String column) {
    	this.column = column;
    }
    
    public String getColumn() {
    	return column;
    }

    private void selectionEnded() {
        selectionRectangle.setVisible(false);
        if (selectionRectangle.getWidth() > SELECTION_RECT_MIN_SIZE && selectionRectangle.getHeight() > SELECTION_RECT_MIN_SIZE) {
        	RegionOfInterest regionOfInterest = new RegionOfInterest(getNewRegionName());
        	if (molecule.getDataTable().getColumnHeadingList().contains(column))
        		regionOfInterest.setColumn(column);
        	
        	if (getAxisMode().equals(AxisMode.X)) {
        		regionOfInterest.setStart(toDataPoint(getCharts().get(0).getYAxis(), selectionStartPoint).getXValue().doubleValue());
        		regionOfInterest.setEnd(toDataPoint(getCharts().get(0).getYAxis(), selectionEndPoint).getXValue().doubleValue());
        	} else if (getAxisMode().equals(AxisMode.Y)) {
        		regionOfInterest.setStart(toDataPoint(getCharts().get(0).getYAxis(), selectionStartPoint).getYValue().doubleValue());
        		regionOfInterest.setEnd(toDataPoint(getCharts().get(0).getYAxis(), selectionEndPoint).getYValue().doubleValue());
        	}
			molecule.putRegion(regionOfInterest);
        }
        selectionStartPoint = selectionEndPoint = null;
    }
    
    private String getNewRegionName() {
    	String newName = "Region 1";
    	int index = 2;
    	while (molecule.getRegionNames().contains(newName)) {
    		newName = "Region " + index;
    		index++;
    	}
    	return newName;
    }
}
