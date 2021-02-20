/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2021 Karl Duderstadt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
