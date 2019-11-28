package de.mpg.biochem.mars.fx.options;

import java.util.ArrayList;

import com.jfoenix.controls.JFXToggleButton;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.layout.BorderPane;

public class MarsOptionsPane {
	private JFXToggleButton smileEncodingButton;
	
	private BorderPane rootPane;
	
	public MarsOptionsPane() {
		//setIcon(FontAwesomeIconFactory.get().createIcon(COG, "1.3em"));
		
		smileEncodingButton = new JFXToggleButton();
		rootPane = new BorderPane();
		rootPane.setCenter(smileEncodingButton);
		
		//setContent(rootPane);
	}
	
	public void handleToggleSmileEncoding() {
		if (smileEncodingButton.isSelected()) {
			//archive.setSMILEOutputEncoding();
		} else {
			//archive.unsetSMILEOutputEncoding();
		}
	}
	
	public Node getNode() {
		return this.rootPane;
	}

}