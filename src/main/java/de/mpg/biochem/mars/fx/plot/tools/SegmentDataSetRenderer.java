/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2025 Karl Duderstadt
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


package de.mpg.biochem.mars.fx.plot.tools;

import io.fair_acc.chartfx.marker.DefaultMarker;
import io.fair_acc.chartfx.renderer.spi.AbstractErrorDataSetRendererParameter;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.FillRule;
import javafx.scene.paint.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.marker.Marker;
import io.fair_acc.chartfx.renderer.ErrorStyle;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.renderer.spi.utils.BezierCurve;
import io.fair_acc.chartfx.ui.css.DataSetNode;
import io.fair_acc.chartfx.ui.css.DataSetStyleParser;
import io.fair_acc.chartfx.utils.FastDoubleArrayCache;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSetError.ErrorType;
import io.fair_acc.dataset.utils.ProcessingProfiler;

/**
 * Renders data points with error bars and/or error surfaces It can be used e.g. to render horizontal and/or vertical
 * errors additional functionality:
 * <ul>
 * <li>bar-type plot
 * <li>polar-axis plotting
 * <li>scatter and/or bubble-chart-type plots
 * </ul>
 *
 * @author R.J. Steinhagen
 */
@SuppressWarnings({ "PMD.LongVariable", "PMD.ShortVariable" }) // short variables like x, y are perfectly fine, as well
// as descriptive long ones
public class SegmentDataSetRenderer extends AbstractErrorDataSetRendererParameter<SegmentDataSetRenderer>
		implements Renderer {
	private static final Logger LOGGER = LoggerFactory.getLogger(SegmentDataSetRenderer.class);

	private final DataSetStyleParser styleParser = DataSetStyleParser.newInstance();

	/**
	 * Creates new <code>ErrorDataSetRenderer</code>.
	 */
	public SegmentDataSetRenderer() {
		this(3);
	}

	/**
	 * Creates new <code>ErrorDataSetRenderer</code>.
	 *
	 * @param dashSize initial size (top/bottom cap) on top of the error bars
	 */
	public SegmentDataSetRenderer(final int dashSize) {
		setDashSize(dashSize);
	}

	/**
	 * @param style the data set for which the representative icon should be generated
	 * @param canvas the canvas in which the representative icon should be drawn
	 * @return true if the renderer generates symbols that should be displayed
	 */
	@Override
	public boolean drawLegendSymbol(final DataSetNode style, final Canvas canvas) {
		final int width = (int) canvas.getWidth();
		final int height = (int) canvas.getHeight();
		final GraphicsContext gc = canvas.getGraphicsContext2D();

		gc.save();

		gc.setLineWidth(style.getLineWidth());
		gc.setLineDashes(style.getLineDashes());
		gc.setStroke(style.getLineColor());

		if (getErrorType() == ErrorStyle.ERRORBARS) {
			final double x = width / 2.0;
			final double y = height / 2.0;
			if (getDashSize() > 2) {
				gc.strokeLine(x - 1.0, 1, x + 1.0, 1.0);
				gc.strokeLine(x - 1.0, height - 2.0, x + 1.0, height - 2.0);
				gc.strokeLine(x, 1.0, x, height - 2.0);
			}
			gc.strokeLine(1, y, width, y);
		} else if (getErrorType() == ErrorStyle.ERRORSURFACE || getErrorType() == ErrorStyle.ERRORCOMBO) {
			final double y = height / 2.0;
			gc.setFill(style.getLineFillPattern());
			gc.fillRect(1, 1, width - 2.0, height - 2.0);
			gc.strokeLine(1, y, width - 2.0, y);
		} else {
			final double x = width / 2.0;
			final double y = height / 2.0;
			if (getDashSize() > 2) {
				gc.strokeLine(x - 1.0, 1.0, x + 1.0, 1.0);
				gc.strokeLine(x - 1.0, height - 2.0, x + 1, height - 2.0);
				gc.strokeLine(x, 1.0, x, height - 2.0);
			}
			gc.strokeLine(1, y, width - 2.0, y);
		}
		gc.restore();
		return true;
	}

	@Override
	protected void render(final GraphicsContext gc, final DataSet dataSet, final DataSetNode style) {
		// N.B. print out for debugging purposes, please keep (used for
		// detecting redundant or too frequent render updates)
		// System.err.println(String.format("render for range [%f,%f] and dataset = '%s'", xMin, xMax, dataSet.getName()));

		Color color = Color.BLACK;
		double width = 1;
		String lineStyle = "";

		if (dataSet instanceof MarsWrappedDoubleDataSet) {
			color = ((MarsWrappedDoubleDataSet) dataSet).getColor();
			width = ((MarsWrappedDoubleDataSet) dataSet).getWidth();
			lineStyle = ((MarsWrappedDoubleDataSet) dataSet).getLineStyle();
		} else if (dataSet instanceof MarsDoubleDataSet) {
			color = ((MarsDoubleDataSet) dataSet).getColor();
			width = ((MarsDoubleDataSet) dataSet).getWidth();
			lineStyle = ((MarsDoubleDataSet) dataSet).getLineStyle();
		}

		style.setLineColor(color);
		style.setLineWidth(width);
		style.setMarkerSize(0);
		style.setStyle(lineStyle);

		var timestamp = ProcessingProfiler.getTimeStamp();
		int indexMin;
		int indexMax; /* indexMax is excluded in the drawing */
		if (isAssumeSortedData()) {
			indexMin = Math.max(0, dataSet.getIndex(DataSet.DIM_X, xMin) - 1);
			indexMax = Math.min(dataSet.getIndex(DataSet.DIM_X, xMax) + 2, dataSet.getDataCount());
		} else {
			indexMin = 0;
			indexMax = dataSet.getDataCount();
		}

		// zero length/range data set -> nothing to be drawn
		if (indexMax - indexMin <= 0) {
			return;
		}

		if (ProcessingProfiler.getDebugState()) {
			timestamp = ProcessingProfiler.getTimeDiff(timestamp,
					"get min/max" + String.format(" from:%d to:%d", indexMin, indexMax));
		}

		final boolean enableErrorsX = true; // TODO: what is this used for?
		final CachedDataPoints points = SHARED_POINTS_CACHE.resizeMin(indexMin, indexMax, dataSet.getDataCount(), enableErrorsX);
		if (ProcessingProfiler.getDebugState()) {
			timestamp = ProcessingProfiler.getTimeDiff(timestamp, "get CachedPoints");
		}

		// compute local screen coordinates
		final boolean isPolarPlot = getChart().isPolarPlot();
		if (isParallelImplementation()) {
			points.computeScreenCoordinatesInParallel(xAxis, yAxis, dataSet, style,
					indexMin, indexMax, getErrorType(), isPolarPlot,
					isallowNaNs());
		} else {
			points.computeScreenCoordinates(xAxis, yAxis, dataSet, style,
					indexMin, indexMax, getErrorType(), isPolarPlot, isallowNaNs());
		}
		if (ProcessingProfiler.getDebugState()) {
			timestamp = ProcessingProfiler.getTimeDiff(timestamp, "computeScreenCoordinates()");
		}

		// invoke data reduction algorithm
		points.reduce(rendererDataReducerProperty().get(), isReducePoints(),
				getMinRequiredReductionSize());

		// draw individual plot components
		if (dataSet.getStyle() != null && dataSet.getStyle().equals("Segments")) drawSegments(gc, style, points);
		else if (dataSet.getStyle().equals("Scatter")) {
			gc.setFill(color);
			style.setMarkerSize(width);
			drawScatter(gc, style, points);
		}
		else drawPolyLineLine(gc, style, points);

		if (ProcessingProfiler.getDebugState()) {
			timestamp = ProcessingProfiler.getTimeDiff(timestamp, "drawChartComponents()");
		}
	}

	/**
	 * @return the instance of this ErrorDataSetRenderer.
	 */
	@Override
	protected SegmentDataSetRenderer getThis() {
		return this;
	}

	protected static void drawPolyLineLine(final GraphicsContext gc, final DataSetNode style, final CachedDataPoints points) {
		gc.save();
		style.applyLineStrokeStyle(gc);

		if (points.allowForNaNs) {
			gc.beginPath();
			gc.moveTo(points.xValues[0], points.yValues[0]);
			boolean lastIsFinite = true;
			double xLastValid = 0.0;
			double yLastValid = 0.0;
			for (int i = 0; i < points.actualDataCount; i++) {
				final double x0 = points.xValues[i];
				final double y0 = points.yValues[i];
				if (Double.isFinite(x0) && Double.isFinite(y0)) {
					if (!lastIsFinite) {
						gc.moveTo(x0, y0);
						lastIsFinite = true;
						continue;
					}
					gc.lineTo(x0, y0);
					xLastValid = x0;
					yLastValid = y0;
					lastIsFinite = true;
				} else {
					lastIsFinite = false;
				}
			}
			gc.moveTo(xLastValid, yLastValid);
			gc.closePath();
			gc.stroke();
		} else {
			if (gc.getLineDashes() != null) {
				gc.strokePolyline(points.xValues, points.yValues, points.actualDataCount);
			} else {
				for (int i = 0; i < points.actualDataCount - 1; i++) {
					final double x1 = points.xValues[i];
					final double x2 = points.xValues[i + 1];
					final double y1 = points.yValues[i];
					final double y2 = points.yValues[i + 1];

					gc.strokeLine(x1, y1, x2, y2);
				}
			}
		}

		gc.restore();
	}

	protected static void drawSegments(final GraphicsContext gc, final DataSetNode style, final CachedDataPoints points) {
		gc.save();
		style.applyLineStrokeStyle(gc);

		// Skip every other segment
		for (int i = 0; i < points.actualDataCount - 1; i += 2) {
			final double x1 = points.xValues[i];
			final double x2 = points.xValues[i + 1];
			final double y1 = points.yValues[i];
			final double y2 = points.yValues[i + 1];
			gc.strokeLine(x1, y1, x2, y2);
		}

		gc.restore();
		/*
		gc.save();
		style.applyLineStrokeStyle(gc);

		if (points.allowForNaNs) {
			gc.beginPath();
			gc.moveTo(points.xValues[0], points.yValues[0]);
			boolean lastIsFinite = true;
			double xLastValid = 0.0;
			double yLastValid = 0.0;
			for (int i = 0; i < points.actualDataCount; i++) {
				final double x0 = points.xValues[i];
				final double y0 = points.yValues[i];
				if (Double.isFinite(x0) && Double.isFinite(y0)) {
					if (!lastIsFinite) {
						gc.moveTo(x0, y0);
						lastIsFinite = true;
						continue;
					}
					gc.lineTo(x0, y0);
					xLastValid = x0;
					yLastValid = y0;
					lastIsFinite = true;
				} else {
					lastIsFinite = false;
				}
			}
			gc.moveTo(xLastValid, yLastValid);
			gc.closePath();
			gc.stroke();
		} else {
			if (gc.getLineDashes() != null) {
				gc.strokePolyline(points.xValues, points.yValues, points.actualDataCount);
			} else {
				for (int i = 0; i < points.actualDataCount - 1; i++) {
					final double x1 = points.xValues[i];
					final double x2 = points.xValues[i + 1];
					final double y1 = points.yValues[i];
					final double y2 = points.yValues[i + 1];

					gc.strokeLine(x1, y1, x2, y2);
				}
			}
		}

		gc.restore();
		*/
	}

	protected static void drawScatter(final GraphicsContext gc, final DataSetNode style, final CachedDataPoints points)
	{
		gc.save();

		final Marker pointMarker = DefaultMarker.CIRCLE;
		for (int i = 0; i < points.actualDataCount; i++) {
			final double x = points.xValues[i];
			final double y = points.yValues[i];
			pointMarker.draw(gc, x, y, style.getMarkerSize());
		}

		gc.restore();
	}

	// The cache can be shared because there can only ever be one renderer accessing it
	// Note: should not be exposed to child classes to guarantee that arrays aren't double used.
	private static final FastDoubleArrayCache SHARED_ARRAYS = new FastDoubleArrayCache(4);
	private static final CachedDataPoints SHARED_POINTS_CACHE = new CachedDataPoints();

	/**
	 * Deletes all arrays that are larger than necessary for the last drawn dataset
	 */
	public static void trimCache() {
		SHARED_ARRAYS.trim();
		SHARED_POINTS_CACHE.trim();
	}
}