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

import java.util.Random

count = 30
Random r = new Random()

//Series 1 Outputs
#@OUTPUT Double[] series1_xvalues
#@OUTPUT Double[] series1_yvalues
#@OUTPUT Double[] series1_error
#@OUTPUT String series1_fillColor
#@OUTPUT String series1_strokeColor
#@OUTPUT Integer series1_strokeWidth

series1_strokeColor = "rgb(" + r.nextInt(255) + "," + r.nextInt(255) + "," + r.nextInt(255) + ")";
series1_fillColor = series1_strokeColor
series1_strokeWidth = 1

series1_xvalues = new Double[count]
series1_yvalues = new Double[count]
series1_error = new Double[count]

double currentY = 0
for (int i=0; i<count; i++) {
	series1_xvalues[i] = i
	series1_yvalues[i] = Double.valueOf(currentY)
	currentY += r.nextGaussian()
	series1_error[i] = Math.abs(r.nextGaussian())
}

//Series 2 Outputs
#@OUTPUT Double[] series2_xvalues
#@OUTPUT Double[] series2_yvalues
#@OUTPUT Double[] series2_error
#@OUTPUT String series2_fillColor
#@OUTPUT String series2_strokeColor

series2_strokeColor = "rgb(" + r.nextInt(255) + "," + r.nextInt(255) + "," + r.nextInt(255) + ")";
series2_fillColor = series1_strokeColor

series2_xvalues = new Double[count]
series2_yvalues = new Double[count]
series2_error = new Double[count]

currentY = 0
for (int i=0; i<count; i++) {
	series2_xvalues[i] = i
	series2_yvalues[i] = Double.valueOf(currentY)
	currentY += r.nextGaussian()
	series2_error[i] = Math.abs(r.nextGaussian())
}

//Series 3 Outputs
#@OUTPUT Double[] series3_xvalues
#@OUTPUT Double[] series3_yvalues
#@OUTPUT Double[] series3_error
#@OUTPUT String series3_fillColor
#@OUTPUT String series3_strokeColor

series3_strokeColor = "rgb(" + r.nextInt(255) + "," + r.nextInt(255) + "," + r.nextInt(255) + ")";
series3_fillColor = series1_strokeColor

series3_xvalues = new Double[count]
series3_yvalues = new Double[count]
series3_error = new Double[count]

currentY = 0
for (int i=0; i<count; i++) {
	series3_xvalues[i] = i
	series3_yvalues[i] = Double.valueOf(currentY)
	currentY += r.nextGaussian()
	series3_error[i] = Math.abs(r.nextGaussian())
}