package de.mpg.biochem.mars.fx.molecule;

import java.util.ArrayList;

import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;

public abstract class AbstractMoleculeArchiveTab extends Tab implements MoleculeArchiveTab {

    protected double tabWidth = 60.0;
    
    protected MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive;
	
	protected EventHandler<Event> replaceBackgroundColorHandler = event -> {
        Tab currentTab = (Tab) event.getTarget();
        if (currentTab.isSelected()) {
            currentTab.setStyle("-fx-background-color: -fx-focus-color;");
        } else {
            currentTab.setStyle("-fx-background-color: -fx-accent;");
        }
    };
    
    public AbstractMoleculeArchiveTab() {
    	super();
        setText("");
        setOnSelectionChanged(replaceBackgroundColorHandler);
        closableProperty().set(false);
    }
	
	protected void setIcon(Node icon) {
		BorderPane tabPane = new BorderPane();
        tabPane.setRotate(90.0);
        tabPane.setMaxWidth(tabWidth);
        tabPane.setCenter(icon);
        setGraphic(tabPane);
	}
    
    public abstract ArrayList<Menu> getMenus();
    
    @Override
    public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive) {
    	this.archive = archive;
    }

    @Override
    public void handle(MoleculeArchiveEvent event) {
        event.invokeHandler(this);
        event.consume();
    }

	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}
	
	public abstract Node getNode();
	
	public MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> getArchive() {
		return this.archive;
	}
	
	//Override any events below that should trigger a specific response.

	@Override
	public void onMoleculeArchiveLockEvent() {}

	@Override
	public void onMoleculeArchiveUnlockEvent() {}

	@Override
	public void onMoleculeArchiveSavingEvent() {}

	@Override
	public void onMoleculeArchiveSavedEvent() {}
}
