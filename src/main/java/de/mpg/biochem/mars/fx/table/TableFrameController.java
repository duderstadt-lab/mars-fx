package de.mpg.biochem.mars.fx.table;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import de.mpg.biochem.mars.fx.dialogs.SinglePlotDialogController;
import de.mpg.biochem.mars.table.MARSResultsTable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.scijava.Context;
import org.scijava.table.Column;
import org.scijava.table.DoubleColumn;
import org.scijava.table.GenericColumn;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

public class TableFrameController {

	@FXML
	private MARSResultsTableFrame tableFrame;
	
	public void setContext(Context context) {
		context.inject(this);
	}
	
	public void setFrame(MARSResultsTableFrame tableFrame) {
		this.tableFrame = tableFrame;
	}
	
	public boolean handleSinglePlot() {
		try {
	        // Load the fxml file and create a new stage for the popup dialog.
	        FXMLLoader loader = new FXMLLoader();
	        loader.setLocation(SinglePlotDialogController.class.getResource("SinglePlotDialog.fxml"));
	        AnchorPane page = (AnchorPane) loader.load();

	        // Create the dialog Stage.
	        Stage dialogStage = new Stage();
	        dialogStage.setTitle("Single plot");
	        dialogStage.initModality(Modality.WINDOW_MODAL);
	        //dialogStage.initOwner(primaryStage);
	        Scene scene = new Scene(page);
	        dialogStage.setScene(scene);

	        // Set the person into the controller.
	        SinglePlotDialogController controller = loader.getController();
	        controller.setDialogStage(dialogStage);
	        controller.setTable(tableFrame.getTable());

	        // Show the dialog and wait until the user closes it
	        dialogStage.showAndWait();

	        return controller.isOkClicked();
	    } catch (IOException e) {
	        e.printStackTrace();
	        return false;
	    }
	}
}



