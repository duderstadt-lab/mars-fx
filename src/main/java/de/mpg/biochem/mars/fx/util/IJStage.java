package de.mpg.biochem.mars.fx.util;

import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Window;

import javax.swing.SwingUtilities;

import javafx.application.Platform;
import javafx.stage.Stage;

public class IJStage extends Frame {
	
	private Stage stage;

	public IJStage(Stage stage) throws HeadlessException {
		super(stage.getTitle());
		this.stage = stage;
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
