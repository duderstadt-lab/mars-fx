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

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.INFO_CIRCLE;

import java.util.ArrayList;

import com.jfoenix.controls.JFXChipView;
import com.jfoenix.controls.JFXDefaultChip;
import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.skins.JFXChipViewSkin;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXChip;

import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.fx.event.DefaultMoleculeArchiveEventHandler;
import de.mpg.biochem.mars.fx.event.MetadataEvent;
import de.mpg.biochem.mars.fx.event.MetadataTagsChangedEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.event.MoleculeTagsChangedEvent;
import de.mpg.biochem.mars.fx.util.MarsJFXChipViewSkin;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class MetadataGeneralTabController implements MetadataSubPane {

	private ScrollPane rootPane;
	private VBox vBox;
	private BorderPane UIDIconContainer;
	private JFXTextField UIDLabel;
	private JFXButton UIDClippyButton;
	private Label tags;
	private JFXChipView<String> chipView;
	private Label notes;
	private TextArea notesTextArea;
	
	final Clipboard clipboard = Clipboard.getSystemClipboard();
	
	private MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;
	
	private ListChangeListener<String> chipsListener;
	private ChangeListener<String> notesListener;
	
	private MarsMetadata marsMetadata;
	
	private MarsJFXChipViewSkin<String> skin;
	
    public MetadataGeneralTabController() {
    	rootPane = new ScrollPane();
    	
    	vBox = new VBox();
    	vBox.setAlignment(Pos.CENTER);
    	vBox.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/moleculesTab/MoleculeGeneralTab.css");
    	
    	UIDIconContainer = new BorderPane();
    	UIDIconContainer.setPrefHeight(60.0);
    	UIDIconContainer.setPrefWidth(70.0);
    	Region microscopeIcon = new Region();
        microscopeIcon.getStyleClass().add("microscopeIcon");
		UIDIconContainer.setCenter(microscopeIcon);
    	vBox.getChildren().add(UIDIconContainer);
    	
    	UIDLabel = new JFXTextField();
    	UIDLabel.setPrefHeight(20.0);
    	UIDLabel.setPrefWidth(100.0);
    	UIDLabel.setText("metaUID");
    	UIDLabel.setEditable(false);
    	
    	UIDClippyButton = new JFXButton();
    	UIDClippyButton.setPrefHeight(20.0);
    	UIDClippyButton.setPrefWidth(20.0);
    	UIDClippyButton.setOnAction(e -> {
    		ClipboardContent content = new ClipboardContent();
    	    content.putString(UIDLabel.getText());
    	    clipboard.setContent(content);
    	});
    	UIDClippyButton.setGraphic(OctIconFactory.get().createIcon(de.jensd.fx.glyphs.octicons.OctIcon.CLIPPY, "1.3em"));
    	
    	HBox hbox = new HBox();
    	hbox.getChildren().add(UIDLabel);
    	hbox.getChildren().add(UIDClippyButton);
    	hbox.setAlignment(Pos.CENTER);
    	vBox.getChildren().add(hbox);
    	
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
    	vBox.getChildren().add(notes);
    	
    	notesTextArea = new TextArea();
    	VBox.setMargin(notesTextArea, new Insets(10, 10, 10, 10));
    	notesTextArea.setMinHeight(150.0);
    	notesTextArea.setPromptText("none");
    	vBox.getChildren().add(notesTextArea);
    	
    	rootPane.setFitToWidth(true);
    	rootPane.setContent(vBox);
		
		getNode().addEventHandler(MetadataEvent.METADATA_EVENT, this);
		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, new DefaultMoleculeArchiveEventHandler() {
        	@Override
        	public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> newArchive) {
        		archive = newArchive;
        	}
        });
		
		chipsListener = new ListChangeListener<String>() {
			@Override
			public void onChanged(Change<? extends String> c) {
				if (marsMetadata == null)
					return;
				
				while (c.next()) {
		             if (c.wasRemoved()) {
		            	 marsMetadata.removeTag(c.getRemoved().get(0));
		             } else if (c.wasAdded()) {
		            	 marsMetadata.addTag(c.getAddedSubList().get(0));
		             }
				}
				getNode().fireEvent(new MetadataTagsChangedEvent(marsMetadata));
			}
		};
	
		if (notesListener == null) {
			notesListener = new ChangeListener<String>() {
			    @Override
			    public void changed(final ObservableValue<? extends String> observable, final String oldValue, final String newValue) {
			    	if (marsMetadata == null)
						return;
			    	
			    	marsMetadata.setNotes(notesTextArea.getText());
			    }
			};
		}
		
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
		
		UIDLabel.setText(marsMetadata.getUID());
		
		chipView.getChips().removeListener(chipsListener);
		chipView.getChips().clear();
		if (marsMetadata.getTags().size() > 0)
			chipView.getChips().addAll(marsMetadata.getTags());

		chipView.getSuggestions().clear();
		chipView.getSuggestions().addAll(archive.properties().getTagSet());
		chipView.getChips().addListener(chipsListener);
		
		notesTextArea.textProperty().removeListener(notesListener);
		notesTextArea.setText(marsMetadata.getNotes());
		notesTextArea.textProperty().addListener(notesListener);
	}

	@Override
	public void handle(MetadataEvent event) {
		event.invokeHandler(this);
		event.consume();
	}
}
