package de.mpg.biochem.mars.fx.plot.tools;

import de.gsi.chart.axes.spi.DefaultNumericAxis;

public class MarsNumericAxis extends DefaultNumericAxis {
	
	public MarsNumericAxis() {
		super();
	}

	public double calculateWidth() {
		return computePrefWidth(getHeight());
	}
	
	@Override
	protected void updateAxisLabelAndUnit() {
        final String axisPrimaryLabel = getName();
        final boolean isAutoScaling = isAutoUnitScaling();
        if (isAutoScaling) {
            updateScaleAndUnitPrefix();
        }

        axisLabel.setText(axisPrimaryLabel);
        axisLabel.applyCss();
    }
}
