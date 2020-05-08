#@OUTPUT String xlabel
#@OUTPUT String ylabel
#@OUTPUT String title
#@OUTPUT Double xmin
#@OUTPUT Double xmax
#@OUTPUT Double ymin
#@OUTPUT Double ymax

# Set global outputs
xlabel = "X"
ylabel = "Y"
title = "Bubble chart"

xmin = -2.0
xmax = 2.0
ymin = -2.0
ymax = 2.0

import math
from java.util import Random
from java.lang import Double

r = Random()

# Series 1 Outputs
#@OUTPUT Double[] series1_xvalues
#@OUTPUT Double[] series1_yvalues
#@OUTPUT Double[] series1_size
#@OUTPUT String[] series1_label
#@OUTPUT String[] series1_color
#@OUTPUT String series1_markerColor

series1_markerColor = "lightgreen"

series1_xvalues = []
series1_yvalues = []
series1_size = []
series1_color = []
series1_label = []

for i in range(99):
	series1_xvalues.append(Double.valueOf(r.nextGaussian()))
	series1_yvalues.append(Double.valueOf(r.nextGaussian()))
	series1_size.append(Double.valueOf(4/math.sqrt(series1_xvalues[i]*series1_xvalues[i] + series1_yvalues[i]*series1_yvalues[i])))
	series1_color.append("rgb(" + str(r.nextInt(255)) + "," + str(r.nextInt(255)) + "," + str(r.nextInt(255)) + ")")
	series1_label.append("point " + str(i))  