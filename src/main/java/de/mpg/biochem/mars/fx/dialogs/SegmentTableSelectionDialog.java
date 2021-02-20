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
/*******************************************************************************
 * Copyright (C) 2019, Duderstadt Lab
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package de.mpg.biochem.mars.fx.dialogs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	
	protected HashMap<String,ArrayList<String>> tabNameToSegmentName;
	
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
