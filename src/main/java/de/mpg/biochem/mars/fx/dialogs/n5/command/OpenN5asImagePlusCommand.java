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

import de.mpg.biochem.mars.fx.molecule.metadataTab.n5browser.N5MinioBrowserDialog;
import de.mpg.biochem.mars.n5.MarsN5ImagePlusOpener;

import ij.IJ;
import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import net.imagej.Dataset;

/**
 * Opens a dataset from an N5 container on an S3/MinIO server as an ImagePlus.
 *
 * <p>Shows exactly the dialog the BDV source panel's Browse button shows in S3
 * mode — {@link N5MinioBrowserDialog}, unmodified: buckets on the left, the
 * folder tree down to the .n5 on the right, and the datasets inside it below.
 * This replaces the legacy Janelia Swing {@code DatasetSelectorDialog} that this
 * menu entry used to show.
 *
 * <p>The open itself is {@link MarsN5ImagePlusOpener}, shared with the Dataset
 * Explorer's double-click, so the Micromanager {@code metadata.txt} sidecar is
 * picked up here too when one is present, and images open virtually — backed by
 * lazily-fetched N5 chunks rather than downloaded up front.
 *
 * @author Karl Duderstadt
 */
@Plugin(type = Command.class, label = "Open N5 as ImagePlus (S3)", menu = { @Menu(
        label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT,
        mnemonic = MenuConstants.PLUGINS_MNEMONIC), @Menu(label = "Mars",
        weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = 'm'), @Menu(
        label = "Import", weight = 1, mnemonic = 'i'), @Menu(
        label = "Open N5 as ImagePlus (S3)", weight = 20, mnemonic = 's') })
public class OpenN5asImagePlusCommand extends DynamicCommand {

    @Parameter
    private UIService uiService;

    @Parameter
    private LogService logService;

    @Override
    public void run() {
        // Keep the JVM/FX runtime alive across dialog open/close cycles, and boot
        // the toolkit if this is the first JavaFX thing in the session. Mirrors
        // OpenCloudArchiveCommand.
        Platform.setImplicitExit(false);
        new javafx.embed.swing.JFXPanel();

        Platform.runLater(() -> {
            final N5MinioBrowserDialog dialog = new N5MinioBrowserDialog(null,
                    null, null);
            dialog.setTitle("Open N5 as ImagePlus (S3)");

            final CheckBox virtual = new CheckBox("Virtual");
            virtual.setSelected(true);
            virtual.setTooltip(new Tooltip(
                    "Leave the image backed by lazily-fetched N5 chunks, so a large "
                            + "volume shows immediately. Uncheck to copy it into memory "
                            + "first — snappier to scrub, but nothing shows until the "
                            + "download finishes."));
            dialog.addOption(virtual);

            dialog.showAndWait().ifPresent(result -> {
                final boolean asVirtual = virtual.isSelected();
                new Thread(() -> open(result.getFullPath(), result.dataset,
                        asVirtual), "OpenN5asImagePlus").start();
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
