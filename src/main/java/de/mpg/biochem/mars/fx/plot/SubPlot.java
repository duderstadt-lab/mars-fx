package de.mpg.biochem.mars.fx.plot;

import com.jfoenix.controls.JFXBadge;

import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.ChartPlugin;
import javafx.event.Event;
import javafx.scene.Cursor;
import javafx.scene.Node;

public interface SubPlot {
	public void resetXYZoom();
	public DefaultNumericAxis getXAxis();
	public void setXLabel(String xAxisLabel);
	public DefaultNumericAxis getYAxis();
	public void setYLabel(String yAxisLabel);
	public DatasetOptionsPane getDatasetOptionsPane();
	public JFXBadge getDatasetOptionsButton();
	public void setTool(ChartPlugin plugin, Cursor cursor);
	public void removeTools();
	public void removeIndicators();
	public Node getNode();
	public void fireEvent(Event event);
	public void update();
	//public void setTable(MarsTable table);
}
