package de.mpg.biochem.mars.fx.table;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.FLOPPY_ALT;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

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
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import de.mpg.biochem.mars.fx.plot.MarsTablePlotPane;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.table.*;
import ij.WindowManager;

public class MarsTableFxFrame implements MarsTableWindow {

	@Parameter
	private LogService log;
	
	@Parameter
    private MarsTableService marsTableService;
	
    @Parameter
    private UIService uiService;

    private JFrame frame;
    protected String title;

    
	private MarsTable table;
	
	private TabPane tabPane;
	private Tab dataTableTab;
	private Tab plotTab;
	
	protected MenuBar menuBar;
	
	protected BorderPane borderPane;

	private JFXPanel fxPanel;

	public MarsTableFxFrame(String name, MarsTable table, MarsTableService marsTableService) {
		this.marsTableService = marsTableService;
		this.table = table;
		this.title = name;
		this.uiService = marsTableService.getUIService();
		table.setWindow(this);
	}

	/**
	 * JFXPanel creates a link between Swing and JavaFX.
	 */
	public void init() {
		frame = new JFrame(title);
		
		frame.addWindowListener(new WindowAdapter() {
	         public void windowClosing(WindowEvent e) {
				close();
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

		Scene scene = new Scene(borderPane);
		
		this.fxPanel.setScene(scene);

        frame.setSize(600, 600);
        frame.setVisible(true);
	}
	
	protected MenuBar buildMenuBar() {
		Action fileSaveAction = new Action("save", "Shortcut+S", FLOPPY_ALT, e -> save());
		Action fileSaveCopyAction = new Action("Save a Copy...", null, null, e -> saveCopy());
		Action fileCloseAction = new Action("close", null, null, e -> close());
		
		Menu fileMenu = ActionUtils.createMenu("File",
				fileSaveAction,
				fileSaveCopyAction,
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
		
		tabPane.getTabs().add(dataTableTab);
		tabPane.getTabs().add(plotTab);
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		
		tabPane.setStyle("");
		tabPane.getStylesheets().clear();
		tabPane.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/moleculesTab/MoleculeTablesPane.css");
		
		tabPane.getSelectionModel().select(dataTableTab);
		
		return tabPane;
	}
	
	public void save() {
		
	}
	
	public void saveCopy() {
		
	}
	
	public void close() {
    	
    }
	
	public MarsTable getTable() {
		return table;
	}

	@Override
	public void update() {
		//dataTableTab.setContent(new MarsTableView(table));
	}
}
