package de.mpg.biochem.mars.fx.plot.tools;

import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.event.RemovedDataEvent;
import de.gsi.dataset.event.UpdatedDataEvent;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

/**
 * Implementation of the {@code DataSetWithSegments} interface which stores x,y, values and segments in separate double arrays. 
 * It provides methods allowing easily manipulate of data points. Based on DoubleDataSetWithSegments but modified for segments.
 *
 * @author Karl Duderstadt
 */
public class DoubleDataSetWithSegments extends AbstractSegmentDataSet<DoubleDataSetWithSegments> {
	    protected DoubleArrayList xValues; // way faster than java default lists
	    protected DoubleArrayList yValues; // way faster than java default lists
	    
	    //These contain a list of x and y points
	    //the first segment will start at index 0 and end at index 1
	    //the second segment will start at index 2 and end at index 3
	    //and so on...
	    protected DoubleArrayList xSegmentValues;
	    protected DoubleArrayList ySegmentValues;

	    public DoubleDataSetWithSegments(final String name) {
	        this(name, 0, 0);
	    }

	    public DoubleDataSetWithSegments(final String name, final int initialValuesSize, final int initialSegmentsSize) {
	        super(name);
	        xValues = new DoubleArrayList(initialValuesSize);
	        yValues = new DoubleArrayList(initialValuesSize);
	        xSegmentValues = new DoubleArrayList(initialSegmentsSize);
	        ySegmentValues = new DoubleArrayList(initialSegmentsSize);
	    }

	    public DoubleDataSetWithSegments(final String name, final double[] xValues, final double[] yValues,
	            final double[] xSegmentValues, final double[] ySegmentValues, final int initialValuesSize, final int initialSegmentsSize, boolean deepCopy) {
	        super(name);

	        if (deepCopy) {
	            this.xValues = new DoubleArrayList(initialValuesSize);
	            this.yValues = new DoubleArrayList(initialValuesSize);
	            this.xSegmentValues = new DoubleArrayList(initialSegmentsSize);
	            this.ySegmentValues = new DoubleArrayList(initialSegmentsSize);
	            this.resize(initialValuesSize);
	            this.resizeSegments(initialSegmentsSize);
	            System.arraycopy(xValues, 0, this.xValues.elements(), 0, xValues.length);
	            System.arraycopy(yValues, 0, this.yValues.elements(), 0, yValues.length);
	            System.arraycopy(xSegmentValues, 0, this.xSegmentValues.elements(), 0, xSegmentValues.length);
	            System.arraycopy(ySegmentValues, 0, this.ySegmentValues.elements(), 0, ySegmentValues.length);
	        } else {
	            this.xValues = DoubleArrayList.wrap(xValues);
	            this.yValues = DoubleArrayList.wrap(yValues);
	            this.xSegmentValues = DoubleArrayList.wrap(xSegmentValues);
	            this.ySegmentValues = DoubleArrayList.wrap(ySegmentValues);
	        }
	    }

	    @Override
	    public double[] getXValues() {
	        return xValues.elements();
	    }

	    @Override
	    public double[] getYValues() {
	        return yValues.elements();
	    }

	    @Override
	    public double[] getXSegmentValues() {
	        return xSegmentValues.elements();
	    }

	    @Override
	    public double[] getYSegmentValues() {
	        return ySegmentValues.elements();
	    }

	    @Override
	    public int getDataCount() {
	        return Math.min(xValues.size(), yValues.size());
	    }
	    
	    public int getSegmentsDataCount() {
	    	return Math.min(xSegmentValues.size(), ySegmentValues.size());
	    }

	    /**
	     * clears all data
	     * 
	     * @return itself (fluent design)
	     */
	    public DoubleDataSetWithSegments clearData() {
	        lock();

	        xValues.clear();
	        yValues.clear();
	        xSegmentValues.clear();
	        ySegmentValues.clear();
	        dataLabels.clear();
	        dataStyles.clear();
	        clearMetaInfo();

	        xRange.empty();
	        yRange.empty();

	        return unlock().fireInvalidated(new RemovedDataEvent(this, "clearData()"));
	    }

	    /**
	     * 
	     * @return storage capacity of dataset
	     */
	    public int getCapacity() {
	        return Math.min(xValues.elements().length, yValues.elements().length);
	    }
	    
	    public int getSegmentsCapacity() {
	    	return Math.min(xSegmentValues.elements().length, ySegmentValues.elements().length);
	    }

	    /**
	     * 
	     * @param amount storage capacity increase
	     * @return itself (fluent design)
	     */
	    public DoubleDataSetWithSegments increaseCapacity(final int valuesAmount) {
	        lock();
	        final int size = getDataCount();
	        final boolean auto = isAutoNotification();
	        this.setAutoNotifaction(false);
	        resize(this.getCapacity() + valuesAmount);
	        resize(size);
	        this.setAutoNotifaction(auto);
	        return unlock();
	    }
	    
	    public DoubleDataSetWithSegments increaseSegmentCapacity(final int segmentsAmount) {
	    	lock();
	        final int size = getSegmentsDataCount();
	        final boolean auto = isAutoNotification();
	        this.setAutoNotifaction(false);
	        resizeSegments(this.getSegmentsCapacity() + segmentsAmount);
	        resizeSegments(size);
	        this.setAutoNotifaction(auto);
	        return unlock();
	    }

	    /**
	     * ensures minimum size, enlarges if necessary
	     * 
	     * @param size the actually used array lengths
	     * @return itself (fluent design)
	     */
	    public DoubleDataSetWithSegments resize(final int valuesSize) {
	        lock();
	        xValues.size(valuesSize);
	        yValues.size(valuesSize);
	        return unlock().fireInvalidated(new UpdatedDataEvent(this, "increaseCapacity()"));
	    }
	    
	    public DoubleDataSetWithSegments resizeSegments(final int segmentsSize) {
	        lock();
	        xSegmentValues.size(segmentsSize);
	        ySegmentValues.size(segmentsSize);
	        return unlock().fireInvalidated(new UpdatedDataEvent(this, "increaseCapacity()"));
	    }

	    /**
	     * Trims the arrays list so that the capacity is equal to the size.
	     *
	     * @see java.util.ArrayList#trimToSize()
	     * @return itself (fluent design)
	     */
	    public DoubleDataSetWithSegments trim() {
	        lock();
	        xValues.trim(0);
	        yValues.trim(0);
	        xSegmentValues.trim(0);
	        ySegmentValues.trim(0);
	        return unlock().fireInvalidated(new UpdatedDataEvent(this, "increaseCapacity()"));
	    }

	    @Override
	    public double getX(final int index) {
	        return xValues.elements()[index];
	    }

	    @Override
	    public double getY(final int index) {
	        return yValues.elements()[index];
	    }
	    
	    @Override
	    public double getSegmentX(final int index) {
	    	return xSegmentValues.elements()[index];
	    }
	    
	    @Override
	    public double getSegmentY(final int index) {
	    	return ySegmentValues.elements()[index];
	    }

	    /**
	     * Add point to the data set.
	     *
	     * @param x the new x coordinate
	     * @param y the new y coordinate
	     * @param yErrorNeg the +dy error
	     * @param yErrorPos the -dy error
	     * @return itself (fluent design)
	     */
	    public DoubleDataSetWithSegments add(final double x, final double y) {
	        return add(x, y, null);
	    }

	    /**
	     * Add point to the data set.
	     *
	     * @param x the new x coordinate
	     * @param y the new y coordinate
	     * @param label the data label
	     * @return itself (fluent design)
	     */
	    public DoubleDataSetWithSegments add(final double x, final double y, final String label) {
	        lock();
	        xValues.add(x);
	        yValues.add(y);

	        if (label != null && !label.isEmpty()) {
	            addDataLabel(xValues.size() - 1, label);
	        }

	        xRange.add(x);
	        yRange.add(y);

	        return unlock().fireInvalidated(new UpdatedDataEvent(this, "add"));
	    }

	    public DoubleDataSetWithSegments addSegment(final double x1, final double y1, final double x2, final double y2) {
	    	lock();
	        xSegmentValues.add(x1);
	        ySegmentValues.add(y1);
	        xSegmentValues.add(x2);
	        ySegmentValues.add(y2);

	        return unlock().fireInvalidated(new UpdatedDataEvent(this, "add"));
	    }

	    public DoubleDataSetWithSegments add(final double[] xValuesNew, final double[] yValuesNew) {
	        lock();

	        xValues.addElements(xValues.size(), xValuesNew);
	        yValues.addElements(yValues.size(), yValuesNew);

	        xRange.add(xValuesNew);
	        yRange.add(yValuesNew);

	        return unlock().fireInvalidated(new AddedDataEvent(this));
	    }
	    
	    public DoubleDataSetWithSegments addSegments(final double[] xSegmentValuesNew, final double[] ySegmentValuesNew) {
	        lock();

	        xSegmentValues.addElements(xValues.size(), xSegmentValuesNew);
	        ySegmentValues.addElements(yValues.size(), ySegmentValuesNew);

	        return unlock().fireInvalidated(new AddedDataEvent(this));
	    }
}