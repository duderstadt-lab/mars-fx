package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.INFO_CIRCLE;

import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

public class ArchivePropertiesWidget extends AbstractDashboardWidget {
	
	public ArchivePropertiesWidget(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive, DashboardTab parent) {
		super(archive, parent);
		
    	VBox vbox = new VBox();
        
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
		vbox.setPrefSize(250, 250);
		
		Tab chartTab = new Tab();
		
		getTabPane().getTabs().add(chartTab);
		
        chartTab.setContent(vbox);
        
        rootPane.setMinSize(250, 250);
        rootPane.setMaxSize(250, 250);
	}

	@Override
	public String getName() {
		return "ArchivePropertiesWidget";
	}
	
	@Override
	public void load() {

	}

	@Override
	protected void createIOMaps() {
		// TODO Auto-generated method stub
	}

	@Override
	public GlyphIcons getIcon() {
		return INFO_CIRCLE;
	}

}
