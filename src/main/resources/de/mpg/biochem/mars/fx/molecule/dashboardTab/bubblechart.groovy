#@ MoleculeArchive(required=false) archive
#@OUTPUT String xlabel
#@OUTPUT String ylabel
#@OUTPUT String title
#@OUTPUT Double xmin
#@OUTPUT Double xmax
#@OUTPUT Double ymin
#@OUTPUT Double ymax

//Set global outputs
xlabel = "X"
ylabel = "Y"
title = "Bubble chart"

xmin = -2.0
xmax = 2.0
ymin = -2.0
ymax = 2.0

//Series 1 Outputs
#@OUTPUT Double[] series1_xvalues
#@OUTPUT Double[] series1_yvalues
#@OUTPUT Double[] series1_size
#@OUTPUT String[] series1_label
#@OUTPUT String[] series1_color
#@OUTPUT String series1_markerColor

series1_markerColor = "lightgreen"

import java.util.Random

count = 100

series1_xvalues = new Double[count]
series1_yvalues = new Double[count]
series1_size = new Double[count]
series1_color = new String[count]
series1_label = new String[count]

Random r = new Random()

for (int i=0; i<count; i++) {
	series1_xvalues[i] = Double.valueOf(r.nextGaussian())
	series1_yvalues[i] = Double.valueOf(r.nextGaussian())
	series1_size[i] = 4/Math.sqrt(series1_xvalues[i]*series1_xvalues[i] + series1_yvalues[i]*series1_yvalues[i])
	series1_color[i] = "rgb(" + r.nextInt(255) + "," + r.nextInt(255) + "," + r.nextInt(255) + ")";
	series1_label[i] = "point " + i 
}