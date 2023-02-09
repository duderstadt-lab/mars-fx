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

package de.mpg.biochem.mars.fx.options;

import de.mpg.biochem.mars.fx.Messages;
import de.mpg.biochem.mars.fx.util.MarsFxGlobalPreferences;
import de.mpg.biochem.mars.fx.util.Utils;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Window;

/**
 * Options dialog
 *
 * @author Karl Tauber
 */
public class OptionsDialog extends Dialog<Void> {

	public OptionsDialog(Window owner) {
		setTitle(Messages.get("OptionsDialog.title"));
		initOwner(owner);

		initComponents();

		tabPane.getStyleClass().add(TabPane.STYLE_CLASS_FLOATING);

		// add "Store in project" checkbox to buttonbar
		// boolean oldStoreInProject = Options.isStoreInProject();
		ButtonType storeInProjectButtonType = new ButtonType(Messages.get(
			"OptionsDialog.storeInProject.text"), ButtonData.LEFT);
		setDialogPane(new DialogPane() {

			@Override
			protected Node createButton(ButtonType buttonType) {
				if (buttonType == storeInProjectButtonType) {
					CheckBox storeInProjectButton = new CheckBox(buttonType.getText());
					ButtonBar.setButtonData(storeInProjectButton, buttonType
						.getButtonData());
					// storeInProjectButton.setSelected(oldStoreInProject);
					// storeInProjectButton.setDisable(ProjectManager.getActiveProject()
					// == null);
					return storeInProjectButton;
				}
				return super.createButton(buttonType);
			}
		});

		DialogPane dialogPane = getDialogPane();
		dialogPane.setContent(tabPane);
		dialogPane.getButtonTypes().addAll(storeInProjectButtonType, ButtonType.OK,
			ButtonType.CANCEL);

		// save options on OK clicked
		dialogPane.lookupButton(ButtonType.OK).addEventHandler(ActionEvent.ACTION,
			e -> {
				boolean newStoreInProject = ((CheckBox) dialogPane.lookupButton(
					storeInProjectButtonType)).isSelected();
				// if (newStoreInProject != oldStoreInProject)
				// Options.storeInProject(newStoreInProject);

				save();
				e.consume();
			});

		Utils.fixSpaceAfterDeadKey(dialogPane.getScene());

		// load options
		load();

		// select last tab
		int tabIndex = MarsFxGlobalPreferences.getState().getInt("lastOptionsTab",
			-1);
		if (tabIndex > 0 && tabIndex < tabPane.getTabs().size()) tabPane
			.getSelectionModel().select(tabIndex);

		// remember last selected tab
		setOnHidden(e -> {
			MarsFxGlobalPreferences.getState().putInt("lastOptionsTab", tabPane
				.getSelectionModel().getSelectedIndex());
		});
	}

	private void load() {
		markdownOptionsPane.load();
	}

	private void save() {
		markdownOptionsPane.save();
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY
		// //GEN-BEGIN:initComponents
		tabPane = new TabPane();
		generalTab = new Tab();
		editorTab = new Tab();
		markdownTab = new Tab();
		markdownOptionsPane = new MarkdownOptionsPane();
		stylesheetsTab = new Tab();

		// ======== tabPane ========
		{
			tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

			// ======== markdownTab ========
			{
				markdownTab.setText(Messages.get("OptionsDialog.markdownTab.text"));
				markdownTab.setContent(markdownOptionsPane);
			}

			tabPane.getTabs().addAll(generalTab, editorTab, markdownTab,
				stylesheetsTab);
		}
		// JFormDesigner - End of component initialization //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY //GEN-BEGIN:variables
	private TabPane tabPane;
	private Tab generalTab;
	private Tab editorTab;
	private Tab markdownTab;
	private MarkdownOptionsPane markdownOptionsPane;
	private Tab stylesheetsTab;
	// JFormDesigner - End of variables declaration //GEN-END:variables
}
