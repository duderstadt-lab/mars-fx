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
package de.mpg.biochem.mars.fx.bdv.commands;

import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

/**
 * Helpers for mapping regions selected in the global BDV coordinate system
 * into the local pixel coordinates of an individual source. BDV sources can
 * carry an affine transform (channel alignment, drift correction) and
 * {@code Source.getSource(t, level)} always returns the untransformed pixels,
 * so any analysis must be done in the local frame and the results mapped
 * back to global coordinates with {@code sourceTransform.apply}.
 */
final class BdvSourceIntervals {

	private BdvSourceIntervals() {}

	/**
	 * Maps a 2D region given in global coordinates into the local pixel
	 * coordinates of a source using the inverse of its source transform. All
	 * four corners are transformed so the result is the bounding box of the
	 * region even when the transform rotates or flips, and the box is clipped
	 * to the bounds of the source image. The returned interval may be empty
	 * (check with {@link Intervals#isEmpty(Interval)}) if the region lies
	 * entirely outside the source.
	 */
	static Interval globalToSource(Interval globalInterval,
		AffineTransform3D sourceTransform, Interval sourceBounds)
	{
		final double[][] corners = new double[][] {
			{ globalInterval.min(0), globalInterval.min(1), 0 },
			{ globalInterval.max(0), globalInterval.min(1), 0 },
			{ globalInterval.min(0), globalInterval.max(1), 0 },
			{ globalInterval.max(0), globalInterval.max(1), 0 } };

		double xmin = Double.POSITIVE_INFINITY;
		double ymin = Double.POSITIVE_INFINITY;
		double xmax = Double.NEGATIVE_INFINITY;
		double ymax = Double.NEGATIVE_INFINITY;
		final double[] local = new double[3];
		for (double[] corner : corners) {
			sourceTransform.applyInverse(local, corner);
			xmin = Math.min(xmin, local[0]);
			ymin = Math.min(ymin, local[1]);
			xmax = Math.max(xmax, local[0]);
			ymax = Math.max(ymax, local[1]);
		}

		final Interval box = Intervals.createMinMax((long) Math.floor(xmin),
			(long) Math.floor(ymin), (long) Math.ceil(xmax), (long) Math.ceil(ymax));
		final Interval bounds2D = Intervals.createMinMax(sourceBounds.min(0),
			sourceBounds.min(1), sourceBounds.max(0), sourceBounds.max(1));
		return Intervals.intersect(box, bounds2D);
	}
}
