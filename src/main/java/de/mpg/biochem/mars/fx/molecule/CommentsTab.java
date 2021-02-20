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

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Menu;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.prefs.Preferences;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.scijava.Context;

import de.mpg.biochem.mars.fx.*;
import de.mpg.biochem.mars.fx.editor.CommentEditor;
import de.mpg.biochem.mars.fx.editor.CommentPane;
import de.mpg.biochem.mars.fx.editor.SmartEdit;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.options.MarkdownExtensionsPane;
import de.mpg.biochem.mars.fx.options.Options;
import de.mpg.biochem.mars.fx.options.Options.RendererType;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.fx.util.Utils;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;

public class CommentsTab extends AbstractMoleculeArchiveTab {
	private CommentPane commentPane;
	
	public CommentsTab(final Context context) {
		super(context);
		
		Region bookIcon = new Region();
        bookIcon.getStyleClass().add("bookIcon");
		
		setIcon(bookIcon);
    	
    	commentPane = new CommentPane();

		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, this);
		
		getTab().setContent(commentPane.getNode());
	}
    
	@Override
    public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive) {
    	super.onInitializeMoleculeArchiveEvent(archive);
    	commentPane.setComments(archive.getComments());
	}
	
	public CommentPane getCommentPane() {
		return commentPane;
	}
	
	public Node getNode() {
		return commentPane.getNode();
	}
	
	public ArrayList<Menu> getMenus() {
		return commentPane.getMenus();
	}

	@Override
	public void onMoleculeArchiveLockEvent() {
		archive.setComments(commentPane.getComments());
	}
	
	public void saveComments() {
		archive.setComments(commentPane.getComments());
	}

	@Override
	public void onMoleculeArchiveSavingEvent() {}

	@Override
	protected void createIOMaps() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getName() {
		return "CommentsTab";
	}
}
