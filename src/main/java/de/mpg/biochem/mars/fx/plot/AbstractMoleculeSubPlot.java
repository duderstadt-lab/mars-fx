package de.mpg.biochem.mars.fx.plot;

import java.util.ArrayList;
import java.util.List;

import cern.extjfx.chart.NumericAxis;
import cern.extjfx.chart.XYChartPlugin;
import cern.extjfx.chart.data.DataReducingObservableList;
import cern.extjfx.chart.plugins.XRangeIndicator;
import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.RegionOfInterest;
import de.mpg.biochem.mars.table.MarsTable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Cursor;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;

public class AbstractMoleculeSubPlot<M extends Molecule> extends AbstractSubPlot implements MoleculeSubPane {
	
	protected M molecule;
	
	protected ArrayList<XRangeIndicator<Number>> xRangeIndicatorList;
	
	public AbstractMoleculeSubPlot(PlotPane plotPane, String plotTitle) {
		super(plotPane, plotTitle);
		
		xRangeIndicatorList = new ArrayList<XRangeIndicator<Number>>();
		
		getNode().addEventHandler(MoleculeEvent.MOLECULE_EVENT, this);
	}
	
	protected void addLine(PlotSeries plotSeries) {
		String xColumn = plotSeries.getXColumn();
		String yColumn = plotSeries.getYColumn();
		
		NumericAxis xAxis = createAxis();
		NumericAxis yAxis = createAxis();
		
		resetXYZoom();
		
		resetXAxis(xAxis);
		resetYAxis(yAxis);
		
		MarsTable segmentsTable = null;
		if (molecule.hasSegmentsTable(xColumn, yColumn))
			segmentsTable = molecule.getSegmentsTable(xColumn, yColumn);
		
		LineChartWithSegments lineChart = new LineChartWithSegments(segmentsTable, plotSeries, xAxis, yAxis);
		lineChart.setCreateSymbols(false);
		lineChart.setAnimated(false);
		
		List<Data<Number, Number>> data = new ArrayList<>();
		for (int row=0;row<getDataTable().getRowCount();row++) {
			double x = getDataTable().getValue(xColumn, row);
			double y = getDataTable().getValue(yColumn, row);
			
			if (!Double.isNaN(x) && !Double.isNaN(y))
				data.add(new Data<>(x, y));
		}
		
		//If the columns are entirely NaN values. Don't add he plot
		if (data.size() == 0)
			return;
		
		ObservableList<Data<Number, Number>> sourceData = FXCollections.observableArrayList(data);
		
		DataReducingObservableList<Number, Number> reducedData = new DataReducingObservableList<>(xAxis, sourceData);
		reducedData.maxPointsCountProperty().bind(plotPane.maxPointsCount());
		
		Series<Number, Number> series = new Series<>(plotSeries.getYColumn(), reducedData);
		lineChart.getData().add(series);
			
		lineChart.updateStyle(plotPane.getStyleSheetUpdater());
		
		chartPane.getOverlayCharts().add(lineChart);
		
		//Add indicators if they exist
		for (String regionName : molecule.getRegionNames()) {
			if (molecule.getRegion(regionName).getColumn().equals(xColumn)) {
				RegionOfInterest roi = molecule.getRegion(regionName);
				XRangeIndicator<Number> xRangeIndicator = new XRangeIndicator<>(roi.getStart(), roi.getEnd(), roi.getName());
				xRangeIndicatorList.add(xRangeIndicator);
				chartPane.getPlugins().add(xRangeIndicator);
			}
		}
	}
	
	protected void addScatter(PlotSeries plotSeries) {
		String xColumn = plotSeries.getXColumn();
		String yColumn = plotSeries.getYColumn();

		NumericAxis xAxis = createAxis();
		NumericAxis yAxis = createAxis();
		
		resetXYZoom();
		
		resetXAxis(xAxis);
		resetYAxis(yAxis);
		
		MarsTable segmentsTable = null;
		if (molecule.hasSegmentsTable(xColumn, yColumn))
			segmentsTable = molecule.getSegmentsTable(xColumn, yColumn);

		ScatterChartWithSegments scatterChart = new ScatterChartWithSegments(segmentsTable, plotSeries, xAxis, yAxis);
		scatterChart.setAnimated(false);
		
		List<Data<Number, Number>> data = new ArrayList<>();
		for (int row=0;row<getDataTable().getRowCount();row++) {
			double x = getDataTable().getValue(xColumn, row);
			double y = getDataTable().getValue(yColumn, row);
			
			if (!Double.isNaN(x) && !Double.isNaN(y))
				data.add(new Data<>(x, y));
		}
		
		//If the columns are entirely NaN values. Don't add he plot
		if (data.size() == 0)
			return;
		
		ObservableList<Data<Number, Number>> sourceData = FXCollections.observableArrayList(data);
		
		DataReducingObservableList<Number, Number> reducedData = new DataReducingObservableList<>(xAxis, sourceData);
		reducedData.maxPointsCountProperty().bind(plotPane.maxPointsCount());
		
		Series<Number, Number> series = new Series<>(plotSeries.getYColumn(), reducedData);
		scatterChart.getData().add(series);
		
		scatterChart.updateStyle(plotPane.getStyleSheetUpdater());
		
		chartPane.getOverlayCharts().add(scatterChart);
		
		//Add indicators if they exist
		for (String regionName : molecule.getRegionNames()) {
			if (molecule.getRegion(regionName).getColumn().equals(xColumn)) {
				RegionOfInterest roi = molecule.getRegion(regionName);
				XRangeIndicator<Number> xRangeIndicator = new XRangeIndicator<>(roi.getStart(), roi.getEnd(), roi.getName());
				chartPane.getPlugins().add(xRangeIndicator);
				xRangeIndicatorList.add(xRangeIndicator);
			}
		}
	}
	
	@Override
	public void removePlugins() {
		for (XYChartPlugin<Number, Number> plugin : chartPane.getPlugins())
			if (!xRangeIndicatorList.contains(plugin))
				chartPane.getPlugins().remove(plugin);
		chartPane.setCursor(Cursor.DEFAULT);
	}
	
	@Override
	public void handle(MoleculeEvent event) {
		event.invokeHandler(this);
		event.consume();
	}

	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onMoleculeSelectionChangedEvent(Molecule molecule) {
		this.molecule = (M) molecule;
		for (XYChartPlugin<Number, Number> plugin : chartPane.getPlugins())
			if (xRangeIndicatorList.contains(plugin))
				chartPane.getPlugins().remove(plugin);
		getDatasetOptionsPane().setTable(molecule.getDataTable());
		update();
	}

	@Override
	protected MarsTable getDataTable() {
		if (molecule != null) {
			return molecule.getDataTable();
		} else 
			return null;
	}
}
