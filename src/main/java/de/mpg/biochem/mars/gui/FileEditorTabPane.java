/*
 * Copyright (c) 2015 Karl Tauber <karl at jformdesigner dot com>
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

package de.mpg.biochem.mars.gui;

import static org.fxmisc.wellbehaved.event.EventPattern.keyPressed;
import static org.fxmisc.wellbehaved.event.InputMap.consume;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.input.KeyCode;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.fxmisc.wellbehaved.event.Nodes;
import de.mpg.biochem.mars.gui.editor.MarkdownEditorPane;
import de.mpg.biochem.mars.gui.options.Options;
import de.mpg.biochem.mars.gui.projects.ProjectManager;
import de.mpg.biochem.mars.gui.util.PrefsBooleanProperty;
import de.mpg.biochem.mars.gui.util.Utils;

/**
 * Tab pane for file editors.
 *
 * @author Karl Tauber
 */
class FileEditorTabPane
{
	private final MainWindow mainWindow;
	private final TabPane tabPane;
	private final ReadOnlyObjectWrapper<FileEditor> activeFileEditor = new ReadOnlyObjectWrapper<>();
	private final ReadOnlyBooleanWrapper anyFileEditorModified = new ReadOnlyBooleanWrapper();

	final PrefsBooleanProperty previewVisible = new PrefsBooleanProperty(true);
	final PrefsBooleanProperty htmlSourceVisible = new PrefsBooleanProperty();
	final PrefsBooleanProperty markdownAstVisible = new PrefsBooleanProperty();
	final PrefsBooleanProperty externalVisible = new PrefsBooleanProperty();

	private boolean saveEditorsStateEnabled = true;
	private boolean inReloadPreviewEditor;

	FileEditorTabPane(MainWindow mainWindow) {
		this.mainWindow = mainWindow;

		tabPane = new TabPane();
		tabPane.setFocusTraversable(false);
		tabPane.setTabClosingPolicy(TabClosingPolicy.ALL_TABS);

		// update activeFileEditor property
		tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
			activeFileEditor.set((newTab != null) ? (FileEditor) newTab.getUserData() : null);
			saveStateActiveEditor();
		});

		// update anyFileEditorModified property
		ChangeListener<Boolean> modifiedListener = (observable, oldValue, newValue) -> {
			boolean modified = false;
			for (Tab tab : tabPane.getTabs()) {
				if (((FileEditor)tab.getUserData()).isModified()) {
					modified = true;
					break;
				}
			}
			anyFileEditorModified.set(modified);
		};
		tabPane.getTabs().addListener((ListChangeListener<Tab>) c -> {
			while (c.next()) {
				if (c.wasAdded()) {
					for (Tab tab : c.getAddedSubList())
						((FileEditor)tab.getUserData()).modifiedProperty().addListener(modifiedListener);
				}
				if (c.wasRemoved()) {
					for (Tab tab : c.getRemoved())
						((FileEditor)tab.getUserData()).modifiedProperty().removeListener(modifiedListener);
				}
			}

			// changes in the tabs may also change anyFileEditorModified property
			// (e.g. closed modified file)
			modifiedListener.changed(null, null, null);

			// save state
			saveStateOpenEditors();
		});

		// re-open files
		restoreState();
		restoreEditorsState();

		// listen to active project
		ProjectManager.activeProjectProperty().addListener((observer, oldProject, newProject) -> {
			if (oldProject != null) {
				runWithoutSavingEditorsState(() -> {
					closeAllEditors(false);
				});
			}
			restoreEditorsState();
		});
	}

	Node getNode() {
		return tabPane;
	}

	// 'activeFileEditor' property
	FileEditor getActiveFileEditor() { return activeFileEditor.get(); }
	ReadOnlyObjectProperty<FileEditor> activeFileEditorProperty() {
		return activeFileEditor.getReadOnlyProperty();
	}

	// 'anyFileEditorModified' property
	ReadOnlyBooleanProperty anyFileEditorModifiedProperty() {
		return anyFileEditorModified.getReadOnlyProperty();
	}

	private FileEditor createFileEditor(Path path) {
		FileEditor fileEditor = new FileEditor(mainWindow, this, path);
		fileEditor.getTab().setOnCloseRequest(e -> {
			if (!canCloseEditor(fileEditor))
				e.consume();
		});
		return fileEditor;
	}

	private FileEditor createFilePreviewEditor(Path path) {
		FileEditor fileEditor = createFileEditor(path);
		setPreviewEditor(fileEditor, true);

		// turn preview editor into normal editor if it is modified
		fileEditor.modifiedProperty().addListener((observable, oldModified, newModified) -> {
			if (newModified && !inReloadPreviewEditor) {
				setPreviewEditor(fileEditor, false);
				getProjectState().remove("previewFile");
			}
		});

		return fileEditor;
	}

	FileEditor newEditor() {
		FileEditor fileEditor = createFileEditor(null);
		Tab tab = fileEditor.getTab();
		tabPane.getTabs().add(tab);
		tabPane.getSelectionModel().select(tab);
		return fileEditor;
	}

	FileEditor[] openEditor() {
		FileChooser fileChooser = createFileChooser(Messages.get("FileEditorTabPane.openChooser.title"));
		List<File> selectedFiles = fileChooser.showOpenMultipleDialog(mainWindow.getScene().getWindow());
		if (selectedFiles == null)
			return null;

		saveLastDirectory(selectedFiles.get(0));
		return openEditors(selectedFiles, 0, -1);
	}

	FileEditor[] openEditors(List<File> files, int activeIndex, int previewIndex) {
		// close single unmodified "Untitled" tab
		closeSingleUntitledEditor();

		FileEditor[] fileEditors = new FileEditor[files.size()];
		runWithoutSavingEditorsState(() -> {
			for (int i = 0; i < files.size(); i++) {
				Path path = files.get(i).toPath();

				// check whether file is already opened
				FileEditor fileEditor = findEditor(path);
				if (fileEditor == null) {
					if (i == previewIndex) {
						// check whether there is already a preview editor
						fileEditor = findPreviewEditor();
						if (fileEditor != null) {
							// replace existing preview editor
							inReloadPreviewEditor = true;
							try {
								MarkdownEditorPane editor = fileEditor.getEditor();
								if (editor != null)
									editor.selectRange(0, 0);
								fileEditor.setPath(path);
								fileEditor.load();
								fileEditor.requestFocus();
								if (editor != null)
									editor.getUndoManager().forgetHistory();
							} finally {
								inReloadPreviewEditor = false;
							}
						} else {
							// create new preview editor
							fileEditor = createFilePreviewEditor(path);
							tabPane.getTabs().add(fileEditor.getTab());
						}
					} else {
						// create new editor
						fileEditor = createFileEditor(path);
						tabPane.getTabs().add(fileEditor.getTab());
					}
				} else
					setPreviewEditor(fileEditor, false);

				// select first file
				if (i == activeIndex)
					tabPane.getSelectionModel().select(fileEditor.getTab());

				fileEditors[i] = fileEditor;
			}
		});

		saveEditorsState();

		return fileEditors;
	}

	private void closeSingleUntitledEditor() {
		if (tabPane.getTabs().size() == 1) {
			FileEditor fileEditor = (FileEditor) tabPane.getTabs().get(0).getUserData();
			if (fileEditor.getPath() == null && !fileEditor.isModified()) {
				runWithoutSavingEditorsState(() -> {
					closeEditor(fileEditor, false);
				});
			}
		}
	}

	boolean saveEditor(FileEditor fileEditor) {
		if (fileEditor == null || !fileEditor.isModified())
			return true;

		if (fileEditor.getPath() == null)
			return saveEditorAs(fileEditor);

		return fileEditor.save();
	}

	boolean saveEditorAs(FileEditor fileEditor) {
		if (fileEditor == null)
			return true;

		tabPane.getSelectionModel().select(fileEditor.getTab());

		FileChooser fileChooser = createFileChooser(Messages.get("FileEditorTabPane.saveChooser.title"));
		File file = fileChooser.showSaveDialog(mainWindow.getScene().getWindow());
		if (file == null)
			return false;

		saveLastDirectory(file);
		fileEditor.setPath(file.toPath());

		saveEditorsState();

		return fileEditor.save();
	}

	boolean saveAllEditors() {
		FileEditor[] allEditors = getAllEditors();

		boolean success = true;
		for (FileEditor fileEditor : allEditors) {
			if (!saveEditor(fileEditor))
				success = false;
		}

		return success;
	}

	boolean canCloseEditor(FileEditor fileEditor) {
		if (!fileEditor.isModified())
			return true;

		Alert alert = mainWindow.createAlert(AlertType.CONFIRMATION,
			Messages.get("FileEditorTabPane.closeAlert.title"),
			Messages.get("FileEditorTabPane.closeAlert.message"), fileEditor.getTab().getText());
		alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);

		// register first characters of Yes and No buttons as keys to close the alert
		for (ButtonType buttonType : Arrays.asList(ButtonType.YES, ButtonType.NO)) {
			Nodes.addInputMap(alert.getDialogPane(),
				consume(keyPressed(KeyCode.getKeyCode(buttonType.getText().substring(0, 1).toUpperCase())), e -> {
					if (!e.isConsumed()) {
						alert.setResult(buttonType);
						alert.close();
					}
				}));
		}

		ButtonType result = alert.showAndWait().get();
		if (result != ButtonType.YES)
			return (result == ButtonType.NO);

		return saveEditor(fileEditor);
	}

	boolean canCloseAllEditos() {
		FileEditor[] allEditors = getAllEditors();
		FileEditor activeEditor = activeFileEditor.get();

		// try to save active tab first because in case the user decides to cancel,
		// then it stays active
		if (activeEditor != null && !canCloseEditor(activeEditor))
			return false;

		// save modified tabs
		for (int i = 0; i < allEditors.length; i++) {
			FileEditor fileEditor = allEditors[i];
			if (fileEditor == activeEditor)
				continue;

			if (fileEditor.isModified()) {
				// activate the modified tab to make its modified content visible to the user
				tabPane.getSelectionModel().select(i);

				if (!canCloseEditor(fileEditor))
					return false;
			}
		}

		return true;
	}

	boolean closeEditor(FileEditor fileEditor, boolean save) {
		if (fileEditor == null)
			return true;

		Tab tab = fileEditor.getTab();

		if (save) {
			Event event = new Event(tab,tab,Tab.TAB_CLOSE_REQUEST_EVENT);
			Event.fireEvent(tab, event);
			if (event.isConsumed())
				return false;
		}

		runWithoutSavingEditorsState(() -> {
			tabPane.getTabs().remove(tab);
			if (tab.getOnClosed() != null)
				Event.fireEvent(tab, new Event(Tab.CLOSED_EVENT));
		});

		saveEditorsState();

		return true;
	}

	boolean closeAllEditors(boolean save) {
		if (save && !canCloseAllEditos())
			return false;

		runWithoutSavingEditorsState(() -> {
			// close all tabs
			for (FileEditor fileEditor : getAllEditors())
				closeEditor(fileEditor, false);
		});

		saveEditorsState();

		return tabPane.getTabs().isEmpty();
	}

	private FileEditor[] getAllEditors() {
		ObservableList<Tab> tabs = tabPane.getTabs();
		FileEditor[] allEditors = new FileEditor[tabs.size()];
		for (int i = 0; i < tabs.size(); i++)
			allEditors[i] = (FileEditor) tabs.get(i).getUserData();
		return allEditors;
	}

	private FileEditor findEditor(Path path) {
		for (Tab tab : tabPane.getTabs()) {
			FileEditor fileEditor = (FileEditor) tab.getUserData();
			if (path.equals(fileEditor.getPath()))
				return fileEditor;
		}
		return null;
	}

	private FileEditor findPreviewEditor() {
		for (Tab tab : tabPane.getTabs()) {
			if (isPreviewEditor((FileEditor) tab.getUserData()))
				return (FileEditor) tab.getUserData();
		}
		return null;
	}

	private boolean isPreviewEditor(FileEditor fileEditor) {
		return fileEditor.getTab().getStyleClass().contains("preview");
	}

	private void setPreviewEditor(FileEditor fileEditor, boolean preview) {
		ObservableList<String> styleClasses = fileEditor.getTab().getStyleClass();
		if (preview) {
			if (!styleClasses.contains("preview"))
				styleClasses.add("preview");
		} else
			styleClasses.remove("preview");
	}

	private FileChooser createFileChooser(String title) {
		String[] extensions = Options.getMarkdownFileExtensions().trim().split("\\s*,\\s*");

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(title);
		fileChooser.getExtensionFilters().addAll(
			new ExtensionFilter(Messages.get("FileEditorTabPane.chooser.markdownFilesFilter"), extensions),
			new ExtensionFilter(Messages.get("FileEditorTabPane.chooser.allFilesFilter"), "*.*"));

		String lastDirectory = getProjectState().get("lastDirectory", null);
		File initialDirectory = (lastDirectory != null) ? new File(lastDirectory) : ProjectManager.getActiveProject();
		if (initialDirectory == null || !initialDirectory.isDirectory())
			initialDirectory = new File(".");
		fileChooser.setInitialDirectory(initialDirectory);
		return fileChooser;
	}

	private void saveLastDirectory(File file) {
		getProjectState().put("lastDirectory", file.getParent());
	}

	private void restoreState() {
		Preferences state = MarkdownWriterFXApp.getState();

		previewVisible.init(state, "previewVisible", true);
		htmlSourceVisible.init(state, "htmlSourceVisible", false);
		markdownAstVisible.init(state, "markdownAstVisible", false);
		externalVisible.init(state, "externalVisible", false);
	}

	private void restoreEditorsState() {
		Preferences projectState = getProjectState();

		String[] fileNames = Utils.getPrefsStrings(projectState, "file");
		String previewFileName = projectState.get("previewFile", null);
		String activeFileName = projectState.get("activeFile", null);

		ArrayList<File> files = new ArrayList<>(fileNames.length);
		for (String fileName : fileNames) {
			File file = new File(fileName);
			if (file.exists())
				files.add(file);
		}

		// save opened editors state if there are already open editors,
		// which may happen when no project is open but files are open,
		// and then opening a project, which keeps already open files
		boolean saveState = !tabPane.getTabs().isEmpty();

		if (files.isEmpty()) {
			if (ProjectManager.getActiveProject() == null)
				newEditor();
			if (saveState)
				saveEditorsState();
			return;
		}

		int activeIndex = (activeFileName != null) ? Math.max(files.indexOf(new File(activeFileName)), 0) : 0;
		int previewIndex = (previewFileName != null) ? files.indexOf(new File(previewFileName)) : -1;

		// temporary disable tab animation when restoring open editors
		tabPane.setStyle("-fx-open-tab-animation: none; -fx-close-tab-animation: none;");
		tabPane.applyCss();

		// open editors
		runWithoutSavingEditorsState(() -> {
			openEditors(files, activeIndex, previewIndex);
		});

		tabPane.setStyle("");

		if (saveState)
			saveEditorsState();
	}

	private void runWithoutSavingEditorsState(Runnable runnable) {
		boolean oldSaveEditorsStateEnabled = saveEditorsStateEnabled;
		saveEditorsStateEnabled = false;
		try {
			runnable.run();
		} finally {
			saveEditorsStateEnabled = oldSaveEditorsStateEnabled;
		}
	}

	private void saveEditorsState() {
		saveStateOpenEditors();
		saveStateActiveEditor();
	}

	private void saveStateOpenEditors() {
		if (!saveEditorsStateEnabled)
			return;

		FileEditor[] allEditors = getAllEditors();

		ArrayList<String> fileNames = new ArrayList<>(allEditors.length);
		String previewFileName = null;
		for (FileEditor fileEditor : allEditors) {
			if (fileEditor.getPath() != null) {
				fileNames.add(fileEditor.getPath().toString());
				if (isPreviewEditor(fileEditor))
					previewFileName = fileEditor.getPath().toString();
			}
		}

		Preferences projectState = getProjectState();
		Utils.putPrefsStrings(projectState, "file", fileNames.toArray(new String[fileNames.size()]));
		Utils.putPrefs(projectState, "previewFile", previewFileName, null);
	}

	private void saveStateActiveEditor() {
		if (!saveEditorsStateEnabled)
			return;

		FileEditor activeEditor = activeFileEditor.get();
		if (activeEditor != null && activeEditor.getPath() != null)
			getProjectState().put("activeFile", activeEditor.getPath().toString());
		else
			getProjectState().remove("activeFile");
	}

	private Preferences getProjectState() {
		Preferences projectState = ProjectManager.getActiveProjectState();
		if (projectState == null)
			projectState = MarkdownWriterFXApp.getState();
		return projectState;
	}
}
