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

package de.mpg.biochem.mars.fx.util;

import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.stage.Stage;

public class IJStage extends Frame {

	private static final long serialVersionUID = 1L;

	private final Stage stage;

	// Guard against re-entrant front/focus handoffs that cause focus
	// oscillation on Linux when a second window (e.g. the Fiji console)
	// is open.
	private final AtomicBoolean raising = new AtomicBoolean(false);

	public IJStage(Stage stage) throws HeadlessException {
		super(stage.getTitle());
		this.stage = stage;
	}

	public void buildShadowFrame() {
		setupWindowMenuIntegration();

		setUndecorated(true);

		java.awt.Label status = new java.awt.Label(
				"Opening " + getTitle() + " …", java.awt.Label.CENTER);
		status.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 13));
		setLayout(new java.awt.BorderLayout());
		add(status, java.awt.BorderLayout.CENTER);

		setSize(1, 1);
		setLocation(-32000, -32000); // off-screen; only Linux/Mutter briefly raises it
		setVisible(true);
	}

	private void setupWindowMenuIntegration() {
		// ONE-DIRECTIONAL ONLY: when Fiji activates this shadow Frame (e.g. the
		// user picks it from the Window menu), raise the JavaFX stage. We do NOT
		// listen to stage focus and push it back to the Frame — that reverse
		// direction is a feedback loop that fights the window manager and makes
		// the stage focus oscillate, killing keyboard input and menus.
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent e) {
				if (!raising.compareAndSet(false, true)) return;
				Platform.runLater(() -> {
					try {
						if (stage != null) stage.toFront();
						// Note: toFront() only, NOT requestFocus(). Raising the
						// window lets the WM grant focus naturally; explicitly
						// grabbing focus is what triggers the war.
					} finally {
						raising.set(false);
					}
				});
			}
		});
	}

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

	@Override
	public void toFront() {
		// Proxy Fiji's frame.toFront() to raising the JavaFX stage, but guard
		// against re-entrancy and never pair it with a focus grab.
		if (!raising.compareAndSet(false, true)) return;
		Platform.runLater(() -> {
			try {
				if (stage != null) stage.toFront();
			} finally {
				raising.set(false);
			}
		});
	}

	public void cleanup() {
		setVisible(false);
		dispose();
	}

	public Stage getStage() {
		return stage;
	}
}
