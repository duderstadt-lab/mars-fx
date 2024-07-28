/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2024 Karl Duderstadt
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

package de.mpg.biochem.mars.fx.table;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.FLOPPY_ALT;

import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.SwingUtilities;

import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.mpg.biochem.mars.fx.dashboard.MarsDashboardWidgetService;
import de.mpg.biochem.mars.fx.plot.MarsTablePlotPane;
import de.mpg.biochem.mars.fx.table.dashboard.MarsTableDashboard;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.fx.util.IJStage;
import de.mpg.biochem.mars.molecule.AbstractJsonConvertibleRecord;
import de.mpg.biochem.mars.table.MarsTable;
import de.mpg.biochem.mars.table.MarsTableService;
import de.mpg.biochem.mars.table.MarsTableWindow;
import ij.WindowManager;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MarsTableFxFrame extends AbstractJsonConvertibleRecord implements
	MarsTableWindow
{

	@Parameter
	private LogService log;

	@Parameter
	private MarsTableService marsTableService;

	@Parameter
	protected MarsDashboardWidgetService marsDashboardWidgetService;

	@Parameter
	private UIService uiService;

	@Parameter
	private LogService logService;

	@Parameter
	private Context context;

	protected String title;
	protected Stage stage;
	protected IJStage ijStage;

	private MarsTable table;
	protected boolean windowStateLoaded = false;

	private TabPane tabPane;
	private Tab dataTableTab;
	private Tab plotTab;
	private Tab dashboardTab;
	private Tab commentTab;

	private MarsTablePlotPane plotPane;

	private MarsTableDashboard marsTableDashboardPane;
//	private CommentPane commentPane;

	protected MenuBar menuBar;

	protected BorderPane borderPane;

	protected static JsonFactory jfactory;

	private JFXPanel fxPanel;
	private Scene scene;

	public MarsTableFxFrame(String name, MarsTable table, final Context context) {
		context.inject(this);
		this.table = table;
		this.title = name;
		table.setWindow(this);
	}

	/**
	 * JFXPanel creates a link between Swing and JavaFX.
	 */
	public void init() {
		new JFXPanel(); // initializes JavaFX environment

		// The call to runLater() avoid a mix between JavaFX thread and Swing
		// thread.
		// Allows multiple runLaters in the same session...
		// Suggested here -
		// https://stackoverflow.com/questions/29302837/javafx-platform-runlater-never-running
		Platform.setImplicitExit(false);

		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				stage = new Stage();
				stage.setTitle(title);
				stage.setOnHidden(e -> SwingUtilities.invokeLater(() -> {
					close();
				}));

				ijStage = new IJStage(stage);

				SwingUtilities.invokeLater(() -> WindowManager.addWindow(ijStage));

				borderPane = new BorderPane();
				borderPane.getStylesheets().add(
					"de/mpg/biochem/mars/fx/molecule/MoleculeArchiveFxFrame.css");

				borderPane.setTop(buildMenuBar());
				borderPane.setCenter(buildTabs());

				scene = new Scene(borderPane);
				stage.setScene(scene);

				if (jfactory == null) jfactory = new JsonFactory();

				try {
					loadState();

					if (!windowStateLoaded) {
						stage.setWidth(800);
						stage.setHeight(600);
						stage.show();
					}
				}
				catch (IOException e) {
					logService.warn(
						"A problem was encountered when loading the rover file " + table
							.getFile().getAbsolutePath() + ".rover" +
							" containing the mars-fx display settings. " +
							"Please check the file to make sure the syntax is correct." +
							"Aborting and opening with the default settings.");
				}
			}
		});
	}

	protected MenuBar buildMenuBar() {
		Action fileSaveAsYAMTAction = new Action("Save as YAMT", "Shortcut+S",
			FLOPPY_ALT, e -> save());
		Action fileSaveAsCSVAction = new Action("Export to CSV", "Shortcut+C", null,
			e -> saveAsCSV());
		Action fileSaveAsJSONAction = new Action("Export to JSON", "Shortcut+C",
			null, e -> saveAsJSON());

		Action fileCloseAction = new Action("close", null, null, e -> close());

		Menu fileMenu = ActionUtils.createMenu("File", fileSaveAsYAMTAction,
			fileSaveAsCSVAction, fileSaveAsJSONAction, null, fileCloseAction);

		menuBar = new MenuBar(fileMenu);

		return menuBar;
	}

	private TabPane buildTabs() {
		tabPane = new TabPane();
		tabPane.setFocusTraversable(false);

		dataTableTab = new Tab();
		dataTableTab.setText("Table");
		dataTableTab.setContent(new MarsTableView(table));

		plotTab = new Tab();
		plotTab.setText("Plot");
		plotPane = new MarsTablePlotPane(table);
		plotTab.setContent(plotPane.getNode());

		dashboardTab = new Tab();
		dashboardTab.setText("");
		dashboardTab.setGraphic(MaterialIconFactory.get().createIcon(
			de.jensd.fx.glyphs.materialicons.MaterialIcon.DASHBOARD, "1.2em"));
		marsTableDashboardPane = new MarsTableDashboard(context, table);
		dashboardTab.setContent(marsTableDashboardPane.getNode());

		// commentTab = new Tab();
		// commentTab.setText("");

		// Region bookIcon = new Region();
		// bookIcon.getStyleClass().add("smallBookIcon");

		// commentTab.setGraphic(bookIcon);
		// commentPane = new CommentPane();
		// commentTab.setContent(commentPane.getNode());

		tabPane.getTabs().add(dataTableTab);
		tabPane.getTabs().add(plotTab);
		tabPane.getTabs().add(dashboardTab);
		// tabPane.getTabs().add(commentTab);
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);

		tabPane.setStyle("");
		tabPane.getStylesheets().clear();
		tabPane.getStylesheets().add(
			"de/mpg/biochem/mars/fx/molecule/MoleculeArchiveFxFrame.css");
		tabPane.getStylesheets().add(
			"de/mpg/biochem/mars/fx/table/TableWindowPane.css");

		tabPane.getSelectionModel().select(dataTableTab);
		/*
		tabPane.getSelectionModel().selectedItemProperty().addListener(
		  		new ChangeListener<Tab>() {
		  			@Override
		  			public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
		  					
		  				if (oldValue == commentTab) {
		  					commentPane.setEditMode(false);
		  					menuBar.getMenus().removeAll(commentPane.getMenus());
		  				}
		  				
		    			if (newValue == commentTab) {
		    				menuBar.getMenus().addAll(commentPane.getMenus());
						} 
		  			}
		  		});
		*/

		return tabPane;
	}

	private void save() {
		FileChooser fileChooser = new FileChooser();

		File saveAsFile = new File(System.getProperty("user.home"));
		fileChooser.setInitialDirectory(saveAsFile.getParentFile());

		String name = table.getName();
		if (!name.endsWith(".yamt")) name += ".yamt";
		fileChooser.setInitialFileName(name);

		File file = fileChooser.showSaveDialog(scene.getWindow());

		if (file != null) {
			try {
				table.saveAsYAMT(file.getAbsolutePath());
				saveState(file.getAbsolutePath());
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void saveAsCSV() {
		FileChooser fileChooser = new FileChooser();

		File saveAsFile = new File(System.getProperty("user.home"));
		fileChooser.setInitialDirectory(saveAsFile.getParentFile());

		String name = table.getName();
		if (!name.endsWith(".csv")) name += ".csv";
		fileChooser.setInitialFileName(name);

		File file = fileChooser.showSaveDialog(scene.getWindow());

		if (file != null) {
			try {
				table.saveAsCSV(file.getAbsolutePath());
				saveState(file.getAbsolutePath());
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void saveAsJSON() {
		FileChooser fileChooser = new FileChooser();

		File saveAsFile = new File(System.getProperty("user.home"));
		fileChooser.setInitialDirectory(saveAsFile.getParentFile());

		String name = table.getName();
		if (!name.endsWith(".json")) name += ".json";
		fileChooser.setInitialFileName(name);

		File file = fileChooser.showSaveDialog(scene.getWindow());

		if (file != null) {
			try {
				table.saveAsJSON(file.getAbsolutePath());
				saveState(file.getAbsolutePath());
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void close() {
		if (marsTableService.contains(table.getName())) marsTableService
			.removeTable(table);

		if (ijStage != null) WindowManager.removeWindow(ijStage);

		if (stage.isShowing()) stage.hide();
	}

	// Creates settings input and output maps to save the current state of the
	// program.
	@Override
	protected void createIOMaps() {

		setJsonField("window", jGenerator -> {
			jGenerator.writeObjectFieldStart("window");
			jGenerator.writeNumberField("x", stage.getX());
			jGenerator.writeNumberField("y", stage.getY());
			jGenerator.writeNumberField("width", stage.getWidth());
			jGenerator.writeNumberField("height", stage.getHeight());
			jGenerator.writeEndObject();
		}, jParser -> {
			Rectangle rect = new Rectangle(0, 0, 800, 600);
			while (jParser.nextToken() != JsonToken.END_OBJECT) {
				if ("x".equals(jParser.getCurrentName())) {
					jParser.nextToken();
					rect.x = jParser.getIntValue();
				}
				if ("y".equals(jParser.getCurrentName())) {
					jParser.nextToken();
					rect.y = jParser.getIntValue();
				}
				if ("width".equals(jParser.getCurrentName())) {
					jParser.nextToken();
					rect.width = jParser.getIntValue();
				}
				if ("height".equals(jParser.getCurrentName())) {
					jParser.nextToken();
					rect.height = jParser.getIntValue();
				}
			}

			windowStateLoaded = true;

			stage.setX(rect.x);
			stage.setY(rect.y);
			stage.setWidth(rect.width);
			stage.setHeight(rect.height);
			stage.show();
		});

		setJsonField("plotPane", jGenerator -> {
			jGenerator.writeFieldName("plotPane");
			plotPane.toJSON(jGenerator);
		}, jParser -> plotPane.fromJSON(jParser));

		setJsonField("marsTableDashboard", jGenerator -> {
			jGenerator.writeFieldName("marsTableDashboard");
			marsTableDashboardPane.toJSON(jGenerator);
		}, jParser -> marsTableDashboardPane.fromJSON(jParser));

//			setJsonField("comments", 
//					jGenerator -> {
//						jGenerator.writeStringField("comments", commentPane.getComments());
//					},
//					jParser -> commentPane.setComments(jParser.getText()));
	}

	protected void saveState(String path) throws IOException {
		OutputStream stream = new BufferedOutputStream(new FileOutputStream(
			new File(path + ".rover")));
		JsonGenerator jGenerator = jfactory.createGenerator(stream);
		jGenerator.useDefaultPrettyPrinter();
		toJSON(jGenerator);
		jGenerator.close();
		stream.flush();
		stream.close();
	}

	protected void loadState() throws IOException {
		if (table.getFile() == null) return;

		File stateFile = new File(table.getFile().getAbsolutePath() + ".rover");
		if (!stateFile.exists()) return;

		InputStream inputStream = new BufferedInputStream(new FileInputStream(
			stateFile));
		JsonParser jParser = jfactory.createParser(inputStream);
		fromJSON(jParser);
		jParser.close();
		inputStream.close();
	}

	@Override
	public void update() {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				dataTableTab.setContent(new MarsTableView(table));
			}
		});
	}
}
