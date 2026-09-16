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

import java.util.Arrays;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

/**
 * Single-threaded 2D filters used by the BDV object finder.
 * <p>
 * The ImageJ Ops equivalents ({@code filter.median}, {@code threshold.otsu}
 * with a neighborhood {@code Shape}) split every call across the SciJava
 * {@code ThreadService} using one chunk per core. When the object finder
 * already runs one task per time point on its own pool, that multiplies to
 * {@code nThreads * cores} runnable threads, which starves the JavaFX
 * application and render threads and freezes the archive window animation.
 * These implementations do no parallelization of their own so the number of
 * threads the command uses is exactly the number the user asked for.
 * <p>
 * The local Otsu threshold also avoids recomputing a full neighborhood
 * histogram per pixel by sliding a hypersphere window across each row and only
 * adding and removing the boundary pixels.
 */
final class SegmentationFilters {

	/** Number of histogram bins for the Otsu thresholds. */
	private static final int BINS = 1024;

	private SegmentationFilters() {}

	/**
	 * Median filter over a hypersphere neighborhood of the given radius with
	 * border extension. The result is a new image positioned at the input min.
	 */
	static <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T>
		median(final RandomAccessibleInterval<T> in, final long radius)
	{
		final int r = (int) radius;
		final Padded padded = read(Views.extendBorder(in), in, r);
		final int[] halfWidths = hyperSphereHalfWidths(r);

		int n = 0;
		for (int w : halfWidths) n += 2 * w + 1;
		final double[] scratch = new double[n];

		final ArrayImgFactory<T> factory = new ArrayImgFactory<>(Util.getTypeFromInterval(in));
		final RandomAccessibleInterval<T> out = factory.create(Intervals.dimensionsAsLongArray(in));

		final Cursor<T> cursor = Views.flatIterable(out).cursor();
		for (int y = 0; y < padded.height; y++) {
			for (int x = 0; x < padded.width; x++) {
				int i = 0;
				for (int dy = -r; dy <= r; dy++) {
					final int w = halfWidths[dy + r];
					final int rowOffset = (y + r + dy) * padded.paddedWidth + x + r;
					for (int dx = -w; dx <= w; dx++)
						scratch[i++] = padded.values[rowOffset + dx];
				}
				Arrays.sort(scratch);
				final double median = (n % 2 == 0) ? (scratch[n / 2 - 1] + scratch[n / 2]) / 2 : scratch[n / 2];
				cursor.next().setReal(median);
			}
		}

		return Views.translate(out, Intervals.minAsLongArray(in));
	}

	/**
	 * Local Otsu threshold over a hypersphere neighborhood of the given radius
	 * with mirror extension. A pixel is set when it is above the Otsu threshold
	 * of its neighborhood histogram. The result is positioned at the input min.
	 */
	static <T extends RealType<T>> RandomAccessibleInterval<BitType> localOtsu(
		final RandomAccessibleInterval<T> in, final long radius)
	{
		final int r = (int) radius;
		final Padded padded = read(Views.extendMirrorSingle(in), in, r);
		final int[] halfWidths = hyperSphereHalfWidths(r);

		final double min = padded.min();
		final double max = padded.max();
		final int[] bins = padded.bins(min, max);
		final double binWidth = (max - min) / BINS;

		final RandomAccessibleInterval<BitType> out = ArrayImgs.bits(Intervals.dimensionsAsLongArray(in));
		final Cursor<BitType> cursor = Views.flatIterable(out).cursor();

		if (max <= min) {
			while (cursor.hasNext()) cursor.next().set(false);
			return Views.translate(out, Intervals.minAsLongArray(in));
		}

		final long[] hist = new long[BINS];
		for (int y = 0; y < padded.height; y++) {
			Arrays.fill(hist, 0);
			for (int dy = -r; dy <= r; dy++) {
				final int w = halfWidths[dy + r];
				final int rowOffset = (y + r + dy) * padded.paddedWidth + r;
				for (int dx = -w; dx <= w; dx++)
					hist[bins[rowOffset + dx]]++;
			}

			for (int x = 0; x < padded.width; x++) {
				final double threshold = min + (otsuBin(hist) + 0.5) * binWidth;
				final double center = padded.values[(y + r) * padded.paddedWidth + x + r];
				cursor.next().set(center > threshold);

				if (x + 1 < padded.width) {
					for (int dy = -r; dy <= r; dy++) {
						final int w = halfWidths[dy + r];
						final int rowOffset = (y + r + dy) * padded.paddedWidth + x + r;
						hist[bins[rowOffset - w]]--;
						hist[bins[rowOffset + 1 + w]]++;
					}
				}
			}
		}

		return Views.translate(out, Intervals.minAsLongArray(in));
	}

	/**
	 * Global Otsu threshold. A pixel is set when it is above the Otsu threshold
	 * of the image histogram. The result is positioned at the input min.
	 */
	static <T extends RealType<T>> RandomAccessibleInterval<BitType> otsu(
		final RandomAccessibleInterval<T> in)
	{
		final Padded padded = read(Views.extendZero(in), in, 0);
		final double min = padded.min();
		final double max = padded.max();

		final RandomAccessibleInterval<BitType> out = ArrayImgs.bits(Intervals.dimensionsAsLongArray(in));
		final Cursor<BitType> cursor = Views.flatIterable(out).cursor();

		if (max <= min) {
			while (cursor.hasNext()) cursor.next().set(false);
			return Views.translate(out, Intervals.minAsLongArray(in));
		}

		final int[] bins = padded.bins(min, max);
		final long[] hist = new long[BINS];
		for (int bin : bins) hist[bin]++;
		final double threshold = min + (otsuBin(hist) + 0.5) * (max - min) / BINS;

		for (double value : padded.values) cursor.next().set(value > threshold);

		return Views.translate(out, Intervals.minAsLongArray(in));
	}

	/**
	 * Otsu's threshold bin, following the ImageJ AutoThreshold implementation
	 * (G. Landini) that ImageJ Ops uses.
	 */
	static int otsuBin(final long[] histogram) {
		final int L = histogram.length;
		long S = 0;
		long N = 0;
		for (int k = 0; k < L; k++) {
			S += k * histogram[k];
			N += histogram[k];
		}

		long Sk = 0;
		long N1 = histogram[0];
		double BCVmax = 0;
		int kStar = 0;
		for (int k = 1; k < L - 1; k++) {
			Sk += k * histogram[k];
			N1 += histogram[k];
			final double denom = (double) N1 * (N - N1);
			double BCV = 0;
			if (denom != 0) {
				final double num = ((double) N1 / N) * S - Sk;
				BCV = (num * num) / denom;
			}
			if (BCV >= BCVmax) {
				BCVmax = BCV;
				kStar = k;
			}
		}
		return kStar;
	}

	/**
	 * Half-width of each row of a 2D hypersphere of the given radius, indexed
	 * by {@code dy + radius}. Matches the pixels visited by imglib2's
	 * {@code HyperSphereShape}.
	 */
	private static int[] hyperSphereHalfWidths(final int radius) {
		final int[] halfWidths = new int[2 * radius + 1];
		for (int dy = -radius; dy <= radius; dy++)
			halfWidths[dy + radius] = (int) Math.floor(Math.sqrt((double) radius * radius - (double) dy * dy));
		return halfWidths;
	}

	/**
	 * Reads the 2D interval, padded on every side by {@code pad} pixels of the
	 * given extension, into a row-major double array. Reading once up front
	 * matters because the input may be an interpolated view whose pixels are
	 * expensive to evaluate.
	 */
	private static <T extends RealType<T>> Padded read(
		final net.imglib2.RandomAccessible<T> extended,
		final net.imglib2.Interval interval, final int pad)
	{
		final Padded padded = new Padded((int) interval.dimension(0), (int) interval.dimension(1), pad);
		final RandomAccessibleInterval<T> region = Views.interval(extended, Intervals.expand(interval, pad));
		final Cursor<T> cursor = Views.flatIterable(region).cursor();
		for (int i = 0; i < padded.values.length; i++)
			padded.values[i] = cursor.next().getRealDouble();
		return padded;
	}

	private static final class Padded {

		final int width;
		final int height;
		final int paddedWidth;
		final double[] values;

		Padded(final int width, final int height, final int pad) {
			this.width = width;
			this.height = height;
			this.paddedWidth = width + 2 * pad;
			this.values = new double[paddedWidth * (height + 2 * pad)];
		}

		double min() {
			double min = Double.POSITIVE_INFINITY;
			for (double v : values) if (v < min) min = v;
			return min;
		}

		double max() {
			double max = Double.NEGATIVE_INFINITY;
			for (double v : values) if (v > max) max = v;
			return max;
		}

		int[] bins(final double min, final double max) {
			final int[] bins = new int[values.length];
			final double scale = BINS / (max - min);
			for (int i = 0; i < values.length; i++) {
				final int bin = (int) ((values[i] - min) * scale);
				bins[i] = Math.max(0, Math.min(BINS - 1, bin));
			}
			return bins;
		}
	}
}
