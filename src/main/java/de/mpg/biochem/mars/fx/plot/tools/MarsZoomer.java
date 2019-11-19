package de.mpg.biochem.mars.fx.plot.tools;

import de.gsi.chart.Chart;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.plugins.Zoomer;

import javafx.scene.input.ScrollEvent;
import java.util.function.Predicate;

import javafx.scene.canvas.Canvas;
import javafx.geometry.Point2D;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;

public class MarsZoomer extends Zoomer {
    
	public final Predicate<ScrollEvent> marsScrollFilter = this::isMouseScrollEventWithinCanvas;
	
    public MarsZoomer() {
        super(AxisMode.XY);
    }

    /**
     * Creates a new instance of Zoomer with animation disabled.
     *
     * @param zoomMode initial value of {@link #axisModeProperty() zoomMode} property
     */
    public MarsZoomer(final AxisMode zoomMode) {
        this(zoomMode, false);
        this.setAddButtonsToToolBar(false);
        this.setZoomScrollFilter(marsScrollFilter);
    }

    /**
     * Creates a new instance of Zoomer.
     *
     * @param zoomMode initial value of {@link #axisModeProperty() axisMode} property
     * @param animated initial value of {@link #animatedProperty() animated} property
     */
    public MarsZoomer(final AxisMode zoomMode, final boolean animated) {
        super(zoomMode, animated);
        this.setAddButtonsToToolBar(false);
        this.setZoomScrollFilter(marsScrollFilter);
    }

    /**
     * Creates a new instance of Zoomer with {@link #axisModeProperty() zoomMode} initialized to {@link AxisMode#XY}.
     *
     * @param animated initial value of {@link #animatedProperty() animated} property
     */
    public MarsZoomer(final boolean animated) {
        super(AxisMode.XY, animated);
        this.setAddButtonsToToolBar(false);
        this.setZoomScrollFilter(marsScrollFilter);
    }
    
    //Doesn't seem to work with screen coordinates, only with scene coordinates.
    private boolean isMouseScrollEventWithinCanvas(final ScrollEvent mouseEvent) {
    	if (getChart() == null || getChart().getCanvas() == null)
    		return false;
        final Canvas canvas = getChart().getCanvas();
        final Point2D mouseLoc = new Point2D(mouseEvent.getSceneX(), mouseEvent.getSceneY());
        final Bounds sceneBounds = canvas.localToScene(canvas.getBoundsInLocal());
        return sceneBounds.contains(mouseLoc);
    }
}
