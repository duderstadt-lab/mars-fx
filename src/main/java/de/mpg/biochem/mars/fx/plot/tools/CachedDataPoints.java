/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2021 Karl Duderstadt
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

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import de.gsi.chart.XYChartCss;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.RendererDataReducer;
import de.gsi.dataset.utils.ArrayCache;
import de.gsi.chart.utils.StyleParser;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet2D;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.DataSetError.ErrorType;
import de.gsi.dataset.utils.CachedDaemonThreadFactory;
import de.gsi.dataset.utils.ProcessingProfiler;
import de.gsi.math.ArrayUtils;

/**
 * Just a copy from de.gsi.chart.renderer.spi so it can be used in custom renderers. Not sure why it was private.
 * 
 * package private class implementation (data point caching) required by ErrorDataSetRenderer local screen data point
 * cache (minimises re-allocation/garbage collection)
 *
 * @author rstein
 */
@SuppressWarnings({ "PMD.TooManyMethods", "PMD.TooManyFields" }) // designated purpose of this class
class CachedDataPoints {
    private static final String STYLES2 = "styles";
    private static final String SELECTED2 = "selected";
    private static final String ERROR_X_POS = "errorXPos";
    private static final String ERROR_X_NEG = "errorXNeg";
    private static final String ERROR_Y_POS = "errorYPos";
    private static final String ERROR_Y_NEG = "errorYNeg";
    private static final String Y_VALUES = "yValues";
    private static final String X_VALUES = "xValues";
    private static final double DEG_TO_RAD = Math.PI / 180.0;

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
    protected boolean allowForNaNs;
    protected ErrorType[] errorType;
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
    protected ErrorStyle rendererErrorStyle;
    protected double xRange;
    protected double yRange;
    protected double maxRadius;
    protected int maxDataCount;
    protected int actualDataCount; // number of data points that remain
                                   // after data reduction

    public CachedDataPoints(final int indexMin, final int indexMax, final int dataLength, final boolean full) {
        maxDataCount = dataLength;
        xValues = ArrayCache.getCachedDoubleArray(X_VALUES, maxDataCount);
        yValues = ArrayCache.getCachedDoubleArray(Y_VALUES, maxDataCount);
        styles = ArrayCache.getCachedStringArray(STYLES2, dataLength);
        this.indexMin = indexMin;
        this.indexMax = indexMax;
        errorYNeg = ArrayCache.getCachedDoubleArray(ERROR_Y_NEG, maxDataCount);
        errorYPos = ArrayCache.getCachedDoubleArray(ERROR_Y_POS, maxDataCount);
        if (full) {
            errorXNeg = ArrayCache.getCachedDoubleArray(ERROR_X_NEG, maxDataCount);
            errorXPos = ArrayCache.getCachedDoubleArray(ERROR_X_POS, maxDataCount);
        }
        selected = ArrayCache.getCachedBooleanArray(SELECTED2, dataLength);
        ArrayUtils.fillArray(styles, null);
    }

    protected void computeBoundaryVariables(final Axis xAxis, final Axis yAxis) {
        xAxisInverted = xAxis.isInvertedAxis();
        yAxisInverted = yAxis.isInvertedAxis();

        // compute cached axis variables ... about 50% faster than the
        // generic template based version from the original ValueAxis<Number>
        if (xAxis.isLogAxis()) {
            xZero = xAxis.getDisplayPosition(xAxis.getMin());
        } else {
            xZero = xAxis.getDisplayPosition(0);
        }
        if (yAxis.isLogAxis()) {
            yZero = yAxis.getDisplayPosition(yAxis.getMin());
        } else {
            yZero = yAxis.getDisplayPosition(0);
        }

        yMin = yAxis.getDisplayPosition(yAxis.getMin());
        yMax = yAxis.getDisplayPosition(yAxis.getMax());
        xMin = xAxis.getDisplayPosition(xAxis.getMin());
        xMax = xAxis.getDisplayPosition(xAxis.getMax());

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

    private void computeErrorStyles(final DataSet dataSet, final int min, final int max) {
        // no error attached
        dataSet.lock().readLockGuardOptimistic(() -> {
            for (int index = min; index < max; index++) {
                styles[index] = dataSet.getStyle(index);
            }
        });
    }

    private void computeFullPolar(final Axis yAxis, final DataSetError dataSet, final int min, final int max) {
        dataSet.lock().readLockGuardOptimistic(() -> {
            for (int index = min; index < max; index++) {
                final double x = dataSet.get(DIM_X, index);
                final double y = dataSet.get(DIM_Y, index);
                // check if error should be surrounded by Math.abs(..)
                // to ensure that they are always positive
                final double phi = x * DEG_TO_RAD;
                final double r = maxRadius * Math.abs(1 - (yAxis.getDisplayPosition(y) / yRange));
                xValues[index] = xZero + (r * Math.cos(phi));
                yValues[index] = yZero + (r * Math.sin(phi));

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
        });
    }

    private void computeNoErrorPolar(final Axis yAxis, final DataSet2D dataSet, final int min, final int max) {
        // experimental transform euclidean to polar coordinates
        dataSet.lock().readLockGuardOptimistic(() -> {
            for (int index = min; index < max; index++) {
                final double x = dataSet.getX(index);
                final double y = dataSet.getY(index);
                // check if error should be surrounded by Math.abs(..)
                // to ensure that they are always positive
                final double phi = x * DEG_TO_RAD;
                final double r = maxRadius * Math.abs(1 - (yAxis.getDisplayPosition(y) / yRange));
                xValues[index] = xZero + (r * Math.cos(phi));
                yValues[index] = yZero + (r * Math.sin(phi));

                if (!Double.isFinite(yValues[index])) {
                    yValues[index] = yZero;
                }
                styles[index] = dataSet.getStyle(index);
            }
        });
    }

    protected void computeScreenCoordinates(final Axis xAxis, final Axis yAxis, final DataSet dataSet,
            final int dsIndex, final int min, final int max, final ErrorStyle localRendErrorStyle,
            final boolean isPolarPlot, final boolean doAllowForNaNs) {
        setBoundaryConditions(xAxis, yAxis, dataSet, dsIndex, min, max, localRendErrorStyle, isPolarPlot,
                doAllowForNaNs);

        // compute data set to screen coordinates
        computeScreenCoordinatesNonThreaded(xAxis, yAxis, dataSet, min, max);
    }

    private void computeScreenCoordinatesEuclidean(final Axis xAxis, final Axis yAxis, final DataSet dataSet,
            final int min, final int max) {
        for (int dimIndex = 0; dimIndex < 2; dimIndex++) {
            switch (errorType[dimIndex]) {
            case NO_ERROR:
                if (allowForNaNs) {
                    computeWithNoErrorAllowingNaNs(dimIndex == DIM_X ? xAxis : yAxis, dataSet, dimIndex, min, max);
                } else {
                    computeWithNoError(dimIndex == DIM_X ? xAxis : yAxis, dataSet, dimIndex, min, max);
                }
                break;
            case SYMMETRIC:
            case ASYMMETRIC:
            default:
                if (allowForNaNs) {
                    computeWithErrorAllowingNaNs(dimIndex == DIM_X ? xAxis : yAxis, dataSet, dimIndex, min, max);
                } else {
                    computeWithError(dimIndex == DIM_X ? xAxis : yAxis, dataSet, dimIndex, min, max);
                }
                break;
            }
        }

        computeErrorStyles(dataSet, min, max);
    }

    protected void computeScreenCoordinatesInParallel(final Axis xAxis, final Axis yAxis, final DataSet dataSet,
            final int dsIndex, final int min, final int max, final ErrorStyle localRendErrorStyle,
            final boolean isPolarPlot, final boolean doAllowForNaNs) {
        setBoundaryConditions(xAxis, yAxis, dataSet, dsIndex, min, max, localRendErrorStyle, isPolarPlot,
                doAllowForNaNs);

        // compute data set to screen coordinates
        computeScreenCoordinatesParallel(xAxis, yAxis, dataSet, min, max);
    }

    protected void computeScreenCoordinatesNonThreaded(final Axis xAxis, final Axis yAxis, final DataSet dataSet,
            final int min, final int max) {
        if (polarPlot) {
            computeScreenCoordinatesPolar(yAxis, dataSet, min, max);
        } else {
            computeScreenCoordinatesEuclidean(xAxis, yAxis, dataSet, min, max);
        }
    }

    protected void computeScreenCoordinatesParallel(final Axis xAxis, final Axis yAxis, final DataSet dataSet,
            final int min, final int max) {
        final int minthreshold = 1000;
        final int divThread = (int) Math
                .ceil(Math.abs(max - min) / (double) CachedDaemonThreadFactory.getNumbersOfThreads());
        final int stepSize = Math.max(divThread, minthreshold);
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
            final List<Future<Boolean>> jobs = CachedDaemonThreadFactory.getCommonPool().invokeAll(workers);
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

    private void computeScreenCoordinatesPolar(final Axis yAxis, final DataSet dataSet, final int min, final int max) {
        if (!(dataSet instanceof DataSet2D)) {
            throw new IllegalStateException("non-DataSet2D implementation not yet propagated");
        }

        if ((errorType[DIM_X] == ErrorType.NO_ERROR) && (errorType[DIM_Y] == ErrorType.NO_ERROR)) {
            computeNoErrorPolar(yAxis, (DataSet2D) dataSet, min, max);
        } else if (errorType[DIM_X] == ErrorType.NO_ERROR) {
            computeYonlyPolar(yAxis, (DataSet2D) dataSet, min, max);
        } else {
            // dataSet may not be non-DataSetError at this stage
            final DataSetError ds = (DataSetError) dataSet;
            computeFullPolar(yAxis, ds, min, max);
        }
    }

    private void computeWithError(final Axis yAxis, final DataSet dataSet, final int dimIndex, final int min,
            final int max) {
        if (dataSet instanceof DataSetError) {
            dataSet.lock().readLockGuardOptimistic(() -> {
                final double[] values = dimIndex == DIM_X ? xValues : yValues;
                final double[] valuesEN = dimIndex == DIM_X ? errorXNeg : errorYNeg;
                final double[] valuesEP = dimIndex == DIM_X ? errorXPos : errorYPos;
                final double minValue = dimIndex == DIM_X ? xMin : yMin;
                final DataSetError ds = (DataSetError) dataSet;
                for (int index = min; index < max; index++) {
                    final double value = dataSet.get(dimIndex, index);

                    values[index] = yAxis.getDisplayPosition(value);

                    if (!Double.isNaN(values[index])) {
                        // if (Double.isFinite(values[index])) {
                        valuesEN[index] = yAxis.getDisplayPosition(value - ds.getErrorNegative(dimIndex, index));
                        valuesEP[index] = yAxis.getDisplayPosition(value + ds.getErrorPositive(dimIndex, index));
                        continue;
                    }
                    values[index] = minValue;
                    valuesEN[index] = minValue;
                    valuesEP[index] = minValue;
                }
            });
            return;
        }

        // default dataset
        dataSet.lock().readLockGuardOptimistic(() -> {
            final double[] values = dimIndex == DIM_X ? xValues : yValues;
            final double[] valuesEN = dimIndex == DIM_X ? errorXNeg : errorYNeg;
            final double[] valuesEP = dimIndex == DIM_X ? errorXPos : errorYPos;
            final double minValue = dimIndex == DIM_X ? xMin : yMin;

            for (int index = min; index < max; index++) {
                values[index] = yAxis.getDisplayPosition(dataSet.get(dimIndex, index));
                if (Double.isFinite(values[index])) {
                    valuesEN[index] = values[index];
                    valuesEP[index] = values[index];
                } else {
                    values[index] = minValue;
                    valuesEN[index] = minValue;
                    valuesEP[index] = minValue;
                }
            }
        });
    }

    private void computeWithErrorAllowingNaNs(final Axis yAxis, final DataSet dataSet, final int dimIndex,
            final int min, final int max) {
        if (dataSet instanceof DataSetError) {
            dataSet.lock().readLockGuardOptimistic(() -> {
                final double[] values = dimIndex == DIM_X ? xValues : yValues;
                final double[] valuesEN = dimIndex == DIM_X ? errorXNeg : errorYNeg;
                final double[] valuesEP = dimIndex == DIM_X ? errorXPos : errorYPos;
                final DataSetError ds = (DataSetError) dataSet;
                for (int index = min; index < max; index++) {
                    final double value = dataSet.get(dimIndex, index);

                    if (!Double.isFinite(value)) {
                        values[index] = Double.NaN;
                        valuesEN[index] = Double.NaN;
                        valuesEP[index] = Double.NaN;
                        continue;
                    }

                    values[index] = yAxis.getDisplayPosition(value);
                    valuesEN[index] = yAxis.getDisplayPosition(value - ds.getErrorNegative(dimIndex, index));
                    valuesEP[index] = yAxis.getDisplayPosition(value + ds.getErrorPositive(dimIndex, index));
                }
            });
            return;
        }

        // default dataset
        dataSet.lock().readLockGuardOptimistic(() -> {
            final double[] values = dimIndex == DIM_X ? xValues : yValues;
            final double[] valuesEN = dimIndex == DIM_X ? errorXNeg : errorYNeg;
            final double[] valuesEP = dimIndex == DIM_X ? errorXPos : errorYPos;

            for (int index = min; index < max; index++) {
                values[index] = yAxis.getDisplayPosition(dataSet.get(dimIndex, index));

                if (Double.isFinite(values[index])) {
                    valuesEN[index] = values[index];
                    valuesEP[index] = values[index];
                } else {
                    values[index] = Double.NaN;
                    valuesEN[index] = Double.NaN;
                    valuesEP[index] = Double.NaN;
                }
            }
        });
    }

    private void computeWithNoError(final Axis axis, final DataSet dataSet, final int dimIndex, final int min,
            final int max) {
        // no error attached
        dataSet.lock().readLockGuardOptimistic(() -> {
            final double[] values = dimIndex == DIM_X ? xValues : yValues;
            final double minValue = dimIndex == DIM_X ? xMin : yMin;
            for (int index = min; index < max; index++) {
                final double value = dataSet.get(dimIndex, index);

                values[index] = axis.getDisplayPosition(value);

                // if (!Double.isFinite(values[index])) {
                if (Double.isNaN(values[index])) {
                    yValues[index] = minValue;
                }
            }

            if ((dimIndex == DIM_Y) && (rendererErrorStyle != ErrorStyle.NONE)) {
                System.arraycopy(values, min, errorYNeg, min, max - min);
                System.arraycopy(values, min, errorYPos, min, max - min);
            }
        });
    }

    private void computeWithNoErrorAllowingNaNs(final Axis axis, final DataSet dataSet, final int dimIndex,
            final int min, final int max) {
        // no error attached
        dataSet.lock().readLockGuardOptimistic(() -> {
            final double[] values = dimIndex == DIM_X ? xValues : yValues;
            for (int index = min; index < max; index++) {
                final double value = dataSet.get(dimIndex, index);

                if (Double.isFinite(value)) {
                    values[index] = axis.getDisplayPosition(value);
                } else {
                    values[index] = Double.NaN;
                }
            }

            if ((dimIndex == DIM_Y) && (rendererErrorStyle != ErrorStyle.NONE)) {
                System.arraycopy(values, min, errorYNeg, min, max - min);
                System.arraycopy(values, min, errorYPos, min, max - min);
            }
        });
    }

    private void computeYonlyPolar(final Axis yAxis, final DataSet2D dataSet, final int min, final int max) {
        dataSet.lock().readLockGuardOptimistic(() -> {
            for (int index = min; index < max; index++) {
                final double x = dataSet.getX(index);
                final double y = dataSet.getY(index);
                // check if error should be surrounded by Math.abs(..)
                // to ensure that they are always positive
                final double phi = x * DEG_TO_RAD;
                final double r = maxRadius * Math.abs(1 - (yAxis.getDisplayPosition(y) / yRange));
                xValues[index] = xZero + (r * Math.cos(phi));
                yValues[index] = yZero + (r * Math.sin(phi));

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
        });
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

        if (!isReducePoints || (Math.abs(indexMax - indexMin) < minRequiredReductionSize)) {
            actualDataCount = indexMax - indexMin;
            System.arraycopy(xValues, indexMin, xValues, 0, actualDataCount);
            System.arraycopy(yValues, indexMin, yValues, 0, actualDataCount);
            System.arraycopy(selected, indexMin, selected, 0, actualDataCount);
            if (errorType[DIM_X] != ErrorType.NO_ERROR) {
                // XY: // symmetric errors around x and y
                // X: // only symmetric errors around x
                // X_ASYMMETRIC: // asymmetric errors around x
                System.arraycopy(errorXNeg, indexMin, errorXNeg, 0, actualDataCount);
                System.arraycopy(errorXPos, indexMin, errorXPos, 0, actualDataCount);
                System.arraycopy(errorYNeg, indexMin, errorYNeg, 0, actualDataCount);
                System.arraycopy(errorYPos, indexMin, errorYPos, 0, actualDataCount);
            } else if (errorType[DIM_Y] != ErrorType.NO_ERROR) {
                // Y: // only symmetric errors around y
                // Y_ASYMMETRIC: // asymmetric errors around y
                System.arraycopy(errorYNeg, indexMin, errorYNeg, 0, actualDataCount);
                System.arraycopy(errorYPos, indexMin, errorYPos, 0, actualDataCount);
            }

            ProcessingProfiler.getTimeDiff(startTimeStamp, String.format("no data reduction (%d)", actualDataCount));
            return;
        }
        if (errorType[DIM_X] == ErrorType.NO_ERROR) {
            actualDataCount = cruncher.reducePoints(xValues, yValues, null, null, errorYPos, errorYNeg, styles,
                    selected, indexMin, indexMax);
        } else {
            actualDataCount = cruncher.reducePoints(xValues, yValues, errorXPos, errorXNeg, errorYPos, errorYNeg,
                    styles, selected, indexMin, indexMax);
        }
        minDataPointDistanceX();
    }

    public void release() {
        ArrayCache.release(X_VALUES, xValues);
        ArrayCache.release(Y_VALUES, yValues);
        ArrayCache.release(ERROR_Y_NEG, errorYNeg);
        ArrayCache.release(ERROR_Y_POS, errorYPos);
        ArrayCache.release(ERROR_X_NEG, errorXNeg);
        ArrayCache.release(ERROR_X_POS, errorXPos);
        ArrayCache.release(SELECTED2, selected);
        ArrayCache.release(STYLES2, styles);
    }

    private void setBoundaryConditions(final Axis xAxis, final Axis yAxis, final DataSet dataSet, final int dsIndex,
            final int min, final int max, final ErrorStyle rendererErrorStyle, final boolean isPolarPlot,
            final boolean doAllowForNaNs) {
        indexMin = min;
        indexMax = max;
        polarPlot = isPolarPlot;
        this.allowForNaNs = doAllowForNaNs;
        this.rendererErrorStyle = rendererErrorStyle;

        computeBoundaryVariables(xAxis, yAxis);
        setStyleVariable(dataSet, dsIndex);
        setErrorType(dataSet, rendererErrorStyle);
    }

    protected void setErrorType(final DataSet dataSet, final ErrorStyle errorStyle) {
        errorType = new ErrorType[dataSet.getDimension()];
        if (dataSet instanceof DataSetError) {
            final DataSetError ds = (DataSetError) dataSet;
            for (int dimIndex = 0; dimIndex < ds.getDimension(); dimIndex++) {
                final int tmpIndex = dimIndex;
                errorType[dimIndex] = dataSet.lock().readLockGuardOptimistic(() -> ds.getErrorType(tmpIndex));
            }
        } else if (errorStyle == ErrorStyle.NONE) {
            // special case where users does not want error bars
            for (int dimIndex = 0; dimIndex < dataSet.getDimension(); dimIndex++) {
                errorType[dimIndex] = ErrorType.NO_ERROR;
            }
        } else {
            // fall-back for standard DataSet

            // default: ErrorType=Y fall-back also for 'DataSet' without
            // errors
            // rationale: scientific honesty
            // if data points are being compressed, the error of compression
            // (e.g. due to local transients that are being suppressed) are
            // nevertheless being computed and shown even if individual data
            // points have no error
            for (int dimIndex = 0; dimIndex < dataSet.getDimension(); dimIndex++) {
                errorType[dimIndex] = dimIndex == DIM_Y ? ErrorType.ASYMMETRIC : ErrorType.NO_ERROR;
            }
        }
    }

    protected void setStyleVariable(final DataSet dataSet, final int dsIndex) {
        dataSet.lock().readLockGuardOptimistic(() -> defaultStyle = dataSet.getStyle());
        final Integer layoutOffset = StyleParser.getIntegerPropertyValue(defaultStyle,
                XYChartCss.DATASET_LAYOUT_OFFSET);
        final Integer dsIndexLocal = StyleParser.getIntegerPropertyValue(defaultStyle, XYChartCss.DATASET_INDEX);

        dataSetStyleIndex = layoutOffset == null ? 0 : layoutOffset.intValue();
        dataSetIndex = dsIndexLocal == null ? dsIndex : dsIndexLocal.intValue();
    }
}
