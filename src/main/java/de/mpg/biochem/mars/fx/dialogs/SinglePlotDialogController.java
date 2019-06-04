package de.mpg.biochem.mars.fx.dialogs;

import de.mpg.biochem.mars.fx.plot.PlotFrame;
import de.mpg.biochem.mars.fx.plot.PlotSeries;
import de.mpg.biochem.mars.table.MARSResultsTable;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;

public class SinglePlotDialogController {

    @FXML
    private ComboBox<String> xColumn;
    @FXML
    private ComboBox<String> yColumn;
    
    private MARSResultsTable table;

    private Stage dialogStage;
    private boolean okClicked = false;

    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     */
    @FXML
    private void initialize() {
    }

    /**
     * Sets the stage of this dialog.
     * 
     * @param dialogStage
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * Sets the table used to determine the available columns.
     * 
     * @param table
     */
    public void setTable(MARSResultsTable table) {
        this.table = table;

        xColumn.getItems().addAll(table.getColumnHeadingList());
        yColumn.getItems().addAll(table.getColumnHeadingList());
    }

    /**
     * Returns true if the user clicked OK, false otherwise.
     * 
     * @return
     */
    public boolean isOkClicked() {
        return okClicked;
    }

    /**
     * Called when the user clicks ok.
     */
    @FXML
    private void handleOk() {
    	//We should create a new plot window using the specified input columns
    	PlotFrame plotFrame = new PlotFrame();
    	plotFrame.setTitle("Plot");
    	plotFrame.init();
    	
    	if (table.hasColumn(xColumn.getValue()) && table.hasColumn(yColumn.getValue())) {
    		//plotFrame.getPlot().addSeries(new PlotSeries(table, xColumn.getValue(), yColumn.getValue()));
    		//plotFrame.getPlot().setXLabel(xColumn.getValue());
    		//plotFrame.getPlot().setYLabel(yColumn.getValue());
    	} else 
    		System.out.println("Column not Found!");
    	
        okClicked = true;
        dialogStage.close();
    }

    /**
     * Called when the user clicks cancel.
     */
    @FXML
    private void handleCancel() {
        dialogStage.close();
    }
}