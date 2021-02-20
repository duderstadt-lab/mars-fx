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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.gsi.chart.axes.AxisLabelOverlapPolicy;
import de.gsi.chart.axes.AxisTransform;
import de.gsi.chart.axes.spi.AxisRange;
import de.gsi.chart.axes.spi.CategoryAxis;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.axes.spi.transforms.DefaultAxisTransform;
import de.gsi.dataset.DataSet;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;

public class MarsCategoryAxis extends DefaultNumericAxis {
	
    private boolean forceAxisCategories = false;

    private boolean changeIsLocal = false;

    private final ObjectProperty<ObservableList<String>> categories = new ObjectPropertyBase<ObservableList<String>>() {

        @Override
        public Object getBean() {
            return MarsCategoryAxis.this;
        }

        @Override
        public String getName() {
            return "categories";
        }
    };

    /**
     * Create a auto-ranging category axis with an empty list of categories.
     */
    public MarsCategoryAxis() {
        this((String) null);
        setTickUnit(1.0);
        changeIsLocal = true;
        setCategories(FXCollections.<String>observableArrayList());
        changeIsLocal = false;
    }

    /**
     * Create a category axis with the given categories. This will not auto-range but be fixed with the given
     * categories.
     *
     * @param categories List of the categories for this axis
     */
    public MarsCategoryAxis(final ObservableList<String> categories) {
        this(null, categories);
    }

    /**
     * Create a {@link #autoRangingProperty() non-auto-ranging} Axis with the given upper bound, lower bound and tick
     * unit.
     *
     * @param axisLabel the axis {@link #nameProperty() label}
     */
    public MarsCategoryAxis(final String axisLabel) {
        super(axisLabel);
        this.setOverlapPolicy(AxisLabelOverlapPolicy.SHIFT_ALT);
        minProperty().addListener((ch, old, val) -> {
            final double range = Math.abs(val.doubleValue() - MarsCategoryAxis.this.getMax());
            final double rangeInt = (int) range;
            final double scale = 0.5 / rangeInt;
            autoRangePaddingProperty().set(scale);
        });

        maxProperty().addListener((ch, old, val) -> {
            final double range = Math.abs(MarsCategoryAxis.this.getMin() - val.doubleValue());
            final double rangeInt = (int) range;
            final double scale = 0.5 / rangeInt;
            autoRangePaddingProperty().set(scale);
        });
    }

    /**
     * Create a {@link #autoRangingProperty() non-auto-ranging} Axis with the given upper bound, lower bound and tick
     * unit.
     *
     * @param axisLabel the axis {@link #nameProperty() label}
     * @param categories List of the categories for this axis
     */
    public MarsCategoryAxis(final String axisLabel, final ObservableList<String> categories) {
        super(axisLabel, 0, categories.size(), 1.0);
        changeIsLocal = true;
        setCategories(categories);
        changeIsLocal = false;
    }
    
    
    //SUPER HACKY drop-in to fix error in chart-fx library related to single bars and 
    //LOGGER firing..
    @Override
    protected List<Double> calculateMajorTickValues(final double axisLength, final AxisRange axisRange) {
        final List<Double> tickValues = new ArrayList<>();
        if (isLogAxis) {
            if (axisRange.getLowerBound() >= axisRange.getUpperBound()) {
                return Arrays.asList(axisRange.getLowerBound());
            }
            
            DefaultAxisTransform linearTransform = new DefaultAxisTransform(this);
            AxisTransform axisTransform = linearTransform;
            
            double exp = Math.ceil(axisTransform.forward(axisRange.getLowerBound()));
            for (double tickValue = axisTransform.backward(exp); tickValue <= axisRange
                                                                                      .getUpperBound();
                    tickValue = axisTransform.backward(++exp)) {
                tickValues.add(tickValue);
            }

            // add minor tick marks to major
            // tickValues.addAll(calculateMinorTickMarks());

            return tickValues;
        }

        if (axisRange.getLowerBound() == axisRange.getUpperBound() || axisRange.getTickUnit() <= 0) {
            return Arrays.asList(axisRange.getLowerBound());
        }

        final double firstTick = computeFistMajorTick(axisRange.getLowerBound(), axisRange.getTickUnit());
        if (firstTick + axisRange.getTickUnit() == firstTick) {
            return tickValues;
        }
        for (double major = firstTick; major <= axisRange.getUpperBound(); major += axisRange.getTickUnit()) {
            tickValues.add(major);
        }
        return tickValues;
    }
    
    private static double computeFistMajorTick(final double lowerBound, final double tickUnit) {
        return Math.ceil(lowerBound / tickUnit) * tickUnit;
    }
    //END DROPIN..
    
    // -------------- CONSTRUCTORS
    // -------------------------------------------------------------------------------------

    @Override
    protected AxisRange autoRange(final double minValue, final double maxValue, final double length,
            final double labelSize) {
        double min = minValue > 0 && isForceZeroInRange() ? 0 : minValue;
        if (isLogAxis && minValue <= 0) {
            min = DefaultNumericAxis.DEFAULT_LOG_MIN_VALUE;
            isUpdating = true;
            setMin(DefaultNumericAxis.DEFAULT_LOG_MIN_VALUE);
            isUpdating = false;
        }
        final double max = maxValue < 0 && isForceZeroInRange() ? 0 : maxValue;
        final double padding = DefaultNumericAxis.getEffectiveRange(min, max) * getAutoRangePadding();
        final double paddingScale = 1.0 + getAutoRangePadding();
        // compared to DefaultNumericAxis clamping wasn't really necessary for
        // CategoryAxis
        // N.B. it was unnecessarily forcing the first bound to 0 (rather than
        // -0.5)
        final double paddedMin = isLogAxis ? minValue / paddingScale : min - padding;
        final double paddedMax = isLogAxis ? maxValue * paddingScale : max + padding;

        return computeRange(paddedMin, paddedMax, length, labelSize);
    }

    @Override
    protected List<Double> calculateMinorTickValues() {
        return Collections.emptyList();
    }

    @Override
    protected double computeTickUnit(final double rawTickUnit) {
        return 1.0;
    }

    /**
     * Returns a {@link ObservableList} of categories plotted on this axis.
     *
     * @return ObservableList of categories for this axis.
     * @see #categories
     */
    public ObservableList<String> getCategories() {
        return categories.get();
    }

    // -------------- METHODS
    // ------------------------------------------------------------------------------------------

    /**
     * @param categories list of strings
     */
    public void setCategories(final List<String> categories) {
        if (categories == null) {
            forceAxisCategories = false;
            setCategories(FXCollections.<String>observableArrayList());
            return;
        }
        forceAxisCategories = true;
        setCategories(FXCollections.observableArrayList(categories));
    }

    /**
     * The ordered list of categories plotted on this axis. This is set automatically based on the charts data if
     * autoRanging is true. If the application sets the categories then auto ranging is turned off. If there is an
     * attempt to add duplicate entry into this list, an {@link IllegalArgumentException} is thrown. setting the
     * category via axis forces the axis' category, setting the axis categories to null forces the dataset's category
     *
     * @param categoryList the category list
     */
    public void setCategories(final ObservableList<String> categoryList) {
        if (categoryList == null) {
            forceAxisCategories = false;
            setCategories(FXCollections.<String>observableArrayList());
            return;
        }

        setTickLabelFormatter(new StringConverter<Number>() {

            @Override
            public Number fromString(String string) {
                for (int i = 0; i < getCategories().size(); i++) {
                    if (getCategories().get(i).equalsIgnoreCase(string)) {
                        return i;
                    }
                }
                throw new IllegalArgumentException("Category not found.");
            }

            @Override
            public String toString(Number object) {
                final int index = Math.round(object.floatValue());
                if (index < 0 || index >= getCategories().size()) {
                    return "unknown category";
                }
                return getCategories().get(index);
            }
        });
        categories.set(categoryList);

        requestAxisLayout();
    }

    /**
     * Update the categories based on the data labels attached to the DataSet values
     *
     * @param dataSet data set from which the data labels are used as category
     * @return true is categories were modified, false, false otherwise
     */
    public boolean updateCategories(final DataSet dataSet) {
        if (dataSet == null || forceAxisCategories) {
            return false;
        }

        final List<String> newCategoryList = new ArrayList<>();
        final boolean result = dataSet.lock().readLockGuard(() -> {
            boolean zeroDataLabels = true;
            for (int i = 0; i < dataSet.getDataCount(DataSet.DIM_X); i++) {
                final String dataLabel = dataSet.getDataLabel(i);
                String sanitizedLabel;
                if (dataLabel == null) {
                    sanitizedLabel = "unknown category";
                } else {
                    sanitizedLabel = dataLabel;
                    zeroDataLabels = false;
                }
                newCategoryList.add(sanitizedLabel);
            }
            return zeroDataLabels;
        });

        if (!result) {
            setCategories(newCategoryList);
            forceAxisCategories = false;
        }

        return false;
    }
    
    public double calculateWidth() {
		return computePrefWidth(getHeight());
	}
    
	// Added just to remove [] units.
	@Override
	protected void updateAxisLabelAndUnit() {
        final String axisPrimaryLabel = getName();
        final boolean isAutoScaling = isAutoUnitScaling();
        if (isAutoScaling) {
            updateScaleAndUnitPrefix();
        }

        getAxisLabel().setText(axisPrimaryLabel);
        getAxisLabel().applyCss();
    }
}
