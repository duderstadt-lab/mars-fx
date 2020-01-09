#@ MoleculeArchive(required=false) archive
#@OUTPUT String xLabel
#@OUTPUT String yLabel
#@OUTPUT String title
#@OUTPUT Integer bins
#@OUTPUT Double min
#@OUTPUT Double max

//Series 1
#@OUTPUT Double[] series1_values
#@OUTPUT Boolean series1_fill
#@OUTPUT String series1_strokeColor
#@OUTPUT Integer series1_strokeWidth

import java.util.Random

//Global Histogram Settings
title = "Histogram"
xLabel = "Position"
yLabel = "Frequency"
bins = 100
min = -10.0
max = 10.0

//Series 1 Settings
series1_fill = new Boolean(false)
series1_strokeColor = "#add8e6"
series1_strokeWidth = 3

series1_values = new Double[1000]

Random r = new Random()

for (int i=0; i<1000; i++) {
	series1_values[i] = Double.valueOf(r.nextGaussian())
}
