package de.mpg.biochem.mars.fx.table;

import java.util.ArrayList;
import java.util.List;

import de.mpg.biochem.mars.fx.plot.PlotSeries;
import de.mpg.biochem.mars.fx.util.StyleSheetUpdater;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.table.MARSResultsTable;
import javafx.scene.chart.Axis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;

public class ScatterChartWithSegments extends ScatterChart<Number,Number> {
	private MARSResultsTable segmentsTable;
	private List<Shape> shapes = new ArrayList<>();
	private PlotSeries plotSeries;
	
	public ScatterChartWithSegments(MARSResultsTable segmentsTable, PlotSeries plotSeries, Axis<Number> xAxis, Axis<Number> yAxis) {
		super(xAxis, yAxis);
		this.segmentsTable = segmentsTable;
		this.plotSeries = plotSeries;
	}
	
	@Override
    public void layoutPlotChildren() {
        super.layoutPlotChildren();
        getPlotChildren().removeAll(shapes);
        shapes.clear();
        if (segmentsTable != null) {
	        for (int row=0;row<segmentsTable.getRowCount();row++) {
	    		if (!Double.isNaN(segmentsTable.getValue("x1", row)) && 
	    			!Double.isNaN(segmentsTable.getValue("y1", row)) &&	
	    			!Double.isNaN(segmentsTable.getValue("x2", row)) &&
	    			!Double.isNaN(segmentsTable.getValue("y2", row))) {
	                    double x1 = getXAxis().getDisplayPosition(segmentsTable.getValue("x1", row));
	                    double y1 = getYAxis().getDisplayPosition(segmentsTable.getValue("y1", row));
	                    double x2 = getXAxis().getDisplayPosition(segmentsTable.getValue("x2", row));
	                    double y2 = getYAxis().getDisplayPosition(segmentsTable.getValue("y2", row));
	                    Line line = new Line(x1, y1, x2, y2);
	                    if (plotSeries != null) {
	                    	line.setStroke(plotSeries.getSegmentsColor());
	                    	line.setStrokeWidth(1);
	                    }
	                    shapes.add(line);
	    		}	
	    	}
        }
        getPlotChildren().addAll(shapes);
    }
	
	public void updateStyle(StyleSheetUpdater styleSheetUpdater) {
		final Color color = plotSeries.getColor();
		final String width = plotSeries.getWidth();
		
		final String colorString = String.format("rgba(%d, %d, %d, %f)", Math.round(color.getRed()*255), Math.round(color.getGreen()*255), Math.round(color.getBlue()*255), color.getOpacity());
		final String symbolStyle = String.format(".chart-symbol { -fx-background-radius: %s; -fx-padding: %s; -fx-background-color: %s; }", width, width, colorString);
		
		styleSheetUpdater.addStyleSheet(this, symbolStyle);
	}
}
