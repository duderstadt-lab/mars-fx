/*******************************************************************************
 * Copyright (C) 2019, Duderstadt Lab
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package de.mpg.biochem.mars.fx.molecule.moleculesTab;

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

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.fx.event.DefaultMoleculeArchiveEventHandler;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.event.MoleculeTagsChangedEvent;
import de.mpg.biochem.mars.fx.util.MarsJFXChipViewSkin;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.text.TextFlow;
import javafx.scene.text.Text;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

public class MoleculeGeneralTabController implements MoleculeSubPane {
	
	private AnchorPane rootPane;
	private BorderPane UIDIconContainer;
	private JFXTextField UIDLabel;
	private JFXButton UIDClippyButton;
	private BorderPane metaUIDIconContainer;
	private JFXTextField metaUIDLabel;
	private JFXButton metaUIDClippyButton;
	private Text iText, iInt, cText, cInt;
	private TextFlow imageAndchannel;
	private Label tags;
	private JFXChipView<String> chipView;
	private Label notes;
	private JFXTextArea notesTextArea;
	
	final Clipboard clipboard = Clipboard.getSystemClipboard();
	
	private ListChangeListener<String> chipsListener;
	private ChangeListener<String> notesListener;
	
	private MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties> archive;
	private Molecule molecule;
	private MarsJFXChipViewSkin<String> skin;
	
    public MoleculeGeneralTabController() {
    	rootPane = new AnchorPane();
    	rootPane.setPrefHeight(350.0);
    	rootPane.setPrefWidth(220.0);
    	rootPane.setMinWidth(220.0);
    	rootPane.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/moleculesTab/MoleculeGeneralTab.css");
    	
    	UIDIconContainer = new BorderPane();
    	UIDIconContainer.setLayoutX(56.0);
    	UIDIconContainer.setLayoutY(32.0);
    	UIDIconContainer.setPrefHeight(55.0);
    	UIDIconContainer.setPrefWidth(60.0);
    	AnchorPane.setLeftAnchor(UIDIconContainer, 70.0);
    	AnchorPane.setTopAnchor(UIDIconContainer, 5.0);
    	UIDIconContainer.setCenter(MaterialIconFactory.get().createIcon(de.jensd.fx.glyphs.materialicons.MaterialIcon.FINGERPRINT, "2.5em"));
    	rootPane.getChildren().add(UIDIconContainer);
    	
    	UIDLabel = new JFXTextField();
    	UIDLabel.setLayoutX(39.0);
        UIDLabel.setLayoutY(78.0);
        UIDLabel.setPrefHeight(20.0);
    	UIDLabel.setPrefWidth(180.0);
    	AnchorPane.setLeftAnchor(UIDLabel, 10.0);
    	AnchorPane.setTopAnchor(UIDLabel, 60.0);
    	UIDLabel.setText("UID");
    	UIDLabel.setEditable(false);
    	rootPane.getChildren().add(UIDLabel);
    	
    	UIDClippyButton = new JFXButton();
    	UIDClippyButton.setPrefHeight(20.0);
    	UIDClippyButton.setPrefWidth(20.0);
    	AnchorPane.setLeftAnchor(UIDClippyButton, 190.0);
    	AnchorPane.setTopAnchor(UIDClippyButton, 60.0);
    	UIDClippyButton.setOnAction(e -> {
    		ClipboardContent content = new ClipboardContent();
    	    content.putString(UIDLabel.getText());
    	    clipboard.setContent(content);
    	});
    	UIDClippyButton.setGraphic(OctIconFactory.get().createIcon(de.jensd.fx.glyphs.octicons.OctIcon.CLIPPY, "1.3em"));
    	rootPane.getChildren().add(UIDClippyButton);
    	
    	metaUIDIconContainer = new BorderPane();
    	metaUIDIconContainer.setPrefHeight(60.0);
    	metaUIDIconContainer.setPrefWidth(70.0);
    	AnchorPane.setLeftAnchor(metaUIDIconContainer, 65.0);
    	AnchorPane.setTopAnchor(metaUIDIconContainer, 90.0);
    	Region microscopeIcon = new Region();
        microscopeIcon.getStyleClass().add("microscopeIcon");
		metaUIDIconContainer.setCenter(microscopeIcon);
    	rootPane.getChildren().add(metaUIDIconContainer);
    	
    	metaUIDLabel = new JFXTextField();
    	metaUIDLabel.setPrefHeight(20.0);
    	metaUIDLabel.setPrefWidth(90.0);
    	AnchorPane.setLeftAnchor(metaUIDLabel, 55.0);
    	AnchorPane.setTopAnchor(metaUIDLabel, 150.0);
    	metaUIDLabel.setText("metaUID");
    	metaUIDLabel.setEditable(false);
    	rootPane.getChildren().add(metaUIDLabel);
    	
    	metaUIDClippyButton = new JFXButton();
    	metaUIDClippyButton.setPrefHeight(20.0);
    	metaUIDClippyButton.setPrefWidth(20.0);
    	AnchorPane.setLeftAnchor(metaUIDClippyButton, 145.0);
    	AnchorPane.setTopAnchor(metaUIDClippyButton, 150.0);
    	metaUIDClippyButton.setOnAction(e -> {
    		ClipboardContent content = new ClipboardContent();
    	    content.putString(metaUIDLabel.getText());
    	    clipboard.setContent(content);
    	});
    	metaUIDClippyButton.setGraphic(OctIconFactory.get().createIcon(de.jensd.fx.glyphs.octicons.OctIcon.CLIPPY, "1.3em"));
    	rootPane.getChildren().add(metaUIDClippyButton);
    	
    	imageAndchannel = new TextFlow();
    	AnchorPane.setLeftAnchor(imageAndchannel, 87.0);
    	AnchorPane.setTopAnchor(imageAndchannel, 200.0);
    	iText = new Text("");
    	iText.setStyle("-fx-font-weight:bold");
    	iInt = new Text("");
    	iInt.setStyle("-fx-font-weight:normal");
    	cText = new Text("");
    	cText.setStyle("-fx-font-weight:bold");
    	cInt = new Text("");
    	cInt.setStyle("-fx-font-weight:normal");
    	imageAndchannel.getChildren().addAll(iText, iInt, cText, cInt);
    	rootPane.getChildren().add(imageAndchannel);
    	
    	tags = new Label();
        AnchorPane.setLeftAnchor(tags, 10.0);
        AnchorPane.setTopAnchor(tags, 220.0);
        tags.setText("Tags");
        rootPane.getChildren().add(tags);
    	
    	chipView = new JFXChipView<String>();
    	chipView.setLayoutY(241.0);
   	    chipView.setMinHeight(170.0);
   	    chipView.setPrefHeight(170.0);
   	    chipView.setPrefWidth(200.0);
   	    AnchorPane.setLeftAnchor(chipView, 5.0);
   	    AnchorPane.setRightAnchor(chipView, 5.0);
   	    AnchorPane.setTopAnchor(chipView, 241.0);
   	    rootPane.getChildren().add(chipView);
    	
    	notes = new Label();
        AnchorPane.setLeftAnchor(notes, 10.0);
        AnchorPane.setTopAnchor(notes, 400.0);
        notes.setText("Notes");
    	rootPane.getChildren().add(notes);
    	
    	notesTextArea = new JFXTextArea();
    	notesTextArea.setLayoutY(421.0);
    	notesTextArea.setMinHeight(149.0);
    	notesTextArea.setPrefHeight(149.0);
    	notesTextArea.setPrefWidth(190.0);
    	AnchorPane.setBottomAnchor(notesTextArea, 15.0);
    	AnchorPane.setLeftAnchor(notesTextArea, 15.0);
    	AnchorPane.setRightAnchor(notesTextArea, 15.0);
    	AnchorPane.setTopAnchor(notesTextArea, 431.0);
    	notesTextArea.setPromptText("none");
    	rootPane.getChildren().add(notesTextArea);
    	
		getNode().addEventHandler(MoleculeEvent.MOLECULE_EVENT, this);
		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, new DefaultMoleculeArchiveEventHandler() {
        	@Override
        	public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties> newArchive) {
        		archive = newArchive;
        	}
        });
		
		chipsListener = new ListChangeListener<String>() {
			@Override
			public void onChanged(Change<? extends String> c) {
				if (molecule == null)
					return;
				
				while (c.next()) {
		             if (c.wasRemoved()) {
		                 molecule.removeTag(c.getRemoved().get(0));
		             } else if (c.wasAdded()) {
		            	 molecule.addTag(c.getAddedSubList().get(0));
		             }
				}
				getNode().fireEvent(new MoleculeTagsChangedEvent(molecule));
			}
		};
		
		if (notesListener == null) {
			notesListener = new ChangeListener<String>() {
			    @Override
			    public void changed(final ObservableValue<? extends String> observable, final String oldValue, final String newValue) {
			    	if (molecule == null)
						return;
			    	
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
		
		UIDLabel.setText(molecule.getUID());
		metaUIDLabel.setText(molecule.getMetadataUID());

		if (molecule.getImage() > -1) {
			iText.setText("I ");
			iInt.setText(String.valueOf(molecule.getImage()) + " ");
		} else {
			iText.setText("");
			iInt.setText("");
		}
		
		if (molecule.getChannel() > -1) {
			cText.setText("C ");
			cInt.setText(String.valueOf(molecule.getChannel()));
		} else {
			cText.setText("");
			cInt.setText("");
		}
		
		if (!iText.getText().equals("") && !cText.getText().equals(""))
			AnchorPane.setLeftAnchor(imageAndchannel, 64.0);
		else
			AnchorPane.setLeftAnchor(imageAndchannel, 87.0);
		
		chipView.getChips().removeListener(chipsListener);
		chipView.getChips().clear();
		if (molecule.getTags().size() > 0)
			chipView.getChips().addAll(molecule.getTags());
		
		chipView.getSuggestions().clear();
		chipView.getSuggestions().addAll(archive.properties().getTagSet());
		chipView.getChips().addListener(chipsListener);
		
		notesTextArea.textProperty().removeListener(notesListener);
		notesTextArea.setText(molecule.getNotes());
		notesTextArea.textProperty().addListener(notesListener);		
	}
}
