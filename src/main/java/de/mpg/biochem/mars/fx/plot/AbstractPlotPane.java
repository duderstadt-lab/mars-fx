package de.mpg.biochem.mars.fx.plot;

import de.gsi.chart.axes.AxisLabelFormatter;
import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.plugins.ChartPlugin;
import de.gsi.chart.plugins.Panner;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.plugins.DataPointTooltip;

import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.input.MouseEvent;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;

import de.mpg.biochem.mars.fx.plot.tools.MarsZoomer;
//import de.mpg.biochem.mars.fx.plot.tools.MarsDataPointTooltip;
//import de.mpg.biochem.mars.fx.plot.tools.MarsPositionSelectionTool;
//import de.mpg.biochem.mars.fx.plot.tools.MarsRegionSelectionTool;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.fx.util.StyleSheetUpdater;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.control.PopOver.ArrowLocation;

import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.beans.value.ChangeListener;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

public abstract class AbstractPlotPane extends BorderPane implements PlotPane {
	
	public static final Predicate<MouseEvent> PAN_MOUSE_FILTER = event -> MouseEvents
		    .isOnlyPrimaryButtonDown(event) && MouseEvents.modifierKeysUp(event);
	
	protected ArrayList<SubPlot> charts;
	protected ArrayList<Action> tools;
	protected ToolBar toolBar;
	
	protected VBox chartsPane;
	
	protected static StyleSheetUpdater styleSheetUpdater;
	
	protected BooleanProperty gridlines = new SimpleBooleanProperty();
	
	protected BooleanProperty trackSelected = new SimpleBooleanProperty();
	protected BooleanProperty zoomXYSelected = new SimpleBooleanProperty();
	protected BooleanProperty zoomXSelected = new SimpleBooleanProperty();
	protected BooleanProperty zoomYSelected = new SimpleBooleanProperty();
	protected BooleanProperty panSelected = new SimpleBooleanProperty();
	
	protected DoubleProperty yAxisWidth = new SimpleDoubleProperty();
	
	protected IntegerProperty maxPointsCount = new SimpleIntegerProperty(10_000);
	
	protected PlotOptionsPane plotOptionsPane;
	
	protected ButtonBase propertiesButton;

	public AbstractPlotPane() {
		if (styleSheetUpdater == null)
			styleSheetUpdater = new StyleSheetUpdater();

		plotOptionsPane = new PlotOptionsPane();
		charts = new ArrayList<SubPlot>();
		tools = new ArrayList<Action>();
		chartsPane = new VBox();
		setCenter(chartsPane);
		
		gridlines.setValue(true);
		
		buildTools();
		setTop(createToolBar());
	}
	
	protected void buildTools() {
		
		Action trackCursor = new Action("Track", "Shortcut+T", CIRCLE_ALT, e -> setTool(trackSelected, () -> new DataPointTooltip(), Cursor.DEFAULT), 
				null, trackSelected);
		addTool(trackCursor);
		
		Action zoomXYCursor = new Action("select XY region", "Shortcut+S", ARROWS, e -> setTool(zoomXYSelected, () -> new MarsZoomer(false), Cursor.CROSSHAIR),
				null, zoomXYSelected);
		addTool(zoomXYCursor);
		
		Action zoomXCursor = new Action("select X region", "Shortcut+X", ARROWS_H, e -> setTool(zoomXSelected, () -> new MarsZoomer(AxisMode.X, false), Cursor.H_RESIZE),
				null, zoomXSelected);
		addTool(zoomXCursor);
		
		Action zoomYCursor = new Action("select Y region", "Shortcut+Y", ARROWS_V, e -> setTool(zoomYSelected, () -> new MarsZoomer(AxisMode.Y, false), Cursor.V_RESIZE),
				null, zoomYSelected);
		addTool(zoomYCursor);
		
		Action panCursor = new Action("pan", "Shortcut+P", HAND_PAPER_ALT, e -> setTool(panSelected, () -> {
			Panner panner = new Panner();
			panner.setMouseFilter(PAN_MOUSE_FILTER);
			return panner;
		}, Cursor.MOVE), null, panSelected);
		addTool(panCursor);
	}
	
	protected void addTool(Action action) {
		tools.add(action);
	}

	protected Node createToolBar() { 
		ToggleButton[] toolButtons = new ToggleButton[tools.size()];
		ToggleGroup toolGroup = new ToggleGroup();
		
		for (int i = 0; i < tools.size(); i++) {
			 if (tools.get(i) != null) {
				 toolButtons[i] = (ToggleButton) ActionUtils.createToolBarButton(tools.get(i));
				 toolButtons[i].setToggleGroup(toolGroup);
			 }
		}

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
			popOver.setTitle("Plot Settings");
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
		
		ButtonBase addSubPlot = ActionUtils.createToolBarButton(new Action("Add Subplot", "Shortcut+P", PLUS, e -> addChart()), "0.6em");
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
	
	protected void setTool(BooleanProperty selected, Supplier<ChartPlugin> supplier, Cursor cursor) {
		for (SubPlot subPlot : charts) {
			if (selected.get()) {
				subPlot.setTool(supplier.get(), cursor);
			} else {
				subPlot.removeTools();
			}
		}
	}
	
	public abstract void addChart();
	
	public void addChart(SubPlot subplot) {
		charts.add(subplot);
		
		VBox.setVgrow(subplot.getNode(), Priority.ALWAYS);
		chartsPane.getChildren().add(subplot.getNode());
		
		subplot.getChart().horizontalGridLinesVisibleProperty().bind(gridlines);
		subplot.getChart().verticalGridLinesVisibleProperty().bind(gridlines);
		
		for (SubPlot otherSubPlot : charts) {
			
			otherSubPlot.getYAxis().setMinWidth(50);
			
			if (subplot.equals(otherSubPlot))
				continue;
			
			subplot.getXAxis().setAutoRanging(false);
			
			if (subplot.getXAxis().getMax() > otherSubPlot.getXAxis().getMin() || subplot.getXAxis().getMin() > otherSubPlot.getXAxis().getMin()) {
				subplot.getXAxis().minProperty().bindBidirectional(otherSubPlot.getXAxis().minProperty());
				subplot.getXAxis().maxProperty().bindBidirectional(otherSubPlot.getXAxis().maxProperty());
			} else {
				subplot.getXAxis().maxProperty().bindBidirectional(otherSubPlot.getXAxis().maxProperty());
				subplot.getXAxis().minProperty().bindBidirectional(otherSubPlot.getXAxis().minProperty());
			}
		}
		
		//Ensures autoranging is turned off when zoom, pan etc are used
		//Then all linked plots moved together. Otherwise, one plot can lock the movement of another..
		subplot.getChart().getCanvas().setOnMousePressed(new EventHandler<MouseEvent>() {
		    @Override
		    public void handle(MouseEvent me) {
		      for (SubPlot subPlot : charts) {
		    	  subPlot.getXAxis().setAutoRanging(false);
		    	  subPlot.getYAxis().setAutoRanging(false);
		      }
		    }
		  });
			
		toolBar.getItems().add(toolBar.getItems().size() - 1, subplot.getDatasetOptionsButton());
		
		subplot.getYAxis().widthProperty().addListener(new ChangeListener<Number>(){
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				updatePlotWidths();
			}
	      });
		
		updateSubPlotBadges();
	}
	
	public void updatePlotWidths() {
		if (charts.size() > 1) {
			double globalYAxisWidth = 0;
			for (SubPlot subPlot : charts) {
				double calcWidth = Math.ceil(subPlot.getYAxis().calculateWidth());
				if (calcWidth > globalYAxisWidth)
					globalYAxisWidth = calcWidth;
			}
			
			for (SubPlot subPlot : charts) {
				double width = subPlot.getYAxis().widthProperty().get();
				if (width != globalYAxisWidth)
					subPlot.getYAxis().setMinWidth(globalYAxisWidth);
			}
		}
	}
	
	public void removeChart() {
		if (charts.size() > 1) {
			charts.remove(charts.size() - 1);
			chartsPane.getChildren().remove(chartsPane.getChildren().size() - 1);
			
			toolBar.getItems().remove(toolBar.getItems().size() - 2);
		}
		updateSubPlotBadges();
	}
	
	protected void updateSubPlotBadges() {
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
	
	public StyleSheetUpdater getStyleSheetUpdater() {
		return styleSheetUpdater;
	}
	
	@Override
	public Node getNode() {
		return this;
	}
	
	class PlotOptionsPane extends VBox  {
		public PlotOptionsPane() {
			BorderPane borderPane = new BorderPane();
			ToggleSwitch gridlineSwitch = new ToggleSwitch();
			gridlineSwitch.selectedProperty().bindBidirectional(gridlines);
			borderPane.setLeft(new Label("Gridlines"));
			borderPane.setRight(gridlineSwitch);
			getChildren().add(borderPane);
			this.setPrefWidth(200);
			this.setPadding(new Insets(10, 10, 10, 10));
		}
	}
}
