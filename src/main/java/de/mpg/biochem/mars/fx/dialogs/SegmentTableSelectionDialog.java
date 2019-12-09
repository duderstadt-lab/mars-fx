package de.mpg.biochem.mars.fx.dialogs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.stage.Window;

import javafx.scene.control.ComboBox;

import javafx.scene.layout.HBox;

/**
 * Segment table selection dialog.
 *
 * @author Karl Duderstadt
 */
public class SegmentTableSelectionDialog extends Dialog<SegmentTableSelectionDialog.SelectionResult> {
	
	protected HashMap<String, ArrayList<String>> tabNameToSegmentName;
	
	public SegmentTableSelectionDialog(Window owner, Set<ArrayList<String>> segmentTableNames, String title) {
		setTitle(title);
		initOwner(owner);
		setResizable(true);
		
		DialogPane dialogPane = getDialogPane();
		dialogPane.setMinWidth(200);
		
		HBox hbox = new HBox();
		hbox.getChildren().add(new Label("Segment table "));
		ComboBox<String> table = new ComboBox<String>();
		
		tabNameToSegmentName = new HashMap<String, ArrayList<String>>();
		
		for (ArrayList<String> segmentTableName : segmentTableNames) {
			String tabName;
			if (segmentTableName.get(2).equals(""))
				tabName = segmentTableName.get(1) + " vs " + segmentTableName.get(0);
			else 
				tabName = segmentTableName.get(1) + " vs " + segmentTableName.get(0) + " - " + segmentTableName.get(2);
			tabNameToSegmentName.put(tabName, segmentTableName);
		}
		
		table.getItems().addAll(tabNameToSegmentName.keySet());
		hbox.getChildren().add(table);
		
		dialogPane.setContent(hbox);
		
		dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		setResultConverter(dialogButton -> {
			return (dialogButton == ButtonType.OK) ? new SelectionResult(table.getSelectionModel().getSelectedItem()) : null;
		});
	}
	
	public class SelectionResult {
		public final String tableName;
		
		public SelectionResult(String tableName) {
			this.tableName = tableName;
		}
		
		public ArrayList<String> getSegmentTableName() {
			return tabNameToSegmentName.get(tableName);
		}
	}
}
