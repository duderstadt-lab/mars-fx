/*******************************************************************************
 * Copyright (C) 2019, Duderstadt Lab
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package de.mpg.biochem.mars.fx.table;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.FLOPPY_ALT;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;

import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.mpg.biochem.mars.fx.dashboard.MarsDashboardWidgetService;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveSavedEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveSavingEvent;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.dashboard.MoleculeDashboard;
import de.mpg.biochem.mars.fx.plot.MarsTablePlotPane;
import de.mpg.biochem.mars.fx.table.dashboard.MarsTableDashboard;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.table.*;
import ij.WindowManager;
import ij.io.SaveDialog;

public class MarsTableFxFrame implements MarsTableWindow {

	@Parameter
	private LogService log;
	
	@Parameter
    private MarsTableService marsTableService;
	
	@Parameter
	protected MarsDashboardWidgetService marsDashboardWidgetService;
	
    @Parameter
    private UIService uiService;
    
    @Parameter
    private Context context;

    private JFrame frame;
    protected String title;

	private MarsTable table;
	
	private TabPane tabPane;
	private Tab dataTableTab;
	private Tab plotTab;
	private Tab dashboardTab;
	
	private MarsTableDashboard marsTableDashboardPane;
	
	protected MenuBar menuBar;
	
	protected BorderPane borderPane;

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
		frame = new JFrame(title);
		
		frame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent windowEvent) {
				//SwingUtilities.invokeLater(() -> {
					close();
				//});
			}
		});
		
		this.fxPanel = new JFXPanel();
		frame.add(this.fxPanel);
		
		// add window to window manager
		// IJ1 style IJ2 doesn't seem to work...
		if (!uiService.isHeadless())
			WindowManager.addWindow(frame);

		// The call to runLater() avoid a mix between JavaFX thread and Swing thread.
		// Allows multiple runLaters in the same session...
		// Suggested here - https://stackoverflow.com/questions/29302837/javafx-platform-runlater-never-running
		Platform.setImplicitExit(false);
		
		// The call to runLater() avoid a mix between JavaFX thread and Swing thread.
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				initFX(fxPanel);
			}
		});
	}

	public void initFX(JFXPanel fxPanel) {	
		borderPane = new BorderPane();
		borderPane.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/MoleculeArchiveFxFrame.css");
		
		borderPane.setTop(buildMenuBar());
		borderPane.setCenter(buildTabs());

		scene = new Scene(borderPane);
		
		this.fxPanel.setScene(scene);

		SwingUtilities.invokeLater(() -> { 
			frame.setSize(600, 600);
	        frame.setVisible(true);
		});
	}
	
	protected MenuBar buildMenuBar() {
		Action fileSaveAsYAMTAction = new Action("Save as YAMT", "Shortcut+S", FLOPPY_ALT, e -> save());
		Action fileSaveAsCSVAction = new Action("Export to CSV", "Shortcut+C", null, e -> saveAsCSV());
		Action fileSaveAsJSONAction = new Action("Export to JSON", "Shortcut+C", null, e -> saveAsJSON());
		
		Action fileCloseAction = new Action("close", null, null, e -> close());
		
		Menu fileMenu = ActionUtils.createMenu("File",
				fileSaveAsYAMTAction,
				fileSaveAsCSVAction,
				fileSaveAsJSONAction,
				null,
				fileCloseAction);

		menuBar = new MenuBar(fileMenu);
		
		return menuBar;
	}
	
	private TabPane buildTabs() {
		tabPane = new TabPane();
		tabPane.setFocusTraversable(false);
		
		dataTableTab = new Tab();		
		dataTableTab.setText("DataTable");
		dataTableTab.setContent(new MarsTableView(table));
		
		plotTab = new Tab();
		plotTab.setText("Plot");
		MarsTablePlotPane plotPane = new MarsTablePlotPane(table);
		plotTab.setContent(plotPane.getNode());
		
		dashboardTab = new Tab();
		dashboardTab.setText("");
		dashboardTab.setGraphic(MaterialIconFactory.get().createIcon(de.jensd.fx.glyphs.materialicons.MaterialIcon.DASHBOARD, "1.0em"));
		marsTableDashboardPane = new MarsTableDashboard(context, table);
		dashboardTab.setContent(marsTableDashboardPane.getNode());
		
		tabPane.getTabs().add(dataTableTab);
		tabPane.getTabs().add(plotTab);
		tabPane.getTabs().add(dashboardTab);
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		
		tabPane.setStyle("");
		tabPane.getStylesheets().clear();
		tabPane.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/MoleculeArchiveFxFrame.css");
		tabPane.getStylesheets().add("de/mpg/biochem/mars/fx/table/TableWindowPane.css");
		
		tabPane.getSelectionModel().select(dataTableTab);
		
		return tabPane;
	}
	
	private void save() {
		FileChooser fileChooser = new FileChooser();
		
		File saveAsFile = new File(System.getProperty("user.home"));
		fileChooser.setInitialDirectory(saveAsFile.getParentFile());
		
		String name = table.getName();
		if (!name.endsWith(".yamt"))
			name += ".yamt";
		fileChooser.setInitialFileName(name);

		File file = fileChooser.showSaveDialog(scene.getWindow());
		
		if (file != null) {
			try {
				table.saveAsYAMT(file.getAbsolutePath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void saveAsCSV() {
		FileChooser fileChooser = new FileChooser();
		
		File saveAsFile = new File(System.getProperty("user.home"));
		fileChooser.setInitialDirectory(saveAsFile.getParentFile());
		
		String name = table.getName();
		if (!name.endsWith(".csv"))
			name += ".csv";
		fileChooser.setInitialFileName(name);

		File file = fileChooser.showSaveDialog(scene.getWindow());
		
		if (file != null) {
			try {
				table.saveAsCSV(file.getAbsolutePath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void saveAsJSON() {
		FileChooser fileChooser = new FileChooser();
		
		File saveAsFile = new File(System.getProperty("user.home"));
		fileChooser.setInitialDirectory(saveAsFile.getParentFile());
		
		String name = table.getName();
		if (!name.endsWith(".json"))
			name += ".json";
		fileChooser.setInitialFileName(name);

		File file = fileChooser.showSaveDialog(scene.getWindow());
		
		if (file != null) {
			try {
				table.saveAsJSON(file.getAbsolutePath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void close() {
		if (marsTableService.contains(table.getName()))
			marsTableService.removeTable(table);
		
		if (!uiService.isHeadless())
			WindowManager.removeWindow(frame);
		
		//frame.setVisible(true);
		frame.dispose();
    }

	@Override
	public void update() {
		dataTableTab.setContent(new MarsTableView(table));
	}
}
