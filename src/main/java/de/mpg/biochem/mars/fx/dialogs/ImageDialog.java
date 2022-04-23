/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2022 Karl Duderstadt
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
/*
 * Copyright (c) 2015 Karl Tauber <karl at jformdesigner dot com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.mpg.biochem.mars.fx.dialogs;

import java.nio.file.Path;

import de.mpg.biochem.mars.fx.Messages;
import de.mpg.biochem.mars.fx.controls.BrowseFileButton;
import de.mpg.biochem.mars.fx.controls.EscapeTextField;
import de.mpg.biochem.mars.fx.util.Utils;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;

/**
 * Dialog to enter a markdown image.
 *
 * @author Karl Tauber
 */
public class ImageDialog extends Dialog<String> {

	private final StringProperty image = new SimpleStringProperty();

	public ImageDialog(Window owner, Path basePath) {
		setTitle(Messages.get("ImageDialog.title"));
		initOwner(owner);
		setResizable(true);

		initComponents();

		linkBrowseFileButton.setBasePath(basePath);
		linkBrowseFileButton.addExtensionFilter(new ExtensionFilter(Messages.get(
			"ImageDialog.chooser.imagesFilter"), "*.png", "*.gif", "*.jpg", "*.svg"));
		linkBrowseFileButton.urlProperty().bindBidirectional(urlField
			.escapedTextProperty());

		DialogPane dialogPane = getDialogPane();
		dialogPane.setContent(pane);
		dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		dialogPane.lookupButton(ButtonType.OK).disableProperty().bind(urlField
			.escapedTextProperty().isEmpty().or(textField.escapedTextProperty()
				.isEmpty()));

		Utils.fixSpaceAfterDeadKey(dialogPane.getScene());

		image.bind(Bindings.when(titleField.escapedTextProperty().isNotEmpty())
			.then(Bindings.format("![%s](%s \"%s\")", textField.escapedTextProperty(),
				urlField.escapedTextProperty(), titleField.escapedTextProperty()))
			.otherwise(Bindings.format("![%s](%s)", textField.escapedTextProperty(),
				urlField.escapedTextProperty())));
		previewField.textProperty().bind(image);

		setResultConverter(dialogButton -> {
			return (dialogButton == ButtonType.OK) ? image.get() : null;
		});

		Platform.runLater(() -> {
			urlField.requestFocus();

			if (urlField.getText().startsWith("http://")) urlField.selectRange(
				"http://".length(), urlField.getLength());
		});
	}

	public void init(String url, String text, String title) {
		urlField.setText(url);
		textField.setText(text);
		titleField.setText(title);
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY
		// //GEN-BEGIN:initComponents
		pane = new VBox();
		Label urlLabel = new Label();
		urlField = new EscapeTextField();
		linkBrowseFileButton = new BrowseFileButton();
		Label textLabel = new Label();
		textField = new EscapeTextField();
		Label titleLabel = new Label();
		titleField = new EscapeTextField();
		Label previewLabel = new Label();
		previewField = new Label();

		// ======== pane ========
		{
			// pane.setCols("[shrink 0,fill][400,grow,fill]");
			// pane.setRows("[][][][]");

			// ---- urlLabel ----
			FlowPane urlBox = new FlowPane();
			urlLabel.setText(Messages.get("ImageDialog.urlLabel.text"));
			urlBox.getChildren().add(urlLabel);

			// ---- urlField ----
			urlField.setEscapeCharacters("()");
			urlField.setText("http://yourlink.com");
			urlField.setPromptText("http://yourlink.com");
			urlBox.getChildren().add(urlField);

			// ---- linkBrowseFileButton ----
			linkBrowseFileButton.setFocusTraversable(false);
			urlBox.getChildren().add(linkBrowseFileButton);

			pane.getChildren().add(urlBox);

			FlowPane textBox = new FlowPane();

			// ---- textLabel ----
			textLabel.setText(Messages.get("ImageDialog.textLabel.text"));
			textBox.getChildren().add(textLabel);

			// ---- textField ----
			textField.setEscapeCharacters("[]");
			textBox.getChildren().add(textField);

			pane.getChildren().add(textBox);

			FlowPane titleBox = new FlowPane();

			// ---- titleLabel ----
			titleLabel.setText(Messages.get("ImageDialog.titleLabel.text"));
			titleBox.getChildren().add(titleLabel);
			titleBox.getChildren().add(titleField);

			pane.getChildren().add(titleBox);

			FlowPane previewBox = new FlowPane();

			// ---- previewLabel ----
			previewLabel.setText(Messages.get("ImageDialog.previewLabel.text"));
			previewBox.getChildren().add(previewLabel);
			previewBox.getChildren().add(previewField);
		}
		// JFormDesigner - End of component initialization //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY //GEN-BEGIN:variables
	private VBox pane;
	private EscapeTextField urlField;
	private BrowseFileButton linkBrowseFileButton;
	private EscapeTextField textField;
	private EscapeTextField titleField;
	private Label previewField;
	// JFormDesigner - End of variables declaration //GEN-END:variables
}
