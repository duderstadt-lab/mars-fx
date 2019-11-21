package de.mpg.biochem.mars.fx.plot.tools;

import de.gsi.chart.axes.Axis;
import de.gsi.chart.plugins.XValueIndicator;
import de.mpg.biochem.mars.fx.plot.DatasetOptionsPane;
import de.mpg.biochem.mars.fx.plot.MarsPlotPlugin;
import de.mpg.biochem.mars.fx.plot.event.NewMetadataRegionEvent;
import de.mpg.biochem.mars.fx.plot.event.NewMoleculeRegionEvent;
import de.mpg.biochem.mars.fx.plot.event.UpdateMetadataPositionEvent;
import de.mpg.biochem.mars.fx.plot.event.UpdateMoleculePositionEvent;
import de.mpg.biochem.mars.util.PositionOfInterest;
import de.mpg.biochem.mars.util.RegionOfInterest;
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
        PositionOfInterest poi = new PositionOfInterest(getText());
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
