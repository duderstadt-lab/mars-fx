/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2025 Karl Duderstadt
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

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Window;

/**
 * Rover error dialog.
 *
 * @author Karl Duderstadt
 */
public class RoverErrorDialog extends Alert {

	public RoverErrorDialog(Window owner, String message) {
		super(AlertType.ERROR);
		initModality(Modality.WINDOW_MODAL);
		initOwner(owner);
		Image image1 = new Image("de/mpg/biochem/mars/fx/images/RoverError.png");
		ImageView imageView = new ImageView(image1);
		imageView.setFitWidth(80);
		imageView.setFitHeight(80);
		setGraphic(imageView);
		setHeaderText(null);
		setResizable(true);
		setContentText(message);
		Text text = new Text(message);
		text.setWrappingWidth(400);
		final AnchorPane anchorPane = new AnchorPane();
		anchorPane.setPadding(new Insets(15, 25, 15, 15));
		AnchorPane.setTopAnchor(text, 0.0);
		AnchorPane.setLeftAnchor(text, 0.0);
		anchorPane.getChildren().add(text);
		getDialogPane().setContent(anchorPane);
	}
}
