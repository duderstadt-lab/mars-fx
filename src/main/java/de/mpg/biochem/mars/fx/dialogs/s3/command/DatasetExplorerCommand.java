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

import java.io.IOException;

import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.menu.MenuConstants;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import de.mpg.biochem.mars.fx.dialogs.s3.explorer.DatasetExplorerWindow;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIOPlugin;

import javafx.application.Platform;

/**
 * Fiji command that opens the {@link DatasetExplorerWindow}.
 *
 * <p>Mirrors {@code OpenCloudArchiveCommand}: keeps the FX runtime alive
 * ({@code setImplicitExit(false)}), boots the toolkit via a {@code JFXPanel},
 * and builds the window on the FX thread. The window is modeless, so
 * {@code run()} returns immediately while the explorer stays open.
 *
 * <p>Double-clicking an archive card invokes the open handler wired here, which
 * loads the archive from its cloud URL on a background thread using the exact
 * same path as {@code OpenCloudArchiveCommand.openArchive}.
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

        // Ensure the JavaFX toolkit is initialized. Constructing a JFXPanel boots
        // the FX runtime synchronously if it hasn't started yet. Mirrors
        // OpenCloudArchiveCommand.
        new javafx.embed.swing.JFXPanel();

        // Open handler: invoked with the archive URL when a card is double-clicked.
        // Loads on a background thread so the FX UI stays responsive.
        Platform.runLater(() -> DatasetExplorerWindow.show(url -> {
            if (url != null)
                new Thread(() -> openArchive(url), "OpenDatasetArchive").start();
        }, getContext()));
    }

    /**
     * Load a MoleculeArchive from a cloud URL — identical to
     * OpenCloudArchiveCommand.openArchive.
     */
    private void openArchive(final String url) {
        if (url == null || url.isEmpty()) return;

        final MoleculeArchiveIOPlugin ioPlugin = new MoleculeArchiveIOPlugin();
        ioPlugin.setContext(getContext());

        try {
            final MoleculeArchive<?, ?, ?, ?> archive = ioPlugin.open(url);

            final boolean newStyleIO = optionsService.getOptions(
                    net.imagej.legacy.ImageJ2Options.class).isSciJavaIO();
            if (!newStyleIO && archive != null) uiService.show(archive);
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
    }
}
