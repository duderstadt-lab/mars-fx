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

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.FillRule;
import javafx.scene.SnapshotParameters;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * JavaFX identicon generator, ported from Jdenticon's shape design
 * (https://github.com/dmester/jdenticon, MIT licensed, Copyright (c)
 * Daniel Mester Pirttijärvi). Reimplemented from scratch for JavaFX:
 * no dependency on the Jdenticon library itself, just its published
 * shape geometry and color-theming approach, translated into
 * GraphicsContext / SVG path calls.
 *
 * Grid: 4x4 cells. Three shape categories, each with true 4-fold
 * rotational symmetry:
 *   - CORNER shapes: 4 outer corner cells
 *   - SIDE shapes:   8 edge cells (corner and side share one shape library)
 *   - CENTER shapes: 4 inner cells (own shape library, no extra hash rotation)
 *
 * Colors: a 5-color palette (dark gray, mid color, light gray, light
 * color, dark color) is derived from one hue per seed. Each category
 * independently picks one of the 5 palette colors via its own hash byte,
 * matching Jdenticon's per-category color-index design.
 */
public final class DatasetIdenticon {

    private DatasetIdenticon() {}

    // ---- Theme configuration -------------------------------------------

    /** Lightness/saturation ranges used to turn a hue into the 5-color palette. */
    public static final class Theme {
        final double[] colorLightness;      // [min, max]
        final double[] grayscaleLightness;   // [min, max]
        final double colorSaturation;
        final double grayscaleSaturation;

        public Theme(double[] colorLightness, double[] grayscaleLightness,
                     double colorSaturation, double grayscaleSaturation) {
            this.colorLightness = colorLightness;
            this.grayscaleLightness = grayscaleLightness;
            this.colorSaturation = colorSaturation;
            this.grayscaleSaturation = grayscaleSaturation;
        }
    }

    /** Same defaults Jdenticon ships with — tuned for a light/white card background. */
    public static final Theme LIGHT = new Theme(
            new double[]{0.40, 0.80}, new double[]{0.30, 0.90}, 0.50, 0.00);

    /** Same hue/saturation derivation, lightness pushed up for contrast on a dark card. */
    public static final Theme DARK = new Theme(
            new double[]{0.55, 0.88}, new double[]{0.45, 0.85}, 0.55, 0.00);

    // ---- Public API -------------------------------------------------------

    /** Generate an identicon Image for the given seed, size, and theme. */
    public static Image generate(String seed, double size, Theme theme) {
        // Jdenticon hashes with SHA-1 and reads hex octets (nibbles). We match
        // that exactly: hex string, getOctet = hex digit at index (0..15).
        String hash = sha1Hex(seed);

        // Hue from the LAST 7 hex chars (Jdenticon: hexdec(substr(hash,-7))/0xfffffff).
        long hueVal = Long.parseLong(hash.substring(hash.length() - 7), 16);
        double hueFraction = hueVal / (double) 0xfffffffL;
        double hue = hueFraction * 360.0;

        Color[] palette = buildPalette(hue, theme);

        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, size, size);

        // Normalize so size is a multiple of 4 (cellCount), like normalizeRectangle.
        double cellCount = 4.0;
        double isize = Math.floor(size);
        isize -= isize % cellCount;
        double offset = (size - isize) / 2.0;
        double cell = isize / cellCount;

        // Three categories, each: colorIndex octet, shapeIndex octet, rotationIndex
        // octet (null for center), shape library, and positions. Straight from
        // Jdenticon's IconGenerator default shapes.
        int[] usedColors = new int[3];
        int usedCount = 0;

        // --- Sides ---
        usedCount = renderCategory(gc, hash, palette, offset, cell,
                /*colorIndex*/ 8, OUTER_SHAPES, /*shapeIndex*/ 2, /*rotationIndex*/ 3,
                new int[]{1,0, 2,0, 2,3, 1,3, 0,1, 3,1, 3,2, 0,2},
                usedColors, usedCount);

        // --- Corners ---
        usedCount = renderCategory(gc, hash, palette, offset, cell,
                /*colorIndex*/ 9, OUTER_SHAPES, /*shapeIndex*/ 4, /*rotationIndex*/ 5,
                new int[]{0,0, 3,0, 3,3, 0,3},
                usedColors, usedCount);

        // --- Center (rotationIndex null => start rotation 0) ---
        usedCount = renderCategory(gc, hash, palette, offset, cell,
                /*colorIndex*/ 10, CENTER_SHAPES, /*shapeIndex*/ 1, /*rotationIndex*/ -1,
                new int[]{1,1, 2,1, 2,2, 1,2},
                usedColors, usedCount);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return canvas.snapshot(params, null);
    }

    /**
     * Renders one shape category exactly as Jdenticon's renderForeground does:
     * pick a color (with the duplicate-avoidance rule), pick a shape, then for
     * each position draw the shape into its cell with rotation incrementing per
     * position starting from the hash-derived start rotation.
     */
    private static int renderCategory(GraphicsContext gc, String hash, Color[] palette,
                                      double offset, double cell,
                                      int colorIndex, List<TriConsumer> shapes,
                                      int shapeIndex, int rotationIndex, int[] positions,
                                      int[] usedColors, int usedCount) {
        int colorCount = palette.length; // 5

        int colorThemeIndex = getOctet(hash, colorIndex) % colorCount;
        // Duplicate-avoidance: disallow {dark gray(0), dark color(4)} and
        // {light gray(2), light color(3)} combos — same as Jdenticon.
        if (isDuplicate(usedColors, usedCount, colorThemeIndex, new int[]{0, 4}) ||
                isDuplicate(usedColors, usedCount, colorThemeIndex, new int[]{2, 3})) {
            colorThemeIndex = 1;
        }
        usedColors[usedCount++] = colorThemeIndex;
        Color color = palette[colorThemeIndex];

        int startRotation = (rotationIndex < 0) ? 0 : getOctet(hash, rotationIndex);
        int shapeIdx = getOctet(hash, shapeIndex) % shapes.size();
        TriConsumer shape = shapes.get(shapeIdx);

        gc.setFill(color);

        int rotation = startRotation;
        for (int i = 0; i + 1 < positions.length; i += 2) {
            int cellX = positions[i];
            int cellY = positions[i + 1];
            Transform t = new Transform(
                    offset + cellX * cell, offset + cellY * cell, cell, rotation % 4);
            rotation++;

            ShapeSink sink = new ShapeSink(t);
            sink.positionIndex = i / 2;
            shape.draw(sink, cell, i / 2);

            gc.beginPath();
            gc.appendSVGPath(sink.build());
            gc.setFillRule(FillRule.NON_ZERO);
            gc.fill();
        }
        return usedCount;
    }

    private static boolean isDuplicate(int[] used, int usedCount, int value, int[] group) {
        boolean valueInGroup = false;
        for (int g : group) if (g == value) { valueInGroup = true; break; }
        if (!valueInGroup) return false;
        for (int g : group)
            for (int i = 0; i < usedCount; i++)
                if (used[i] == g) return true;
        return false;
    }

    /** Convenience overload defaulting to the light theme. */
    public static Image generate(String seed, double size) {
        return generate(seed, size, LIGHT);
    }

    // ---- Palette ------------------------------------------------------

    private static Color[] buildPalette(double hue, Theme t) {
        return new Color[] {
                correctedHsl(hue, t.grayscaleSaturation, lerp(t.grayscaleLightness, 0)),   // dark gray
                correctedHsl(hue, t.colorSaturation, lerp(t.colorLightness, 0.5)),          // mid color
                correctedHsl(hue, t.grayscaleSaturation, lerp(t.grayscaleLightness, 1)),    // light gray
                correctedHsl(hue, t.colorSaturation, lerp(t.colorLightness, 1)),            // light color
                correctedHsl(hue, t.colorSaturation, lerp(t.colorLightness, 0))             // dark color
        };
    }

    private static double lerp(double[] range, double t) {
        return range[0] + (range[1] - range[0]) * t;
    }

    /** Standard HSL -> RGB. ("corrected" in the sense of matching Jdenticon's palette intent —
     *  a full perceptual correction isn't reproduced here, this is plain HSL.) */
    private static Color correctedHsl(double hueDeg, double saturation, double lightness) {
        return Color.hsb(hueDeg, saturation, lightnessToBrightness(saturation, lightness));
    }

    // JavaFX's Color.hsb expects HSB(HSV), Jdenticon works in HSL — convert L->B holding S fixed
    // via the standard HSL->HSV relation for a matching visual lightness.
    private static double lightnessToBrightness(double s, double l) {
        double v = l + s * Math.min(l, 1 - l);
        return v;
    }

    // ---- Transform (ported from Jdenticon's Transform) ------------------

    /**
     * Jdenticon's coordinate transform: rotates a shape's in-cell coordinates by
     * {@code rotation} quarter-turns and translates to the cell's position. This
     * is the exact math from Jdenticon's Transform.transformPoint — each shape
     * defines points in an unrotated cell, and this maps them to the canvas.
     */
    private static final class Transform {
        final double x, y, size;
        final int rotation; // 0..3 quarter turns

        Transform(double x, double y, double size, int rotation) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.rotation = rotation;
        }

        /**
         * Transforms an in-cell point (px, py), optionally offset by (w, h) which
         * are used by Jdenticon so a point can be expressed relative to a rotated
         * bounding box. Matches Transform.transformPoint($x, $y, $w, $h).
         */
        double transformX(double px, double py, double w, double h) {
            double right = x + size;
            switch (rotation) {
                case 1:  return right - py - h;
                case 2:  return right - px - w;
                case 3:  return x + py;
                default: return x + px;
            }
        }

        double transformY(double px, double py, double w, double h) {
            double bottom = y + size;
            switch (rotation) {
                case 1:  return y + px;
                case 2:  return bottom - py - h;
                case 3:  return bottom - px - w;
                default: return y + py;
            }
        }
    }

    @FunctionalInterface
    private interface TriConsumer {
        void draw(ShapeSink sink, double cell, int positionIndex);
    }

    // ---- SVG path builder: transforms points, supports holes via winding ----

    /**
     * Accumulates a shape's path, routing every point through the cell
     * {@link Transform} (so rotation is applied uniformly). Shapes are defined in
     * plain unrotated cell coordinates; the transform maps them to the canvas.
     * Holes are cut by reversing winding order and filling with NON_ZERO.
     *
     * The primitives mirror Jdenticon's renderer: addTriangle uses a direction
     * that removes one corner of the cell; addRhombus/addCircle/addRectangle are
     * inscribed in the given box; addPolygon takes explicit points.
     */
    private static final class ShapeSink {
        int positionIndex;
        private final Transform t;
        private final StringBuilder svg = new StringBuilder();

        ShapeSink(Transform t) { this.t = t; }

        // Triangle directions match Jdenticon's TriangleDirection:
        // 0=SW, 1=NW, 2=NE, 3=SE — the named corner is the RIGHT-ANGLE corner
        // that is *cut away* (the triangle omits that corner).
        void addTriangle(double x, double y, double w, double h, int direction, boolean hole) {
            // Four corners of the box, then drop the one indicated by direction.
            // points listed clockwise starting top-left.
            double[][] corners = {
                    {x, y},         // 0 TL
                    {x + w, y},     // 1 TR
                    {x + w, y + h}, // 2 BR
                    {x, y + h}      // 3 BL
            };
            // Jdenticon removes the corner OPPOSITE to keep the triangle pointing
            // toward `direction`. Mapping (verified against ShapeDefinitions use):
            //   SOUTH_WEST(0) removes TR(1); NORTH_WEST(1) removes BR(2);
            //   NORTH_EAST(2) removes BL(3); SOUTH_EAST(3) removes TL(0).
            int remove;
            switch (direction) {
                case 1:  remove = 2; break; // NORTH_WEST
                case 2:  remove = 3; break; // NORTH_EAST
                case 3:  remove = 0; break; // SOUTH_EAST
                default: remove = 1;        // SOUTH_WEST
            }
            java.util.List<double[]> pts = new java.util.ArrayList<>();
            for (int i = 0; i < 4; i++) if (i != remove) pts.add(corners[i]);
            double[] xs = {pts.get(0)[0], pts.get(1)[0], pts.get(2)[0]};
            double[] ys = {pts.get(0)[1], pts.get(1)[1], pts.get(2)[1]};
            if (hole) reverse(xs, ys);
            polygon(xs, ys);
        }

        void addRhombus(double x, double y, double w, double h, boolean hole) {
            double[] xs = {x + w / 2, x + w, x + w / 2, x};
            double[] ys = {y, y + h / 2, y + h, y + h / 2};
            if (hole) reverse(xs, ys);
            polygon(xs, ys);
        }

        void addRectangle(double x, double y, double w, double h, boolean hole) {
            double[] xs = {x, x + w, x + w, x};
            double[] ys = {y, y, y + h, y + h};
            if (hole) reverse(xs, ys);
            polygon(xs, ys);
        }

        void addCircle(double x, double y, double diameter, boolean hole) {
            // Transform the circle's bounding box; since our transforms are pure
            // quarter-turn rotations + translation, the circle stays a circle and
            // we can transform just its center. Radius is unaffected by rotation.
            double r = diameter / 2;
            double cx = t.transformX(x + r, y + r, 0, 0);
            double cy = t.transformY(x + r, y + r, 0, 0);
            int sweep = hole ? 0 : 1;
            svg.append(String.format(java.util.Locale.ROOT,
                    "M%.3f,%.3f A%.3f,%.3f 0 1,%d %.3f,%.3f A%.3f,%.3f 0 1,%d %.3f,%.3f Z ",
                    cx + r, cy, r, r, sweep, cx - r, cy, r, r, sweep, cx + r, cy));
        }

        void addPolygon(double[] xs, double[] ys, boolean hole) {
            double[] xs2 = xs.clone(), ys2 = ys.clone();
            if (hole) reverse(xs2, ys2);
            polygon(xs2, ys2);
        }

        // Emit a polygon, transforming each point through the cell transform.
        private void polygon(double[] xs, double[] ys) {
            double tx0 = t.transformX(xs[0], ys[0], 0, 0);
            double ty0 = t.transformY(xs[0], ys[0], 0, 0);
            svg.append(String.format(java.util.Locale.ROOT, "M%.3f,%.3f ", tx0, ty0));
            for (int i = 1; i < xs.length; i++) {
                double tx = t.transformX(xs[i], ys[i], 0, 0);
                double ty = t.transformY(xs[i], ys[i], 0, 0);
                svg.append(String.format(java.util.Locale.ROOT, "L%.3f,%.3f ", tx, ty));
            }
            svg.append("Z ");
        }

        private static void reverse(double[] a, double[] b) {
            for (int i = 0, j = a.length - 1; i < j; i++, j--) {
                double tmp = a[i]; a[i] = a[j]; a[j] = tmp;
                tmp = b[i]; b[i] = b[j]; b[j] = tmp;
            }
        }

        String build() { return svg.toString(); }
    }

    // ---- Shape library — geometry ported from Jdenticon's ShapeDefinitions ------
    // Direction constants used by addTriangle: 0=NORTH_WEST 1=NORTH_EAST 2=SOUTH_EAST 3=SOUTH_WEST

    private static final List<TriConsumer> OUTER_SHAPES = List.of(
            (sink, cell, idx) -> sink.addTriangle(0, 0, cell, cell, 0, false),
            (sink, cell, idx) -> sink.addTriangle(0, cell / 2, cell, cell / 2, 0, false),
            (sink, cell, idx) -> sink.addRhombus(0, 0, cell, cell, false),
            (sink, cell, idx) -> { double m = cell / 6; sink.addCircle(m, m, cell - 2 * m, false); }
    );

    private static final List<TriConsumer> CENTER_SHAPES = List.of(
            // 0: pentagon, corner cut
            (sink, cell, idx) -> {
                double k = cell * 0.42;
                sink.addPolygon(
                        new double[]{0, cell, cell, cell - k, 0},
                        new double[]{0, 0, cell - 2 * k, cell, cell}, false);
            },
            // 1: NE triangle
            (sink, cell, idx) -> {
                double w = cell * 0.5, h = cell * 0.8;
                sink.addTriangle(cell - w, 0, w, h, 1, false);
            },
            // 2: inset rectangle, flush bottom-right
            (sink, cell, idx) -> {
                double s = cell / 3;
                sink.addRectangle(s, s, cell - s, cell - s, false);
            },
            // 3: solid inset square (border-ish, no hole)
            (sink, cell, idx) -> {
                double inner = cell * 0.1, outer = cell * 0.25;
                sink.addRectangle(outer, outer, cell - inner - outer, cell - inner - outer, false);
            },
            // 4: small circle, bottom-right
            (sink, cell, idx) -> {
                double m = cell * 0.15, s = cell * 0.5;
                sink.addCircle(cell - s - m, cell - s - m, s, false);
            },
            // 5: square with triangular notch cut out
            (sink, cell, idx) -> {
                double inner = cell * 0.1, outer = inner * 4;
                sink.addRectangle(0, 0, cell, cell, false);
                sink.addPolygon(
                        new double[]{outer, cell - inner, outer + (cell - outer - inner) / 2},
                        new double[]{outer, outer, cell - inner}, true);
            },
            // 6: concave hexagon (arrow/chevron)
            (sink, cell, idx) -> sink.addPolygon(
                    new double[]{0, cell, cell, cell * 0.4, cell * 0.7, 0},
                    new double[]{0, 0, cell * 0.7, cell * 0.4, cell, cell}, false),
            // 7: SE triangle, bottom-right quarter
            (sink, cell, idx) -> sink.addTriangle(cell / 2, cell / 2, cell / 2, cell / 2, 2, false),
            // 8: pentagon, larger corner cut
            (sink, cell, idx) -> sink.addPolygon(
                    new double[]{0, cell, cell, cell / 2, 0},
                    new double[]{0, 0, cell / 2, cell, cell}, false),
            // 9: hollow square ring
            (sink, cell, idx) -> {
                double inner = cell * 0.14, outer = cell * 0.35;
                sink.addRectangle(0, 0, cell, cell, false);
                sink.addRectangle(outer, outer, cell - outer - inner, cell - outer - inner, true);
            },
            // 10: square with circular hole
            (sink, cell, idx) -> {
                double inner = cell * 0.12, outer = inner * 3;
                sink.addRectangle(0, 0, cell, cell, false);
                sink.addCircle(outer, outer, cell - inner - outer, true);
            },
            // 11: SE triangle (duplicate in the original library — kept for fidelity)
            (sink, cell, idx) -> sink.addTriangle(cell / 2, cell / 2, cell / 2, cell / 2, 2, false),
            // 12: square with rhombus (diamond) hole
            (sink, cell, idx) -> {
                double m = cell * 0.25;
                sink.addRectangle(0, 0, cell, cell, false);
                sink.addRhombus(m, m, cell - m, cell - m, true);
            },
            // 13: large circle, suppressed at position index 0
            (sink, cell, idx) -> {
                if (idx != 0) {
                    double m = cell * 0.4, s = cell * 1.2;
                    sink.addCircle(m, m, s, false);
                }
            }
    );

    // ---- Hashing helpers -------------------------------------------------

    /** SHA-1 as a lowercase hex string — Jdenticon's default hash. */
    private static String sha1Hex(String seed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(seed.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Jdenticon's getOctet: the value of a single hex digit (nibble) at the given
     * index in the hash string, i.e. 0..15. NOT a full byte — this matters because
     * shape/rotation/color indices are taken from single hex digits.
     */
    private static int getOctet(String hash, int index) {
        return Character.digit(hash.charAt(index), 16);
    }
}
