package de.mpg.biochem.mars.fx.dialogs;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.controlsfx.control.ToggleSwitch;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.stage.Window;

import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.scene.control.TextField;

/**
 * Show video options dialog.
 *
 * @author Karl Duderstadt
 */
public class ShowVideoDialog extends Dialog<ShowVideoDialog.SelectionResult> {
	
	protected HashMap<String, ArrayList<String>> tabNameToSegmentName;
	
	public ShowVideoDialog(Window owner, Set<String> columnNames, Set<String> propertyNames) {
		setTitle("BDV Options");
		initOwner(owner);
		setResizable(true);
		
		DialogPane dialogPane = getDialogPane();
		dialogPane.setMinWidth(200);
		
		GridPane gridpane = new GridPane();
		
		Label moleculeLocationLabel = new Label("Specify molecule location:");
		gridpane.add(moleculeLocationLabel, 0, 0);
		GridPane.setMargin(moleculeLocationLabel, new Insets(5, 5, 5, 5));
		
		Label xColumnLabel = new Label("X column");
		gridpane.add(xColumnLabel, 0, 1);
		GridPane.setMargin(xColumnLabel, new Insets(5, 5, 5, 5));
		
		ComboBox<String> xColumn = new ComboBox<String>();
		xColumn.getItems().addAll(columnNames.stream().sorted().collect(toList()));
		xColumn.getSelectionModel().select("x");
		gridpane.add(xColumn, 1, 1);
		GridPane.setMargin(xColumn, new Insets(5, 5, 5, 5));
		
		Label yColumnLabel = new Label("Y column");
		gridpane.add(yColumnLabel, 0, 2);
		GridPane.setMargin(yColumnLabel, new Insets(5, 5, 5, 5));
		
		ComboBox<String> yColumn = new ComboBox<String>();
		yColumn.getItems().addAll(columnNames.stream().sorted().collect(toList()));
		yColumn.getSelectionModel().select("y");
		gridpane.add(yColumn, 1, 2);
		GridPane.setMargin(yColumn, new Insets(5, 5, 5, 5));
		
		Label usePropertiesLabel = new Label("Use properties");
		gridpane.add(usePropertiesLabel, 0, 3);
		GridPane.setMargin(usePropertiesLabel, new Insets(5, 5, 5, 5));
		
		ToggleSwitch usePropertiesSwitch = new ToggleSwitch();
		gridpane.add(usePropertiesSwitch, 1, 3);
		GridPane.setMargin(usePropertiesSwitch, new Insets(5, 5, 5, 5));
		
		Label xPropertyLabel = new Label("X parameter");
		gridpane.add(xPropertyLabel, 0, 4);
		GridPane.setMargin(xPropertyLabel, new Insets(5, 5, 5, 5));
		
		ComboBox<String> xProperty = new ComboBox<String>();
		xProperty.getItems().addAll(propertyNames.stream().sorted().collect(toList()));
		gridpane.add(xProperty, 1, 4);
		GridPane.setMargin(xProperty, new Insets(5, 5, 5, 5));
		
		Label yPropertyLabel = new Label("Y parameter");
		gridpane.add(yPropertyLabel, 0, 5);
		GridPane.setMargin(yPropertyLabel, new Insets(5, 5, 5, 5));
		
		ComboBox<String> yProperty = new ComboBox<String>();
		yProperty.getItems().addAll(propertyNames.stream().sorted().collect(toList()));
		gridpane.add(yProperty, 1, 5);
		GridPane.setMargin(yProperty, new Insets(5, 5, 5, 5));
		
		Label viewsLabel = new Label("View number");
		gridpane.add(viewsLabel, 0, 6);
		GridPane.setMargin(viewsLabel, new Insets(5, 5, 5, 5));
		
		ComboBox<Integer> views = new ComboBox<Integer>();
		for (int i=1; i<7; i++)
			views.getItems().add(i);
		views.getSelectionModel().select(0);
		gridpane.add(views, 1, 6);
		GridPane.setMargin(views, new Insets(5, 5, 5, 5));
		
		Label overlayLabel = new Label("Show Overlay");
		gridpane.add(overlayLabel, 0, 7);
		GridPane.setMargin(overlayLabel, new Insets(5, 5, 5, 5));
		
		ToggleSwitch overlaySwitch = new ToggleSwitch();
		gridpane.add(overlaySwitch, 1, 7);
		overlaySwitch.setSelected(true);
		GridPane.setMargin(overlaySwitch, new Insets(5, 5, 5, 5));
		
		Label volatileLabel = new Label("N5 volatile view");
		gridpane.add(volatileLabel, 0, 8);
		GridPane.setMargin(volatileLabel, new Insets(5, 5, 5, 5));
		
		ToggleSwitch volatileSwitch = new ToggleSwitch();
		gridpane.add(volatileSwitch, 1, 8);
		volatileSwitch.setSelected(true);
		GridPane.setMargin(volatileSwitch, new Insets(5, 5, 5, 5));
		
		dialogPane.setContent(gridpane);
		
		dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		setResultConverter(dialogButton -> {
			return (dialogButton == ButtonType.OK) ? new SelectionResult(
					xColumn.getSelectionModel().getSelectedItem(),
					yColumn.getSelectionModel().getSelectedItem(),
					xProperty.getSelectionModel().getSelectedItem(),
					yProperty.getSelectionModel().getSelectedItem(),
					usePropertiesSwitch.isSelected(),
					views.getSelectionModel().getSelectedItem(),
					overlaySwitch.isSelected(),
					volatileSwitch.isSelected()) : null;
		});
	}
	
	public class SelectionResult {
		public final String xColumn, yColumn, xProperty, yProperty;
		public final boolean useProperties, useVolatile, showOverlay;
		public final int views;
		
		public SelectionResult(String xColumn, String yColumn, 
				String xProperty, String yProperty, 
				boolean useProperties, int views, boolean showOverlay, boolean useVolatile) {
			this.xColumn = xColumn;
			this.yColumn = yColumn;
			this.xProperty = xProperty;
			this.yProperty = yProperty;
			this.useProperties = useProperties;
			this.views = views;
			this.showOverlay = showOverlay;
			this.useVolatile = useVolatile;
		}
		
		public String getXColumn() {
			return xColumn;
		}
		
		public String getYColumn() {
			return yColumn;
		}
		
		public String getXProperty() {
			return xProperty;
		}
		
		public String getYProperty() {
			return yProperty;
		}
		
		public boolean useProperties() {
			return useProperties;
		}
		
		public int getViewNumber() {
			return views;
		}
		
		public boolean showOverlay() {
			return showOverlay;
		}
		
		public boolean useVolatile() {
			return useVolatile;
		}
	}
}