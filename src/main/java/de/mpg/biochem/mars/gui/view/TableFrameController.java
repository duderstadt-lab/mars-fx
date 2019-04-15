package de.mpg.biochem.mars.gui.view;

import java.util.List;
import java.util.Map;

import de.mpg.biochem.mars.gui.MARSResultsTableFrame;
import de.mpg.biochem.mars.table.MARSResultsTable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.scijava.Context;
import org.scijava.table.Column;
import org.scijava.table.DoubleColumn;
import org.scijava.table.GenericColumn;

import javafx.fxml.FXML;

public class TableFrameController {

	@FXML
	private MARSResultsTableFrame tableFrame;
	
	public void setContext(Context context) {
		context.inject(this);
	}

	public void fill(MARSResultsTable table) {
		//Build and populate the table
	}
	
	public void setFrame(MARSResultsTableFrame tableFrame) {
		this.tableFrame = tableFrame;
	}
}



