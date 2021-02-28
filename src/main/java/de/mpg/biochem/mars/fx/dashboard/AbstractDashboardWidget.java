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
package de.mpg.biochem.mars.fx.dashboard;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CLOSE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.REFRESH;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.QUESTION_CIRCLE_ALT;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.fx.dialogs.RoverConfirmationDialog;
import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.AbstractJsonConvertibleRecord;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.molecule.MoleculeArchiveService;
import de.mpg.biochem.mars.util.MarsUtil;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import javafx.scene.layout.AnchorPane;

import javafx.animation.RotateTransition;
import javafx.util.Duration;
import javafx.animation.Interpolator;
import javafx.animation.Animation;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;

import net.imagej.ops.Initializable;
import org.scijava.plugin.Parameter;
import org.scijava.ui.DialogPrompt.Result;

public abstract class AbstractDashboardWidget extends AbstractJsonConvertibleRecord
		implements MarsDashboardWidget, Initializable {

	protected AnchorPane rootPane;
	protected TabPane tabs;

	// This will hold the main widget content
	// plots or otherwise...
	protected Tab contentTab;

	protected static final int RESIZE_REGION = 2;
	protected double MINIMUM_WIDTH = 250;
	protected double MINIMUM_HEIGHT = 250;
	protected double y, x;
	protected AtomicBoolean running = new AtomicBoolean(false);
	protected boolean initHeight, initWidth;
	protected boolean dragX = false;
	protected boolean dragY = false;
	protected RotateTransition rt;
	protected Button closeButton, loadButton;

	@Parameter
	protected MarsDashboard parent;

	@Override
	public void initialize() {
		rootPane = new AnchorPane();
		tabs = new TabPane();
		tabs.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		tabs.setStyle("");
		tabs.getStylesheets().clear();
		tabs.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/WidgetTabPane.css");

		AnchorPane.setTopAnchor(tabs, 0.0);
		AnchorPane.setLeftAnchor(tabs, 0.0);
		AnchorPane.setRightAnchor(tabs, 0.0);
		AnchorPane.setBottomAnchor(tabs, 0.0);

		tabs.setBorder(new Border(
				new BorderStroke(Color.TRANSPARENT, BorderStrokeStyle.NONE, new CornerRadii(5), new BorderWidths(1))));

		rootPane.setBorder(new Border(
				new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, new CornerRadii(5), new BorderWidths(1))));

		// Add capture image button and resize button...
		// resize button will open a dialog that allows entering a custom size for the
		// widget...

		Text closeIcon = OctIconFactory.get().createIcon(CLOSE, "1.0em");
		closeButton = new Button();
		closeButton.setPickOnBounds(true);
		closeButton.setCenterShape(true);
		closeButton.setGraphic(closeIcon);
		closeButton.getStyleClass().add("icon-button");
		closeButton.setAlignment(Pos.CENTER);
		closeButton.setOnMouseClicked(e -> {
			close();
		});
		AnchorPane.setTopAnchor(closeButton, 5.0);
		AnchorPane.setLeftAnchor(closeButton, 5.0);
		closeButton.setPrefWidth(20);
		closeButton.setPrefHeight(20);

		Text syncIcon = OctIconFactory.get().createIcon(REFRESH, "1.0em");
		loadButton = new Button();
		loadButton.setGraphic(syncIcon);
		loadButton.setCenterShape(true);
		loadButton.getStyleClass().add("icon-button");
		loadButton.setAlignment(Pos.CENTER);

		rt = new RotateTransition(Duration.millis(500), loadButton);
		rt.setInterpolator(Interpolator.LINEAR);
		rt.setByAngle(0);
		rt.setByAngle(360);
		rt.setCycleCount(Animation.INDEFINITE);

		loadButton.setOnMouseClicked(e -> {
			if (getParent() != null) {
				if (isRunning()) {
					getParent().stopWidget(this);
				} else {
					rt.play();
					getParent().runWidget(this);
				}
			}
		});
		AnchorPane.setTopAnchor(loadButton, 5.0);
		AnchorPane.setRightAnchor(loadButton, 5.0);
		loadButton.setPrefWidth(20);
		loadButton.setPrefHeight(20);

		rootPane.getChildren().addAll(tabs, closeButton, loadButton);

		contentTab = new Tab();
		getTabPane().getTabs().add(contentTab);

		rootPane.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				mousePressed(event);
			}
		});

		rootPane.setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				mouseDragged(event);
			}
		});
		rootPane.setOnMouseMoved(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				mouseOver(event);
			}
		});
		tabs.setOnMouseMoved(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				mouseOver(event);
			}
		});
		rootPane.setOnMouseReleased(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				mouseReleased(event);
			}
		});
	}

	protected void mouseReleased(MouseEvent event) {
		dragX = dragY = false;
		rootPane.setCursor(Cursor.DEFAULT);
		tabs.setCursor(Cursor.DEFAULT);
	}

	protected void mouseOver(MouseEvent event) {
		if (isInDraggableY(event)) {
			rootPane.setCursor(Cursor.S_RESIZE);
		} else if (isInDraggableX(event)) {
			rootPane.setCursor(Cursor.E_RESIZE);
		} else {
			rootPane.setCursor(Cursor.DEFAULT);
		}
	}

	protected boolean isInDraggableY(MouseEvent event) {
		return event.getY() > (rootPane.getHeight() - RESIZE_REGION);
	}

	protected boolean isInDraggableX(MouseEvent event) {
		return event.getX() > (rootPane.getWidth() - RESIZE_REGION);
	}

	protected void mouseDragged(MouseEvent event) {
		if (!dragX && !dragY)
			return;
		
		Bounds boundsInScene = parent.getNode().localToScene(parent.getNode().getBoundsInLocal());
		if (!boundsInScene.contains(event.getSceneX(), event.getSceneY())) {
			return;
		}

		if (dragY) {
			double mousey = event.getY();
			double newHeight = rootPane.getMinHeight() + (mousey - y);
			if (newHeight > MINIMUM_HEIGHT) {
				rootPane.setMinHeight(newHeight);
				rootPane.setMaxHeight(newHeight);
				y = mousey;
			}
		}

		if (dragX) {
			double mousex = event.getX();
			
			double newWidth = rootPane.getMinWidth() + (mousex - x);
			if (newWidth > MINIMUM_WIDTH) {
				rootPane.setMinWidth(newWidth);
				rootPane.setMaxWidth(newWidth);
				x = mousex;
			}

			//int hCells = (int)((parent.getWidgetPane().getWidth() - 20) / parent.getWidgetPane().getCellWidth());
			int hCells = (int)(parent.getWidgetPane().getWidth() / parent.getWidgetPane().getCellWidth());
			double containerWidth = hCells*parent.getWidgetPane().getCellWidth();
			
			if (rootPane.getMinWidth() > containerWidth 
					&& rootPane.getMaxWidth() > containerWidth) {
				setWidth(containerWidth);
			}
			
		}

		parent.getWidgetPane().clearLayout();
		parent.getWidgetPane().layout();
	}

	protected void mousePressed(MouseEvent event) {
		if (isInDraggableX(event)) {
			dragX = true;
		} else if (isInDraggableY(event)) {
			dragY = true;
		} else
			return;

		if (!initHeight) {
			rootPane.setMinHeight(rootPane.getHeight());
			rootPane.setMaxHeight(rootPane.getHeight());
			initHeight = true;
		}

		y = event.getY();

		if (!initWidth) {
			rootPane.setMinWidth(rootPane.getWidth());
			rootPane.setMaxWidth(rootPane.getWidth());
			initWidth = true;
		}

		x = event.getX();
	}

	public void spin() {
		rt.play();
	}

	public void stopSpinning() {
		rt.stop();
	}

	@Override
	public Node getIcon() {
		return (Node) FontAwesomeIconFactory.get().createIcon(QUESTION_CIRCLE_ALT, "1.0em");
	}

	@Override
	public Node getNode() {
		return rootPane;
	}

	public void setContent(Node node) {
		contentTab.setContent(node);
	}

	public void setContent(Node icon, Node node) {
		contentTab.setContent(node);
		if (icon != null)
			getContentTab().setGraphic(icon);
	}

	protected Tab getContentTab() {
		return contentTab;
	}

	public TabPane getTabPane() {
		return tabs;
	}
	
	public double getWidth() {
		return rootPane.getMinWidth();
	}
	
	public void setWidth(double width) {
		if (width < MINIMUM_WIDTH)
			return;
		
		rootPane.setMinWidth(width);
		rootPane.setMaxWidth(width);
	}

	@Override
	public void setParent(MarsDashboard parent) {
		this.parent = parent;
	}

	@Override
	public MarsDashboard getParent() {
		return this.parent;
	}

	@Override
	public void close() {
		rt.stop();
		
		RoverConfirmationDialog alert = new RoverConfirmationDialog(getNode().getScene().getWindow(), 
				"Are you sure you want to remove the widget?");
		
		Optional<ButtonType> result = alert.showAndWait();
		if(result.get() == ButtonType.OK && parent != null) {
			parent.stopWidget(this);
			parent.removeWidget(this);
		}
	}

	@Override
	public boolean isRunning() {
		return running.get();
	}

	@Override
	public void setRunning(boolean running) {
		this.running.set(running);
	}

	@Override
	protected void createIOMaps() {
		
		setJsonField("Width", 
			jGenerator -> {
				jGenerator.writeNumberField("Width", rootPane.getWidth());
			}, 
			jParser -> {
				rootPane.setMinWidth(jParser.getDoubleValue());
				rootPane.setMaxWidth(jParser.getDoubleValue());
			});
			
			
		setJsonField("Height", 
			jGenerator -> {
				jGenerator.writeNumberField("Height", rootPane.getHeight());
			}, 
			jParser -> {
				rootPane.setMinHeight(jParser.getDoubleValue());
				rootPane.setMaxHeight(jParser.getDoubleValue());
			});
		
	}

	public abstract String getName();
}
