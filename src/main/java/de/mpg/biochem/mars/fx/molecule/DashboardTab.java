package de.mpg.biochem.mars.fx.molecule;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

import javafx.geometry.Insets;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;

import javafx.scene.control.ScrollPane;
import com.jfoenix.controls.JFXMasonryPane;
import com.jfoenix.controls.JFXScrollPane;
import javafx.scene.layout.VBox;
import javafx.application.Platform;

import javafx.scene.layout.BorderStroke;
import javafx.scene.paint.Color;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.BorderWidths;

public class DashboardTab extends AbstractMoleculeArchiveTab {
    protected ScrollPane scrollPane;
    
    protected JFXMasonryPane masonryPane;
	
    public DashboardTab() {
    	super();
    	setIcon(MaterialIconFactory.get().createIcon(de.jensd.fx.glyphs.materialicons.MaterialIcon.DASHBOARD, "1.3em"));
    	
    	masonryPane = new JFXMasonryPane();
    	scrollPane = new ScrollPane();
    	
    	scrollPane.setContent(masonryPane);
        JFXScrollPane.smoothScrolling(scrollPane);
        
        getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, this);
        
    	setContent(scrollPane);
    }
    
    public BorderPane buildInfoCard() {
    	BorderPane borderPane = new BorderPane();
    	VBox vbox = new VBox();
        double width = 450;
        borderPane.setMinWidth(width);
        double height = 200;
        borderPane.setMinHeight(height);
        
        borderPane.setPadding(new Insets(15, 15, 15, 15));
        vbox.setBorder(new Border(new BorderStroke(Color.BLACK, 
                BorderStrokeStyle.SOLID, new CornerRadii(20), new BorderWidths(1))));
        vbox.setPadding(new Insets(20, 20, 20, 20));
		vbox.setSpacing(5);
		
		BorderPane iconContainer = new BorderPane();
		iconContainer.setCenter(FontAwesomeIconFactory.get().createIcon(INFO_CIRCLE, "2em"));

		vbox.getChildren().add(iconContainer);
		
        vbox.getChildren().add(new Label(archive.getName()));
        vbox.getChildren().add(new Label(archive.getClass().getName()));
        vbox.getChildren().add(new Label(archive.getNumberOfMolecules() + " Molecules"));
        vbox.getChildren().add(new Label(archive.getNumberOfImageMetadataRecords() + " Metadata"));
        
		if (archive.isVirtual()) {
			vbox.getChildren().add(new Label("Virtual memory store"));
		} else {
			vbox.getChildren().add(new Label("Normal memory"));
		}
		
		borderPane.setCenter(vbox);
		
		return borderPane;
    }
    
    public Node getNode() {
		return scrollPane;
	}
    
	public ArrayList<Menu> getMenus() {
		return null;
	}
	
    @Override
    public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive) {
    	this.archive = archive;
    	masonryPane.getChildren().add(buildInfoCard());    
    }
}
