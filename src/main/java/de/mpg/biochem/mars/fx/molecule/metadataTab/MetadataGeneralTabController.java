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

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.fx.event.MarsImageMetadataEvent;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

public class MetadataGeneralTabController implements MetadataSubPane<MarsImageMetadata> {
	
	@FXML
	private BorderPane UIDIconContainer;
	
	@FXML
	private JFXTextField UIDLabel;
	
	@FXML
	private JFXButton UIDClippyButton;
	
	@FXML
	private Label Tags;
	
	@FXML
	private JFXChipView<String> chipView;
	
	@FXML
	private Label Notes;
	
	@FXML
	private JFXTextArea notesTextArea;
	
	final Clipboard clipboard = Clipboard.getSystemClipboard();
	
	private MarsImageMetadata marsImageMetadata;
	
	private MoleculeArchive<Molecule,MarsImageMetadata,MoleculeArchiveProperties> archive;
	
	private ListChangeListener<String> chipsListener;
	private ChangeListener<String> notesListener;
	
	@FXML
    public void initialize() {
		Region microscopeIcon = new Region();
        microscopeIcon.getStyleClass().add("microscopeIcon");
        UIDIconContainer.setCenter(microscopeIcon);
        UIDClippyButton.setGraphic(OctIconFactory.get().createIcon(de.jensd.fx.glyphs.octicons.OctIcon.CLIPPY, "1.3em"));
		
		UIDLabel.setEditable(false);
		
		notesTextArea.setPromptText("none");
    }
	
	public void update() {
		if (chipsListener == null) {
			chipsListener = new ListChangeListener<String>() {
				@Override
				public void onChanged(Change<? extends String> c) {
					while (c.next()) {
			             if (c.wasRemoved()) {
			                 marsImageMetadata.removeTag(c.getRemoved().get(0));
			             } else if (c.wasAdded()) {
			            	 marsImageMetadata.addTag(c.getAddedSubList().get(0));
			             }
					}
				}
			};
		}
		
		if (notesListener == null) {
			notesListener = new ChangeListener<String>() {
			    @Override
			    public void changed(final ObservableValue<? extends String> observable, final String oldValue, final String newValue) {
			        marsImageMetadata.setNotes(notesTextArea.getText());
			    }
			};
		}
		
		UIDLabel.setText(marsImageMetadata.getUID());
		
		chipView.getChips().removeListener(chipsListener);
		chipView.getChips().clear();
		if (marsImageMetadata.getTags().size() > 0)
			chipView.getChips().addAll(marsImageMetadata.getTags());
		chipView.getChips().addListener(chipsListener);
		
		chipView.getSuggestions().clear();
		chipView.getSuggestions().addAll(archive.getProperties().getTagSet());
		
		notesTextArea.textProperty().removeListener(notesListener);
		notesTextArea.setText(marsImageMetadata.getNotes());
		notesTextArea.textProperty().addListener(notesListener);
	}
	
	@FXML
	private void handleUIDClippy() {
		ClipboardContent content = new ClipboardContent();
	    content.putString(UIDLabel.getText());
	    clipboard.setContent(content);
	}

	public void setArchive(MoleculeArchive<Molecule,MarsImageMetadata,MoleculeArchiveProperties> archive) {
		this.archive = archive;
	}

	public Node getNode() {
		// TODO Auto-generated method stub
		//For the moment this is not needed
		//and the Pane is only in the fxml??
		return null;
	}

	public void setMetadata(MarsImageMetadata marsImageMetadata) {
		this.marsImageMetadata = marsImageMetadata;
		update();
	}

	@Override
	public void fireEvent(Event event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMarsImageMetadataSelectionChangedEvent(MarsImageMetadata marsImageMetadata) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handle(MarsImageMetadataEvent event) {
		event.invokeHandler(this);
	}
}
