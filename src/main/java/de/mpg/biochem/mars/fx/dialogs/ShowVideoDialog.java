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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.ColumnConstraints;
import javafx.geometry.HPos;

/**
 * Show video options dialog.
 *
 * @author Karl Duderstadt
 */
public class ShowVideoDialog extends Dialog<ShowVideoDialog.SelectionResult> {
	
	protected HashMap<String, ArrayList<String>> tabNameToSegmentName;
	
	public ShowVideoDialog(Window owner, Set<String> columnNames, Set<String> propertyNames) {
		setTitle("BDV Setup");
		initOwner(owner);
		setResizable(true);
		
		DialogPane dialogPane = getDialogPane();
		dialogPane.setMinWidth(250);
		
		GridPane gridpane = new GridPane();
		gridpane.setMinWidth(250);
		gridpane.setPrefWidth(250);

		Label viewsLabel = new Label("View number");
		gridpane.add(viewsLabel, 0, 4);
		GridPane.setMargin(viewsLabel, new Insets(5, 5, 5, 5));
		
		ComboBox<Integer> views = new ComboBox<Integer>();
		for (int i=1; i<7; i++)
			views.getItems().add(i);
		views.getSelectionModel().select(0);
		gridpane.add(views, 1, 4);
		GridPane.setMargin(views, new Insets(5, 5, 5, 5));
		
		Label volatileLabel = new Label("N5 volatile view");
		gridpane.add(volatileLabel, 0, 5);
		GridPane.setMargin(volatileLabel, new Insets(5, 5, 5, 5));
		
		ToggleSwitch volatileSwitch = new ToggleSwitch();
		gridpane.add(volatileSwitch, 1, 5);
		volatileSwitch.setSelected(true);
		GridPane.setMargin(volatileSwitch, new Insets(5, 5, 5, 5));
		
		dialogPane.setContent(gridpane);
		
		dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		setResultConverter(dialogButton -> {
			return (dialogButton == ButtonType.OK) ? new SelectionResult(
					views.getSelectionModel().getSelectedItem(),
					volatileSwitch.isSelected()) : null;
		});
	}
	
	public class SelectionResult {
		public final boolean useVolatile;
		public final int views;
		
		public SelectionResult(int views, boolean useVolatile) {
			this.views = views;
			this.useVolatile = useVolatile;
		}
		
		public int getViewNumber() {
			return views;
		}
		
		public boolean useVolatile() {
			return useVolatile;
		}
	}
}