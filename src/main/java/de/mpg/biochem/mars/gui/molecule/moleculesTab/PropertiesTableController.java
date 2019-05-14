package de.mpg.biochem.mars.gui.molecule.moleculesTab;

import java.util.ArrayList;
import java.util.List;

import de.mpg.biochem.mars.molecule.Molecule;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

public class PropertiesTableController implements MoleculeSubTab {
    
	private Molecule molecule;
	
	private BorderPane borderPane;
	
    private TextField addParameterField;
    private TableView<ParameterRow> parameterTable;
    private ObservableList<ParameterRow> parameterRowList = FXCollections.observableArrayList();

    public PropertiesTableController() {        
        initialize();
    }

    private void initialize() {
    	parameterTable = new TableView<ParameterRow>();

        TableColumn<ParameterRow, String> ParameterColumn = new TableColumn<>("Parameter");
        ParameterColumn.setCellValueFactory(parameterRow ->
                new ReadOnlyObjectWrapper<>(parameterRow.getValue().getName())
        );
        ParameterColumn.setSortable(false);
        parameterTable.getColumns().add(ParameterColumn);
        
        //TODO how to also allow editing of this parameter
        TableColumn<ParameterRow, Double> valueColumn = new TableColumn<>("Value");
        valueColumn.setCellValueFactory(molIndexRow ->
                new ReadOnlyObjectWrapper<>(molIndexRow.getValue().getValue())
        );
        valueColumn.setSortable(false);
        parameterTable.getColumns().add(valueColumn);
        
        borderPane = new BorderPane();
        borderPane.setBottom(addParameterField);
        borderPane.setCenter(parameterTable);
    }
    
    public Node getNode() {
    	return borderPane;
    }
    
    public void loadData() {
    	parameterRowList.clear();

    	for (String parameter : molecule.getParameters().keySet()) {
        	parameterRowList.add(new ParameterRow(parameter));
        }
	}
    
    public void setMolecule(Molecule molecule) {
    	this.molecule = molecule;
    	loadData();
    }
    
    private class ParameterRow {
    	private String parameter;
    	
    	ParameterRow(String parameter) {
    		this.parameter = parameter;
    	}
    	
    	public String getName() {
    		return parameter;
    	}
    	
    	public double getValue() {
    		return molecule.getParameter(parameter);
    	}
    }
}
