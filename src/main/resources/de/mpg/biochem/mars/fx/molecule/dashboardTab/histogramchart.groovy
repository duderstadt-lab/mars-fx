#@ MoleculeArchive(required=false) archive
#@OUTPUT String xlabel
#@OUTPUT String ylabel
#@OUTPUT String title
#@OUTPUT Integer bins
#@OUTPUT Double xmin
#@OUTPUT Double xmax

//Set global outputs
xlabel = "Position"
ylabel = "Frequency"
title = "Histogram"
bins = 100
xmin = -10.0
xmax = 10.0

//Series 1 Outputs
#@OUTPUT Double[] series1_values
#@OUTPUT String series1_strokeColor
#@OUTPUT Integer series1_strokeWidth

series1_strokeColor = "#add8e6"
series1_strokeWidth = 2

import java.util.Random

series1_values = new Double[1000]

Random r = new Random()

for (int i=0; i<1000; i++)
	series1_values[i] = Double.valueOf(r.nextGaussian())
