package de.mpg.biochem.mars.gui.molecule.moleculesTab;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.INFO_CIRCLE;

import java.util.ArrayList;

import com.jfoenix.controls.JFXChipView;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
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
	private Label UIDLabel;
	
	@FXML
	private Button UIDClippyButton;
	
	@FXML
	private BorderPane metaUIDIconContainer;
	
	@FXML
	private Label metaUIDLabel;
	
	@FXML
	private Button metaUIDClippyButton;
	
	@FXML
	private Label Tags;
	
	@FXML
	private JFXChipView<String> chipView;
	
	private Molecule molecule;
	
	private MoleculeArchive archive;
	
	@FXML
    public void initialize() {
		UIDIconContainer.setCenter(MaterialIconFactory.get().createIcon(de.jensd.fx.glyphs.materialicons.MaterialIcon.FINGERPRINT, "2em"));
		
		//BorderPane UIDClippy = new BorderPane();
		//UIDClippy.setCenter(OctIconFactory.get().createIcon(de.jensd.fx.glyphs.octicons.OctIcon.CLIPPY, "1em"));
		//UIDClippyButton.setGraphic(UIDClippy);
		
		Region microscopeIcon = new Region();
        microscopeIcon.getStyleClass().add("microscopeIcon");
		metaUIDIconContainer.setCenter(microscopeIcon);
		//metaUIDClippyButton.setGraphic(UIDClippy);
    }
	
	public void update() {
		UIDLabel.setText(molecule.getUID());
		metaUIDLabel.setText(molecule.getImageMetaDataUID());
		
		chipView.getChips().clear();
		chipView.getChips().addAll(molecule.getTags());
		
		chipView.getSuggestions().clear();
		chipView.getSuggestions().addAll(archive.getProperties().getTagSet());
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
