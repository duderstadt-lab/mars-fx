/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2026 Karl Duderstadt
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
package de.mpg.biochem.mars.fx.dialogs.s3.explorer;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import de.mpg.biochem.mars.io.MoleculeArchiveIOFactory;
import de.mpg.biochem.mars.io.MoleculeArchiveSource;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;
import org.scijava.Context;
import org.scijava.module.ModuleService;
import org.scijava.script.ScriptInfo;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;

import de.mpg.biochem.mars.fx.editor.MarsScriptEditor;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIOPlugin;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Scripting tab for the Dataset Explorer.
 *
 * <p>Provides a Groovy script editor over a log console (adjustable split). When
 * "Run" is pressed, the currently selected archive is opened <b>headless</b>
 * (loaded as a variable via {@link MoleculeArchiveIOPlugin#open}, with no
 * archive window shown), injected into the script as {@code archive} along with
 * {@code scijavaContext}, and the script runs. Script output/errors stream to the
 * log, each run prefixed with a timestamp. The archive reference is released when
 * the run finishes. If a script needs to persist changes back to cloud storage,
 * it can call {@code archive.save()} itself.
 *
 * <p>Modeled on the Mars dashboard scriptable widgets (AbstractScriptableWidget):
 * same {@link ScriptInfo}/{@link ScriptModule} execution, same output-console
 * plumbing over an {@link InlineCssTextArea}.
 *
 * <p>Only Groovy is supported. The script has this header prepended so the two
 * inputs are available:
 * <pre>#@ MoleculeArchive archive
 * #@ Context scijavaContext</pre>
 */
public class DatasetScriptPane extends BorderPane {

    private static final String DEFAULT_SCRIPT =
            "#@ MoleculeArchive archive\n" +
                    "#@ Context scijavaContext\n" +
                    "\n" +
                    "// 'archive' is the selected Molecule Archive (opened headless).\n" +
                    "// 'scijavaContext' is the SciJava context.\n" +
                    "// Example: print some basic info.\n" +
                    "println \"Name: \" + archive.getName()\n" +
                    "println \"Molecules: \" + archive.getNumberOfMolecules()\n" +
                    "println \"Metadata: \" + archive.getNumberOfMetadatas()\n";

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final java.util.prefs.Preferences PREFS =
            java.util.prefs.Preferences.userNodeForPackage(DatasetScriptPane.class);
    private static final String PREF_SCRIPT_DIVIDER = "script.dividerPosition";

    // Injected by context.inject(this) in the constructor. @Parameter fields can't
    // be final (injection assigns them after the object is constructed).
    @org.scijava.plugin.Parameter
    private Context context;
    @org.scijava.plugin.Parameter
    private ScriptService scriptService;
    @org.scijava.plugin.Parameter
    private ModuleService moduleService;
    @org.scijava.plugin.Parameter
    private de.mpg.biochem.mars.molecule.MoleculeArchiveService moleculeArchiveService;

    private final MarsScriptEditor codeArea;
    private final InlineCssTextArea logArea;
    private final Button runButton;
    private final Button stopButton;
    private final CheckBox allCheckBox;

    // Supplies the archive URL to open+run against (the currently selected card).
    private java.util.function.Supplier<String> archiveUrlSupplier = () -> null;
    // Supplies ALL archive URLs currently shown by the search filter (for "All").
    private java.util.function.Supplier<java.util.List<String>> allArchiveUrlsSupplier =
            java.util.Collections::emptyList;

    private volatile Thread runThread;
    private volatile boolean stopRequested = false;

    public DatasetScriptPane(Context context) {
        // Populate all @Parameter service fields (context, scriptService,
        // moduleService, moleculeArchiveService) in one call.
        context.inject(this);

        // --- Script editor (top) ---
        codeArea = new MarsScriptEditor();
        codeArea.replaceText(DEFAULT_SCRIPT);
        BorderPane editorBox = new BorderPane();
        editorBox.setCenter(new VirtualizedScrollPane<>(codeArea));

        // --- Log console (bottom) ---
        logArea = new InlineCssTextArea("");
        logArea.getStyleClass().add("log-area");
        logArea.getStyleClass().add("markdown-editor");
        logArea.setEditable(false);
        BorderPane logBox = new BorderPane();
        logBox.setCenter(new VirtualizedScrollPane<>(logArea));

        // --- Adjustable split between editor and log ---
        // Start centered (or at the last-saved position) so the editor has real
        // room. SplitPane can reset an early setDividerPositions during initial
        // layout, so we also re-apply once after the scene lays out.
        double savedDiv = PREFS.getDouble(PREF_SCRIPT_DIVIDER, 0.5);
        SplitPane split = new SplitPane(editorBox, logBox);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(savedDiv);
        setCenter(split);
        Platform.runLater(() -> split.setDividerPositions(savedDiv));
        // Persist whenever the user drags the divider.
        if (!split.getDividers().isEmpty()) {
            split.getDividers().get(0).positionProperty().addListener((o, a, b) ->
                    PREFS.putDouble(PREF_SCRIPT_DIVIDER, b.doubleValue()));
        }

        // --- Toolbar: Run / Stop / Save-on-exit ---
        runButton = new Button("Run");
        runButton.setOnAction(e -> runScriptAsync());
        stopButton = new Button("Stop");
        stopButton.setDisable(true);
        stopButton.setOnAction(e -> requestStop());
        allCheckBox = new CheckBox("All");
        allCheckBox.setTooltip(new javafx.scene.control.Tooltip(
                "Run on every dataset currently shown by the search filter"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label lang = new Label("Groovy");
        lang.setStyle("-fx-opacity: 0.6;");
        HBox toolbar = new HBox(6, runButton, stopButton, allCheckBox, spacer, lang);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 8, 6, 8));
        setTop(toolbar);
    }

    /** Set the supplier that yields the archive URL to run against (selected card). */
    public void setArchiveUrlSupplier(java.util.function.Supplier<String> supplier) {
        this.archiveUrlSupplier = supplier == null ? () -> null : supplier;
    }

    /** Set the supplier that yields all filtered archive URLs (for the "All" mode). */
    public void setAllArchiveUrlsSupplier(
            java.util.function.Supplier<java.util.List<String>> supplier)
    {
        this.allArchiveUrlsSupplier = supplier == null
                ? java.util.Collections::emptyList : supplier;
    }

    // ---- execution -----------------------------------------------------

    private void runScriptAsync() {
        if (runThread != null) return; // a run is already in progress

        // Build the list of archive URLs to run against: either every dataset the
        // filter currently shows ("All"), or just the selected one.
        final java.util.List<String> urls = new java.util.ArrayList<>();
        if (allCheckBox.isSelected()) {
            java.util.List<String> all = allArchiveUrlsSupplier.get();
            if (all != null) urls.addAll(all);
            if (urls.isEmpty()) {
                appendLog("\n" + LocalDateTime.now().format(TS)
                        + " — No archives in the current filter to run on.\n");
                return;
            }
        } else {
            String url = archiveUrlSupplier.get();
            if (url == null || url.isEmpty()) {
                appendLog("\n" + LocalDateTime.now().format(TS)
                        + " — No archive selected. Select a dataset card first.\n");
                return;
            }
            urls.add(url);
        }

        stopRequested = false;
        runButton.setDisable(true);
        stopButton.setDisable(false);

        final String scriptText = codeArea.getText();
        final boolean runAll = urls.size() > 1;

        runThread = new Thread(() -> {
            try {
                appendLog("\n" + LocalDateTime.now().format(TS) + " — Running script"
                        + (runAll ? " on " + urls.size() + " archives…" : "…") + "\n");

                int i = 0;
                for (String url : urls) {
                    if (stopRequested) { appendLog("Stopped.\n"); break; }
                    i++;
                    if (runAll) appendLog("\n[" + i + "/" + urls.size() + "] " + url + "\n");

                    MoleculeArchive<?, ?, ?, ?> archive = openHeadless(url);
                    if (archive == null) {
                        appendLog("Could not open archive: " + url + "\n");
                        continue;
                    }
                    try {
                        runScript(scriptText, archive);
                    } finally {
                        // No window/service holds the archive, so dropping the
                        // reference lets it be garbage-collected.
                        archive = null;
                    }
                }

                appendLog(LocalDateTime.now().format(TS) + " — Done.\n");
            } catch (Exception ex) {
                appendLog("Error: " + ex.getMessage() + "\n");
            } finally {
                runThread = null;
                Platform.runLater(() -> {
                    runButton.setDisable(false);
                    stopButton.setDisable(true);
                });
            }
        }, "DatasetScriptRun");
        runThread.setDaemon(true);
        runThread.start();
    }

    /**
     * Open an archive HEADLESS — as a variable only, with no window shown and
     * without registering it in the ObjectService. This deliberately bypasses
     * MoleculeArchiveIOPlugin.open(), which under SciJava-IO both registers the
     * archive and calls uiService.show(). We only do the minimal load steps.
     */
    private MoleculeArchive<?, ?, ?, ?> openHeadless(String url) throws IOException {
        MoleculeArchiveSource virtualSource =
                new MoleculeArchiveIOFactory().openSource(url);
        String archiveType = virtualSource.getArchiveType();
        return moleculeArchiveService.createArchive(archiveType, virtualSource);
    }

    /**
     * Execute the Groovy script against the archive, streaming output/errors to
     * the log. Mirrors AbstractScriptableWidget.runScript's module plumbing.
     */
    private void runScript(String scriptText, MoleculeArchive<?, ?, ?, ?> archive) {
        ScriptLanguage groovy = scriptService.getLanguageByName("Groovy");
        Reader reader = new StringReader(scriptText);
        ScriptInfo scriptInfo = new ScriptInfo(context, "script.groovy", reader);
        scriptInfo.setLanguage(groovy);

        ScriptModule module;
        try {
            module = scriptInfo.createModule();
            context.inject(module);
        } catch (Exception e) {
            appendLog("Failed to create script module: " + e.getMessage() + "\n");
            return;
        }

        OutputConsole out = new OutputConsole(false);
        PrintStream outPS = new PrintStream(out, true);
        OutputConsole err = new OutputConsole(true);
        PrintStream errPS = new PrintStream(err, true);
        try {
            module.setOutputWriter(new OutputStreamWriter(outPS, "UTF-8"));
            module.setErrorWriter(new OutputStreamWriter(errPS, "UTF-8"));
        } catch (Exception e) {
            outPS.close();
            errPS.close();
            appendLog("Failed to set script writers: " + e.getMessage() + "\n");
            return;
        }

        // Inject the two script inputs, matching the #@ header.
        // Inject the two script inputs, matching the #@ header. Because we call
        // moduleService.run(module, false) below (processing/harvesting OFF), no
        // pre-processor will prompt for these, so setInput alone is sufficient —
        // no resolveInput() needed.
        module.setInput("scijavaContext", context);
        module.setInput("archive", archive);

        try {
            moduleService.run(module, false).get();
        } catch (Exception e) {
            appendLog("Script error: " + e.getMessage() + "\n");
        } finally {
            outPS.close();
            errPS.close();
        }
    }

    private void requestStop() {
        stopRequested = true;
        Thread t = runThread;
        if (t != null) {
            appendLog(LocalDateTime.now().format(TS) + " — Stop requested…\n");
            // Interrupt the run thread. Note: SciJava/Groovy execution may not
            // respond immediately to interruption; this is best-effort.
            t.interrupt();
        }
    }

    // ---- log helpers ---------------------------------------------------

    /** Append text to the log on the FX thread. The log is session-only (not persisted). */
    private void appendLog(String text) {
        Platform.runLater(() -> logArea.appendText(text));
    }

    /** Clear the log. */
    public void clearLog() {
        Platform.runLater(() -> logArea.replaceText(""));
    }

    /** Called by the window on close to interrupt any running script. */
    public void cleanup() {
        stopRequested = true;
        Thread t = runThread;
        if (t != null) t.interrupt();
        try { codeArea.cleanup(); } catch (Exception ignore) {}
    }

    // An OutputStream that appends written bytes to the log area (one console for
    // stdout, one for stderr — both just append text here).
    private final class OutputConsole extends OutputStream {
        private final boolean isError;
        OutputConsole(boolean isError) { this.isError = isError; }
        @Override
        public void write(int b) {
            final String s = String.valueOf((char) b);
            Platform.runLater(() -> logArea.appendText(s));
        }
    }
}