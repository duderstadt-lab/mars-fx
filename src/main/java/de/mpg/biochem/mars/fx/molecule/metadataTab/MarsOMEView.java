/*
 * The MIT License
 *
 * Copyright 2016 Fiji.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.mpg.biochem.mars.fx.molecule.metadataTab;

import java.util.List;

import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ome.xml.meta.OMEXMLMetadata;

/**
 * FXML Controller class
 *
 * @author Hadrien Mary
 * 
 * Moved all portions of the fxml into the code here and removed the image update interaction..
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

	private TableView<List<String>> imageTable;
	private TableColumn<List<String>, String> imageNameColumn;
	private TableColumn<List<String>, String> imageValueColumn;

	private TableView<List<String>> tiffDataTable;
	private TableColumn<List<String>, String> tiffDataNameColumn;
	private TableColumn<List<String>, String> tiffDataValueColumn;

	public MarsOMEView(final Context context) {
		context.inject(this);
		
		//Build everything that was in the fxml
		
		//Global container
		rootAnchorPane = new AnchorPane();
		rootAnchorPane.setPrefSize(841.0, 743.0);
		rootAnchorPane.setPadding(new Insets(10.0, 10.0, 10.0, 10.0));
		
		//Global SplitPane
		SplitPane splitPane = new SplitPane();
		splitPane.setDividerPositions(0.35);
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
		AnchorPane planeDetailsPane = new AnchorPane();
		planeDetailsPane.minHeight(0.0);
		planeDetailsPane.minWidth(0.0);
		planeDetailsPane.prefHeight(520.0);
		planeDetailsPane.prefWidth(320.0);
		planeDetailsPane.setPadding(new Insets(10.0, 10.0, 10.0, 10.0));
		
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
		
		imageTable = new TableView<List<String>>();
		VBox.setVgrow(imageTable, Priority.ALWAYS);
		
		imageNameColumn = new TableColumn<List<String>, String>();
		imageNameColumn.setEditable(false);
		imageNameColumn.setPrefWidth(207.0);
		imageNameColumn.setSortable(false);
		imageNameColumn.setText("Name ");
		imageTable.getColumns().add(imageNameColumn);
		
		imageValueColumn = new TableColumn<List<String>, String>();
		imageValueColumn.setEditable(false);
		imageValueColumn.setPrefWidth(211.0);
		imageValueColumn.setSortable(false);
		imageValueColumn.setText("Value ");
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
		vBoxBottom.getChildren().add(new Label("TiffData"));
		
		Separator separator2 = new Separator();
		separator2.prefWidth(200.0);
		VBox.setMargin(separator2, new Insets(10.0, 0.0, 10.0, 0.0));
		vBoxBottom.getChildren().add(separator);
		
		tiffDataTable = new TableView<List<String>>();
		VBox.setVgrow(tiffDataTable, Priority.ALWAYS);
		
		tiffDataNameColumn = new TableColumn<List<String>, String>();
		tiffDataNameColumn.setEditable(false);
		tiffDataNameColumn.setPrefWidth(207.0);
		tiffDataNameColumn.setSortable(false);
		tiffDataNameColumn.setText("Name ");
		tiffDataTable.getColumns().add(tiffDataNameColumn);
		
		tiffDataValueColumn = new TableColumn<List<String>, String>();
		tiffDataValueColumn.setEditable(false);
		tiffDataValueColumn.setPrefWidth(211.0);
		tiffDataValueColumn.setSortable(false);
		tiffDataValueColumn.setText("Value ");
		tiffDataTable.getColumns().add(tiffDataValueColumn);
		
		vBoxBottom.getChildren().add(tiffDataTable);
		positionAnchorPane.getChildren().add(vBoxBottom);
		planeSplitPane.getItems().add(positionAnchorPane);
		planeDetailsPane.getChildren().add(planeSplitPane);
	}

	public void fill(OMEXMLMetadata md) {
		TreeItem<GenericModel<?>> root = new TreeItem<>();
		testTree.setRoot(root);
		testTree.setShowRoot(false);

		// Build and populate the tree
		for (int i = 0; i < md.getImageCount(); i++) {
			ImageModel imageModel = new ImageModel(i, md);
			TreeItem<GenericModel<?>> imageItem = new TreeItem<>(imageModel);
			root.getChildren().add(imageItem);

			for (int j = 0; j < md.getTiffDataCount(i); j++) {
				TiffDataModel dataModel = new TiffDataModel(i, j, md, imageModel);
				// md.getTiffDataFirstC(i, j),
				// md.getTiffDataFirstT(i, j), md.getTiffDataFirstZ(i, j), md.getTiffDataIFD(i,
				// j));
				TreeItem<GenericModel<?>> dataItem = new TreeItem<>(dataModel);
				imageItem.getChildren().add(dataItem);

			}
		}

		// Handle selection in the tree
		testTree.getSelectionModel().selectedItemProperty()
				.addListener((ObservableValue obs, Object oldValue, Object newValue) -> {
					TreeItem<GenericModel<?>> selectedItem = (TreeItem<GenericModel<?>>) newValue;
					GenericModel<?> model = selectedItem.getValue();

					if (model instanceof TiffDataModel) {
						// Display informations relative to TiffData
						populateTiffDataInformations((TiffDataModel) model);

					}
				});

	}

	private void populateImageInformations(ImageModel model) {

		this.imageTable.getItems().clear();

		this.imageNameColumn.setCellValueFactory(data -> {
			return new ReadOnlyStringWrapper(data.getValue().get(0));
		});

		this.imageValueColumn.setCellValueFactory(data -> {
			return new ReadOnlyStringWrapper(data.getValue().get(1));
		});

		for (List<String> row : model.getInformationsRow()) {
			this.imageTable.getItems().add(row);
		}

	}

	private void populateTiffDataInformations(TiffDataModel model) {
		ImageModel imageModel = model.getImageModel();
		this.populateImageInformations(imageModel);

		// Populate tiffData
		this.tiffDataTable.getItems().clear();

		this.tiffDataNameColumn.setCellValueFactory(data -> {
			return new ReadOnlyStringWrapper(data.getValue().get(0));
		});

		this.tiffDataValueColumn.setCellValueFactory(data -> {
			return new ReadOnlyStringWrapper(data.getValue().get(1));
		});

		for (List<String> row : model.getInformationsRow()) {
			this.tiffDataTable.getItems().add(row);
		}
	}
	
	public Node getNode() {
		return rootAnchorPane;
	}
}
