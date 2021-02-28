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

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.BOMB;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.REFRESH;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.STOP;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.REORDER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.scijava.Context;
import org.scijava.plugin.Parameter;

import com.fasterxml.jackson.core.JsonToken;
import com.jfoenix.controls.JFXMasonryPane;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import de.mpg.biochem.mars.fx.dialogs.RoverConfirmationDialog;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.molecule.AbstractJsonConvertibleRecord;
import de.mpg.biochem.mars.util.MarsUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Menu;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public abstract class AbstractDashboard<W extends MarsDashboardWidget> extends AbstractJsonConvertibleRecord
		implements MarsDashboard<W> {

	@Parameter
	protected MarsDashboardWidgetService marsDashboardWidgetService;

	private BorderPane borderPane;

	private ScrollPane scrollPane;
	private JFXMasonryPane widgetPane;
	private ToolBar toolbar;
	private ComboBox<String> widgetScriptLanguage;

	private final int MAX_THREADS = 1;

	private final List<WidgetRunnable> activeWidgets = Collections.synchronizedList(new ArrayList<>());

	private final ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS, runnable -> {
		Thread t = new Thread(runnable);
		t.setDaemon(true);
		return t;
	});

	protected ObservableList<W> widgets = FXCollections.observableArrayList();

	public AbstractDashboard(final Context context) {
		super();
		context.inject(this);

		borderPane = new BorderPane();

		Action removeAllWidgets = new Action("Remove all", null, BOMB, e -> {
			
			widgets.stream().filter(widget -> widget.isRunning()).forEach(widget -> stopWidget(widget));
			
			RoverConfirmationDialog alert = new RoverConfirmationDialog(getNode().getScene().getWindow(), 
					"Are you sure you want to remove all widgets?");
			
			Optional<ButtonType> result = alert.showAndWait();
			if(result.get() == ButtonType.OK) {
				widgets.clear();
				widgetPane.getChildren().clear();
			}
		});

		Action stopAllWidgets = new Action("Stop all", null, STOP,
				e -> widgets.stream().filter(widget -> widget.isRunning()).forEach(widget -> stopWidget(widget)));

		Action reloadWidgets = new Action("Reload", null, REFRESH, e -> {
			// executor.shutdownNow();
			widgets.stream().filter(widget -> !widget.isRunning()).forEach(widget -> runWidget(widget));
		});

		toolbar = new ToolBar();
		toolbar.getStylesheets().add("de/mpg/biochem/mars/fx/MarkdownWriter.css");

		// horizontal spacer
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		toolbar.getItems().add(spacer);

		// preview renderer type choice box
		widgetScriptLanguage = new ComboBox<>();
		widgetScriptLanguage.setFocusTraversable(false);
		ArrayList<String> languages = new ArrayList<>();
		languages.add("Groovy");
		languages.add("Python");
		widgetScriptLanguage.getItems().addAll(languages);
		widgetScriptLanguage.getSelectionModel().selectedItemProperty().addListener((ob, o, n) -> {
			if (marsDashboardWidgetService != null)
				marsDashboardWidgetService.setDefaultScriptingLanguage(n);
		});
		toolbar.getItems().add(widgetScriptLanguage);

		toolbar.getItems().addAll(ActionUtils.createToolBarButton(removeAllWidgets),
				ActionUtils.createToolBarButton(stopAllWidgets), ActionUtils.createToolBarButton(reloadWidgets));

		borderPane.setTop(toolbar);

		widgetPane = new JFXMasonryPane();
		widgetPane.setLayoutMode(JFXMasonryPane.LayoutMode.BIN_PACKING);
		widgetPane.setCellWidth(50);
		widgetPane.setCellHeight(50);
		widgetPane.setPadding(new Insets(10, 10, 10, 10));
		
		widgetPane.layoutBoundsProperty().addListener(new ChangeListener<Object>() {
			@Override
			public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
				//int hCells = (int)((getWidgetPane().getWidth() - 20) / getWidgetPane().getCellWidth());
				int hCells = (int)(getWidgetPane().getWidth() / getWidgetPane().getCellWidth());
				double containerWidth = hCells*getWidgetPane().getCellWidth();
				
				boolean outOfBoundsWidget = widgets.stream().filter(widget -> 
					widget.getWidth() > getWidgetPane().getWidth()).findFirst().isPresent();
				
				if (outOfBoundsWidget) {
					widgets.stream().filter(widget -> widget.getWidth() > containerWidth).forEach(widget ->
					widget.setWidth(containerWidth));
					
					getWidgetPane().clearLayout();
					getWidgetPane().layout();
				}
			}
		});

		scrollPane = new ScrollPane();
		scrollPane.setContent(widgetPane);
		scrollPane.setFitToWidth(true);
		borderPane.setCenter(scrollPane);
	}

	public void runWidget(MarsDashboardWidget widget) {
		executor.execute(new WidgetRunnable(widget));
	}

	public void stopWidget(MarsDashboardWidget widget) {
		activeWidgets.stream().filter(wr -> wr.getWidget().equals(widget)).findFirst()
				.ifPresent(activeWidget -> activeWidget.stop());
	}

	public Node getNode() {
		return borderPane;
	}

	public JFXMasonryPane getWidgetPane() {
		return widgetPane;
	}

	public ObservableList<W> getWidgets() {
		return widgets;
	}

	public void addWidget(W widget) {
		widgets.add(widget);
		widgetPane.getChildren().add(widget.getNode());
		scrollPane.layout();
	}

	public void removeWidget(W widget) {
		widgets.remove(widget);
		widgetPane.getChildren().remove(widget.getNode());
	}

	public ButtonBase createWidgetButton(String widgetName) {
		// HACK to get the Icon for the toolbar before any widgets have been added to
		// the Dashboard...
		// We create a dummy widget just to get the Icon but never use it.
		// What is the workaround - can't seem to use a static method because that
		// couldn't be in the interface.
		MarsDashboardWidget dummyWidgetForIcon = marsDashboardWidgetService.createWidget(widgetName);

		ButtonBase widgetButton = ActionUtils.createToolBarButton(widgetName, dummyWidgetForIcon.getIcon(), e -> {
			W widget = createWidget(widgetName);
			addWidget(widget);
		}, null);

		return widgetButton;
	}

	protected void discoverWidgets() {
		if (marsDashboardWidgetService == null)
			return;

		widgetScriptLanguage.getSelectionModel().select(marsDashboardWidgetService.getDefaultScriptingLanguage());

		Set<String> discoveredWidgets = getWidgetNames();

		ArrayList<Node> widgetButtons = new ArrayList<Node>();

		// Add all the expected widgets in the order defined by widgetToolbarOrder
		getWidgetToolbarOrder().stream().filter(widgetName -> discoveredWidgets.contains(widgetName))
				.forEach(widgetName -> widgetButtons.add(createWidgetButton(widgetName)));

		// Now add any newly discovered widgets besides the default set
		discoveredWidgets.stream().filter(widgetName -> !getWidgetToolbarOrder().contains(widgetName))
				.forEach(widgetName -> widgetButtons.add(createWidgetButton(widgetName)));

		toolbar.getItems().addAll(0, widgetButtons);
	}

	@Override
	protected void createIOMaps() {
		
		setJsonField("widgets", 
			jGenerator -> {
				jGenerator.writeArrayFieldStart("widgets");
				for (MarsDashboardWidget widget : widgets) {
					jGenerator.writeStartObject();
					jGenerator.writeStringField("name", widget.getName());
					jGenerator.writeFieldName("settings");
					widget.toJSON(jGenerator);
					jGenerator.writeEndObject();
				}
				jGenerator.writeEndArray();
			}, 
			jParser -> {
				while (jParser.nextToken() != JsonToken.END_ARRAY) {
					while (jParser.nextToken() != JsonToken.END_OBJECT) {
						W widget = null;
	
						if ("name".equals(jParser.getCurrentName())) {
							jParser.nextToken();
							widget = createWidget(jParser.getText());
							addWidget(widget);
						}
	
						jParser.nextToken();
	
						if ("settings".equals(jParser.getCurrentName())) {
							jParser.nextToken();
							if (widget != null)
								widget.fromJSON(jParser);
						}
					}
				}
			});
		
		/*
		 * 
		 * The fields below are needed for backwards compatibility.
		 * 
		 * Please remove for a future release.
		 * 
		 */
		
		setJsonField("Widgets", null, 
			jParser -> {
				while (jParser.nextToken() != JsonToken.END_ARRAY) {
					while (jParser.nextToken() != JsonToken.END_OBJECT) {
						W widget = null;
	
						if ("Name".equals(jParser.getCurrentName())) {
							jParser.nextToken();
							widget = createWidget(jParser.getText());
							addWidget(widget);
						}
	
						jParser.nextToken();
	
						if ("Settings".equals(jParser.getCurrentName())) {
							jParser.nextToken();
							if (widget != null)
								widget.fromJSON(jParser);
						}
					}
				}
			});
	}

	public abstract W createWidget(String widgetName);

	public abstract ArrayList<String> getWidgetToolbarOrder();

	public abstract Set<String> getWidgetNames();

	class WidgetRunnable implements Runnable {

		private final MarsDashboardWidget runnable;

		private Thread thread;
		private AtomicBoolean canceled = new AtomicBoolean(false);

		public WidgetRunnable(MarsDashboardWidget runnable) {
			this.runnable = runnable;
			runnable.setRunning(true);
			runnable.spin();
			activeWidgets.add(this);
		}

		@Override
		public void run() {
			if (canceled.get())
				return;
			thread = Thread.currentThread();
			runnable.run();
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					runnable.stopSpinning();
				}
			});
			activeWidgets.remove(this);
			runnable.setRunning(false);
		}

		public MarsDashboardWidget getWidget() {
			return runnable;
		}

		public void stop() {
			if (thread != null)
				thread.interrupt();
			canceled.set(true);
			runnable.stopSpinning();
			runnable.setRunning(false);
		}
	}
}
