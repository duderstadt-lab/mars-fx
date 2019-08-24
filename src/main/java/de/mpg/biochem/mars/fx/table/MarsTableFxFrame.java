package de.mpg.biochem.mars.fx.table;

import java.io.IOException;

import javax.swing.JFrame;

import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;

import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import de.mpg.biochem.mars.table.*;
import ij.WindowManager;

public class MarsTableFxFrame extends JFrame implements MarsTableWindow {

	private static final long serialVersionUID = 1L;

	@Parameter
	private LogService log;
	
	@Parameter
    private MarsTableService marsTableService;
	
    @Parameter
    private UIService uiService;

	private MarsTable table;
	private MarsTableFxView tableView;
	
	private BorderPane TableFrameLayout;

	private JFXPanel fxPanel;

	public MarsTableFxFrame(String name, MarsTable table, MarsTableService marsTableService) {
		//ij.context().inject(this);
		//this.ij = ij;
		this.marsTableService = marsTableService;
		this.table = table;
		this.setName(name);
		this.uiService = marsTableService.getUIService();
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
            loader.setLocation(MarsTableFxFrame.class.getResource("TableFrameLayout.fxml"));
            
            TableFrameLayout = (BorderPane) loader.load();

            Scene scene = new Scene(TableFrameLayout);
            this.fxPanel.setScene(scene);

            TableFrameController controller = loader.getController();
            controller.setFrame(this);
            
            tableView = new MarsTableFxView(table);
            TableFrameLayout.setCenter(tableView);
            
            this.setSize(600, 600);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	public MarsTable getTable() {
		return table;
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub
		
	}
}
