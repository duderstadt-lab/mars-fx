/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2023 Karl Duderstadt
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
package de.mpg.biochem.mars.fx.bdv.DnaMolecule;

import bdv.util.BdvOverlay;
import de.mpg.biochem.mars.image.DNASegment;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.numeric.ARGBType;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class LineOverlay extends BdvOverlay {

    private int thickness = 5;
    private List<DNASegment> segments;

    public LineOverlay() {
        segments = new ArrayList<DNASegment>();
    }

    @Override
    protected void draw(Graphics2D g) {
        AffineTransform2D transform = new AffineTransform2D();
        getCurrentTransform2D(transform);

        if (segments.size() > 0) {
            for (DNASegment segment : segments) {
                if (Double.isNaN(segment.getX1()) || Double.isNaN(segment.getY1()) || Double.isNaN(segment.getX2()) || Double
                        .isNaN(segment.getY2())) return;

                final double[] globalCoords = new double[]{segment.getX1(), segment.getY1()};
                final double[] viewerCoords = new double[2];
                transform.apply(globalCoords, viewerCoords);

                int xSource = (int) Math.round(viewerCoords[0]);
                int ySource = (int) Math.round(viewerCoords[1]);

                final double[] globalCoords2 = new double[]{segment.getX2(), segment.getY2()};
                final double[] viewerCoords2 = new double[2];
                transform.apply(globalCoords2, viewerCoords2);

                int xTarget = (int) Math.round(viewerCoords2[0]);
                int yTarget = (int) Math.round(viewerCoords2[1]);

                g.setColor(getColor());
                g.setStroke(new BasicStroke(thickness));
                g.drawLine(xSource, ySource, xTarget, yTarget);
            }
        }
    }

    public void setThickness(int thickness) {
        this.thickness = thickness;
    }

    public void setSegments(List<DNASegment> segments) {
        this.segments = segments;
    }

    public List<DNASegment> getSegments() {
        return segments;
    }

    private Color getColor() {
        int alpha = (int) info.getDisplayRangeMax();

        if (alpha > 255 || alpha < 0) alpha = 255;

        final int r = ARGBType.red(info.getColor().get());
        final int g = ARGBType.green(info.getColor().get());
        final int b = ARGBType.blue(info.getColor().get());
        return new Color(r, g, b, alpha);
    }
}
