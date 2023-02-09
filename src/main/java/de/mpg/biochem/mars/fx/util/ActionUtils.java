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

package de.mpg.biochem.mars.fx.util;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import javafx.beans.property.BooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;

/**
 * Action utilities
 *
 * @author Karl Tauber
 */
public class ActionUtils {

	public static Menu createMenu(String text, Action... actions) {
		return new Menu(text, null, createMenuItems(actions));
	}

	public static MenuItem[] createMenuItems(Action... actions) {
		MenuItem[] menuItems = new MenuItem[actions.length];
		for (int i = 0; i < actions.length; i++) {
			menuItems[i] = (actions[i] != null) ? createMenuItem(actions[i])
				: new SeparatorMenuItem();
		}
		return menuItems;
	}

	public static MenuItem createMenuItem(Action action) {
		MenuItem menuItem = (action.selected != null) ? new CheckMenuItem(
			action.text) : new MenuItem(action.text);
		if (action.accelerator != null) menuItem.setAccelerator(action.accelerator);
		if (action.icon != null) menuItem.setGraphic(FontAwesomeIconFactory.get()
			.createIcon(action.icon));
		if (action.action != null) menuItem.setOnAction(action.action);
		if (action.disable != null) menuItem.disableProperty().bind(action.disable);
		if (action.selected != null) ((CheckMenuItem) menuItem).selectedProperty()
			.bindBidirectional(action.selected);
		return menuItem;
	}

	public static ToolBar createToolBar(Action... actions) {
		return new ToolBar(createToolBarButtons(actions));
	}

	public static ToolBar createToolBar(ButtonBase... buttonbase) {
		Node[] buttons = new Node[buttonbase.length];
		for (int i = 0; i < buttonbase.length; i++) {
			buttons[i] = (buttonbase[i] != null) ? buttonbase[i] : new Separator();
		}
		return new ToolBar(buttons);
	}

	public static Node[] createToolBarButtons(Action... actions) {
		Node[] buttons = new Node[actions.length];
		for (int i = 0; i < actions.length; i++) {
			buttons[i] = (actions[i] != null) ? createToolBarButton(actions[i])
				: new Separator();
		}
		return buttons;
	}

	public static ButtonBase createToolBarButton(Action action) {
		return createToolBarButton(action, "1.2em");
	}

	public static ButtonBase createToolBarButton(Action action, String size) {
		ButtonBase button = (action.selected != null) ? new ToggleButton()
			: new Button();
		button.setGraphic(FontAwesomeIconFactory.get().createIcon(action.icon,
			size));
		String tooltip = action.text;
		if (tooltip.endsWith("...")) tooltip = tooltip.substring(0, tooltip
			.length() - 3);
		if (action.accelerator != null) tooltip += " (" + action.accelerator
			.getDisplayText() + ')';
		button.setTooltip(new Tooltip(tooltip));
		button.setFocusTraversable(false);
		if (action.action != null) button.setOnAction(action.action);
		if (action.disable != null) button.disableProperty().bind(action.disable);
		if (action.selected != null) ((ToggleButton) button).selectedProperty()
			.bindBidirectional(action.selected);
		return button;
	}

	public static ButtonBase createToolBarButton(String text, Node icon,
		EventHandler<ActionEvent> action, BooleanProperty selected)
	{
		ButtonBase button = (selected != null) ? new ToggleButton() : new Button();
		button.setGraphic(icon);
		String tooltip = text;
		if (tooltip.endsWith("...")) tooltip = tooltip.substring(0, tooltip
			.length() - 3);
		button.setTooltip(new Tooltip(tooltip));
		button.setFocusTraversable(false);
		if (action != null) button.setOnAction(action);
		if (selected != null) ((ToggleButton) button).selectedProperty()
			.bindBidirectional(selected);
		return button;
	}
}
