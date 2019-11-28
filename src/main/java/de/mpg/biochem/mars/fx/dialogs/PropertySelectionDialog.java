package de.mpg.biochem.mars.fx.dialogs;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.controlsfx.control.ToggleSwitch;

import com.jfoenix.controls.JFXChipView;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.stage.Window;

import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

import javafx.scene.layout.BorderPane;

/**
 * Property selection dialog.
 *
 * @author Karl Duderstadt
 */
public class PropertySelectionDialog extends Dialog<PropertySelectionDialog.SelectionResult> {
	
	public PropertySelectionDialog(Window owner, Collection<String> possibleItems, String title, String itemListLabel) {
		this(owner, possibleItems, title, itemListLabel, null);
	}	
	
	public PropertySelectionDialog(Window owner, Collection<String> possibleItems, String title, String itemListLabel, String allLabel) {
		setTitle(title);
		initOwner(owner);
		setResizable(true);
		
		DialogPane dialogPane = getDialogPane();
		dialogPane.setMinWidth(250);

		Label itemList = new Label(itemListLabel);
		JFXChipView<String> chipView = new JFXChipView<String>();
		Label removeWithNoTags = new Label(allLabel);
		ToggleSwitch toggleSwitch = new ToggleSwitch();
		
		chipView.getSuggestions().addAll(possibleItems);
		
		VBox vBox = new VBox();
		vBox.getChildren().add(itemList);
		vBox.getChildren().add(chipView);
		if (allLabel != null) {
			BorderPane borderPane = new BorderPane();
			borderPane.setLeft(removeWithNoTags);
			borderPane.setRight(toggleSwitch);
			vBox.getChildren().add(borderPane);
		}
		
		dialogPane.setContent(vBox);
		
		dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		setResultConverter(dialogButton -> {
			return (dialogButton == ButtonType.OK) ? new SelectionResult(chipView.getChips(), toggleSwitch.isSelected()) : null;
		});
	}
	
	public class SelectionResult {
		public final List<String> list;
		public final boolean removeAll;
		
		public SelectionResult(List<String> list, boolean removeAll) {
			this.list = list;
			this.removeAll = removeAll;
		}
		
		public List<String> getList() {
			return this.list;
		}
		
		public boolean removeAll() {
			return this.removeAll;
		}
	}
}
