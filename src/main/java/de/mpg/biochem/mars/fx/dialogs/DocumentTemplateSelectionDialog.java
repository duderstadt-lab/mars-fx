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

import java.io.File;
import java.io.FilenameFilter;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

/**
 * Property selection dialog.
 *
 * @author Karl Duderstadt
 */
public class DocumentTemplateSelectionDialog extends
	Dialog<DocumentTemplateSelectionDialog.SelectionResult>
{

	private File templateDirectory;

	public DocumentTemplateSelectionDialog(Window owner, String title,
		File templateDirectory)
	{
		this.templateDirectory = templateDirectory;

		setTitle(title);
		initOwner(owner);
		setResizable(true);

		DialogPane dialogPane = getDialogPane();
		dialogPane.setMinWidth(250);
		dialogPane.setMinHeight(250);
		dialogPane.setPrefWidth(350);
		dialogPane.setPrefHeight(350);

		VBox vBox = new VBox();
		vBox.setPadding(new Insets(15, 20, 15, 20));
		vBox.setSpacing(5);

		GridPane gridpane = new GridPane();

		Label nameLabel = new Label("Name");
		gridpane.add(nameLabel, 0, 0);
		GridPane.setMargin(nameLabel, new Insets(0, 5, 10, 5));

		TextField nameField = new TextField();
		nameField.setText("report");
		gridpane.add(nameField, 1, 0);
		GridPane.setMargin(nameField, new Insets(0, 5, 10, 5));
		vBox.getChildren().add(gridpane);

		vBox.getChildren().add(new Label("Select template"));

		ListView<String> templateList = new ListView<String>();

		if (templateDirectory != null && templateDirectory.exists() && templateDirectory.isDirectory()) {
			ObservableList<String> templateFiles = FXCollections.observableArrayList(
				templateDirectory.list());
			templateFiles.add(0, "<none>");
			templateList.setItems(templateFiles);
		}

		VBox.setVgrow(templateList, Priority.ALWAYS);
		vBox.getChildren().add(templateList);

		Button setTemplateButton = new Button("Set template directory");
		setTemplateButton.setOnAction(e -> {
			DirectoryChooser chooser = new DirectoryChooser();
			chooser.setTitle("Select template directory");
			if (templateDirectory != null && templateDirectory.exists() && templateDirectory.isDirectory()) 
				chooser.setInitialDirectory(templateDirectory);
			File selectedDirectory = chooser.showDialog(owner);

			if (selectedDirectory == null) return;

			FilenameFilter fileNameFilter = new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					if (name.startsWith(".")) return false;

					if (name.endsWith(".md")) return true;
					else return false;
				}
			};

			// Get all virtual archive folders
			String[] fileList = selectedDirectory.list(fileNameFilter);

			ObservableList<String> templateFiles = FXCollections.observableArrayList(
				fileList);
			templateFiles.add(0, "<none>");
			templateList.setItems(templateFiles);
			this.templateDirectory = selectedDirectory;
		});

		vBox.getChildren().add(setTemplateButton);

		dialogPane.setContent(vBox);

		dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		setResultConverter(dialogButton -> {
			String templateFileName = (templateList.getSelectionModel()
				.getSelectedIndex() != 0) ? templateList.getSelectionModel()
					.getSelectedItem() : null;
			return (dialogButton == ButtonType.OK) ? new SelectionResult(nameField
				.getText(), templateFileName, this.templateDirectory) : null;
		});
	}

	public class SelectionResult {

		public final String name, template;
		public final File selectedDirectory;

		public SelectionResult(String name, String template,
			File selectedDirectory)
		{
			this.name = name;
			this.template = template;
			this.selectedDirectory = selectedDirectory;
		}

		public String getName() {
			return name;
		}

		public String getTemplateFileName() {
			return template;
		}

		public File getSelectedDirectory() {
			return selectedDirectory;
		}
	}
}
