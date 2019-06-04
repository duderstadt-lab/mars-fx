package de.mpg.biochem.mars.fx.molecule.moleculesTab;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.INFO_CIRCLE;

import java.util.ArrayList;

import com.jfoenix.controls.JFXChipView;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.skins.JFXChipViewSkin;
import com.jfoenix.controls.JFXButton;
import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.fx.molecule.MoleculeArchiveSubTab;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
	
	final Clipboard clipboard = Clipboard.getSystemClipboard();
	
	private Molecule molecule;
	
	private MoleculeArchive archive;
	
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
    }
	
	public void update() {
		UIDLabel.setText(molecule.getUID());
		metaUIDLabel.setText(molecule.getImageMetaDataUID());
		
		chipView.getChips().clear();
		chipView.getChips().addAll(molecule.getTags());
		
		chipView.getSuggestions().clear();
		chipView.getSuggestions().addAll(archive.getProperties().getTagSet());
		
		// JFXChipViewSkin<String> skin = new JFXChipViewSkin<>(chipView);
		// chipView.setSkin(skin);
	        
		/*
		for (Node node : ((FlowPane) ((ScrollPane) skin.getChildren().get(0)).getContent()).getChildren()) {
			if (node instanceof TextArea) {
				//((TextArea) node).cancelEdit();
				System.out.println("Cancel Edit");
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
	public void setArchive(MoleculeArchive archive) {
		this.archive = archive;
	}
}
