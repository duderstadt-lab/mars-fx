package de.mpg.biochem.mars.gui.molecule.moleculesTab;
/*
 * Copyright (c) 2016 Karl Tauber <karl at jformdesigner dot com>
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

import java.util.Arrays;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import org.controlsfx.control.ToggleSwitch;
import de.mpg.biochem.mars.gui.options.Options.RendererType;
import org.tbee.javafx.scene.layout.fxml.MigPane;

/**
 * Markdown extensions pane
 *
 * @author Karl Tauber
 */
public class DatasetOptionsPane extends MigPane {
	private class Ext {
		final String id;
		final String displayName;
		ToggleSwitch toggleSwitch;

		Ext(String id, String displayName) {
			this.id = id;
			this.displayName = displayName;
		}
	}

	private final Ext[] extensions;
	private final ListProperty<String> enabledExtensions = new SimpleListProperty<>();

	public DatasetOptionsPane() {
		setLayout(popover ? "insets dialog" : "insets 0");

		for (Ext ext : extensions) {
			boolean available = MarkdownExtensions.isAvailable(rendererType, ext.id);
			if (popover && !available)
				continue;

			ext.toggleSwitch = new ToggleSwitch(ext.displayName);
			ext.toggleSwitch.selectedProperty().addListener((ob, oldSelected, newSelected) -> {
				if (newSelected) {
					if (!enabledExtensions.contains(ext.id))
						enabledExtensions.add(ext.id);
				} else
					enabledExtensions.remove(ext.id);
			});

			if (!popover && !available)
				ext.toggleSwitch.setDisable(true);

			add(ext.toggleSwitch, "grow, wrap");
		}
	}
}

