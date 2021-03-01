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
package de.mpg.biochem.mars.fx.molecule.metadataTab;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.controlsfx.control.textfield.CustomTextField;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.metadata.GenericModel;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.metadata.MarsOMEImage;
import de.mpg.biochem.mars.metadata.MarsOMEPlane;
import impl.org.controlsfx.skin.CustomTextFieldSkin;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Label;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ome.xml.meta.OMEXMLMetadata;

/**
 * Inspired by FXML Controller class from Hadrien Mary. 
 * 
 * @author Karl Duderstadt
 */
public class MarsOMEView {

	@Parameter
	private LogService log;

	@Parameter
	private ConvertService convert;
	
	private AnchorPane rootAnchorPane;

	private TreeView testTree;
	
	private CustomTextField filterField;

	private TableView<String> imageTable;
	private Map<String, String> imageData = new HashMap<String, String>();
	private ObservableList<String> imageFieldNameList = FXCollections.observableArrayList();
	private FilteredList<String> filteredImageFieldNameList = new FilteredList<>(imageFieldNameList, p -> true);

	private TableView<String> planeTable;
	private Map<String, String> planeData = new HashMap<String, String>();
	private ObservableList<String> planeFieldNameList = FXCollections.observableArrayList();
	private FilteredList<String> filteredPlaneFieldNameList = new FilteredList<>(planeFieldNameList, p -> true);

	public MarsOMEView(final Context context) {
		context.inject(this);
		
		//Global container
		rootAnchorPane = new AnchorPane();
		rootAnchorPane.setPrefSize(841.0, 743.0);
		rootAnchorPane.setPadding(new Insets(10.0, 10.0, 10.0, 10.0));
		
		//Global SplitPane
		SplitPane splitPane = new SplitPane();
		splitPane.setDividerPositions(0.4525);
		splitPane.setLayoutX(206.0);
		splitPane.setLayoutY(128.0);
		splitPane.setPrefSize(714.0, 646.0);
		AnchorPane.setBottomAnchor(splitPane, 0.0);
		AnchorPane.setTopAnchor(splitPane, 0.0);
		AnchorPane.setRightAnchor(splitPane, 0.0);
		AnchorPane.setLeftAnchor(splitPane, 0.0);
		rootAnchorPane.getChildren().add(splitPane);
		
		//PlaneIndex Left...
		AnchorPane planeIndexPane = new AnchorPane();
		planeIndexPane.setPrefSize(88.0, 190.0);
		planeIndexPane.setMinWidth(0.0);
		planeIndexPane.setMinHeight(0.0);
		
		testTree = new TreeView();
		testTree.setLayoutX(4.0);
		testTree.setLayoutY(56.0);
		testTree.setPrefSize(208.0, 644.0);
		AnchorPane.setBottomAnchor(testTree, 0.0);
		AnchorPane.setTopAnchor(testTree, 0.0);
		AnchorPane.setLeftAnchor(testTree, 0.0);
		AnchorPane.setRightAnchor(testTree, 0.0);
		planeIndexPane.getChildren().add(testTree);
		
		splitPane.getItems().add(planeIndexPane);
		
		//Global Right side container
		//AnchorPane planeDetailsPane = new AnchorPane();
		//planeDetailsPane.minHeight(0.0);
		//planeDetailsPane.minWidth(0.0);
		//planeDetailsPane.prefHeight(520.0);
		//planeDetailsPane.prefWidth(320.0);
		//planeDetailsPane.setPadding(new Insets(10.0, 10.0, 10.0, 10.0));
		
		//splitPane.getItems().add(planeDetailsPane);
		
		//Right side vertical splitpane
		SplitPane planeSplitPane = new SplitPane();
		planeSplitPane.setDividerPositions(0.4);
		planeSplitPane.setOrientation(Orientation.VERTICAL);
		planeSplitPane.prefHeight(644.0);
		planeSplitPane.prefWidth(459.0);
		AnchorPane.setBottomAnchor(planeSplitPane, 0.0);
		AnchorPane.setTopAnchor(planeSplitPane, 0.0);
		AnchorPane.setLeftAnchor(planeSplitPane, 0.0);
		AnchorPane.setRightAnchor(planeSplitPane, 0.0);
		
		//Right Top container
		AnchorPane imageAnchorPane = new AnchorPane();
		imageAnchorPane.minHeight(0.0);
		imageAnchorPane.minWidth(0.0);
		imageAnchorPane.prefHeight(308.0);
		imageAnchorPane.prefWidth(328.0);
		
		VBox vbox = new VBox();
		vbox.setPrefSize(499.0, 261.0);
		AnchorPane.setBottomAnchor(vbox, 0.0);
		AnchorPane.setTopAnchor(vbox, 0.0);
		AnchorPane.setRightAnchor(vbox, 0.0);
		AnchorPane.setLeftAnchor(vbox, 0.0);
		vbox.setPadding(new Insets(10.0, 10.0, 10.0, 10.0));
		vbox.getChildren().add(new Label("Image"));
		
		Separator separator = new Separator();
		separator.prefWidth(200.0);
		VBox.setMargin(separator, new Insets(10.0, 0.0, 10.0, 0.0));
		vbox.getChildren().add(separator);
		
		imageTable = new TableView<String>();
		VBox.setVgrow(imageTable, Priority.ALWAYS);
		imageTable.setItems(filteredImageFieldNameList);	
		
		TableColumn<String, String> imageNameColumn = new TableColumn<String, String>();
		imageNameColumn.setEditable(false);
		imageNameColumn.setPrefWidth(135.0);
		imageNameColumn.setSortable(false);
		imageNameColumn.setText("Name ");
		imageNameColumn.setCellValueFactory(field -> {
			return new ReadOnlyStringWrapper(field.getValue());
		});
		imageTable.getColumns().add(imageNameColumn);
		
		TableColumn<String, String> imageValueColumn = new TableColumn<String, String>();
		imageValueColumn.setEditable(false);
		imageValueColumn.setPrefWidth(135.0);
		imageValueColumn.setSortable(false);
		imageValueColumn.setText("Value ");
		imageValueColumn.setCellValueFactory(field -> {
			return new ReadOnlyStringWrapper(imageData.get(field.getValue()));
		});
		imageTable.getColumns().add(imageValueColumn);
		
		vbox.getChildren().add(imageTable);
		imageAnchorPane.getChildren().add(vbox);
		planeSplitPane.getItems().add(imageAnchorPane);
		
		//Right bottom container
		AnchorPane positionAnchorPane = new AnchorPane();
		positionAnchorPane.minHeight(0.0);
		positionAnchorPane.minWidth(0.0);
		positionAnchorPane.setPrefSize(160.0, 100.0);
		
		VBox vBoxBottom = new VBox();
		vBoxBottom.setPrefSize(437.0, 394.0);
		AnchorPane.setBottomAnchor(vBoxBottom, 0.0);
		AnchorPane.setTopAnchor(vBoxBottom, 0.0);
		AnchorPane.setLeftAnchor(vBoxBottom, 0.0);
		AnchorPane.setRightAnchor(vBoxBottom, 0.0);
		vBoxBottom.setPadding(new Insets(10.0, 10.0, 10.0, 10.0));
		vBoxBottom.getChildren().add(new Label("Plane"));
		
		Separator separator2 = new Separator();
		separator2.prefWidth(200.0);
		VBox.setMargin(separator2, new Insets(10.0, 0.0, 10.0, 0.0));
		vBoxBottom.getChildren().add(separator2);
		
		planeTable = new TableView<String>();
		VBox.setVgrow(planeTable, Priority.ALWAYS);
		planeTable.setItems(filteredPlaneFieldNameList);
		
		TableColumn<String, String> tiffDataNameColumn = new TableColumn<String, String>();
		tiffDataNameColumn.setEditable(false);
		tiffDataNameColumn.setPrefWidth(135.0);
		tiffDataNameColumn.setSortable(false);
		tiffDataNameColumn.setText("Name ");
		tiffDataNameColumn.setCellValueFactory(field -> {
			return new ReadOnlyStringWrapper(field.getValue());
		});
		planeTable.getColumns().add(tiffDataNameColumn);
		
		TableColumn<String, String> tiffDataValueColumn = new TableColumn<String, String>();
		tiffDataValueColumn.setEditable(false);
		tiffDataValueColumn.setPrefWidth(135.0);
		tiffDataValueColumn.setSortable(false);
		tiffDataValueColumn.setText("Value ");
		tiffDataValueColumn.setCellValueFactory(field -> {
			return new ReadOnlyStringWrapper(planeData.get(field.getValue()));
		});
		planeTable.getColumns().add(tiffDataValueColumn);
		
		vBoxBottom.getChildren().add(planeTable);
		positionAnchorPane.getChildren().add(vBoxBottom);
		planeSplitPane.getItems().add(positionAnchorPane);
		
		BorderPane borderPane = new BorderPane();
		
		filterField = new CustomTextField();
        filterField.setLeft(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.SEARCH));
        filterField.getStyleClass().add("find");        
        filterField.textProperty().addListener((observable, oldValue, newValue) -> {
        	//If we don't clear the selection while we are searching the table will
        	//steal the focus after every letter we type.
        	imageTable.getSelectionModel().clearSelection();
        	filteredImageFieldNameList.setPredicate(name -> {
        		// If filter text is empty, display everything.
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                return name.contains(newValue);
        	});
        	
        	planeTable.getSelectionModel().clearSelection();
        	filteredPlaneFieldNameList.setPredicate(name -> {
        		// If filter text is empty, display everything.
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
        		
        		return name.contains(newValue);
        	});
        });
        
		filterField.setStyle(
              "-fx-background-radius: 2em; "
        );

        borderPane.setTop(filterField);
        BorderPane.setMargin(filterField, new Insets(5));
		
		borderPane.setCenter(planeSplitPane);
		splitPane.getItems().add(borderPane);
	}

	public void fill(MarsMetadata meta) {
		TreeItem<GenericModel> root = new TreeItem<>();
		testTree.setRoot(root);
		testTree.setShowRoot(false);

		// Build and populate the tree
		//for (int imageIndex = 0; imageIndex < meta.getImageCount(); imageIndex++) {
		meta.images().forEach(image -> {
			TreeItem<GenericModel> imageItem = new TreeItem<>(image);
			root.getChildren().add(imageItem);

			image.planes().forEach(plane -> {
					TreeItem<GenericModel> dataItem = new TreeItem<>(plane);
					imageItem.getChildren().add(dataItem);
				});
		});

		// Handle selection in the tree
		testTree.getSelectionModel().selectedItemProperty()
				.addListener((ObservableValue obs, Object oldValue, Object newValue) -> {
					TreeItem<GenericModel> selectedItem = (TreeItem<GenericModel>) newValue;
					
					if (selectedItem != null && selectedItem.getValue() instanceof MarsOMEPlane)
						populateTiffDataInformations((MarsOMEPlane) selectedItem.getValue());
				});

	}

	private void populateTiffDataInformations(MarsOMEPlane plane) {
		MarsOMEImage imageModel = plane.getImage();
		
		imageData.clear();
		imageFieldNameList.clear();
		for (List<String> row : imageModel.getInformationsRow()) {
			imageData.put(row.get(0), row.get(1));
			imageFieldNameList.add(row.get(0));
		}

		planeData.clear();
		planeFieldNameList.clear();
		for (List<String> row : plane.getInformationsRow()) {
			planeData.put(row.get(0), row.get(1));
			planeFieldNameList.add(row.get(0));
		}
	}
	
	public Node getNode() {
		return rootAnchorPane;
	}
}
