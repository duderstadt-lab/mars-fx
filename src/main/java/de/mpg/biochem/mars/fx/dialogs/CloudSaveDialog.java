/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2023 Karl Duderstadt
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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

/**
 * Cloud location save dialog.
 *
 * @author Karl Duderstadt
 */
public class CloudSaveDialog extends
	Dialog<CloudSaveDialog.SelectionResult>
{

	public CloudSaveDialog(Window owner, String title,
                           List<String> recentSaveURLs)
	{
		setTitle(title);
		initOwner(owner);
		setResizable(true);

		DialogPane dialogPane = getDialogPane();
		dialogPane.setMinWidth(250);
		dialogPane.setMinHeight(250);
		dialogPane.setPrefWidth(500);
		dialogPane.setPrefHeight(350);

		VBox vBox = new VBox();
		vBox.setPadding(new Insets(15, 20, 15, 20));
		vBox.setSpacing(5);

		GridPane gridpane = new GridPane();

		Label urlLabel = new Label("URL");
		gridpane.add(urlLabel, 0, 0);
		GridPane.setMargin(urlLabel, new Insets(0, 5, 10, 5));

		TextField urlField = new TextField();
		urlField.setText("");
		gridpane.add(urlField, 1, 0);
		GridPane.setMargin(urlField, new Insets(0, 5, 10, 5));
		GridPane.setHgrow(urlField, Priority.ALWAYS);
		vBox.getChildren().add(gridpane);

		vBox.getChildren().add(new Label("Recent Locations"));

		ListView<String> templateList = new ListView<String>();

		ObservableList<String> templateFiles = FXCollections.observableArrayList(
				recentSaveURLs);
		templateList.setItems(templateFiles);
		templateList.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent click) {
				if (click.getClickCount() == 2) {
					urlField.setText(templateList.getSelectionModel()
							.getSelectedItem());
				}
			}
		});

		VBox.setVgrow(templateList, Priority.ALWAYS);
		HBox hBox = new HBox();
		HBox.setHgrow(templateList, Priority.ALWAYS);
		hBox.getChildren().add(templateList);
		vBox.getChildren().add(hBox);

		dialogPane.setContent(vBox);

		dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		setResultConverter(dialogButton -> {
			return (dialogButton == ButtonType.OK) ? new SelectionResult(urlField
				.getText()) : null;
		});
	}

	public class SelectionResult {

		public final String url;

		public SelectionResult(String url)
		{
			this.url = url;
		}

		public String getURL() {
			return url;
		}
	}
}
