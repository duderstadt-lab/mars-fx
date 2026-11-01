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
package de.mpg.biochem.mars.fx.molecule.metadataTab;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Named 2D affine transforms for BDV sources (e.g. one per microscope and
 * channel), kept in the user's local preferences so they survive Fiji
 * restarts. Each preset is the six coefficients the source options pane
 * edits — m00, m01, m02 (x translation), m10, m11, m12 (y translation) —
 * stored as a comma-separated string under the preset's name.
 */
public final class BdvTransformPresets {

    /** Name shown for the identity transform; never stored. */
    public static final String NONE = "None";

    /** Longest name the preferences store accepts as a key. */
    public static final int MAX_NAME_LENGTH = Preferences.MAX_KEY_LENGTH;

    private static final double TOLERANCE = 1e-9;

    private static final double[] IDENTITY = { 1, 0, 0, 0, 1, 0 };

    private BdvTransformPresets() {}

    private static Preferences node() {
        return Preferences.userNodeForPackage(BdvTransformPresets.class).node(
                "bdvTransformPresets");
    }

    /** All saved presets by name, sorted case-insensitively. */
    public static Map<String, double[]> load() {
        Map<String, double[]> presets = new LinkedHashMap<>();
        try {
            List<String> names = new ArrayList<>(List.of(node().keys()));
            names.sort(String.CASE_INSENSITIVE_ORDER);
            for (String name : names) {
                double[] values = parse(node().get(name, null));
                if (values != null) presets.put(name, values);
            }
        }
        catch (BackingStoreException e) {
            // No presets readable — the dropdown just offers None.
        }
        return presets;
    }

    /** Save (or overwrite) a preset. */
    public static void save(String name, double[] values) throws BackingStoreException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (i > 0) sb.append(',');
            sb.append(values[i]);
        }
        node().put(name, sb.toString());
        node().flush();
    }

    public static void remove(String name) throws BackingStoreException {
        node().remove(name);
        node().flush();
    }

    public static double[] identity() {
        return IDENTITY.clone();
    }

    /** True if the two coefficient sets are equal within floating-point noise. */
    public static boolean matches(double[] a, double[] b) {
        if (a == null || b == null) return false;
        for (int i = 0; i < 6; i++)
            if (Math.abs(a[i] - b[i]) > TOLERANCE) return false;
        return true;
    }

    private static double[] parse(String s) {
        if (s == null) return null;
        String[] parts = s.split(",");
        if (parts.length != 6) return null;
        double[] values = new double[6];
        try {
            for (int i = 0; i < 6; i++) values[i] = Double.parseDouble(parts[i].trim());
        }
        catch (NumberFormatException e) {
            return null;
        }
        return values;
    }
}
