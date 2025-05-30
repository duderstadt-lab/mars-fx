/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2025 Karl Duderstadt
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import de.mpg.biochem.mars.n5.*;
import org.controlsfx.control.textfield.CustomTextField;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.event.MetadataEvent;
import de.mpg.biochem.mars.fx.event.MetadataEventHandler;
import de.mpg.biochem.mars.fx.util.EditCell;
import de.mpg.biochem.mars.metadata.MarsBdvSource;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class BdvViewTable implements MetadataEventHandler {

	protected MarsMetadata marsImageMetadata;

	protected SplitPane rootPane;

	protected CustomTextField addBdvSourceNameField;
	protected ObservableList<MarsBdvSource> bdvRowList = FXCollections
		.observableArrayList();

	protected Button typeButton;
	protected int buttonType = 0;

	protected BdvSourceOptionsPane bdvSourceOptionsPane;

	protected ChangeListener<MarsBdvSource> bdvIndexTableListener;

	public BdvViewTable() {
		rootPane = new SplitPane();
		ObservableList<Node> splitItems = rootPane.getItems();

		rootPane.setDividerPositions(0.2f, 0.8f);

		Node bdvTableIndexContainer = buildBdvTableIndex();
		SplitPane.setResizableWithParent(bdvTableIndexContainer, Boolean.FALSE);
		splitItems.add(bdvTableIndexContainer);

		bdvSourceOptionsPane = new BdvSourceOptionsPane();
		SplitPane.setResizableWithParent(bdvSourceOptionsPane, Boolean.FALSE);
		splitItems.add(bdvSourceOptionsPane);
		bdvSourceOptionsPane.setMarsBdvSource(null);

		getNode().addEventHandler(MetadataEvent.METADATA_EVENT, this);
	}

	protected BorderPane buildBdvTableIndex() {
		TableView<MarsBdvSource> bdvTable = new TableView<MarsBdvSource>();
		addBdvSourceNameField = new CustomTextField();

		TableColumn<MarsBdvSource, String> typeColumn = new TableColumn<>();
		typeColumn.setCellFactory(TextFieldTableCell.forTableColumn());
		typeColumn.setCellValueFactory(bdvSource -> new ReadOnlyObjectWrapper<>(
			(bdvSource.getValue().isN5()) ? "N5" : "HD5"));
		typeColumn.setSortable(false);
		typeColumn.setEditable(false);
		typeColumn.setPrefWidth(40);
		typeColumn.setMinWidth(40);
		typeColumn.setStyle("-fx-alignment: CENTER-LEFT;");
		bdvTable.getColumns().add(typeColumn);

		TableColumn<MarsBdvSource, MarsBdvSource> deleteColumn =
			new TableColumn<>();
		deleteColumn.setPrefWidth(30);
		deleteColumn.setMinWidth(30);
		deleteColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param
			.getValue()));
		deleteColumn.setCellFactory(
			param -> new TableCell<MarsBdvSource, MarsBdvSource>()
			{

				private final Button removeButton = new Button();

				@Override
				protected void updateItem(MarsBdvSource pRow, boolean empty) {
					super.updateItem(pRow, empty);

					if (pRow == null) {
						setGraphic(null);
						return;
					}

					removeButton.setGraphic(FontAwesomeIconFactory.get().createIcon(
						de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.MINUS, "1.0em"));
					removeButton.setCenterShape(true);
					removeButton.setStyle("-fx-background-radius: 5em; " +
						"-fx-min-width: 18px; " + "-fx-min-height: 18px; " +
						"-fx-max-width: 18px; " + "-fx-max-height: 18px;");

					setGraphic(removeButton);
					removeButton.setOnAction(e -> {
						marsImageMetadata.removeBdvSource(pRow.getName());
						loadBdvSources();
					});
				}
			});
		deleteColumn.setStyle("-fx-alignment: CENTER;");
		deleteColumn.setSortable(false);
		bdvTable.getColumns().add(deleteColumn);

		TableColumn<MarsBdvSource, String> nameColumn = new TableColumn<>("Name");

		nameColumn.setCellFactory(column -> EditCell.createStringEditCell());
		nameColumn.setOnEditCommit(event -> {
			String newBdvName = event.getNewValue();
			if (!marsImageMetadata.hasBdvSource(newBdvName)) {
				MarsBdvSource bdvSource = event.getRowValue();
				String oldName = bdvSource.getName();
				bdvSource.setName(newBdvName);

				// We need to rebuild the map to maintain the order in the table
				List<MarsBdvSource> bdvSourceList = new ArrayList<MarsBdvSource>();
				for (String key : marsImageMetadata.getBdvSourceNames()) {
					if (key.equals(oldName)) bdvSourceList.add(bdvSource);
					else bdvSourceList.add(marsImageMetadata.getBdvSource(key));
				}
				marsImageMetadata.removeAllBdvSources();

				for (MarsBdvSource item : bdvSourceList)
					marsImageMetadata.putBdvSource(item);
			}
			else {
				((MarsBdvSource) event.getTableView().getItems().get(event
					.getTablePosition().getRow())).setName(event.getOldValue());
				bdvTable.refresh();
			}
		});
		nameColumn.setCellValueFactory(bdvSource -> new ReadOnlyObjectWrapper<>(
			bdvSource.getValue().getName()));
		nameColumn.setSortable(false);
		nameColumn.setPrefWidth(100);
		nameColumn.setMinWidth(100);
		nameColumn.setStyle("-fx-alignment: CENTER-LEFT;");
		bdvTable.getColumns().add(nameColumn);

		bdvTable.setItems(bdvRowList);
		bdvTable.setEditable(true);

		bdvIndexTableListener = new ChangeListener<MarsBdvSource>() {

			public void changed(ObservableValue<? extends MarsBdvSource> observable,
				MarsBdvSource oldMarsBdvSource, MarsBdvSource newMarsBdvSource)
			{
				if (newMarsBdvSource != null) bdvSourceOptionsPane.setMarsBdvSource(
					newMarsBdvSource);
				else bdvSourceOptionsPane.setMarsBdvSource(null);
			}
		};

		bdvTable.getSelectionModel().selectedItemProperty().addListener(
			bdvIndexTableListener);

		Button addButton = new Button();
		addButton.setGraphic(FontAwesomeIconFactory.get().createIcon(
			de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLUS, "1.0em"));
		addButton.setCenterShape(true);
		addButton.setCursor(Cursor.DEFAULT);
		addButton.setStyle("-fx-background-radius: 5em; " +
			"-fx-min-width: 18px; " + "-fx-min-height: 18px; " +
			"-fx-max-width: 18px; " + "-fx-max-height: 18px;");
		addButton.setOnAction(e -> {
			if (!addBdvSourceNameField.getText().equals("") && !marsImageMetadata
				.hasBdvSource(addBdvSourceNameField.getText()))
			{
				MarsBdvSource bdvSource = new MarsBdvSource(addBdvSourceNameField
					.getText());

				switch (this.buttonType) {
					case 0:
						bdvSource.setN5(true);
						break;
					case 1:
						bdvSource.setN5(false);
						break;
				}

				marsImageMetadata.putBdvSource(bdvSource);
				loadBdvSources();
			}
		});

		addBdvSourceNameField.textProperty().addListener((observable, oldValue,
			newValue) -> {
			if (addBdvSourceNameField.getText().isEmpty()) {
				addBdvSourceNameField.setRight(new Label(""));
			}
			else {
				addBdvSourceNameField.setRight(addButton);
			}
		});
		addBdvSourceNameField.getStyleClass().add("rounded-corners-textfield");

		typeButton = new Button();
		typeButton.setText("N5");
		typeButton.setCenterShape(true);
		typeButton.setStyle("-fx-background-radius: 2em; " +
			"-fx-min-width: 60px; " + "-fx-min-height: 30px; " +
			"-fx-max-width: 60px; " + "-fx-max-height: 30px;");
		typeButton.setOnAction(e -> {
			buttonType++;
			if (buttonType > 1) buttonType = 0;

			switch (buttonType) {
				case 0:
					typeButton.setText("N5");
					typeButton.setGraphic(null);
					break;
				case 1:
					typeButton.setText("HD5");
					typeButton.setGraphic(null);
					break;
			}
		});

		BorderPane bomttomPane = new BorderPane();
		bomttomPane.setCenter(addBdvSourceNameField);
		bomttomPane.setLeft(typeButton);

		BorderPane bdvTableIndex = new BorderPane();
		bdvTableIndex.setCenter(bdvTable);
		bdvTableIndex.setBottom(bomttomPane);

		BorderPane.setMargin(addBdvSourceNameField, new Insets(5));
		BorderPane.setMargin(typeButton, new Insets(5));

		return bdvTableIndex;
	}

	public Node getNode() {
		return rootPane;
	}

	public void loadBdvSources() {
		bdvRowList.setAll(marsImageMetadata.getBdvSourceNames().stream().map(
			name -> marsImageMetadata.getBdvSource(name)).collect(Collectors
				.toList()));
	}

	@Override
	public void handle(MetadataEvent event) {
		event.invokeHandler(this);
		event.consume();
	}

	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}

	@Override
	public void onMetadataSelectionChangedEvent(MarsMetadata marsImageMetadata) {
		this.marsImageMetadata = marsImageMetadata;
		if (marsImageMetadata == null) {
			bdvSourceOptionsPane.setMarsBdvSource(null);
			return;
		}
		loadBdvSources();
	}
}
