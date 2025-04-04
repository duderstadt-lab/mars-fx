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

import org.scijava.table.DoubleColumn;

import io.fair_acc.chartfx.axes.spi.AxisRange;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSet2D;
import io.fair_acc.dataset.spi.AbstractDataSet;
import javafx.scene.paint.Color;

/**
 * Implementation of the {@code DataSet} warpper that retrieves x,y values from
 * backing DoubleColumns and passes them to plots.
 *
 * @author karlduderstadt
 */
public class MarsWrappedDoubleDataSet extends
	AbstractDataSet<MarsWrappedDoubleDataSet> implements DataSet2D
{

	private static final long serialVersionUID = -493232313124620828L;

	private Color color;
	private double width;
	private String lineStyle;
	private DoubleColumn xValues, yValues;

	private MarsNumericAxis axis;
	private int maxPointCount;
	private double pointsPerX;
	private double binSize = 1;
	private boolean downsampling = false;

	/**
	 * Creates a new instance of <code>DoubleDataSet</code>.
	 *
	 * @param name name of this DataSet.
	 * @throws IllegalArgumentException if {@code name} is {@code null}
	 */
	public MarsWrappedDoubleDataSet(final String name) {
		super(name, 2);
	}

	public MarsWrappedDoubleDataSet(String name, Color color, double width,
		String lineStyle)
	{
		this(name);
		this.color = color;
		this.width = width;
		this.lineStyle = lineStyle;
	}

	public MarsWrappedDoubleDataSet(final String name, final int initalSize,
		Color color, double width, String lineStyle)
	{
		this(name);
		this.color = color;
		this.width = width;
		this.lineStyle = lineStyle;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public Color getColor() {
		return color;
	}

	public void setWidth(double width) {
		this.width = width;
	}

	public double getWidth() {
		return width;
	}

	public void setLineStyle(String lineStyle) {
		this.lineStyle = lineStyle;
	}

	public String getLineStyle() {
		return lineStyle;
	}

	public void add(DoubleColumn xValues, DoubleColumn yValues) {
		this.xValues = xValues;
		this.yValues = yValues;
	}

	@Override
	public final double get(final int dimIndex, final int index) {
		if (downsampling && rangeTooLarge()) {
			int newIndex = (int) (index * binSize);
			return dimIndex == DataSet.DIM_X ? xValues.getArray()[newIndex] : yValues
				.getArray()[newIndex];
		}
		else return dimIndex == DataSet.DIM_X ? xValues.getArray()[index] : yValues
			.getArray()[index];
	}

	@Override
	public int getDataCount() {
		if (downsampling && rangeTooLarge()) {
			AxisRange range = axis.getRange();
			int fullRangePointCount = (int) (maxPointCount * (xValues.getValue(xValues
				.size() - 1) - xValues.getValue(0)) / (range.getMax() - range
					.getMin()));
			fullRangePointCount = (fullRangePointCount < maxPointCount)
				? maxPointCount : fullRangePointCount;
			binSize = ((double) xValues.size()) / fullRangePointCount;
			return fullRangePointCount;
		}
		else return Math.min(xValues.size(), yValues.size());
	}

	@Override
	public final double[] getValues(final int dimIndex) {
		return dimIndex == DataSet.DIM_X ? xValues.getArray() : yValues.getArray();
	}

	@Override
	public MarsWrappedDoubleDataSet set(DataSet other, boolean copy) {
		//No implementation for now.
		return this;
	}

	public void stopDownsampling() {
		this.downsampling = false;
	}

	public void downsample(final MarsNumericAxis axis, final int maxPointCount) {
		if (xValues.size() < 2) return;
		this.downsampling = true;
		this.axis = axis;
		this.maxPointCount = maxPointCount;
		this.pointsPerX = xValues.size() / (xValues.getValue(xValues.size() - 1) -
			xValues.getValue(0));
	}

	private boolean rangeTooLarge() {
		if (xValues.size() == 0 || Double.isInfinite(pointsPerX) || Double.isNaN(
			pointsPerX)) return false;
		AxisRange range = axis.getRange();
		int pointCount = (int) (pointsPerX * (range.getMax() - range.getMin()));
		if (pointCount > maxPointCount) return true;
		else return false;
	}
}
