/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2021 Karl Duderstadt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package de.mpg.biochem.mars.fx.molecule;

import java.util.ArrayList;

import org.scijava.Context;
import org.scijava.plugin.Parameter;

import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.AbstractJsonConvertibleRecord;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.molecule.MoleculeArchiveService;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;

public abstract class AbstractMoleculeArchiveTab extends AbstractJsonConvertibleRecord implements MoleculeArchiveTab {

	protected Tab tab;
    protected double tabWidth = 60.0;
    
    @Parameter
    protected MoleculeArchiveService moleculeArchiveService;
    
    protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;
	
	protected EventHandler<Event> replaceBackgroundColorHandler = event -> {
        Tab currentTab = (Tab) event.getTarget();
        if (currentTab.isSelected()) {
            currentTab.setStyle("-fx-background-color: -fx-focus-color;");
        } else {
            currentTab.setStyle("-fx-background-color: -fx-accent;");
        }
    };
    
    public AbstractMoleculeArchiveTab(final Context context) {
    	super();
    	context.inject(this);
    	tab = new Tab();
    	tab.setText("");
    	tab.setOnSelectionChanged(replaceBackgroundColorHandler);
    	tab.closableProperty().set(false);
    }
	
	protected void setIcon(Node icon) {
		BorderPane tabPane = new BorderPane();
        tabPane.setRotate(90.0);
        tabPane.setMaxWidth(tabWidth);
        tabPane.setCenter(icon);
        tab.setGraphic(tabPane);
	}
    
    public abstract ArrayList<Menu> getMenus();
    
    @Override
    public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive) {
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
	
	public Tab getTab() {
		return tab;
	}
	
	public MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> getArchive() {
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
