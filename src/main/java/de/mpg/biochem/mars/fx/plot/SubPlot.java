package de.mpg.biochem.mars.fx.plot;

import com.jfoenix.controls.JFXBadge;

import cern.extjfx.chart.NumericAxis;
import cern.extjfx.chart.XYChartPlugin;
import de.mpg.biochem.mars.table.MarsTable;
import javafx.event.Event;
import javafx.scene.Cursor;
import javafx.scene.Node;

public interface SubPlot {
	public void resetXYZoom();
	public NumericAxis getXAxis();
	public void setXLabel(String xAxisLabel);
	public NumericAxis getYAxis();
	public void setYLabel(String yAxisLabel);
	public DatasetOptionsPane getDatasetOptionsPane();
	public JFXBadge getDatasetOptionsButton();
	public void setTool(XYChartPlugin<Number, Number> plugin, Cursor cursor);
	public void removeTools();
	public void removeIndicators();
	public Node getNode();
	public void fireEvent(Event event);
	public void update();
	//public void setTable(MarsTable table);
}
