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
package de.mpg.biochem.mars.fx.dialogs.s3.command;

import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.menu.MenuConstants;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import de.mpg.biochem.mars.fx.dialogs.s3.explorer.DatasetExplorerWindow;
import javafx.application.Platform;

/**
 * Fiji command that opens the {@link DatasetExplorerWindow}.
 *
 * <p>Mirrors {@code OpenCloudArchiveCommand}: it keeps the FX runtime alive
 * ({@code setImplicitExit(false)}), ensures the toolkit is booted via a
 * {@code JFXPanel}, and then builds the window on the FX thread with
 * {@code Platform.runLater}. The window is modeless, so {@code run()} returns
 * immediately while the explorer stays open in the background.
 */
@Plugin(type = Command.class, label = "Dataset Explorer", menu = {
    @Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT,
        mnemonic = MenuConstants.PLUGINS_MNEMONIC),
    @Menu(label = "Mars", weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = 'm'),
    @Menu(label = "Molecule", weight = 2, mnemonic = 'm'),
    @Menu(label = "Dataset Explorer", weight = 4, mnemonic = 'd') })
public class DatasetExplorerCommand extends DynamicCommand {

    @Parameter
    private UIService uiService;

    @Parameter
    private OptionsService optionsService;

    @Override
    public void run() {
        // Keep the JVM/FX runtime alive across window open/close cycles.
        Platform.setImplicitExit(false);

        // Ensure the JavaFX toolkit is initialized (boots FX synchronously if
        // this is the first JavaFX thing this session). Mirrors the archive
        // frame's init and OpenCloudArchiveCommand.
        new javafx.embed.swing.JFXPanel();

        Platform.runLater(DatasetExplorerWindow::show);
    }
}
