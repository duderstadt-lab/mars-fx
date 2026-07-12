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

package de.mpg.biochem.mars.fx.molecule.metadataTab;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXChipView;
import com.jfoenix.controls.JFXTextField;

import de.mpg.biochem.mars.fx.editor.MarkdownNotesPane;
import de.mpg.biochem.mars.fx.event.DefaultMoleculeArchiveEventHandler;
import de.mpg.biochem.mars.fx.event.MetadataEvent;
import de.mpg.biochem.mars.fx.event.MetadataTagsChangedEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.util.MarsJFXChipViewSkin;
import de.mpg.biochem.mars.fx.util.Utils;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;

public class MetadataGeneralTabController implements MetadataSubPane {

	private BorderPane rootPane;
	private VBox vBox;
	private BorderPane UIDIconContainer;
	private Label metaUIDLabel;
	private Label tags;
	private JFXChipView<String> chipView;
	private Label notes;
	private MarkdownNotesPane notesPane;

	final Clipboard clipboard = Clipboard.getSystemClipboard();

	private MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;

	private ListChangeListener<String> chipsListener;

	private MarsMetadata marsMetadata;

	private MarsJFXChipViewSkin<String> skin;

	public MetadataGeneralTabController() {
		rootPane = new BorderPane();

		vBox = new VBox();
		vBox.setAlignment(Pos.CENTER);

		UIDIconContainer = new BorderPane();
		UIDIconContainer.setPrefHeight(60.0);
		UIDIconContainer.setPrefWidth(70.0);
		Region microscopeIcon = new Region();
		microscopeIcon.getStyleClass().add("microscopeIcon");
		microscopeIcon.getStyleClass().add("icon-lg"); //Should work to increase the size but is not working currently
		microscopeIcon.setStyle("-fx-min-height: 30px; -fx-min-width: 30px; -fx-max-height: 30px; -fx-max-width: 30px;"); //Remove once issue is resolved
		UIDIconContainer.setCenter(microscopeIcon);
		vBox.getChildren().add(UIDIconContainer);

		metaUIDLabel = new Label();
		metaUIDLabel.setText("metaUID");
		StackPane copyableMetaUIDLabel = Utils.createCopyableLabel(metaUIDLabel);
		vBox.getChildren().add(copyableMetaUIDLabel);

		tags = new Label();
		tags.setText("Tags");
		VBox.setMargin(tags, new Insets(20, 5, 10, 5));
		vBox.getChildren().add(tags);

		chipView = new JFXChipView<String>();
		VBox.setMargin(chipView, new Insets(10, 10, 10, 10));
		chipView.setMinHeight(200.0);
		vBox.getChildren().add(chipView);

		notes = new Label();
		notes.setText("Notes");
		VBox.setMargin(notes, new Insets(5, 5, 5, 5));

		// Notes pane fills whatever vertical space remains below the (fixed-height)
		// UID/tags content above, same layout as the Dataset Explorer's notes
		// section.
		notesPane = new MarkdownNotesPane();
		notesPane.setOnMarkdownChanged(md -> {
			if (marsMetadata != null) marsMetadata.setNotes(md);
		});

		VBox notesSection = new VBox(4, notes, notesPane);
		notesSection.setPadding(new Insets(8));
		VBox.setVgrow(notesPane, Priority.ALWAYS);

		rootPane.setTop(vBox);
		rootPane.setCenter(notesSection);

		getNode().addEventHandler(MetadataEvent.METADATA_EVENT, this);
		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT,
			new DefaultMoleculeArchiveEventHandler()
			{

				@Override
				public void onInitializeMoleculeArchiveEvent(
					MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> newArchive)
			  {
					archive = newArchive;
					if (archive == null) marsMetadata = null;
				}
			});

		chipsListener = new ListChangeListener<String>() {

			@Override
			public void onChanged(Change<? extends String> c) {
				if (marsMetadata == null) return;

				while (c.next()) {
					if (c.wasRemoved()) {
						marsMetadata.removeTag(c.getRemoved().get(0));
					}
					else if (c.wasAdded()) {
						marsMetadata.addTag(c.getAddedSubList().get(0));
					}
				}
				getNode().fireEvent(new MetadataTagsChangedEvent(marsMetadata));
			}
		};

		skin = new MarsJFXChipViewSkin<>(chipView);
		chipView.setSkin(skin);
	}

	public Node getNode() {
		return rootPane;
	}

	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}

	@Override
	public void onMetadataSelectionChangedEvent(MarsMetadata marsImageMetadata) {
		this.marsMetadata = marsImageMetadata;
		
		if (marsImageMetadata == null) return;

		metaUIDLabel.setText(marsMetadata.getUID());

		chipView.getChips().removeListener(chipsListener);
		chipView.getChips().clear();
		if (marsMetadata.getTags().size() > 0) chipView.getChips().addAll(
			marsMetadata.getTags());

		chipView.getSuggestions().clear();
		chipView.getSuggestions().addAll(archive.properties().getTagSet());
		chipView.getChips().addListener(chipsListener);

		// setMarkdown() suppresses the change callback internally, so this doesn't
		// bounce straight back into marsMetadata.setNotes(...).
		notesPane.setMarkdown(marsMetadata.getNotes());
	}

	@Override
	public void handle(MetadataEvent event) {
		event.invokeHandler(this);
		event.consume();
	}
}
