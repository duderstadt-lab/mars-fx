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

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.renderer.spi.AbstractErrorDataSetRendererParameter;
import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.XYChartCss;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.spi.CategoryAxis;
import de.gsi.chart.marker.DefaultMarker;
import de.gsi.chart.marker.Marker;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.renderer.spi.utils.BezierCurve;
import de.gsi.dataset.utils.ArrayCache;
import de.gsi.chart.renderer.spi.utils.DefaultRenderColorScheme;
import de.gsi.chart.utils.StyleParser;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSetError.ErrorType;
import de.gsi.dataset.spi.utils.Triple;
import de.gsi.dataset.utils.ProcessingProfiler;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.FillRule;

import javafx.scene.control.Label;

/**
 * Renders data points with error bars and/or error surfaces 
 * It can be used e.g. to render horizontal and/or vertical errors
 * 
 * additional functionality:
 * <ul>
 * <li> bar-type plot
 * <li> polar-axis plotting
 * <li> scatter and/or bubble-chart-type plots
 * </ul>
 *
 * @author R.J. Steinhagen
 * @author Karl Duderstadt - many changes to all for segment plotting and custom styling.
 */
public class SegmentDataSetRenderer extends AbstractErrorDataSetRendererParameter<SegmentDataSetRenderer>
        implements Renderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SegmentDataSetRenderer.class);
    private static final String Y_DRAW_POLY_LINE_STAIR_CASE = "yDrawPolyLineStairCase";
    private static final String X_DRAW_POLY_LINE_STAIR_CASE = "xDrawPolyLineStairCase";
    private static final String Y_DRAW_POLY_LINE_AREA = "yDrawPolyLineArea";
    private static final String X_DRAW_POLY_LINE_AREA = "xDrawPolyLineArea";
    private static final String Y_DRAW_POLY_LINE_HISTOGRAM = "yDrawPolyLineHistogram";
    private static final String X_DRAW_POLY_LINE_HISTOGRAM = "xDrawPolyLineHistogram";
    private Marker marker = DefaultMarker.RECTANGLE; // default: rectangle
    private long stopStamp;

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
        super();
        setDashSize(dashSize);
    }

    @Override
    public void render(final GraphicsContext gc, final Chart chart, final int dataSetOffset,
            final ObservableList<DataSet> datasets) {
        if (!(chart instanceof XYChart)) {
            throw new InvalidParameterException(
                    "must be derivative of XYChart for renderer - " + this.getClass().getSimpleName());
        }

        // make local copy and add renderer specific data sets
        final List<DataSet> localDataSetList = new ArrayList<>(datasets);
        localDataSetList.addAll(super.getDatasets());

        // If there are no data sets
        if (localDataSetList.isEmpty()) {
            return;
        }

        final Axis xAxis = getFirstAxis(Orientation.HORIZONTAL);
        if (xAxis == null) {
            throw new InvalidParameterException("x-Axis must not be null - axesList() = " + getAxes());
        }
        final Axis yAxis = getFirstAxis(Orientation.VERTICAL);
        if (yAxis == null) {
            throw new InvalidParameterException("y-Axis must not be null - axesList() = " + getAxes());
        }
        final long start = ProcessingProfiler.getTimeStamp();
        final double xAxisWidth = xAxis.getWidth();
        final double xMin = xAxis.getValueForDisplay(0);
        final double xMax = xAxis.getValueForDisplay(xAxisWidth);

        ProcessingProfiler.getTimeDiff(start, "init");
        
        for (int dataSetIndex = localDataSetList.size() - 1; dataSetIndex >= 0; dataSetIndex--) {
        	final int ldataSetIndex = dataSetIndex;
            stopStamp = ProcessingProfiler.getTimeStamp();
            final DataSet dataSet = localDataSetList.get(dataSetIndex);
            
            Color color = Color.BLACK;
            double width = 1;
            String lineStyle = "";
            
            if (dataSet instanceof MarsDoubleDataSet) {
            	color = ((MarsDoubleDataSet) dataSet).getColor();
            	width = ((MarsDoubleDataSet) dataSet).getWidth();
            	lineStyle = ((MarsDoubleDataSet) dataSet).getLineStyle();
            }
            
            int indexMin;
            int indexMax; /* indexMax is excluded in the drawing */
            if (isAssumeSortedData()) {
                indexMin = Math.max(0, dataSet.getIndex(DataSet.DIM_X, xMin));
                indexMax = Math.min(dataSet.getIndex(DataSet.DIM_X, xMax) + 1, dataSet.getDataCount(DataSet.DIM_X));
            } else {
                indexMin = 0;
                indexMax = dataSet.getDataCount(DataSet.DIM_X);
            }
            
            if (indexMax - indexMin <= 0) {
                // zero length/range data set -> nothing to be drawn                
                continue;
            }

            if (dataSet.getStyle().equals("Segments")) {
                
                if (indexMin > 0 && indexMin%2!=0) {
                	indexMin--;
                }
                
                if (indexMax + 2 < dataSet.getDataCount()) {
                	if (indexMax+1 % 2 != 0)
                		indexMax+=1;
                	else if (indexMax+2 %2 != 0)
                		indexMax+=2;
                } else {
                	indexMax = dataSet.getDataCount();
                }

            	final CachedDataPoints localCachedPoints = new CachedDataPoints(indexMin, indexMax, dataSet.getDataCount(), true);

            	// compute local screen coordinates
                final boolean isPolarPlot = ((XYChart) chart).isPolarPlot();
                if (isParallelImplementation()) {
                    localCachedPoints.computeScreenCoordinatesInParallel(xAxis, yAxis, dataSet,
                            dataSetOffset + ldataSetIndex, indexMin, indexMax, getErrorType(), isPolarPlot,
                            isallowNaNs());
                } else {
                    localCachedPoints.computeScreenCoordinates(xAxis, yAxis, dataSet, dataSetOffset + ldataSetIndex,
                            indexMin, indexMax, getErrorType(), isPolarPlot, isallowNaNs());
                }
            	
                // invoke data reduction algorithm
                localCachedPoints.reduce(rendererDataReducerProperty().get(), false,
                        getMinRequiredReductionSize());
                
            	drawSegments(gc, localCachedPoints, color, width);

                localCachedPoints.release();
            } else {
                if (xAxis.isInvertedAxis()) {
                    final int temp = indexMin;
                    indexMin = indexMax - 1;
                    indexMax = temp + 1;
                }
                
                if (indexMin > 0)
                	indexMin--;
                
                if (indexMax < dataSet.getDataCount())
                	indexMax++;

                if (indexMax - indexMin <= 0) {
                    // zero length/range data set -> nothing to be drawn                
                    continue;
                }

                stopStamp = ProcessingProfiler.getTimeDiff(stopStamp,
                        "get min/max" + String.format(" from:%d to:%d", indexMin, indexMax));
                
            	final CachedDataPoints localCachedPoints = new CachedDataPoints(indexMin, indexMax, dataSet.getDataCount(),
                        true);
            	stopStamp = ProcessingProfiler.getTimeDiff(stopStamp, "get CachedPoints");

            	// compute local screen coordinates
                final boolean isPolarPlot = ((XYChart) chart).isPolarPlot();
                if (isParallelImplementation()) {
                    localCachedPoints.computeScreenCoordinatesInParallel(xAxis, yAxis, dataSet,
                            dataSetOffset + ldataSetIndex, indexMin, indexMax, getErrorType(), isPolarPlot,
                            isallowNaNs());
                } else {
                    localCachedPoints.computeScreenCoordinates(xAxis, yAxis, dataSet, dataSetOffset + ldataSetIndex,
                            indexMin, indexMax, getErrorType(), isPolarPlot, isallowNaNs());
                }
                
                stopStamp = ProcessingProfiler.getTimeDiff(stopStamp, "computeScreenCoordinates()");

                // invoke data reduction algorithm
                //Seems to always remove the second to last point in all plots why ???? What should be do???
                localCachedPoints.reduce(rendererDataReducerProperty().get(), isReducePoints(),
                        getMinRequiredReductionSize());
                
            	if (dataSet.getStyle().equals("Scatter"))
            		drawScatter(gc, localCachedPoints, color, width);
                else
                	drawPolyLine(gc, localCachedPoints, color, width, getDashPattern(lineStyle));
            	
            	if (dataSet.getStyle().equals("Bar"))
            		drawBars(gc, localCachedPoints);
            	
            	stopStamp = ProcessingProfiler.getTimeStamp();

                localCachedPoints.release();
                ProcessingProfiler.getTimeDiff(stopStamp, "localCachedPoints.release()");
            }
        } // end of 'dataSetIndex' loop
        	
        ProcessingProfiler.getTimeDiff(start);
    }

    /**
     * @param dataSet for which the representative icon should be generated
     * @param dsIndex index within renderer set
     * @param width requested width of the returning Canvas
     * @param height requested height of the returning Canvas
     * @return a graphical icon representation of the given data sets
     */
    @Override
    public Canvas drawLegendSymbol(final DataSet dataSet, final int dsIndex, final int width, final int height) {
        final Canvas canvas = new Canvas(width, height);
        final GraphicsContext gc = canvas.getGraphicsContext2D();

        final String style = dataSet.getStyle();
        final Integer layoutOffset = StyleParser.getIntegerPropertyValue(style, XYChartCss.DATASET_LAYOUT_OFFSET);
        final Integer dsIndexLocal = StyleParser.getIntegerPropertyValue(style, XYChartCss.DATASET_INDEX);

        final int dsLayoutIndexOffset = layoutOffset == null ? 0 : layoutOffset.intValue(); // TODO:
                                                                                            // rationalise

        final int plotingIndex = dsLayoutIndexOffset + (dsIndexLocal == null ? dsIndex : dsIndexLocal.intValue());

        gc.save();

        DefaultRenderColorScheme.setLineScheme(gc, dataSet.getStyle(), plotingIndex);
        DefaultRenderColorScheme.setGraphicsContextAttributes(gc, dataSet.getStyle());
        DefaultRenderColorScheme.setFillScheme(gc, dataSet.getStyle(), plotingIndex);
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
        return canvas;
    }

    protected static void drawSegments(final GraphicsContext gc, final CachedDataPoints localCachedPoints, Color color, double width) {
        gc.save();

        gc.setLineWidth(width);
        gc.setFill(color);
        gc.setStroke(color);

        // Skip every other segment
        for (int i=0; i < localCachedPoints.actualDataCount - 1; i+=2) {
            final double x1 = localCachedPoints.xValues[i];
            final double x2 = localCachedPoints.xValues[i + 1];
            final double y1 = localCachedPoints.yValues[i];
            final double y2 = localCachedPoints.yValues[i + 1];
            gc.strokeLine(x1, y1, x2, y2);
        }

        gc.restore();
    }
    
    protected static void drawPolyLine(final GraphicsContext gc, final CachedDataPoints localCachedPoints, Color color, double width, double[] dashPattern) {
        gc.save();
        
        gc.setLineWidth(width);
        gc.setFill(color);
        gc.setStroke(color);
        
        if (dashPattern != null) {
        	gc.setLineDashes(dashPattern);
        }
        //gc.setFont(font);

        for (int i = 0; i < localCachedPoints.actualDataCount - 1; i++) {
            final double x1 = localCachedPoints.xValues[i];
            final double x2 = localCachedPoints.xValues[i + 1];
            final double y1 = localCachedPoints.yValues[i];
            final double y2 = localCachedPoints.yValues[i + 1];
            gc.strokeLine(x1, y1, x2, y2);
        }

        gc.restore();
    }
    
    protected double[] getDashPattern(String lineStyle) {
    	if (lineStyle == null) {
            return null;
        }

        try {
            final String[] splitValues = lineStyle.split(" ");
            if (splitValues == null || splitValues.length == 0) {
                return null;
            }
            final double[] retArray = new double[splitValues.length];
            for (int i = 0; i < splitValues.length; i++) {
                retArray[i] = Double.parseDouble(splitValues[i]);
            }
            return retArray;
        } catch (final NumberFormatException ex) {
            return null;
        }
    }
    
    protected static void drawScatter(final GraphicsContext gc, final CachedDataPoints localCachedPoints, Color color, double width) {
        gc.save();
        
        gc.setFill(color);
        //gc.setStroke(color);
        
        final Marker pointMarker = DefaultMarker.CIRCLE;
        for (int i = 0; i < localCachedPoints.actualDataCount; i++) {
            final double x = localCachedPoints.xValues[i];
            final double y = localCachedPoints.yValues[i];
            pointMarker.draw(gc, x, y, width);
        }

        gc.restore();
    }

    protected static void drawPolyLineArea(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        final int n = localCachedPoints.actualDataCount;
        if (n == 0) {
            return;
        }

        // need to allocate new array :-(
        final double[] newX = ArrayCache.getCachedDoubleArray(X_DRAW_POLY_LINE_AREA, n + 2);
        final double[] newY = ArrayCache.getCachedDoubleArray(Y_DRAW_POLY_LINE_AREA, n + 2);

        final double zero = localCachedPoints.yZero;
        System.arraycopy(localCachedPoints.xValues, 0, newX, 0, n);
        System.arraycopy(localCachedPoints.yValues, 0, newY, 0, n);
        newX[n] = localCachedPoints.xValues[n - 1];
        newY[n] = zero;
        newX[n + 1] = localCachedPoints.xValues[0];
        newY[n + 1] = zero;

        gc.save();
        DefaultRenderColorScheme.setLineScheme(gc, localCachedPoints.defaultStyle,
                localCachedPoints.dataSetIndex + localCachedPoints.dataSetStyleIndex);
        DefaultRenderColorScheme.setGraphicsContextAttributes(gc, localCachedPoints.defaultStyle);
        // use stroke as fill colour
        gc.setFill(gc.getStroke());
        gc.fillPolygon(newX, newY, n + 2);
        gc.restore();

        // release arrays to cache
        ArrayCache.release(X_DRAW_POLY_LINE_AREA, newX);
        ArrayCache.release(Y_DRAW_POLY_LINE_AREA, newY);
    }

    protected static void drawPolyLineStairCase(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        final int n = localCachedPoints.actualDataCount;
        if (n == 0) {
            return;
        }

        // need to allocate new array :-(
        final double[] newX = ArrayCache.getCachedDoubleArray(X_DRAW_POLY_LINE_STAIR_CASE, 2 * n);
        final double[] newY = ArrayCache.getCachedDoubleArray(Y_DRAW_POLY_LINE_STAIR_CASE, 2 * n);

        for (int i = 0; i < n - 1; i++) {
            newX[2 * i] = localCachedPoints.xValues[i];
            newY[2 * i] = localCachedPoints.yValues[i];
            newX[2 * i + 1] = localCachedPoints.xValues[i + 1];
            newY[2 * i + 1] = localCachedPoints.yValues[i];
        }
        // last point
        newX[2 * (n - 1)] = localCachedPoints.xValues[n - 1];
        newY[2 * (n - 1)] = localCachedPoints.yValues[n - 1];
        newX[2 * n - 1] = localCachedPoints.xMax;
        newY[2 * n - 1] = localCachedPoints.yValues[n - 1];

        gc.save();
        DefaultRenderColorScheme.setLineScheme(gc, localCachedPoints.defaultStyle,
                localCachedPoints.dataSetIndex + localCachedPoints.dataSetStyleIndex);
        DefaultRenderColorScheme.setGraphicsContextAttributes(gc, localCachedPoints.defaultStyle);
        // gc.strokePolyline(newX, newY, 2*n);

        for (int i = 0; i < 2 * n - 1; i++) {
            final double x1 = newX[i];
            final double x2 = newX[i + 1];
            final double y1 = newY[i];
            final double y2 = newY[i + 1];
            gc.strokeLine(x1, y1, x2, y2);
        }

        gc.restore();

        // release arrays to cache
        ArrayCache.release(X_DRAW_POLY_LINE_STAIR_CASE, newX);
        ArrayCache.release(Y_DRAW_POLY_LINE_STAIR_CASE, newY);
    }

    protected static void drawPolyLineHistogram(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        final int n = localCachedPoints.actualDataCount;
        if (n == 0) {
            return;
        }

        // need to allocate new array :-(
        final double[] newX = ArrayCache.getCachedDoubleArray(X_DRAW_POLY_LINE_HISTOGRAM, 2 * (n + 1));
        final double[] newY = ArrayCache.getCachedDoubleArray(Y_DRAW_POLY_LINE_HISTOGRAM, 2 * (n + 1));

        final double xRange = localCachedPoints.xMax - localCachedPoints.xMin;
        double diffLeft;
        double diffRight = n > 0 ? 0.5 * (localCachedPoints.xValues[1] - localCachedPoints.xValues[0]) : 0.5 * xRange;
        newX[0] = localCachedPoints.xValues[0] - diffRight;
        newY[0] = localCachedPoints.yZero;
        for (int i = 0; i < n; i++) {
            diffLeft = localCachedPoints.xValues[i] - newX[2 * i];
            diffRight = i + 1 < n ? 0.5 * (localCachedPoints.xValues[i + 1] - localCachedPoints.xValues[i]) : diffLeft;
            if (i == 0) {
                diffLeft = diffRight;
            }

            newX[2 * i + 1] = localCachedPoints.xValues[i] - diffLeft;
            newY[2 * i + 1] = localCachedPoints.yValues[i];
            newX[2 * i + 2] = localCachedPoints.xValues[i] + diffRight;
            newY[2 * i + 2] = localCachedPoints.yValues[i];
        }
        // last point
        newX[2 * (n + 1) - 1] = localCachedPoints.xValues[n - 1] + diffRight;
        newY[2 * (n + 1) - 1] = localCachedPoints.yZero;

        gc.save();
        DefaultRenderColorScheme.setLineScheme(gc, localCachedPoints.defaultStyle,
                localCachedPoints.dataSetIndex + localCachedPoints.dataSetStyleIndex);
        DefaultRenderColorScheme.setGraphicsContextAttributes(gc, localCachedPoints.defaultStyle);

        for (int i = 0; i < 2 * (n + 1) - 1; i++) {
            final double x1 = newX[i];
            final double x2 = newX[i + 1];
            final double y1 = newY[i];
            final double y2 = newY[i + 1];
            gc.strokeLine(x1, y1, x2, y2);
        }

        gc.restore();

        // release arrays to cache
        ArrayCache.release(X_DRAW_POLY_LINE_HISTOGRAM, newX);
        ArrayCache.release(Y_DRAW_POLY_LINE_HISTOGRAM, newY);
    }

    protected static void drawPolyLineHistogramFilled(final GraphicsContext gc,
            final CachedDataPoints localCachedPoints) {
        final int n = localCachedPoints.actualDataCount;
        if (n == 0) {
            return;
        }

        // need to allocate new array :-(
        final double[] newX = ArrayCache.getCachedDoubleArray(X_DRAW_POLY_LINE_HISTOGRAM, 2 * (n + 1));
        final double[] newY = ArrayCache.getCachedDoubleArray(Y_DRAW_POLY_LINE_HISTOGRAM, 2 * (n + 1));

        final double xRange = localCachedPoints.xMax - localCachedPoints.xMin;
        double diffLeft;
        double diffRight = n > 0 ? 0.5 * (localCachedPoints.xValues[1] - localCachedPoints.xValues[0]) : 0.5 * xRange;
        newX[0] = localCachedPoints.xValues[0] - diffRight;
        newY[0] = localCachedPoints.yZero;
        for (int i = 0; i < n; i++) {
            diffLeft = localCachedPoints.xValues[i] - newX[2 * i];
            diffRight = i + 1 < n ? 0.5 * (localCachedPoints.xValues[i + 1] - localCachedPoints.xValues[i]) : diffLeft;
            if (i == 0) {
                diffLeft = diffRight;
            }

            newX[2 * i + 1] = localCachedPoints.xValues[i] - diffLeft;
            newY[2 * i + 1] = localCachedPoints.yValues[i];
            newX[2 * i + 2] = localCachedPoints.xValues[i] + diffRight;
            newY[2 * i + 2] = localCachedPoints.yValues[i];
        }
        // last point
        newX[2 * (n + 1) - 1] = localCachedPoints.xValues[n - 1] + diffRight;
        newY[2 * (n + 1) - 1] = localCachedPoints.yZero;

        gc.save();
        DefaultRenderColorScheme.setLineScheme(gc, localCachedPoints.defaultStyle,
                localCachedPoints.dataSetIndex + localCachedPoints.dataSetStyleIndex);
        DefaultRenderColorScheme.setGraphicsContextAttributes(gc, localCachedPoints.defaultStyle);
        // use stroke as fill colour
        gc.setFill(gc.getStroke());
        gc.fillPolygon(newX, newY, 2 * (n + 1));
        gc.restore();

        // release arrays to cache
        ArrayCache.release(X_DRAW_POLY_LINE_HISTOGRAM, newX);
        ArrayCache.release(Y_DRAW_POLY_LINE_HISTOGRAM, newY);
    }

    /**
     * @param gc the graphics context from the Canvas parent
     * @param localCachedPoints reference to local cached data point object
     */
    protected void drawMarker(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        if (!isDrawMarker()) {
            return;
        }
        gc.save();
        DefaultRenderColorScheme.setMarkerScheme(gc, localCachedPoints.defaultStyle,
                localCachedPoints.dataSetIndex + localCachedPoints.dataSetStyleIndex);

        final Triple<Marker, Color, Double> markerTypeColorAndSize = getDefaultMarker(localCachedPoints.defaultStyle);
        final Marker defaultMarker = markerTypeColorAndSize.getFirst();
        final Color defaultMarkerColor = markerTypeColorAndSize.getSecond();
        final double defaultMarkerSize = markerTypeColorAndSize.getThird();
        if (defaultMarkerColor != null) {
            gc.setFill(defaultMarkerColor);
        }
        for (int i = 0; i < localCachedPoints.actualDataCount; i++) {
            final double x = localCachedPoints.xValues[i];
            final double y = localCachedPoints.yValues[i];
            if (localCachedPoints.styles[i] == null) {
                defaultMarker.draw(gc, x, y, defaultMarkerSize);
            } else {
                final Triple<Marker, Color, Double> markerForPoint = getDefaultMarker(
                        localCachedPoints.defaultStyle + localCachedPoints.styles[i]);
                gc.save();
                if (markerForPoint.getSecond() != null) {
                    gc.setFill(markerForPoint.getSecond());
                }
                final Marker pointMarker = markerForPoint.getFirst() == null ? defaultMarker : markerForPoint.getFirst();
                pointMarker.draw(gc, x, y, markerForPoint.getThird());
                gc.restore();
            }
        }

        gc.restore();
    }

    protected Triple<Marker, Color, Double> getDefaultMarker(final String dataSetStyle) {
        Marker defaultMarker = getMarker();
        // N.B. the markers are drawn in the same colour
        // as the polyline (ie. stroke color)
        Color defaultMarkerColor = StyleParser.getColorPropertyValue(dataSetStyle, XYChartCss.STROKE_COLOR);
        double defaultMarkerSize = getMarkerSize();

        if (dataSetStyle == null) {
            return new Triple<>(defaultMarker, defaultMarkerColor, defaultMarkerSize);
        }

        // parse style:
        final Map<String, String> map = StyleParser.splitIntoMap(dataSetStyle);

        final String markerType = map.get(XYChartCss.MARKER_TYPE.toLowerCase(Locale.UK));
        if (markerType != null) {
            try {
                final Marker tempType = DefaultMarker.get(markerType);
                defaultMarker = tempType;
            } catch (final IllegalArgumentException ex) {
                if (LOGGER.isErrorEnabled()) {
                LOGGER.error("could not parse marker type description for '" + XYChartCss.MARKER_TYPE + "'='"
                        + markerType + "'", ex);
                }
            }
        }
        final String markerSize = map.get(XYChartCss.MARKER_SIZE.toLowerCase(Locale.UK));
        if (markerSize != null) {
            try {
                final double tempSize = Double.parseDouble(markerSize);
                defaultMarkerSize = tempSize;
            } catch (final NumberFormatException ex) {
                if (LOGGER.isErrorEnabled()) {
                LOGGER.error("could not parse marker size description for '" + XYChartCss.MARKER_SIZE + "'='"
                        + markerSize + "'", ex);
                }
            }
        }

        final String markerColor = map.get(XYChartCss.MARKER_COLOR.toLowerCase(Locale.UK));
        if (markerColor != null) {
            try {
                final Color tempColor = Color.web(markerColor);
                defaultMarkerColor = tempColor;
            } catch (final IllegalArgumentException ex) {
                if (LOGGER.isErrorEnabled()) {
                LOGGER.error("could not parse marker color description for '" + XYChartCss.MARKER_COLOR + "'='"
                        + markerColor + "'", ex);
                }
            }
        }

        return new Triple<>(defaultMarker, defaultMarkerColor, defaultMarkerSize);
    }

    /**
     * @param gc the graphics context from the Canvas parent
     * @param localCachedPoints reference to local cached data point object
     */
    protected void drawBars(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        if (!isDrawBars()) {
            return;
        }

        final int xOffset = localCachedPoints.dataSetIndex >= 0 ? localCachedPoints.dataSetIndex : 0;
        final int minRequiredWidth = Math.max(getDashSize(), localCachedPoints.minDistanceX);

        final double barWPercentage = getBarWidthPercentage();
        final double dynBarWidth = minRequiredWidth * barWPercentage / 100;
        final double constBarWidth = getBarWidth();
        final double localBarWidth = isDynamicBarWidth() ? dynBarWidth : constBarWidth;
        final double barWidthHalf = localBarWidth / 2 - (isShiftBar() ? xOffset * getShiftBarOffset() : 0);

        gc.save();
        DefaultRenderColorScheme.setMarkerScheme(gc, localCachedPoints.defaultStyle,
                localCachedPoints.dataSetIndex + localCachedPoints.dataSetStyleIndex);
        DefaultRenderColorScheme.setGraphicsContextAttributes(gc, localCachedPoints.defaultStyle);

        if (localCachedPoints.polarPlot) {
            for (int i = 0; i < localCachedPoints.actualDataCount; i++) {
                if (localCachedPoints.selected[i]) {
                    gc.strokeLine(localCachedPoints.xZero, localCachedPoints.yZero, localCachedPoints.xValues[i],
                            localCachedPoints.yValues[i]);
                } else {
                    // work-around: bar colour controlled by the marker color
                    gc.save();
                    gc.setLineWidth(barWidthHalf);
                    gc.strokeLine(localCachedPoints.xZero, localCachedPoints.yZero, localCachedPoints.xValues[i],
                            localCachedPoints.yValues[i]);
                    gc.restore();
                }
            }
        } else {
            for (int i = 0; i < localCachedPoints.actualDataCount; i++) {
                double yDiff = localCachedPoints.yValues[i] - localCachedPoints.yZero;
                double yMin;
                if (yDiff > 0) {
                    yMin = localCachedPoints.yZero;
                } else {
                    yMin = localCachedPoints.yValues[i];
                    yDiff = Math.abs(yDiff);
                }

                if (localCachedPoints.selected[i]) {
                    gc.strokeRect(localCachedPoints.xValues[i] - barWidthHalf, yMin, localBarWidth, yDiff);
                } else {
                    // work-around: bar colour controlled by the marker color
                    gc.fillRect(localCachedPoints.xValues[i] - barWidthHalf, yMin, localBarWidth, yDiff);
                }
            }
        }

        gc.restore();
    }

    /**
     * @return the instance of this ErrorDataSetRenderer.
     */
    @Override
    protected SegmentDataSetRenderer getThis() {
        return this;
    }

    /**
     * Returns the marker used by this renderer.
     *
     * @return the marker to be drawn on the data points
     */
    public Marker getMarker() {
        return marker;
    }

    /**
     * Replaces marker used by this renderer.
     *
     * @param marker the marker to be drawn on the data points
     */
    public void setMarker(final Marker marker) {
        this.marker = marker;
    }
}
