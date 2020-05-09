#@OUTPUT String xlabel
#@OUTPUT String ylabel
#@OUTPUT String title
#@OUTPUT Integer bins
#@OUTPUT Double xmin
#@OUTPUT Double xmax

# Set global outputs
xlabel = "Position"
ylabel = "Frequency"
title = "Histogram"
bins = 100
xmin = -10.0
xmax = 10.0

from java.util import Random
from java.lang import Double

r = Random()

# Series 1 Outputs
#@OUTPUT Double[] series1_values
#@OUTPUT String series1_strokeColor
#@OUTPUT Integer series1_strokeWidth

series1_strokeColor = "rgb(" + str(r.nextInt(255)) + "," + str(r.nextInt(255)) + "," + str(r.nextInt(255)) + ")"
series1_strokeWidth = 2

series1_values = []

for i in range(999):
	series1_values.append(Double.valueOf(r.nextGaussian()))
	
# Series 2 Outputs
#@OUTPUT Double[] series2_values
#@OUTPUT String series2_strokeColor
#@OUTPUT Integer series2_strokeWidth

series2_strokeColor = "rgb(" + str(r.nextInt(255)) + "," + str(r.nextInt(255)) + "," + str(r.nextInt(255)) + ")"
series2_strokeWidth = 2

series2_values = []

for i in range(999):
	series2_values.append(Double.valueOf(r.nextGaussian() - 5))
	
# Series 3 Outputs
#@OUTPUT Double[] series3_values
#@OUTPUT String series3_strokeColor
#@OUTPUT Integer series3_strokeWidth

series3_strokeColor = "rgb(" + str(r.nextInt(255)) + "," + str(r.nextInt(255)) + "," + str(r.nextInt(255)) + ")"
series3_strokeWidth = 2

series3_values = []

for i in range(999):
	series3_values.append(Double.valueOf(r.nextGaussian() + 5))
