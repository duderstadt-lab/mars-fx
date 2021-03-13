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
	
	public ShowVideoDialog(Window owner) {
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
		
		dialogPane.setContent(gridpane);
		
		dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		setResultConverter(dialogButton -> {
			return (dialogButton == ButtonType.OK) ? new SelectionResult(
					views.getSelectionModel().getSelectedItem()) : null;
		});
	}
	
	public class SelectionResult {
		public final int views;
		
		public SelectionResult(int views) {
			this.views = views;
		}
		
		public int getViewNumber() {
			return views;
		}
	}
}
