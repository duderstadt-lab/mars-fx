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
package de.mpg.biochem.mars.fx.dialogs.n5.command;

import java.io.IOException;

import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import de.mpg.biochem.mars.fx.molecule.metadataTab.n5browser.N5LocalBrowserDialog;
import de.mpg.biochem.mars.n5.MarsN5ImagePlusOpener;

import ij.IJ;
import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import net.imagej.Dataset;

/**
 * Opens a dataset from an N5 container on disk as an ImagePlus — the local
 * counterpart to {@link OpenN5asImagePlusCommand}, showing exactly the dialog
 * the BDV source panel's Browse button shows in Local mode.
 *
 * <p>Kept alongside the MinIO command because the Swing dialog these replaced
 * could reach local containers as well as remote ones.
 *
 * @author Karl Duderstadt
 */
@Plugin(type = Command.class, label = "Open N5 as ImagePlus (local)", menu = { @Menu(
        label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT,
        mnemonic = MenuConstants.PLUGINS_MNEMONIC), @Menu(label = "Mars",
        weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = 'm'), @Menu(
        label = "Import", weight = 1, mnemonic = 'i'), @Menu(
        label = "Open N5 as ImagePlus (local)", weight = 21, mnemonic = 'l') })
public class OpenLocalN5asImagePlusCommand extends DynamicCommand {

    @Parameter
    private UIService uiService;

    @Parameter
    private LogService logService;

    @Override
    public void run() {
        Platform.setImplicitExit(false);
        new javafx.embed.swing.JFXPanel();

        Platform.runLater(() -> {
            final N5LocalBrowserDialog dialog = new N5LocalBrowserDialog(null,
                    null);
            dialog.setTitle("Open N5 as ImagePlus (local)");

            final CheckBox virtual = new CheckBox("Virtual");
            virtual.setSelected(true);
            virtual.setTooltip(new Tooltip(
                    "Leave the image backed by lazily-fetched N5 chunks. Uncheck to "
                            + "copy the whole volume into memory first."));
            dialog.addOption(virtual);

            dialog.showAndWait().ifPresent(result -> {
                final boolean asVirtual = virtual.isSelected();
                new Thread(() -> open(result.n5Path, result.dataset, asVirtual),
                        "OpenLocalN5asImagePlus").start();
            });
        });
    }

    private void open(final String n5Root, final String dataset,
            final boolean virtual)
    {
        try {
            final Dataset image = MarsN5ImagePlusOpener.open(n5Root, dataset,
                    virtual, getContext());
            uiService.show(image);
        }
        catch (final IOException | RuntimeException e) {
            logService.error(e);
            IJ.error("Could not open " + dataset + " from " + n5Root + ": " + e
                    .getMessage());
        }
    }
}
