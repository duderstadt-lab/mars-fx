package de.mpg.biochem.mars.fx.plot;

import cern.extjfx.chart.AxisMode;
import cern.extjfx.chart.NumericAxis;
import cern.extjfx.chart.XYChartPane;
import cern.extjfx.chart.XYChartPlugin;
import cern.extjfx.chart.data.DataReducingObservableList;
import cern.extjfx.chart.plugins.CrosshairIndicator;
import cern.extjfx.chart.plugins.DataPointTooltip;
import cern.extjfx.chart.plugins.Panner;
import cern.extjfx.chart.plugins.Zoomer;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.shape.Shape;
import javafx.scene.shape.Line;
import javafx.scene.shape.Circle;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.chart.XYChart;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.scene.text.*;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

import de.mpg.biochem.mars.table.*;

import cern.extjfx.chart.plugins.*;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubTab;
import de.mpg.biochem.mars.fx.options.MarkdownExtensionsPane;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.fx.util.StyleSheetUpdater;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.tbee.javafx.scene.layout.fxml.MigPane;

import com.jfoenix.controls.JFXBadge;

import de.mpg.biochem.mars.molecule.Molecule;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

public class PlotPane extends BorderPane implements MoleculeSubTab {
	
	public static final Predicate<MouseEvent> PAN_MOUSE_FILTER = event -> MouseEvents
		    .isOnlyPrimaryButtonDown(event) && MouseEvents.modifierKeysUp(event);
	
	private ArrayList<SubPlot> charts;
	private ToolBar toolBar;
	
	private StyleSheetUpdater styleSheetUpdater;
	
	enum ChartPlugin {
	  TRACK, ZOOMXY, ZOOMX, ZOOMY, PAN
	}
	
	private VBox chartsPane;
	
	private Molecule molecule;
	
	private BooleanProperty trackSelected = new SimpleBooleanProperty();
	private BooleanProperty zoomXYSelected = new SimpleBooleanProperty();
	private BooleanProperty zoomXSelected = new SimpleBooleanProperty();
	private BooleanProperty zoomYSelected = new SimpleBooleanProperty();
	private BooleanProperty panSelected = new SimpleBooleanProperty();
	
	private IntegerProperty maxPointsCount = new SimpleIntegerProperty(10_000);
	
	private PlotOptionsPane plotOptionsPane;
	
	private ButtonBase propertiesButton;

	public PlotPane() {
		setTop(createToolBar());
		
		styleSheetUpdater = new StyleSheetUpdater();
		
		plotOptionsPane = new PlotOptionsPane();
		
		charts = new ArrayList<SubPlot>();
		chartsPane = new VBox();
        setCenter(chartsPane);
        
		addChart();
	}

	private Node createToolBar() { 
		Action trackCursor = new Action("Track", "Shortcut+T", CIRCLE_ALT, e -> addPlugin(ChartPlugin.TRACK, Cursor.DEFAULT),
				null, trackSelected);
		Action zoomXYCursor = new Action("select XY region", "Shortcut+S", ARROWS, e -> addPlugin(ChartPlugin.ZOOMXY, Cursor.CROSSHAIR),
				null, zoomXYSelected);
		Action zoomXCursor = new Action("select X region", "Shortcut+X", ARROWS_H, e -> addPlugin(ChartPlugin.ZOOMX, Cursor.H_RESIZE),
				null, zoomXSelected);
		Action zoomYCursor = new Action("select Y region", "Shortcut+Y", ARROWS_V, e -> addPlugin(ChartPlugin.ZOOMY, Cursor.V_RESIZE),
				null, zoomYSelected);
		Action panCursor = new Action("pan", "Shortcut+P", HAND_PAPER_ALT, e -> addPlugin(ChartPlugin.PAN, Cursor.MOVE),
				null, panSelected);
		
		Node[] toolButtons = ActionUtils.createToolBarButtons(
				trackCursor,
				zoomXYCursor,
				zoomXCursor,
				zoomYCursor,
				panCursor);
		
		ToggleGroup toolGroup = new ToggleGroup();
		for (Node n : toolButtons)
			((ToggleButton)n).setToggleGroup(toolGroup);
		
		toolBar = new ToolBar(toolButtons);
		toolBar.getItems().add(new Separator());

		Action resetXYZoom = new Action("Reset Zoom", "Shortcut+R", EXPAND, e -> {
			for (SubPlot subPlot : charts)
				subPlot.resetXYZoom();
		});
		toolBar.getItems().add(ActionUtils.createToolBarButton(resetXYZoom));
		
		//settings
		propertiesButton = ActionUtils.createToolBarButton(new Action("settings", "Shortcut+S", COG, e -> {
			PopOver popOver = new PopOver();
			popOver.setTitle("Dataset");
			popOver.setHeaderAlwaysVisible(true);
			popOver.setAutoHide(false);
			popOver.setArrowLocation(ArrowLocation.TOP_CENTER);
			popOver.setContentNode(plotOptionsPane);
			popOver.show(propertiesButton);				
		}));
		toolBar.getItems().add(propertiesButton);
		
		// horizontal spacer
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		toolBar.getItems().add(spacer);
		
		ButtonBase addSubPlot = ActionUtils.createToolBarButton(new Action("Remove Subplot", "Shortcut+P", PLUS, e -> addChart()), "0.6em");
		addSubPlot.setStyle(
                "-fx-background-radius: 5em; " +
                "-fx-min-width: 13px; " +
                "-fx-min-height: 13px; " +
                "-fx-max-width: 13px; " +
                "-fx-max-height: 13px;"
        );
		ButtonBase removeSubPlot = ActionUtils.createToolBarButton(new Action("Remove Subplot", "Shortcut+M", MINUS, e -> removeChart()), "0.6em");
		removeSubPlot.setStyle(
                "-fx-background-radius: 5em; " +
                "-fx-min-width: 13px; " +
                "-fx-min-height: 13px; " +
                "-fx-max-width: 13px; " +
                "-fx-max-height: 13px;"
        );
		
		VBox plotManagementPane = new VBox(2);
		plotManagementPane.getChildren().add(addSubPlot);
		plotManagementPane.getChildren().add(removeSubPlot);
		toolBar.getItems().add(plotManagementPane);
		
		return toolBar;
	}
	
	private void addPlugin(ChartPlugin pluginName, Cursor cursor) {
		for (SubPlot subPlot : charts) {
			if (toolSelected()) {
				subPlot.addPlugin(generateChartPlugin(pluginName), cursor);
			} else {
				subPlot.removePlugins();
			}
		}
	}
	
	public void addChart() {
		SubPlot subplot = new SubPlot(this);
		if (molecule != null)
			subplot.setMolecule(molecule);
		charts.add(subplot);
		
		VBox.setVgrow(subplot.getNode(), Priority.ALWAYS);
		chartsPane.getChildren().add(subplot.getNode());
		
		for (SubPlot otherSubPlot : charts) {
			if (subplot.equals(otherSubPlot))
				continue;
			
			subplot.getXAxis().setAutoRanging(false);
			
			if (otherSubPlot.getXAxis().getLowerBound() > subplot.getXAxis().getUpperBound() || otherSubPlot.getXAxis().getLowerBound() > subplot.getXAxis().getLowerBound()) {
				subplot.getXAxis().lowerBoundProperty().bindBidirectional(otherSubPlot.getXAxis().lowerBoundProperty());
				subplot.getXAxis().upperBoundProperty().bindBidirectional(otherSubPlot.getXAxis().upperBoundProperty());
			} else {
				subplot.getXAxis().upperBoundProperty().bindBidirectional(otherSubPlot.getXAxis().upperBoundProperty());
				subplot.getXAxis().lowerBoundProperty().bindBidirectional(otherSubPlot.getXAxis().lowerBoundProperty());
			}
		}
			
		toolBar.getItems().add(toolBar.getItems().size() - 1, subplot.getDatasetOptionsButton());

		if (charts.size() > 1) {
			for (SubPlot subPlot : charts) {
				subPlot.getYAxis().setTickLabelRotation(270);
			}
		}
		updateSubPlotBadges();
	}
	
	public void removeChart() {
		if (charts.size() > 1) {
			charts.remove(charts.size() - 1);
			chartsPane.getChildren().remove(chartsPane.getChildren().size() - 1);
			
			toolBar.getItems().remove(toolBar.getItems().size() - 2);
			
			if (charts.size() == 1) {
				for (SubPlot subPlot : charts) {
					subPlot.getYAxis().setTickLabelRotation(0);
				}
			}
		}
		updateSubPlotBadges();
	}
	
	private void updateSubPlotBadges() {
		if (charts.size() > 1) {
			for (int num=1; num <= charts.size(); num++) {
				charts.get(num-1).getDatasetOptionsButton().setEnabled(true);
				charts.get(num-1).getDatasetOptionsButton().setText("" + num);
				charts.get(num-1).getDatasetOptionsButton().refreshBadge();
			}
		} else {
			charts.get(0).getDatasetOptionsButton().setEnabled(false);
			charts.get(0).getDatasetOptionsButton().refreshBadge();
		}
	}
	
	private XYChartPlugin<Number, Number> generateChartPlugin(ChartPlugin pluginName) {
		switch (pluginName) {
	      case TRACK: return new MARSDataPointTooltip<Number, Number>();
	      case ZOOMXY: return new Zoomer(true);
	      case ZOOMX: return new Zoomer(AxisMode.X, true);
	      case ZOOMY: return new Zoomer(AxisMode.Y, true);
	      case PAN: Panner panner = new Panner();
	    			panner.setMouseFilter(PAN_MOUSE_FILTER);
	                return panner;
	    }
		return null;
	}
	
	private boolean toolSelected() {
		if (trackSelected.get() || zoomXYSelected.get() || zoomXSelected.get() || zoomYSelected.get() || panSelected.get())
			return true;
		else
			return false;
	}
	
	public IntegerProperty maxPointsCount() {
		return maxPointsCount;
	}
	
	public StyleSheetUpdater getStyleSheetUpdater() {
		return this.styleSheetUpdater;
	}
	
	public void setMaxPointsCount(int maxPoints) {
		maxPointsCount.set(maxPoints);
	}

	@Override
	public void setMolecule(Molecule molecule) {
		this.molecule = molecule;
		for (SubPlot subPlot : charts) 
			subPlot.setMolecule(molecule);
	}
	
	class PlotOptionsPane extends MigPane  {
		public PlotOptionsPane() {
			setLayout("insets dialog");
			
			Spinner<Integer> pointsCountSpinner = new Spinner<>(10, 20_000, maxPointsCount.get(), 10);
			pointsCountSpinner.setPrefWidth(100);
			pointsCountSpinner.setEditable(true);
			maxPointsCount.bind(pointsCountSpinner.valueProperty());
			
			add(new Label("Points #: "), "");
			add(pointsCountSpinner, "");
		}
	}
}
