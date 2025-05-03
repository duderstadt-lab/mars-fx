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

package de.mpg.biochem.mars.fx.molecule.moleculesTab;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXChipView;
import com.jfoenix.controls.JFXTextField;

import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.event.MoleculeTagsChangedEvent;
import de.mpg.biochem.mars.fx.util.MarsJFXChipViewSkin;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.fx.util.Utils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.scene.layout.StackPane;

public class MoleculeGeneralTabController implements MoleculeSubPane {

	private ScrollPane rootPane;
	private VBox vBox;

	private BorderPane UIDIconContainer;
	private Label UIDLabel;
	private BorderPane metaUIDIconContainer;
	private Label metaUIDLabel;
	private Text iText, iInt, cText, cInt;
	private TextFlow imageAndchannel;
	private Label tags;
	private JFXChipView<String> chipView;
	private Label notes;
	private TextArea notesTextArea;

	final Clipboard clipboard = Clipboard.getSystemClipboard();

	private ListChangeListener<String> chipsListener;
	private ChangeListener<String> notesListener;

	private MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;
	private Molecule molecule;
	private MarsJFXChipViewSkin<String> skin;

	public MoleculeGeneralTabController() {
		rootPane = new ScrollPane();
		rootPane.getStyleClass().add("dashboard-scroll-pane");

		vBox = new VBox();
		vBox.setAlignment(Pos.CENTER);

		UIDIconContainer = new BorderPane();
		UIDIconContainer.setPrefHeight(55.0);
		UIDIconContainer.setPrefWidth(60.0);
		UIDIconContainer.setCenter(MaterialIconFactory.get().createIcon(
			de.jensd.fx.glyphs.materialicons.MaterialIcon.FINGERPRINT, "2.5em"));
		vBox.getChildren().add(UIDIconContainer);

		UIDLabel = new Label();
		UIDLabel.setText("UID");
		StackPane copyableUIDLabel = Utils.createCopyableLabel(UIDLabel);
		vBox.getChildren().add(copyableUIDLabel);

		metaUIDIconContainer = new BorderPane();
		metaUIDIconContainer.setPrefHeight(60.0);
		metaUIDIconContainer.setPrefWidth(70.0);
		Region microscopeIcon = new Region();
		microscopeIcon.getStyleClass().add("microscopeIcon");
		microscopeIcon.getStyleClass().add("icon-lg"); //Should work to increase the size but is not working currently
		microscopeIcon.setStyle("-fx-min-height: 30px; -fx-min-width: 30px; -fx-max-height: 30px; -fx-max-width: 30px;"); //Remove once issue is resolved
		metaUIDIconContainer.setCenter(microscopeIcon);
		vBox.getChildren().add(metaUIDIconContainer);

		metaUIDLabel = new Label();
		metaUIDLabel.setText("metaUID");
		StackPane copyableMetaUIDLabel = Utils.createCopyableLabel(metaUIDLabel);
		vBox.getChildren().add(copyableMetaUIDLabel);

		imageAndchannel = new TextFlow();
		iText = new Text("");
		iText.setStyle("-fx-font-weight:bold");
		iInt = new Text("");
		iInt.setStyle("-fx-font-weight:normal");
		cText = new Text("");
		cText.setStyle("-fx-font-weight:bold");
		cInt = new Text("");
		cInt.setStyle("-fx-font-weight:normal");
		imageAndchannel.getChildren().addAll(iText, iInt, cText, cInt);
		HBox hbox3 = new HBox();
		hbox3.getChildren().add(imageAndchannel);
		hbox3.setAlignment(Pos.CENTER);
		HBox.setMargin(imageAndchannel, new Insets(10, 10, 10, 10));
		vBox.getChildren().add(hbox3);

		tags = new Label();
		tags.setText("Tags");
		VBox.setMargin(tags, new Insets(5, 5, 5, 5));
		vBox.getChildren().add(tags);

		chipView = new JFXChipView<String>();
		VBox.setMargin(chipView, new Insets(10, 10, 10, 10));
		chipView.setMinHeight(250.0);
		vBox.getChildren().add(chipView);

		notes = new Label();
		notes.setText("Notes");
		VBox.setMargin(notes, new Insets(5, 5, 5, 5));
		vBox.getChildren().add(notes);

		notesTextArea = new TextArea();
		VBox.setMargin(notesTextArea, new Insets(10, 10, 10, 10));
		notesTextArea.setMinHeight(150.0);
		notesTextArea.setPromptText("none");
		notesTextArea.setWrapText(true);
		vBox.getChildren().add(notesTextArea);

		rootPane.setFitToWidth(true);
		rootPane.setContent(vBox);

		getNode().addEventHandler(MoleculeEvent.MOLECULE_EVENT, this);
		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT,
			new EventHandler<MoleculeArchiveEvent>()
		{
			@Override
			public void handle(MoleculeArchiveEvent e) {
				if (e.getEventType().getName().equals(
					"INITIALIZE_MOLECULE_ARCHIVE"))
		    {
					archive = e.getArchive();
					if (archive == null) molecule = null;
					e.consume();
				}
			}
		});

		chipsListener = new ListChangeListener<String>() {

			@Override
			public void onChanged(Change<? extends String> c) {
				if (molecule == null) return;

				while (c.next()) {
					if (c.wasRemoved()) {
						molecule.removeTag(c.getRemoved().get(0));
					}
					else if (c.wasAdded()) {
						molecule.addTag(c.getAddedSubList().get(0));
					}
				}
				getNode().fireEvent(new MoleculeTagsChangedEvent(molecule));
			}
		};

		if (notesListener == null) {
			notesListener = new ChangeListener<String>() {

				@Override
				public void changed(final ObservableValue<? extends String> observable,
					final String oldValue, final String newValue)
				{
					if (molecule == null) return;

					molecule.setNotes(notesTextArea.getText());
				}
			};
		}

		skin = new MarsJFXChipViewSkin<>(chipView);
		chipView.setSkin(skin);
	}

	@Override
	public void handle(MoleculeEvent event) {
		event.invokeHandler(this);
		event.consume();
	}

	@Override
	public Node getNode() {
		return rootPane;
	}

	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}

	@Override
	public void onMoleculeSelectionChangedEvent(Molecule molecule) {
		this.molecule = molecule;
		if (molecule == null) {
			iText.setText("");
			iInt.setText("");
			cText.setText("");
			cInt.setText("");
			chipView.getChips().clear();
			notesTextArea.setText("");
			UIDLabel.setText("");
			metaUIDLabel.setText("");
			return;
		}

		UIDLabel.setText(molecule.getUID());
		metaUIDLabel.setText(molecule.getMetadataUID());

		if (molecule.getImage() > -1) {
			iText.setText("I ");
			iInt.setText(String.valueOf(molecule.getImage()) + " ");
		}
		else {
			iText.setText("");
			iInt.setText("");
		}

		if (molecule.getChannel() > -1) {
			cText.setText("C ");
			cInt.setText(String.valueOf(molecule.getChannel()));
		}
		else {
			cText.setText("");
			cInt.setText("");
		}

		if (!iText.getText().equals("") && !cText.getText().equals("")) AnchorPane
			.setLeftAnchor(imageAndchannel, 79.0);
		else AnchorPane.setLeftAnchor(imageAndchannel, 87.0);

		chipView.getChips().removeListener(chipsListener);
		chipView.getChips().clear();
		if (molecule.getTags().size() > 0) chipView.getChips().addAll(molecule
			.getTags());

		chipView.getSuggestions().clear();
		chipView.getSuggestions().addAll(archive.properties().getTagSet());
		chipView.getChips().addListener(chipsListener);

		notesTextArea.textProperty().removeListener(notesListener);
		notesTextArea.setText(molecule.getNotes());
		notesTextArea.textProperty().addListener(notesListener);
	}
}
