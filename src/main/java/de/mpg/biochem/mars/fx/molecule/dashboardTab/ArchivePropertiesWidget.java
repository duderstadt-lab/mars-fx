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
package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.dashboard.AbstractBubbleChartWidget;
import de.mpg.biochem.mars.fx.dashboard.AbstractDashboardWidget;
import de.mpg.biochem.mars.fx.dashboard.MarsDashboardWidget;
import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.plugin.Parameter;
import net.imagej.ops.Initializable;

@Plugin( type = MoleculeArchiveDashboardWidget.class, name = "ArchivePropertiesWidget" )
public class ArchivePropertiesWidget extends AbstractDashboardWidget implements MoleculeArchiveDashboardWidget, SciJavaPlugin, Initializable {
	
	@Parameter
	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;
	
	private Label archiveName = new Label();
	private Label className = new Label();
	private Label moleculeNumber = new Label();
	private Label metadataNumber = new Label();
	private Label memorySetting = new Label();
	
	@Override
	public void initialize() {
		super.initialize();
		
		run();
		
    	VBox vbox = new VBox();
        
        vbox.setPadding(new Insets(20, 20, 20, 20));
		vbox.setSpacing(5);
		
		BorderPane iconContainer = new BorderPane();
		iconContainer.setCenter(FontAwesomeIconFactory.get().createIcon(INFO_CIRCLE, "2em"));

		vbox.getChildren().add(iconContainer);
		
        vbox.getChildren().add(archiveName);
        vbox.getChildren().add(className);
        vbox.getChildren().add(moleculeNumber);
        vbox.getChildren().add(metadataNumber);
        vbox.getChildren().add(memorySetting);

		vbox.setPrefSize(250, 250);
		
        setContent(vbox);
        
        rootPane.setMinSize(250, 250);
        rootPane.setMaxSize(250, 250);
	}
	
	@Override
	public void run() {
	    Platform.runLater(new Runnable() {
			@Override
			public void run() {
				archiveName.setText(archive.getName());
				String archiveType = archive.getClass().getName();
				int i = archiveType.lastIndexOf('.');
				if (i > 0)
				    archiveType = archiveType.substring(i+1);
				className.setText(archiveType);
				moleculeNumber.setText(archive.getNumberOfMolecules() + " Molecules");
				metadataNumber.setText(archive.getNumberOfMetadatas() + " Metadata");
				if (archive.isVirtual()) {
					memorySetting.setText("Virtual memory store");
				} else {
					memorySetting.setText("Normal memory");
				}
			}
    	});
	}
	
	public void setArchive(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive) {
		this.archive = archive;
	}
	
	public MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> getArchive() {
		return archive;
	}

	@Override
	public Node getIcon() {
		return (Node) FontAwesomeIconFactory.get().createIcon(INFO_CIRCLE, "1.2em");
	}

	@Override
	public String getName() {
		return "ArchivePropertiesWidget";
	}
}
