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

package de.mpg.biochem.mars.fx.util;

import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.MenuBar;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javafx.application.Platform;
import javafx.stage.Stage;

public class IJStage extends Frame {

	private Stage stage;
	private MenuBar menuBar;

	public IJStage(Stage stage) throws HeadlessException {
		super(stage.getTitle());
		this.stage = stage;

		// Create a menu bar specific to this window
		setupMenuBar();

		// Forward focus events between the JavaFX stage and this Frame
		setupFocusSync();

		// Make this Frame "invisible" but keep it in the window system
		// This is important so it can receive focus events and have a menu bar
		// but doesn't show as a duplicate window
		setSize(1, 1);
		setLocation(-100, -100); // Off-screen
		setUndecorated(true);
		setVisible(true);
	}

	private void setupMenuBar() {
		// Create a custom menu bar for this window
		menuBar = new MenuBar();

		// Add menus that make sense for your JavaFX application
		// No need to worry about accelerator conflicts since this
		// will only be active when your JavaFX stage has focus
		java.awt.Menu editMenu = new java.awt.Menu("Edit");
		java.awt.MenuItem unlockItem = new java.awt.MenuItem("Unlock");

		// Add action listener to the save menu item
		unlockItem.addActionListener(e -> {
			System.out.println("Unlock needs to be implemented");
		});

		editMenu.add(unlockItem);
		menuBar.add(editMenu);

		// Set this menu bar on the Frame
		setMenuBar(menuBar);
	}

	private void setupFocusSync() {
		// When JavaFX stage gets focus, make this Frame active in AWT/Swing
		stage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
			if (isFocused) {
				// Request focus asynchronously on AWT thread
				java.awt.EventQueue.invokeLater(() -> {
					toFront();
					requestFocus();
				});
			}
		});

		// Add a window listener to track when this Frame gets focus
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent e) {
				// When this Frame is activated, make sure JavaFX stage is front
				Platform.runLater(() -> {
					stage.toFront();
					stage.requestFocus();
				});
			}
		});
	}

	// Override key methods from Frame to proxy to JavaFX stage

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		Platform.runLater(() -> stage.setOpacity(visible ? 1.0 : 0.0));
	}

	@Override
	public void setBounds(Rectangle r) {
		Platform.runLater(() -> {
			stage.setX(r.x);
			stage.setY(r.y);
			stage.setWidth(r.width);
			stage.setHeight(r.height);
		});
		super.setBounds(r);
	}

	@Override
	public void setTitle(String title) {
		super.setTitle(title);
		Platform.runLater(() -> stage.setTitle(title));
	}

	// Call this method when closing your JavaFX stage
	public void cleanup() {
		setVisible(false);
		dispose();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Stage getStage() {
		return stage;
	}

	@Override
	public void toFront() {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				if (stage != null) stage.toFront();
			}
		});
	}
}
