/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2026 Karl Duderstadt
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

package de.mpg.biochem.mars.fx.editor;

import java.util.function.Consumer;

import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import javafx.stage.Window;

/**
 * Searchable, virtualized emoji picker. Editor-agnostic: constructed with a
 * callback that receives the chosen {@link EmojiEntry}; the caller decides how
 * to insert it (so the same picker serves both the archive comments editor and
 * the Dataset Explorer notes editor). Cells show the actual Unicode glyph
 * (JavaFX has no SVG image support, and the OS emoji font renders these natively
 * in ordinary controls just fine); the bundled Twemoji SVGs are only used for
 * the markdown preview's HTML rendering, so editor/picker and preview stay
 * visually distinct but both trace back to the same {@link EmojiData} table.
 */
public class EmojiPicker {

	private static final String ALL_CATEGORIES = "All";

	private final Popup popup = new Popup();
	private final TextField searchField = new TextField();
	private final ComboBox<String> categoryBox = new ComboBox<>();
	private final GridView<EmojiEntry> gridView = new GridView<>();

	public EmojiPicker(Consumer<EmojiEntry> onChosen) {
		EmojiData data = EmojiData.getInstance();

		searchField.setPromptText("Search emoji…");
		searchField.setPrefColumnCount(18);

		ObservableList<String> categories = FXCollections.observableArrayList();
		categories.add(ALL_CATEGORIES);
		categories.addAll(data.categories());
		categoryBox.setItems(categories);
		categoryBox.getSelectionModel().select(ALL_CATEGORIES);

		// Cell/font size chosen to land on a size the OS's bitmap-strike color-emoji
		// font (e.g. Apple Color Emoji) actually has a native glyph for -- an
		// in-between size like the previous 20px forces the OS to upscale its
		// nearest strike, which is what reads as "blurry".
		gridView.setCellWidth(44);
		gridView.setCellHeight(44);
		gridView.setHorizontalCellSpacing(2);
		gridView.setVerticalCellSpacing(2);
		gridView.setCellFactory(gv -> new EmojiCell(onChosen, this::hide));
		gridView.setItems(FXCollections.observableArrayList(data.all()));

		Runnable refresh = () -> {
			String query = searchField.getText() == null ? "" : searchField
				.getText().trim().toLowerCase();
			String category = categoryBox.getSelectionModel().getSelectedItem();

			java.util.List<EmojiEntry> base = (category == null || category.equals(
				ALL_CATEGORIES)) ? data.all() : data.byCategory(category);
			if (query.isEmpty()) gridView.setItems(FXCollections.observableArrayList(
				base));
			else {
				java.util.List<EmojiEntry> filtered = new java.util.ArrayList<>();
				for (EmojiEntry entry : base)
					if (entry.matches(query)) filtered.add(entry);
				gridView.setItems(FXCollections.observableArrayList(filtered));
			}
		};

		searchField.textProperty().addListener((ob, o, n) -> refresh.run());
		categoryBox.getSelectionModel().selectedItemProperty().addListener((ob, o,
			n) -> refresh.run());

		searchField.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ESCAPE) hide();
		});

		HBox searchRow = new HBox(6, searchField, categoryBox);
		searchRow.setAlignment(Pos.CENTER_LEFT);
		searchRow.setPadding(new Insets(6));

		BorderPane content = new BorderPane();
		content.setTop(searchRow);
		content.setCenter(gridView);
		content.setPrefSize(320, 320);
		content.getStyleClass().add("emoji-picker");
		content.setStyle(
			"-fx-background-color: -fx-control-inner-background; -fx-border-color: -fx-box-border; -fx-border-width: 1;");

		popup.getContent().add(content);
		popup.setAutoHide(true);
		popup.setHideOnEscape(true);
	}

	public void show(javafx.scene.Node anchor) {
		Window window = anchor.getScene().getWindow();
		javafx.geometry.Bounds bounds = anchor.localToScreen(anchor
			.getBoundsInLocal());
		popup.show(window, bounds.getMinX(), bounds.getMaxY());
		searchField.clear();
		categoryBox.getSelectionModel().select(ALL_CATEGORIES);
		searchField.requestFocus();
	}

	public void hide() {
		popup.hide();
	}

	private static class EmojiCell extends GridCell<EmojiEntry> {

		private final Label label = new Label();

		EmojiCell(Consumer<EmojiEntry> onChosen, Runnable afterChoose) {
			label.setStyle("-fx-font-size: 26px;");
			setGraphic(label);
			setOnMouseClicked(e -> {
				EmojiEntry entry = getItem();
				if (entry != null) {
					onChosen.accept(entry);
					afterChoose.run();
				}
			});
		}

		@Override
		protected void updateItem(EmojiEntry entry, boolean empty) {
			super.updateItem(entry, empty);
			if (empty || entry == null) {
				label.setText("");
				setTooltip(null);
			}
			else {
				label.setText(entry.getUnicode());
				Tooltip tooltip = new Tooltip(entry.getName() + "  :" + entry
					.getShortcode() + ":");
				setTooltip(tooltip);
			}
		}
	}
}
