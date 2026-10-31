/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2026 Karl Duderstadt
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

package de.mpg.biochem.mars.fx.molecule.moleculesTab.kymograph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.controlsfx.control.ToggleSwitch;

import com.jfoenix.controls.JFXChipView;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Options for generating figures for many molecules at once.
 */
public class GenerateFiguresDialog extends
	Dialog<GenerateFiguresDialog.Options>
{

	public GenerateFiguresDialog(Window owner, Collection<String> tags,
		String figureName)
	{
		setTitle("Generate " + figureName + "s");
		initOwner(owner);
		setResizable(true);

		DialogPane dialogPane = getDialogPane();
		dialogPane.setMinWidth(320);

		JFXChipView<String> chipView = new JFXChipView<>();
		chipView.getSuggestions().addAll(tags);

		ToggleGroup settingsGroup = new ToggleGroup();
		RadioButton currentSettings = new RadioButton(
			"Use the current settings for all molecules");
		RadioButton storedSettings = new RadioButton(
			"Use the settings stored with each molecule (current settings if none)");
		currentSettings.setToggleGroup(settingsGroup);
		storedSettings.setToggleGroup(settingsGroup);
		currentSettings.setSelected(true);

		ToggleSwitch overwrite = new ToggleSwitch();
		BorderPane overwriteRow = new BorderPane();
		overwriteRow.setLeft(new Label("Replace existing " + figureName + "s"));
		overwriteRow.setRight(overwrite);

		VBox vBox = new VBox(8, new Label(
			"Molecules with tags (leave empty for all molecules):"), chipView,
			currentSettings, storedSettings, overwriteRow);
		vBox.setPadding(new Insets(4));
		dialogPane.setContent(vBox);
		dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		setResultConverter(button -> button == ButtonType.OK ? new Options(
			new ArrayList<>(chipView.getChips()), storedSettings.isSelected(),
			overwrite.isSelected()) : null);
	}

	public static class Options {

		private final List<String> tags;
		private final boolean useStoredSettings;
		private final boolean overwrite;

		public Options(List<String> tags, boolean useStoredSettings,
			boolean overwrite)
		{
			this.tags = tags;
			this.useStoredSettings = useStoredSettings;
			this.overwrite = overwrite;
		}

		/**
		 * @return Only molecules with one of these tags, or all if empty.
		 */
		public List<String> getTags() {
			return tags;
		}

		public boolean useStoredSettings() {
			return useStoredSettings;
		}

		public boolean overwrite() {
			return overwrite;
		}
	}
}
