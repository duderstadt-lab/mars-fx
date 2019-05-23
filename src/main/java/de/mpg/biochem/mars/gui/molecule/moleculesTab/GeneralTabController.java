package de.mpg.biochem.mars.gui.molecule.moleculesTab;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.INFO_CIRCLE;

import java.util.ArrayList;

import com.jfoenix.controls.JFXChipView;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.skins.JFXChipViewSkin;
import com.jfoenix.controls.JFXButton;
import javafx.scene.control.TextArea;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.gui.molecule.MoleculeArchiveSubTab;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
		
		/*
		JFXChipViewSkin<String> skin = new JFXChipViewSkin<>(chipView);
		chipView.setSkin(skin);
        TextArea textArea = (TextArea) ((FlowPane)skin.getChildren().get(0)).getChildren().get(0);
        textArea.focusedProperty().addListener((obs, oldValue, newValue) -> {
            System.out.println(newValue);
            if (!newValue) {
                if (StringUtils.isNotBlank(textArea.getText())) {
                    view.getChips().add(textArea.getText());
                    textArea.clear();
                }
            }
        });
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