/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2022 Karl Duderstadt
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

import de.gsi.chart.axes.spi.AxisRange;
import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet2D;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.event.RemovedDataEvent;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.spi.AbstractDataSet;
import de.gsi.dataset.spi.DoubleErrorDataSet;
import de.gsi.dataset.utils.AssertUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import javafx.scene.paint.Color;

/**
 * Implementation of the {@code DataSet} interface which stores x,y values in
 * two separate arrays. It provides methods allowing easily manipulate of data
 * points.
 *
 * @see DoubleErrorDataSet for an implementation with asymmetric errors in Y
 * @author rstein
 */
@SuppressWarnings("PMD.TooManyMethods") // part of the flexible class nature
public class MarsDoubleDataSet extends AbstractDataSet<MarsDoubleDataSet>
	implements DataSet2D
{

	private static final long serialVersionUID = -493232313124620828L;
	protected DoubleArrayList xValues; // way faster than java default lists
	protected DoubleArrayList yValues; // way faster than java default lists

	private Color color;
	private double width;
	private String lineStyle;

	private MarsNumericAxis axis;
	private int maxPointCount;
	private double pointsPerX;
	private double binSize = 1;
	private boolean downsampling = false;

	/**
	 * Creates a new instance of <code>DoubleDataSet</code> as copy of another
	 * (deep-copy).
	 *
	 * @param another name of this DataSet.
	 */
	public MarsDoubleDataSet(final DataSet another) {
		super(another.getName(), another.getDimension());
		set(another); // NOPMD by rstein on 25/06/19 07:42
	}

	/**
	 * Creates a new instance of <code>DoubleDataSet</code>.
	 *
	 * @param name name of this DataSet.
	 * @throws IllegalArgumentException if {@code name} is {@code null}
	 */
	public MarsDoubleDataSet(final String name) {
		this(name, 0);
	}

	/**
	 * <p>
	 * Creates a new instance of <code>DoubleDataSet</code>.
	 * </p>
	 * The user than specify via the copy parameter, whether the dataset wraps the
	 * input arrays themselves or on a copies of the input arrays.
	 *
	 * @param name name of this data set.
	 * @param xValues X coordinates
	 * @param yValues Y coordinates
	 * @param initalSize how many data points are relevant to be taken
	 * @param deepCopy if true, the input array is copied
	 * @throws IllegalArgumentException if any of the parameters is {@code null}
	 *           or if arrays with coordinates have different lengths
	 */
	public MarsDoubleDataSet(final String name, final double[] xValues,
		final double[] yValues, final int initalSize, final boolean deepCopy)
	{
		super(name, 2);
		set(xValues, yValues, initalSize, deepCopy); // NOPMD
	}

	/**
	 * Creates a new instance of <code>DoubleDataSet</code>.
	 *
	 * @param name name of this DataSet.
	 * @param initalSize initial capacity of buffer (N.B. size=0)
	 * @throws IllegalArgumentException if {@code name} is {@code null}
	 */
	public MarsDoubleDataSet(final String name, final int initalSize) {
		super(name, 2);
		AssertUtils.gtEqThanZero("initalSize", initalSize);
		xValues = new DoubleArrayList(initalSize);
		yValues = new DoubleArrayList(initalSize);
	}

	public MarsDoubleDataSet(String name, Color color, double width,
		String lineStyle)
	{
		this(name);
		this.color = color;
		this.width = width;
		this.lineStyle = lineStyle;
	}

	public MarsDoubleDataSet(final String name, final int initalSize, Color color,
		double width, String lineStyle)
	{
		this(name, initalSize);
		this.color = color;
		this.width = width;
		this.lineStyle = lineStyle;
	}

	public MarsDoubleDataSet(final DataSet2D another, Color color, double width,
		String lineStyle)
	{
		this(another);
		this.color = color;
		this.width = width;
		this.lineStyle = lineStyle;
	}

	public MarsDoubleDataSet(final String name, final double[] xValues,
		final double[] yValues, final int initalSize, final boolean deepCopy,
		Color color, double width, String lineStyle)
	{
		this(name, xValues, yValues, initalSize, deepCopy);
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

	/**
	 * Add point to the end of the data set
	 *
	 * @param x index
	 * @param y index
	 * @return itself
	 */
	public MarsDoubleDataSet add(final double x, final double y) {
		return add(this.getDataCount(), x, y, null);
	}

	/**
	 * Add point to the data set.
	 *
	 * @param x horizontal coordinate of the new data point
	 * @param y vertical coordinate of the new data point
	 * @param label the data label
	 * @return itself (fluent design)
	 */
	public MarsDoubleDataSet add(final double x, final double y,
		final String label)
	{
		lock().writeLockGuard(() -> {
			xValues.add(x);
			yValues.add(y);

			if ((label != null) && !label.isEmpty()) {
				addDataLabel(xValues.size() - 1, label);
			}

			getAxisDescription(DIM_X).add(x);
			getAxisDescription(DIM_Y).add(y);
		});
		return fireInvalidated(new UpdatedDataEvent(this, "add"));
	}

	/**
	 * Add array vectors to data set.
	 *
	 * @param xValuesNew X coordinates
	 * @param yValuesNew Y coordinates
	 * @return itself
	 */
	public MarsDoubleDataSet add(final double[] xValuesNew,
		final double[] yValuesNew)
	{
		AssertUtils.notNull("X coordinates", xValuesNew);
		AssertUtils.notNull("Y coordinates", yValuesNew);
		AssertUtils.equalDoubleArrays(xValuesNew, yValuesNew);

		lock().writeLockGuard(() -> {
			final int addAt = xValues.size();
			final int newElements = Math.min(xValuesNew.length, yValuesNew.length);
			resize(addAt + newElements);
			xValues.setElements(addAt, xValuesNew);
			yValues.setElements(addAt, yValuesNew);

			getAxisDescription(DIM_X).add(xValuesNew);
			getAxisDescription(DIM_Y).add(yValuesNew);
		});
		return fireInvalidated(new AddedDataEvent(this));
	}

	/**
	 * add point to the data set
	 *
	 * @param index data point index at which the new data point should be added
	 * @param x horizontal coordinate of the new data point
	 * @param y vertical coordinate of the new data point
	 * @return itself (fluent design)
	 */
	public MarsDoubleDataSet add(final int index, final double x,
		final double y)
	{
		return add(index, x, y, null);
	}

	/**
	 * add point to the data set
	 *
	 * @param index data point index at which the new data point should be added
	 * @param x horizontal coordinates of the new data point
	 * @param y vertical coordinates of the new data point
	 * @param label data point label (see CategoryAxis)
	 * @return itself (fluent design)
	 */
	public MarsDoubleDataSet add(final int index, final double x, final double y,
		final String label)
	{
		lock().writeLockGuard(() -> {
			final int indexAt = Math.max(0, Math.min(index, getDataCount() + 1));

			xValues.add(indexAt, x);
			yValues.add(indexAt, y);
			getDataLabelMap().addValueAndShiftKeys(indexAt, xValues.size(), label);
			getDataStyleMap().shiftKeys(indexAt, xValues.size());
			getAxisDescription(DIM_X).add(x);
			getAxisDescription(DIM_Y).add(y);
		});
		return fireInvalidated(new AddedDataEvent(this));
	}

	/**
	 * add point to the data set
	 *
	 * @param index data point index at which the new data point should be added
	 * @param x horizontal coordinate of the new data point
	 * @param y vertical coordinate of the new data point
	 * @return itself (fluent design)
	 */
	public MarsDoubleDataSet add(final int index, final double[] x,
		final double[] y)
	{
		AssertUtils.notNull("X coordinates", x);
		AssertUtils.notNull("Y coordinates", y);
		final int min = Math.min(x.length, y.length);
		AssertUtils.equalDoubleArrays(x, y, min);

		lock().writeLockGuard(() -> {
			final int indexAt = Math.max(0, Math.min(index, getDataCount() + 1));
			xValues.addElements(indexAt, x, 0, min);
			yValues.addElements(indexAt, y, 0, min);
			getAxisDescription(DIM_X).add(x, min);
			getAxisDescription(DIM_Y).add(y, min);
			getDataLabelMap().shiftKeys(indexAt, xValues.size());
			getDataStyleMap().shiftKeys(indexAt, xValues.size());
		});
		return fireInvalidated(new AddedDataEvent(this));
	}

	/**
	 * clear all data points
	 *
	 * @return itself (fluent design)
	 */
	public MarsDoubleDataSet clearData() {
		lock().writeLockGuard(() -> {
			xValues.clear();
			yValues.clear();
			getDataLabelMap().clear();
			getDataStyleMap().clear();
			clearMetaInfo();

			getAxisDescriptions().forEach(AxisDescription::clear);
		});
		return fireInvalidated(new RemovedDataEvent(this, "clearData()"));
	}

	/**
	 * @return storage capacity of dataset
	 */
	public int getCapacity() {
		return Math.min(xValues.elements().length, yValues.elements().length);
	}

	@Override
	public final double get(final int dimIndex, final int index) {
		if (downsampling && rangeTooLarge()) {
			int newIndex = (int) (index * binSize);
			return dimIndex == DataSet.DIM_X ? xValues.elements()[newIndex] : yValues
				.elements()[newIndex];
		}
		else return dimIndex == DataSet.DIM_X ? xValues.elements()[index] : yValues
			.elements()[index];
	}

	@Override
	public int getDataCount(final int dimIndex) {
		if (downsampling && rangeTooLarge()) {
			AxisRange range = axis.getRange();
			int fullRangePointCount = (int) (maxPointCount * (xValues.getDouble(
				xValues.size() - 1) - xValues.getDouble(0)) / (range.getMax() - range
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
		return dimIndex == DataSet.DIM_X ? xValues.elements() : yValues.elements();
	}

	public void stopDownsampling() {
		this.downsampling = false;
	}

	public void downsample(final MarsNumericAxis axis, final int maxPointCount) {
		this.downsampling = true;
		this.axis = axis;
		this.maxPointCount = maxPointCount;
		this.pointsPerX = xValues.size() / (xValues.getDouble(xValues.size() - 1) -
			xValues.getDouble(0));
	}

	private boolean rangeTooLarge() {
		if (xValues.size() == 0 || Double.isInfinite(pointsPerX) || Double.isNaN(
			pointsPerX)) return false;
		AxisRange range = axis.getRange();
		int pointCount = (int) (pointsPerX * (range.getMax() - range.getMin()));
		if (pointCount > maxPointCount) return true;
		else return false;
	}

	/**
	 * @param amount storage capacity increase
	 * @return itself (fluent design)
	 */
	public MarsDoubleDataSet increaseCapacity(final int amount) {
		lock().writeLockGuard(() -> {
			final int size = getDataCount();
			resize(getCapacity() + amount);
			resize(size);
		});
		return getThis();
	}

	/**
	 * removes sub-range of data points
	 *
	 * @param fromIndex start index
	 * @param toIndex stop index
	 * @return itself (fluent design)
	 */
	public MarsDoubleDataSet remove(final int fromIndex, final int toIndex) {
		lock().writeLockGuard(() -> {
			AssertUtils.indexInBounds(fromIndex, getDataCount(), "fromIndex");
			AssertUtils.indexOrder(fromIndex, "fromIndex", toIndex, "toIndex");

			final int clampedToIndex = Math.min(toIndex, getDataCount());
			xValues.removeElements(fromIndex, clampedToIndex);
			yValues.removeElements(fromIndex, clampedToIndex);

			// remove old label and style keys
			getDataLabelMap().remove(fromIndex, clampedToIndex);
			getDataStyleMap().remove(fromIndex, clampedToIndex);

			// invalidate ranges
			getAxisDescriptions().forEach(AxisDescription::clear);
		});
		return fireInvalidated(new RemovedDataEvent(this));
	}

	/**
	 * ensures minimum size, enlarges if necessary
	 *
	 * @param size the actually used array lengths
	 * @return itself (fluent design)
	 */
	public MarsDoubleDataSet resize(final int size) {
		lock().writeLockGuard(() -> {
			xValues.size(size);
			yValues.size(size);
		});
		return fireInvalidated(new UpdatedDataEvent(this, "increaseCapacity()"));
	}

	/**
	 * clear old data and overwrite with data from 'other' data set (deep copy)
	 *
	 * @param other the source data set
	 * @return itself (fluent design)
	 */
	public MarsDoubleDataSet set(final DataSet other) {
		lock().writeLockGuard(() -> other.lock().writeLockGuard(() -> {
			// copy data
			this.set(other.getValues(DIM_X), other.getValues(DIM_Y), other
				.getDataCount(), true);

			// deep copy data point labels and styles
			getDataLabelMap().clear();
			for (int index = 0; index < other.getDataCount(); index++) {
				final String label = other.getDataLabel(index);
				if (label != null && !label.isEmpty()) {
					this.addDataLabel(index, label);
				}
			}
			getDataStyleMap().clear();
			for (int index = 0; index < other.getDataCount(); index++) {
				final String style = other.getStyle(index);
				if (style != null && !style.isEmpty()) {
					this.addDataStyle(index, style);
				}
			}
			this.setStyle(other.getStyle());

			// synchronise axis description
			for (int dimIndex = 0; dimIndex < getDimension(); dimIndex++) {
				this.getAxisDescription(dimIndex).set(other.getAxisDescription(
					dimIndex));
			}
		}));
		return fireInvalidated(new UpdatedDataEvent(this));
	}

	/**
	 * <p>
	 * Initialises the data set with specified data.
	 * </p>
	 * Note: The method copies values from specified double arrays.
	 *
	 * @param xValues X coordinates
	 * @param yValues Y coordinates
	 * @return itself
	 */
	public MarsDoubleDataSet set(final double[] xValues, final double[] yValues) {
		return set(xValues, yValues, true);
	}

	/**
	 * <p>
	 * Initialises the data set with specified data.
	 * </p>
	 * Note: The method copies values from specified double arrays.
	 *
	 * @param xValues X coordinates
	 * @param yValues Y coordinates
	 * @param copy true: makes an internal copy, false: use the pointer as is
	 *          (saves memory allocation
	 * @return itself
	 */
	public MarsDoubleDataSet set(final double[] xValues, final double[] yValues,
		final boolean copy)
	{
		return set(xValues, yValues, -1, true);
	}

	/**
	 * <p>
	 * Initialises the data set with specified data.
	 * </p>
	 * Note: The method copies values from specified double arrays.
	 *
	 * @param xValues X coordinates
	 * @param yValues Y coordinates
	 * @param nSamples number of samples to be copied
	 * @param copy true: makes an internal copy, false: use the pointer as is
	 *          (saves memory allocation
	 * @return itself
	 */
	public MarsDoubleDataSet set(final double[] xValues, final double[] yValues,
		final int nSamples, final boolean copy)
	{
		AssertUtils.notNull("X coordinates", xValues);
		AssertUtils.notNull("Y coordinates", yValues);
		final int dataMaxIndex = Math.min(xValues.length, yValues.length);
		AssertUtils.equalDoubleArrays(xValues, yValues, dataMaxIndex);
		if (nSamples >= 0) {
			AssertUtils.indexInBounds(nSamples, xValues.length + 1, "xValues bounds");
			AssertUtils.indexInBounds(nSamples, yValues.length + 1, "yValues bounds");
		}
		final int nSamplesToAdd = nSamples >= 0 ? Math.min(nSamples, xValues.length)
			: xValues.length;

		lock().writeLockGuard(() -> {
			getDataLabelMap().clear();
			getDataStyleMap().clear();
			if (copy) {
				if (this.xValues == null) {
					this.xValues = new DoubleArrayList();
				}
				if (this.yValues == null) {
					this.yValues = new DoubleArrayList();
				}
				resize(0);

				this.xValues.addElements(0, xValues, 0, nSamplesToAdd);
				this.yValues.addElements(0, yValues, 0, nSamplesToAdd);
			}
			else {
				this.xValues = DoubleArrayList.wrap(xValues, nSamplesToAdd);
				this.yValues = DoubleArrayList.wrap(yValues, nSamplesToAdd);
			}

			// invalidate ranges
			getAxisDescriptions().forEach(AxisDescription::clear);
		});
		return fireInvalidated(new UpdatedDataEvent(this));
	}

	/**
	 * replaces point coordinate of existing data point
	 *
	 * @param index the index of the data point
	 * @param x new horizontal coordinate
	 * @param y new vertical coordinate N.B. errors are implicitly assumed to be
	 *          zero
	 * @return itself (fluent design)
	 */
	public MarsDoubleDataSet set(final int index, final double x,
		final double y)
	{
		lock().writeLockGuard(() -> {
			final int dataCount = Math.max(index + 1, this.getDataCount());
			xValues.size(dataCount);
			yValues.size(dataCount);
			xValues.elements()[index] = x;
			yValues.elements()[index] = y;
			getDataLabelMap().remove(index);
			getDataStyleMap().remove(index);

			// invalidate ranges
			getAxisDescriptions().forEach(AxisDescription::clear);
		});
		return fireInvalidated(new UpdatedDataEvent(this, "set - single"));
	}

	public MarsDoubleDataSet set(final int index, final double[] x,
		final double[] y)
	{
		lock().writeLockGuard(() -> {
			resize(Math.max(index + x.length, xValues.size()));
			System.arraycopy(x, 0, xValues.elements(), index, x.length);
			System.arraycopy(y, 0, yValues.elements(), index, y.length);
			getDataLabelMap().remove(index, index + x.length);
			getDataStyleMap().remove(index, index + x.length);

			// invalidate ranges
			getAxisDescriptions().forEach(AxisDescription::clear);
		});
		return fireInvalidated(new UpdatedDataEvent(this, "set - via arrays"));
	}

	/**
	 * Trims the arrays list so that the capacity is equal to the size.
	 *
	 * @see java.util.ArrayList#trimToSize()
	 * @return itself (fluent design)
	 */
	public MarsDoubleDataSet trim() {
		lock().writeLockGuard(() -> {
			xValues.trim(0);
			yValues.trim(0);
		});
		return fireInvalidated(new UpdatedDataEvent(this, "increaseCapacity()"));
	}
}
