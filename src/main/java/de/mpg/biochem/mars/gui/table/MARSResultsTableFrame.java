package de.mpg.biochem.mars.gui.table;

import java.io.IOException;

import javax.swing.JFrame;

import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;

import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import de.mpg.biochem.mars.table.MARSResultsTable;
import de.mpg.biochem.mars.table.ResultsTableService;
import ij.WindowManager;

public class MARSResultsTableFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	@Parameter
	private LogService log;
	
	@Parameter
    private ResultsTableService resultsTableService;
	
    @Parameter
    private UIService uiService;

	private MARSResultsTable table;
	private MARSTableView tableView;
	
	private BorderPane TableFrameLayout;

	private JFXPanel fxPanel;

	public MARSResultsTableFrame(String name, MARSResultsTable table, ResultsTableService resultsTableService) {
		//ij.context().inject(this);
		//this.ij = ij;
		this.resultsTableService = resultsTableService;
		this.table = table;
		this.setName(name);
		this.uiService = resultsTableService.getUIService();
		table.setWindow(this);
		
		// add window to window manager
		// IJ1 style IJ2 doesn't seem to work...
		if (!uiService.isHeadless())
			WindowManager.addWindow(this);
	}
	
	public void reloadData() {
		tableView.reloadData();
	}

	/**
	 * JFXPanel creates a link between Swing and JavaFX.
	 */
	public void init() {
		//TODO add PlotPanel for JFXPanel..
		
		this.fxPanel = new JFXPanel();
		this.add(this.fxPanel);
		this.setVisible(true);

		// The call to runLater() avoid a mix between JavaFX thread and Swing thread.
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				initFX(fxPanel);
			}
		});

	}

	public void initFX(JFXPanel fxPanel) {	
		try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MARSResultsTableFrame.class.getResource("TableFrameLayout.fxml"));
            
            TableFrameLayout = (BorderPane) loader.load();

            Scene scene = new Scene(TableFrameLayout);
            this.fxPanel.setScene(scene);

            TableFrameController controller = loader.getController();
            controller.setFrame(this);
            
            tableView = new MARSTableView(table);
            TableFrameLayout.setCenter(tableView);
            
            this.setSize(600, 600);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	public MARSResultsTable getTable() {
		return table;
	}
}
