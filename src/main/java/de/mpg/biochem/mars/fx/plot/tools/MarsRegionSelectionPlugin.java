package de.mpg.biochem.mars.fx.plot.tools;

import java.util.Objects;
import java.util.function.Predicate;

import de.gsi.chart.plugins.*;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.plugins.ChartPlugin;
import de.mpg.biochem.mars.fx.plot.DatasetOptionsPane;
import de.mpg.biochem.mars.fx.plot.MarsPlotPlugin;
import de.mpg.biochem.mars.fx.plot.event.NewRegionAddedEvent;
import de.mpg.biochem.mars.util.RegionOfInterest;
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
 * Region along X or Y axis.
 * <ul>
 * <li>region selection start - triggered on {@link MouseEvent#MOUSE_PRESSED MOUSE_PRESSED}
 * event that is accepted by {@link #getregionInMouseFilter() region-in filter}. It
 * shows a rectangle determining the region once mouse button is
 * released.</li>
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
        if (getRegionSelectionMouseFilter() == null || getRegionSelectionMouseFilter().test(event)) {
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

    public MarsRegionSelectionPlugin() {
        this(AxisMode.X);
    }

    /**
     * Creates a new instance of region selector with animation disabled.
     *
     * @param regionMode
     *            initial value of {@link #axisModeProperty() regionMode} property
     */
    public MarsRegionSelectionPlugin(final AxisMode axisMode) {
        super();
        setAxisMode(axisMode);
        setDragCursor(Cursor.CROSSHAIR);

        regionRectangle.setManaged(false);
        regionRectangle.getStyleClass().add(STYLE_CLASS_REGION_RECT);
        getChartChildren().add(regionRectangle);
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
     * @see #setregionInMouseFilter(Predicate)
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
     * Sets filter on {@link MouseEvent#DRAG_DETECTED DRAG_DETECTED} events that
     * should start region-in operation.
     *
     * @param regionInMouseFilter
     *            the filter to accept region-in mouse event. If {@code null} then
     *            any DRAG_DETECTED event will start region-in operation. By
     *            default it's set to {@link #defaultregionInMouseFilter}.
     * @see #getregionInMouseFilter()
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

    private void registerMouseHandlers() {
        registerInputEventHandler(MouseEvent.MOUSE_PRESSED, regionSelectionStartHandler);
        registerInputEventHandler(MouseEvent.MOUSE_DRAGGED, regionSelectionDragHandler);
        registerInputEventHandler(MouseEvent.MOUSE_RELEASED, regionSelectionEndHandler);
        //registerInputEventHandler(MouseEvent.MOUSE_CLICKED, regionOutHandler);
        //registerInputEventHandler(MouseEvent.MOUSE_CLICKED, regionOriginHandler);
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
        if (regionRectangle.getWidth() > REGION_RECT_MIN_SIZE && regionRectangle.getHeight() > REGION_RECT_MIN_SIZE) {
            final double minX = regionRectangle.getX();
            final double minY = regionRectangle.getY() + regionRectangle.getHeight();
            final double maxX = regionRectangle.getX() + regionRectangle.getWidth();
            final double maxY = regionRectangle.getY();

            // pixel coordinates w.r.t. plot area
            final Point2D minPlotCoordinate = getChart().toPlotArea(minX, minY);
            final Point2D maxPlotCoordinate = getChart().toPlotArea(maxX, maxY);
            
            /*
            for (Axis axis : getChart().getAxes()) {
                double dataMin;
                double dataMax;
                if (axis.getSide().isVertical()) {
                    dataMin = axis.getValueForDisplay(minPlotCoordinate.getY());
                    dataMax = axis.getValueForDisplay(maxPlotCoordinate.getY());
                } else {
                    dataMin = axis.getValueForDisplay(minPlotCoordinate.getX());
                    dataMax = axis.getValueForDisplay(maxPlotCoordinate.getX());
                }
                System.out.println("min " + dataMin + " max " + dataMax);
            }
            */
            Axis axis = ((XYChart) getChart()).getXAxis();
            double dataMin = axis.getValueForDisplay(minPlotCoordinate.getX());
            double dataMax = axis.getValueForDisplay(maxPlotCoordinate.getX());
            
            RegionOfInterest roi = new RegionOfInterest("Region 1");
            roi.setColumn(datasetOptionsPane.getTrackingSeries().getXColumn());
            roi.setStart(dataMin);
            roi.setEnd(dataMax);
            
            getChart().fireEvent(new NewRegionAddedEvent(roi));
        }
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

    private boolean regionSelectionOngoing() {
        return regionStartPoint != null;
    }

	@Override
	public void setDatasetOptionsPane(DatasetOptionsPane datasetOptionsPane) {
		this.datasetOptionsPane = datasetOptionsPane;
	}
}
