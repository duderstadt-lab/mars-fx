/*
 * Copyright (c) 2018 Karl Tauber <karl at jformdesigner dot com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.mpg.biochem.mars.gui.projects;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.util.Duration;
import de.mpg.biochem.mars.gui.FileEditorManager;
import de.mpg.biochem.mars.gui.controls.FileTreeCell;
import de.mpg.biochem.mars.gui.controls.FileTreeItem;
import de.mpg.biochem.mars.gui.controls.FileTreeView;
import de.mpg.biochem.mars.gui.util.Utils;

/**
 * A tree view of directories and files of a project.
 *
 * @author Karl Tauber
 */
class ProjectFileTreeView
	extends FileTreeView
{
	private static final String KEY_SELECTION = "treeSelection";
	private static final String KEY_EXPANDED = "treeExpanded";
	private static final String KEY_VSCROLL = "treeVScroll";

	private final FileEditorManager fileEditorManager;

	private boolean inSetRoot;
	private ScrollBar vScrollBar;
	private Timeline saveScrollStateTimeline;

	ProjectFileTreeView(FileEditorManager fileEditorManager) {
		this.fileEditorManager = fileEditorManager;

		getStyleClass().add("project-tree-view");
		setShowRoot(false);

		setCellFactory(this::createCell);

		getSelectionModel().selectedItemProperty().addListener((observer, oldSelectedItem, newSelectedItem) -> {
			saveSelection();
		});

		Platform.runLater(() -> {
			vScrollBar = Utils.findVScrollBar(this);
			if (vScrollBar != null)
				vScrollBar.valueProperty().addListener((ob, o, n) -> saveScrollState());
		});

		projectChanged(ProjectManager.getActiveProject());
		ProjectManager.activeProjectProperty().addListener((observer, oldProject, newProject) -> {
			projectChanged(newProject);
		});
	}

	private TreeCell<File> createCell(TreeView<File> treeView) {
		FileTreeCell treeCell = new FileTreeCell();
		treeCell.setOnDragDetected(event -> {
			TreeItem<File> draggedItem = treeCell.getTreeItem();
			Dragboard db = treeCell.startDragAndDrop(TransferMode.COPY);

			ClipboardContent content = new ClipboardContent();
			content.putString(draggedItem.getValue().getAbsolutePath());
			content.put(DataFormat.FILES, Collections.singletonList(draggedItem.getValue()));
			db.setContent(content);

			event.consume();
		});
		return treeCell;
	}

	@Override
	protected void handleClicks(TreeItem<File> item, MouseButton button, int clickCount) {
		if (button == MouseButton.PRIMARY && item != null && item.getValue().isFile()) {
			if (clickCount == 1)
				fileEditorManager.openPreviewEditor(item.getValue());
			else if (clickCount == 2)
				fileEditorManager.openEditor(item.getValue());
		}
	}

	private void projectChanged(File activeProject) {
		if (activeProject == null) {
			setRoot(null);
			return;
		}

		FileTreeItem newRoot = new FileTreeItem(activeProject, this::acceptFile);

		inSetRoot = true;
		try {
			setRoot(newRoot);
		} finally {
			inSetRoot = false;
		}

		loadExpanded();
		loadSelection();
		loadScrollState();

		newRoot.addEventHandler(TreeItem.branchExpandedEvent(), event -> saveExpanded());
		newRoot.addEventHandler(TreeItem.branchCollapsedEvent(), event -> saveExpanded());
	}

	private boolean acceptFile(File dir, String name) {
		return !name.startsWith(".") ||
			!(name.equals(".git") || name.equals(".hg") || name.equals(".svn") || name.equals(".DS_Store"));
	}

	private void loadExpanded() {
		File project = getRoot().getValue();
		Preferences projectState = ProjectManager.getProjectState(project);
		if (projectState == null)
			return;

		String[] expanded = Utils.getPrefsStrings(projectState, KEY_EXPANDED);
		List<File> expandedDirectories = Stream.of(expanded)
			.map(relativePath -> new File(project, relativePath))
			.collect(Collectors.toList());
		setExpandedDirectories(expandedDirectories);
	}

	private void saveExpanded() {
		Platform.runLater(() -> {
			File project = getRoot().getValue();
			Preferences projectState = ProjectManager.getProjectState(project);
			if (projectState == null)
				return;

			List<File> expandedDirectories = getExpandedDirectories();
			Path projectPath = project.toPath();
			Utils.putPrefsStrings(projectState, KEY_EXPANDED, expandedDirectories.stream()
				.filter(f -> f != project)
				.map(f -> projectPath.relativize(f.toPath()).toString())
				.toArray(String[]::new));
		});
	}

	private void loadSelection() {
		Preferences projectState = getProjectState();
		if (projectState == null)
			return;

		String path = projectState.get(KEY_SELECTION, null);
		if (path != null) {
			File f = new File(path);
			List<TreeItem<File>> items = findItems(item -> item.getValue().equals(f));
			if (!items.isEmpty())
				getSelectionModel().select(items.get(0));
		}
	}

	private void saveSelection() {
		if (inSetRoot)
			return;

		Preferences projectState = getProjectState();
		if (projectState == null)
			return;

		TreeItem<File> selectedItem = getSelectionModel().getSelectedItem();
		String path = (selectedItem != null) ? selectedItem.getValue().getAbsolutePath() : null;
		Utils.putPrefs(projectState, KEY_SELECTION, path, null);
	}

	private void loadScrollState() {
		// slightly delay setting scrollbar value to give the VirtualFlow the chance
		// to complete layout, which may change scrollbar value
		Timeline timeline = new Timeline(
			new KeyFrame(Duration.millis(100),
			event -> loadScrollState2()));
		timeline.play();
	}

	private void loadScrollState2() {
		Preferences projectState = getProjectState();
		if (projectState == null)
			return;

		if (vScrollBar != null)
			vScrollBar.setValue(projectState.getDouble(KEY_VSCROLL, 0.0));
	}

	private void saveScrollState() {
		if (inSetRoot)
			return;

		// scroll bar value changes very often when user drags scroll thumb
		// --> use timeline to delay saving scroll state
		if (saveScrollStateTimeline == null) {
			saveScrollStateTimeline = new Timeline(
				new KeyFrame(Duration.millis(500),
				event -> saveScrollState2()));
		}

		saveScrollStateTimeline.playFromStart();
	}

	private void saveScrollState2() {
		Preferences projectState = getProjectState();
		if (projectState == null)
			return;

		if (vScrollBar != null)
			Utils.putPrefsDouble(projectState, KEY_VSCROLL, vScrollBar.getValue(), 0.0);
	}

	private Preferences getProjectState() {
		TreeItem<File> root = getRoot();
		return (root != null)
			? ProjectManager.getProjectState(root.getValue())
			: null;
	}
}
