package de.mpg.biochem.mars.fx.plot.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.gsi.chart.XYChartCss;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.RendererDataReducer;
import de.gsi.chart.renderer.spi.utils.Cache;
import de.gsi.chart.utils.StyleParser;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.DataSetError.ErrorType;
import de.gsi.dataset.utils.ProcessingProfiler;
import de.gsi.math.ArrayUtils;

/**
 * Just a copy from de.gsi.chart.renderer.spi so it can be used in custom renderers. Not sure why it was private.
 * 
 * package private class implementation (data point caching) required by ErrorDataSetRenderer
 * local screen data point cache (minimises re-allocation/garbage collection)
 * 
 * @author rstein
 */
@SuppressWarnings({ "PMD.TooManyMethods", "PMD.TooManyFields" }) // designated purpose of this class
public class CachedDataPoints {
    private static final String STYLES2 = "styles";
    private static final String SELECTED2 = "selected";
    private static final String ERROR_X_POS = "errorXPos";
    private static final String ERROR_X_NEG = "errorXNeg";
    private static final String ERROR_Y_POS = "errorYPos";
    private static final String ERROR_Y_NEG = "errorYNeg";
    private static final String Y_VALUES = "yValues";
    private static final String X_VALUES = "xValues";
    private static final double DEG_TO_RAD = Math.PI / 180.0;
    private static final int MAX_THREADS = Math.max(4, Runtime.getRuntime().availableProcessors());
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(2 * MAX_THREADS);

    protected double[] xValues;
    protected double[] yValues;
    protected double[] errorXNeg;
    protected double[] errorXPos;
    protected double[] errorYNeg;
    protected double[] errorYPos;
    protected boolean[] selected;
    protected String[] styles;
    protected boolean xAxisInverted;
    protected boolean yAxisInverted;
    protected String defaultStyle;
    protected int dataSetIndex;
    protected int dataSetStyleIndex;
    protected ErrorType errorType;
    protected int indexMin;
    protected int indexMax;
    protected int minDistanceX = +Integer.MAX_VALUE;
    protected double xZero; // reference zero 'x' axis coordinate
    protected double yZero; // reference zero 'y' axis coordinate
    protected double yMin;
    protected double yMax;
    protected double xMin;
    protected double xMax;
    protected boolean polarPlot;
    protected double xRange;
    protected double yRange;
    protected double maxRadius;
    protected int maxDataCount;
    protected int actualDataCount; // number of data points that remain
                                   // after data reduction

    public CachedDataPoints(final int indexMin, final int indexMax, final int dataLength, final boolean full) {
        maxDataCount = dataLength;
        xValues = Cache.getCachedDoubleArray(X_VALUES, maxDataCount);
        yValues = Cache.getCachedDoubleArray(Y_VALUES, maxDataCount);
        styles = Cache.getCachedStringArray(STYLES2, dataLength);
        this.indexMin = indexMin;
        this.indexMax = indexMax;
        errorYNeg = Cache.getCachedDoubleArray(ERROR_Y_NEG, maxDataCount);
        errorYPos = Cache.getCachedDoubleArray(ERROR_Y_POS, maxDataCount);
        if (full) {
            errorXNeg = Cache.getCachedDoubleArray(ERROR_X_NEG, maxDataCount);
            errorXPos = Cache.getCachedDoubleArray(ERROR_X_POS, maxDataCount);
        }
        selected = Cache.getCachedBooleanArray(SELECTED2, dataLength);
        // ArrayUtils.fillArray(selected, true);
        ArrayUtils.fillArray(styles, null);
    }

    /**
     * computes the minimum distance in between data points N.B. assumes sorted data set points
     * 
     * @return min distance
     */
    protected int getMinXDistance() {
        if (minDistanceX < Integer.MAX_VALUE) {
            return minDistanceX;
        }

        if (indexMin >= indexMax) {
            minDistanceX = 1;
            return minDistanceX;
        }

        minDistanceX = Integer.MAX_VALUE;
        for (int i = 1; i < actualDataCount; i++) {
            final double x0 = xValues[i - 1];
            final double x1 = xValues[i];
            minDistanceX = Math.min(minDistanceX, (int) Math.abs(x1 - x0));
        }
        return minDistanceX;
    }

    protected void computeScreenCoordinates(final Axis xAxis, final Axis yAxis, final DataSet dataSet,
            final int dsIndex, final int min, final int max, final ErrorStyle errorStyle, final boolean isPolarPlot) {
        setBoundaryConditions(xAxis, yAxis, dataSet, dsIndex, min, max, errorStyle, isPolarPlot);

        // compute data set to screen coordinates
        computeScreenCoordinatesNonThreaded(xAxis, yAxis, dataSet, min, max);
    }

    protected void computeScreenCoordinatesInParallel(final Axis xAxis, final Axis yAxis, final DataSet dataSet,
            final int dsIndex, final int min, final int max, final ErrorStyle errorStyle, final boolean isPolarPlot) {
        setBoundaryConditions(xAxis, yAxis, dataSet, dsIndex, min, max, errorStyle, isPolarPlot);

        // compute data set to screen coordinates       
        computeScreenCoordinatesParallel(xAxis, yAxis, dataSet, min, max);
    }

    private void setBoundaryConditions(final Axis xAxis, final Axis yAxis, final DataSet dataSet, final int dsIndex,
            final int min, final int max, final ErrorStyle errorStyle, final boolean isPolarPlot) {
        indexMin = min;
        indexMax = max;
        polarPlot = isPolarPlot;

        computeBoundaryVariables(xAxis, yAxis);
        setStyleVariable(dataSet, dsIndex);
        setErrorType(dataSet, errorStyle);
    }

    protected void computeBoundaryVariables(final Axis xAxis, final Axis yAxis) {
        xAxisInverted = xAxis.isInvertedAxis();
        yAxisInverted = yAxis.isInvertedAxis();

        // compute cached axis variables ... about 50% faster than the
        // generic template based version from the original ValueAxis<Number>
        if (xAxis.isLogAxis()) {
            xZero = xAxis.getDisplayPosition(xAxis.getLowerBound());
        } else {
            xZero = xAxis.getDisplayPosition(0);
        }
        if (yAxis.isLogAxis()) {
            yZero = yAxis.getDisplayPosition(yAxis.getLowerBound());
        } else {
            yZero = yAxis.getDisplayPosition(0);
        }

        yMin = yAxis.getDisplayPosition(yAxis.getLowerBound());
        yMax = yAxis.getDisplayPosition(yAxis.getUpperBound());
        xMin = xAxis.getDisplayPosition(xAxis.getLowerBound());
        xMax = xAxis.getDisplayPosition(xAxis.getUpperBound());

        xRange = Math.abs(xMax - xMin);
        yRange = Math.abs(yMax - yMin);
        maxRadius = 0.5 * Math.max(Math.min(xRange, yRange), 20) * 0.9;
        // TODO: parameterise '0.9' -> radius axis fills 90% of min canvas
        // axis
        if (polarPlot) {
            xZero = 0.5 * xRange;
            yZero = 0.5 * yRange;
        }
    }

    protected void setStyleVariable(final DataSet dataSet, final int dsIndex) {
        dataSet.lock();
        defaultStyle = dataSet.getStyle();
        final Integer layoutOffset = StyleParser.getIntegerPropertyValue(defaultStyle,
                XYChartCss.DATASET_LAYOUT_OFFSET);
        final Integer dsIndexLocal = StyleParser.getIntegerPropertyValue(defaultStyle, XYChartCss.DATASET_INDEX);

        dataSetStyleIndex = layoutOffset == null ? 0 : layoutOffset.intValue();
        dataSetIndex = dsIndexLocal == null ? dsIndex : dsIndexLocal.intValue();
        dataSet.unlock();
    }

    protected void setErrorType(final DataSet dataSet, final ErrorStyle errorStyle) {
        dataSet.lock();
        // compute screen coordinates of other points
        if (dataSet instanceof DataSetError) {
            final DataSetError ds = (DataSetError) dataSet;
            errorType = ds.getErrorType();
        } else {
            // fall-back for standard DataSet

            // default: ErrorType=Y fall-back also for 'DataSet' without
            // errors
            // rationale: scientific honesty
            // if data points are being compressed, the error of compression
            // (e.g. due to local transients that are being suppressed) are
            // nevertheless being computed and shown even if individual data
            // points have no error
            errorType = ErrorType.Y;
        }

        // special case where users does not want error bars
        if (errorStyle == ErrorStyle.NONE) {
            errorType = ErrorType.NO_ERROR;
        }
        dataSet.unlock();
    }

    protected void computeScreenCoordinatesParallel(final Axis xAxis, final Axis yAxis, final DataSet dataSet,
            final int min, final int max) {
        final int minthreshold = 1000;
        int divThread = (int) Math.ceil(Math.abs(max - min) / (double) MAX_THREADS);
        int stepSize = Math.max(divThread, minthreshold);
        final List<Callable<Boolean>> workers = new ArrayList<>();
        for (int i = min; i < max; i += stepSize) {
            final int start = i;
            workers.add(() -> {
                if (polarPlot) {
                    computeScreenCoordinatesPolar(yAxis, dataSet, start, Math.min(max, start + stepSize));
                } else {
                    computeScreenCoordinatesEuclidean(xAxis, yAxis, dataSet, start, Math.min(max, start + stepSize));
                }
                return Boolean.TRUE;
            });
        }

        try {
            final List<Future<Boolean>> jobs = EXECUTOR_SERVICE.invokeAll(workers);
            for (final Future<Boolean> future : jobs) {
                final Boolean r = future.get();
                if (!r) {
                    throw new IllegalStateException("one parallel worker thread finished execution with error");
                }
            }
        } catch (final InterruptedException | ExecutionException e) {
            throw new IllegalStateException("one parallel worker thread finished execution with error", e);
        }
    }

    protected void computeScreenCoordinatesNonThreaded(final Axis xAxis, final Axis yAxis, final DataSet dataSet,
            final int min, final int max) {
        if (polarPlot) {
            computeScreenCoordinatesPolar(yAxis, dataSet, min, max);
        } else {
            computeScreenCoordinatesEuclidean(xAxis, yAxis, dataSet, min, max);
        }
    }

    private void computeNoError(final Axis xAxis, final Axis yAxis, final DataSet dataSet, final int min,
            final int max) {
        // no error attached
        dataSet.lock();
        for (int index = min; index < max; index++) {
            final double x = dataSet.getX(index);
            final double y = dataSet.getY(index);
            // check if error should be surrounded by Math.abs(..)
            // to ensure that they are always positive
            xValues[index] = xAxis.getDisplayPosition(x);
            yValues[index] = yAxis.getDisplayPosition(y);
            if (!Double.isFinite(yValues[index])) {
                yValues[index] = yMin;
            }
            styles[index] = dataSet.getStyle(index);
        }
        dataSet.unlock();
    }

    private void computeNoErrorPolar(final Axis yAxis, final DataSet dataSet, final int min, final int max) {
        // experimental transform euclidean to polar coordinates
        dataSet.lock();
        for (int index = min; index < max; index++) {
            final double x = dataSet.getX(index);
            final double y = dataSet.getY(index);
            // check if error should be surrounded by Math.abs(..)
            // to ensure that they are always positive
            final double phi = x * DEG_TO_RAD;
            final double r = maxRadius * Math.abs(1 - yAxis.getDisplayPosition(y) / yRange);
            xValues[index] = xZero + r * Math.cos(phi);
            yValues[index] = yZero + r * Math.sin(phi);

            if (!Double.isFinite(yValues[index])) {
                yValues[index] = yZero;
            }
            styles[index] = dataSet.getStyle(index);
        }
        dataSet.unlock();
    }

    private void computeYonly(final Axis xAxis, final Axis yAxis, final DataSet dataSet, final int min, final int max) {
        if (dataSet instanceof DataSetError) {
            final DataSetError ds = (DataSetError) dataSet;
            dataSet.lock();
            for (int index = min; index < max; index++) {
                final double x = dataSet.getX(index);
                final double y = dataSet.getY(index);
                // check if error should be surrounded by
                // Math.abs(..)
                // to ensure that they are always positive
                xValues[index] = xAxis.getDisplayPosition(x);
                yValues[index] = yAxis.getDisplayPosition(y);
                if (Double.isFinite(yValues[index])) {
                    errorYNeg[index] = yAxis.getDisplayPosition(y - ds.getYErrorNegative(index));
                    errorYPos[index] = yAxis.getDisplayPosition(y + ds.getYErrorPositive(index));
                } else {
                    yValues[index] = yMin;
                    errorYNeg[index] = yMin;
                    errorYPos[index] = yMin;
                }
                styles[index] = dataSet.getStyle(index);
            }
            dataSet.unlock();
            return;
        }

        // default dataset
        dataSet.lock();
        for (int index = min; index < max; index++) {
            final double x = dataSet.getX(index);
            final double y = dataSet.getY(index);
            // check if error should be surrounded by Math.abs(..)
            // to ensure that they are always positive
            xValues[index] = xAxis.getDisplayPosition(x);
            yValues[index] = yAxis.getDisplayPosition(y);

            if (!Double.isFinite(xValues[index])) {
                xValues[index] = xMin;
            }
            if (Double.isFinite(yValues[index])) {
                errorYNeg[index] = yValues[index];
                errorYPos[index] = yValues[index];
            } else {
                yValues[index] = yMin;
                errorYNeg[index] = yMin;
                errorYPos[index] = yMin;
            }
            styles[index] = dataSet.getStyle(index);
        }
        dataSet.unlock();
    }

    private void computeYonlyPolar(final Axis yAxis, final DataSet dataSet, final int min, final int max) {

        dataSet.lock();
        for (int index = min; index < max; index++) {
            final double x = dataSet.getX(index);
            final double y = dataSet.getY(index);
            // check if error should be surrounded by Math.abs(..)
            // to ensure that they are always positive
            final double phi = x * DEG_TO_RAD;
            final double r = maxRadius * Math.abs(1 - yAxis.getDisplayPosition(y) / yRange);
            xValues[index] = xZero + r * Math.cos(phi);
            yValues[index] = yZero + r * Math.sin(phi);

            // ignore errors (for now) -> TODO: add proper transformation
            errorXNeg[index] = 0.0;
            errorXPos[index] = 0.0;
            errorYNeg[index] = 0.0;
            errorYPos[index] = 0.0;

            if (!Double.isFinite(yValues[index])) {
                yValues[index] = yZero;
            }
            styles[index] = dataSet.getStyle(index);
        }
        dataSet.unlock();
    }

    private void computeFull(final Axis xAxis, final Axis yAxis, final DataSetError dataSet, final int min,
            final int max) {
        dataSet.lock();
        for (int index = min; index < max; index++) {
            final double x = dataSet.getX(index);
            final double y = dataSet.getY(index);
            // check if error should be surrounded by
            // Math.abs(..) to ensure that they are always positive
            xValues[index] = xAxis.getDisplayPosition(x);
            yValues[index] = yAxis.getDisplayPosition(y);

            if (Double.isFinite(xValues[index])) {
                errorXNeg[index] = xAxis.getDisplayPosition(x - dataSet.getXErrorNegative(index));
                errorXPos[index] = xAxis.getDisplayPosition(x + dataSet.getXErrorPositive(index));
            } else {
                xValues[index] = xMin;
                errorXNeg[index] = xMin;
                errorXPos[index] = xMin;
            }

            if (Double.isFinite(yValues[index])) {
                errorYNeg[index] = yAxis.getDisplayPosition(y - dataSet.getYErrorNegative(index));
                errorYPos[index] = yAxis.getDisplayPosition(y + dataSet.getYErrorPositive(index));
            } else {
                yValues[index] = yMin;
                errorYNeg[index] = yMin;
                errorYPos[index] = yMin;
            }
            styles[index] = dataSet.getStyle(index);
        }
        dataSet.unlock();
    }

    private void computeFullPolar(final Axis yAxis, final DataSetError dataSet, final int min, final int max) {
        dataSet.lock();
        for (int index = min; index < max; index++) {
            final double x = dataSet.getX(index);
            final double y = dataSet.getY(index);
            // check if error should be surrounded by Math.abs(..)
            // to ensure that they are always positive
            final double phi = x * DEG_TO_RAD;
            final double r = maxRadius * Math.abs(1 - yAxis.getDisplayPosition(y) / yRange);
            xValues[index] = xZero + r * Math.cos(phi);
            yValues[index] = yZero + r * Math.sin(phi);

            // ignore errors (for now) -> TODO: add proper
            // transformation
            errorXNeg[index] = 0.0;
            errorXPos[index] = 0.0;
            errorYNeg[index] = 0.0;
            errorYPos[index] = 0.0;

            if (!Double.isFinite(yValues[index])) {
                yValues[index] = yZero;
            }
            styles[index] = dataSet.getStyle(index);
        }
        dataSet.unlock();
    }

    private void computeScreenCoordinatesEuclidean(final Axis xAxis, final Axis yAxis, final DataSet dataSet,
            final int min, final int max) {
        switch (errorType) {
        case NO_ERROR:
            computeNoError(xAxis, yAxis, dataSet, min, max);
            return;
        case Y:
        case Y_ASYMMETRIC:
            computeYonly(xAxis, yAxis, dataSet, min, max);
            return;
        case X:
        case X_ASYMMETRIC:
        case XY:
        case XY_ASYMMETRIC:
        default:
            // dataSet may not be non-DataSetError at this stage
            final DataSetError ds = (DataSetError) dataSet;
            computeFull(xAxis, yAxis, ds, min, max);
            return;
        }
    }

    private void computeScreenCoordinatesPolar(final Axis yAxis, final DataSet dataSet, final int min, final int max) {
        switch (errorType) {
        case NO_ERROR:
            computeNoErrorPolar(yAxis, dataSet, min, max);
            return;
        case Y:
        case Y_ASYMMETRIC:
            computeYonlyPolar(yAxis, dataSet, min, max);
            return;
        case X:
        case X_ASYMMETRIC:
        case XY:
        case XY_ASYMMETRIC:
        default:
            // dataSet may not be non-DataSetError at this stage
            final DataSetError ds = (DataSetError) dataSet;
            computeFullPolar(yAxis, ds, min, max);
            return;
        }
    }

    private int minDataPointDistanceX() {
        if (actualDataCount <= 1) {
            minDistanceX = 1;
            return minDistanceX;
        }
        minDistanceX = Integer.MAX_VALUE;
        for (int i = 1; i < actualDataCount; i++) {
            final double x0 = xValues[i - 1];
            final double x1 = xValues[i];
            minDistanceX = Math.min(minDistanceX, (int) Math.abs(x1 - x0));
        }
        return minDistanceX;
    }

    protected void reduce(final RendererDataReducer cruncher, final boolean isReducePoints,
            final int minRequiredReductionSize) {
        final long startTimeStamp = ProcessingProfiler.getTimeStamp();
        actualDataCount = 1;

        if (!isReducePoints || Math.abs(indexMax - indexMin) < minRequiredReductionSize) {
            actualDataCount = indexMax - indexMin;
            System.arraycopy(xValues, indexMin, xValues, 0, actualDataCount);
            System.arraycopy(yValues, indexMin, yValues, 0, actualDataCount);
            System.arraycopy(selected, indexMin, selected, 0, actualDataCount);
            switch (errorType) {
            case NO_ERROR: // no error attached
                break;
            case Y: // only symmetric errors around y
            case Y_ASYMMETRIC: // asymmetric errors around y
                System.arraycopy(errorYNeg, indexMin, errorYNeg, 0, actualDataCount);
                System.arraycopy(errorYPos, indexMin, errorYPos, 0, actualDataCount);
                break;
            case XY: // symmetric errors around x and y
            case X: // only symmetric errors around x
            case X_ASYMMETRIC: // asymmetric errors around x
            default:
                System.arraycopy(errorXNeg, indexMin, errorXNeg, 0, actualDataCount);
                System.arraycopy(errorXPos, indexMin, errorXPos, 0, actualDataCount);
                System.arraycopy(errorYNeg, indexMin, errorYNeg, 0, actualDataCount);
                System.arraycopy(errorYPos, indexMin, errorYPos, 0, actualDataCount);
                break;
            }

            ProcessingProfiler.getTimeDiff(startTimeStamp, String.format("no data reduction (%d)", actualDataCount));
            return;
        }

        switch (errorType) {
        case NO_ERROR: // see comment above
        case Y:
            actualDataCount = cruncher.reducePoints(xValues, yValues, null, null, errorYPos, errorYNeg, styles,
                    selected, indexMin, indexMax);
            minDataPointDistanceX();
            break;
        case X:
        case XY:
        default:
            actualDataCount = cruncher.reducePoints(xValues, yValues, errorXPos, errorXNeg, errorYPos, errorYNeg,
                    styles, selected, indexMin, indexMax);

            minDataPointDistanceX();
            break;
        }
        // ProcessingProfiler.getTimeDiff(startTimeStamp,
    }

    //    @Override
    //    protected void finalize() throws Throwable {
    //        release();
    //        super.finalize();
    //    }

    public void release() {
        Cache.release(X_VALUES, xValues);
        Cache.release(Y_VALUES, yValues);
        Cache.release(ERROR_Y_NEG, errorYNeg);
        Cache.release(ERROR_Y_POS, errorYPos);
        Cache.release(ERROR_X_NEG, errorXNeg);
        Cache.release(ERROR_X_POS, errorXPos);
        Cache.release(SELECTED2, selected);
        Cache.release(STYLES2, styles);
    }
}
