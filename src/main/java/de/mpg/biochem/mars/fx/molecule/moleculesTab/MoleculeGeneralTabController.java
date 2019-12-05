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
import de.mpg.biochem.mars.fx.util.MarsJFXChipViewSkin;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

public class MoleculeGeneralTabController implements MoleculeSubPane {
	
	@FXML
	private AnchorPane rootPane;
	
	@FXML
	private BorderPane UIDIconContainer;
	
	@FXML
	private JFXTextField UIDLabel;
	
	@FXML
	private JFXButton UIDClippyButton;
	
	@FXML
	private BorderPane metaUIDIconContainer;
	
	@FXML
	private JFXTextField metaUIDLabel;
	
	@FXML
	private JFXButton metaUIDClippyButton;
	
	@FXML
	private Label Tags;
	
	@FXML
	private JFXChipView<String> chipView;
	
	@FXML
	private Label Notes;
	
	@FXML
	private JFXTextArea notesTextArea;
	
	final Clipboard clipboard = Clipboard.getSystemClipboard();
	
	private ListChangeListener<String> chipsListener;
	private ChangeListener<String> notesListener;
	
	private MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive;
	private Molecule molecule;
	private MarsJFXChipViewSkin<String> skin;
	
	@FXML
    public void initialize() {
		UIDIconContainer.setCenter(MaterialIconFactory.get().createIcon(de.jensd.fx.glyphs.materialicons.MaterialIcon.FINGERPRINT, "2.5em"));
		UIDClippyButton.setGraphic(OctIconFactory.get().createIcon(de.jensd.fx.glyphs.octicons.OctIcon.CLIPPY, "1.3em"));
		
		Region microscopeIcon = new Region();
        microscopeIcon.getStyleClass().add("microscopeIcon");
		metaUIDIconContainer.setCenter(microscopeIcon);
		metaUIDClippyButton.setGraphic(OctIconFactory.get().createIcon(de.jensd.fx.glyphs.octicons.OctIcon.CLIPPY, "1.3em"));
		
		UIDLabel.setEditable(false);
		metaUIDLabel.setEditable(false);
		
		notesTextArea.setPromptText("none");
		
		getNode().addEventHandler(MoleculeEvent.MOLECULE_EVENT, this);
		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, new DefaultMoleculeArchiveEventHandler() {
        	@Override
        	public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> newArchive) {
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
	
	@FXML
	private void handleUIDClippy() {
		ClipboardContent content = new ClipboardContent();
	    content.putString(UIDLabel.getText());
	    clipboard.setContent(content);
	}

	@FXML
	private void handleMetaUIDClippy() {
		ClipboardContent content = new ClipboardContent();
	    content.putString(metaUIDLabel.getText());
	    clipboard.setContent(content);
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
		metaUIDLabel.setText(molecule.getImageMetadataUID());
		
		chipView.getChips().removeListener(chipsListener);
		chipView.getChips().clear();
		if (molecule.getTags().size() > 0)
			chipView.getChips().addAll(molecule.getTags());
		
		chipView.getSuggestions().clear();
		chipView.getSuggestions().addAll(archive.getProperties().getTagSet());
		chipView.getChips().addListener(chipsListener);
		
		notesTextArea.textProperty().removeListener(notesListener);
		notesTextArea.setText(molecule.getNotes());
		notesTextArea.textProperty().addListener(notesListener);		
	}
}
