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
import de.mpg.biochem.mars.fx.molecule.MoleculeArchiveSubTab;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

public class GeneralTabController implements MoleculeSubTab, MoleculeArchiveSubTab {
	
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
	
	private Molecule molecule;
	
	private MoleculeArchive<?,?,?> archive;
	
	private ListChangeListener<String> chipsListener;
	private ChangeListener<String> notesListener;
	
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
    }
	
	public void update() {
		if (chipsListener == null) {
			chipsListener = new ListChangeListener<String>() {
				@Override
				public void onChanged(Change<? extends String> c) {
					while (c.next()) {
			             if (c.wasRemoved()) {
			                 molecule.removeTag(c.getRemoved().get(0));
			             } else if (c.wasAdded()) {
			            	 molecule.addTag(c.getAddedSubList().get(0));
			             }
					}
				}
			};
		}
		
		if (notesListener == null) {
			notesListener = new ChangeListener<String>() {
			    @Override
			    public void changed(final ObservableValue<? extends String> observable, final String oldValue, final String newValue) {
			        molecule.setNotes(notesTextArea.getText());
			    }
			};
		}
		
		UIDLabel.setText(molecule.getUID());
		metaUIDLabel.setText(molecule.getImageMetadataUID());
		
		chipView.getChips().removeListener(chipsListener);
		chipView.getChips().clear();
		if (molecule.getTags().size() > 0)
			chipView.getChips().addAll(molecule.getTags());
		chipView.getChips().addListener(chipsListener);
		
		chipView.getSuggestions().clear();
		chipView.getSuggestions().addAll(archive.getProperties().getTagSet());
		
		notesTextArea.textProperty().removeListener(notesListener);
		notesTextArea.setText(molecule.getNotes());
		notesTextArea.textProperty().addListener(notesListener);

		
		JFXChipViewSkin<String> skin = new JFXChipViewSkin<>(chipView);
		chipView.setSkin(skin);
	     /*   
		for (Node node : ((FlowPane) ((ScrollPane) skin.getChildren().get(0)).getContent()).getChildren()) {
			if (node instanceof TextArea) {
				//((TextArea) node).cancelEdit();
				
				//((TextArea) node).setEditable(false);
				if (((TextArea) node).isFocused())
					System.out.println("Focused");
			}
		}
		*/
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
	public void setMolecule(Molecule molecule) {
		this.molecule = molecule;
		update();
	}

	@Override
	public void setArchive(MoleculeArchive<?,?,?> archive) {
		this.archive = archive;
	}
}
