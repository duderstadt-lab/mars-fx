#@ MoleculeArchive(required=false) archive
#@OUTPUT String xlabel
#@OUTPUT String ylabel
#@OUTPUT String title

//OUTPUT Double xmin
//OUTPUT Double xmax
//OUTPUT Double ymin
//OUTPUT Double ymax
//xmin = -2.0
//xmax = 2.0
//ymin = -2.0
//ymax = 2.0

//Set global outputs
xlabel = "X"
ylabel = "Y"
title = "XY Chart"

//Series 1 Outputs
#@OUTPUT Double[] series1_xvalues
#@OUTPUT Double[] series1_yvalues
#@OUTPUT String series1_fillColor
#@OUTPUT String series1_strokeColor

import java.util.Random

count = 100

Random r = new Random()

series1_strokeColor = "rgb(" + r.nextInt(255) + "," + r.nextInt(255) + "," + r.nextInt(255) + ")";
series1_fillColor = series1_strokeColor

series1_xvalues = new Double[count]
series1_yvalues = new Double[count]

double currentY = 0
for (int i=0; i<count; i++) {
	series1_xvalues[i] = i
	series1_yvalues[i] = Double.valueOf(currentY)
	currentY += r.nextGaussian()
}