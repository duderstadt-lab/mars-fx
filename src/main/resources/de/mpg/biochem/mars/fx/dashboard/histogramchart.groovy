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

import java.util.Random

Random r = new Random()

//Series 1 Outputs
#@OUTPUT Double[] series1_values
#@OUTPUT String series1_strokeColor
#@OUTPUT Integer series1_strokeWidth

series1_strokeColor = "rgb(" + r.nextInt(255) + "," + r.nextInt(255) + "," + r.nextInt(255) + ")";
series1_strokeWidth = 2

series1_values = new Double[1000]

for (int i=0; i<1000; i++)
	series1_values[i] = Double.valueOf(r.nextGaussian())
	
//Series 2 Outputs
#@OUTPUT Double[] series2_values
#@OUTPUT String series2_strokeColor
#@OUTPUT Integer series2_strokeWidth

series2_strokeColor = "rgb(" + r.nextInt(255) + "," + r.nextInt(255) + "," + r.nextInt(255) + ")";
series2_strokeWidth = 2

series2_values = new Double[1000]

for (int i=0; i<1000; i++)
	series2_values[i] = Double.valueOf(r.nextGaussian() - 5)
	
//Series 3 Outputs
#@OUTPUT Double[] series3_values
#@OUTPUT String series3_strokeColor
#@OUTPUT Integer series3_strokeWidth

series3_strokeColor = "rgb(" + r.nextInt(255) + "," + r.nextInt(255) + "," + r.nextInt(255) + ")";
series3_strokeWidth = 2

series3_values = new Double[1000]

for (int i=0; i<1000; i++)
	series3_values[i] = Double.valueOf(r.nextGaussian() + 5)
