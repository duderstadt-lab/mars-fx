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

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;

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

/**
 * Delete Molecules Dialog.
 *
 * @author Karl Duderstadt
 */
public class DeleteMoleculesDialog extends Dialog<DeleteMoleculesDialog.DeleteMoleculesResult> {
	private final StringProperty tagList = new SimpleStringProperty();

	public DeleteMoleculesDialog(Window owner) {
		setTitle("Delete Molecules");
		initOwner(owner);
		setResizable(true);
		
		DialogPane dialogPane = getDialogPane();

		Label tagList = new Label("Tags (comma separated list) ");
		TextField tagTextField = new TextField();
       
		CheckBox removeWithNoTags = new CheckBox("remove molecules with no tags");
		//removeWithNoTags.setSelected(false);
		
		GridPane grid = new GridPane();
		grid.add(tagList, 0, 0);
		grid.add(tagTextField, 1, 0);
		grid.add(removeWithNoTags, 0, 1);
		dialogPane.setContent(grid);
		
		dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		
		setResultConverter(dialogButton -> {
			return (dialogButton == ButtonType.OK) ? new DeleteMoleculesResult(getTagList(tagTextField.getText()), removeWithNoTags.selectedProperty().getValue()) : null;
		});
	}
	
	private String[] getTagList(String tagsToDelete) {
 		String[] tagList = tagsToDelete.split(",");
        for (int i=0; i<tagList.length; i++) {
        	tagList[i] = tagList[i].trim();
        }
 		
        return tagList;
	}
	
	public class DeleteMoleculesResult {
		public final String[] tagsList;
		public final boolean removeWithNoTags;
		
		public DeleteMoleculesResult(String[] tagsList, boolean removeWithNoTags) {
			this.tagsList = tagsList;
			this.removeWithNoTags = removeWithNoTags;
		}
		
		public String[] getTagList() {
			return this.tagsList;
		}
		
		public boolean removeWithNoTags() {
			return this.removeWithNoTags;
		}
	}
}
