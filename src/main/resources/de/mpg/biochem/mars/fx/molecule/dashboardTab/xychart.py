#@ MoleculeArchive(required=false) archive
#@OUTPUT String xlabel
#@OUTPUT String ylabel
#@OUTPUT String title

# OUTPUT Double xmin
# OUTPUT Double xmax
# OUTPUT Double ymin
# OUTPUT Double ymax
# xmin = -2.0
# xmax = 2.0
# ymin = -2.0
# ymax = 2.0

# Set global outputs
xlabel = "X"
ylabel = "Y"
title = "XY Chart"

import math
from java.util import Random
from java.lang import Double

r = Random()

# Series 1 Outputs
#@OUTPUT Double[] series1_xvalues
#@OUTPUT Double[] series1_yvalues
#@OUTPUT Double[] series1_error
#@OUTPUT String series1_fillColor
#@OUTPUT String series1_strokeColor
#@OUTPUT Integer series1_strokeWidth

series1_strokeColor = "rgb(" + str(r.nextInt(255)) + "," + str(r.nextInt(255)) + "," + str(r.nextInt(255)) + ")"
series1_fillColor = series1_strokeColor
series1_strokeWidth = 1

series1_xvalues = []
series1_yvalues = []
series1_error = []

currentY = 0
for i in range(29):
	series1_xvalues.append(i)
	series1_yvalues.append(currentY)
	currentY += r.nextGaussian()
	series1_error.append(abs(r.nextGaussian()))
	
	r = Random()

# Series 2 Outputs
#@OUTPUT Double[] series2_xvalues
#@OUTPUT Double[] series2_yvalues
#@OUTPUT Double[] series2_error
#@OUTPUT String series2_fillColor
#@OUTPUT String series2_strokeColor
#@OUTPUT Integer series2_strokeWidth

series2_strokeColor = "rgb(" + str(r.nextInt(255)) + "," + str(r.nextInt(255)) + "," + str(r.nextInt(255)) + ")"
series2_fillColor = series1_strokeColor
series2_strokeWidth = 1

series2_xvalues = []
series2_yvalues = []
series2_error = []

currentY = 0
for i in range(29):
	series2_xvalues.append(i)
	series2_yvalues.append(currentY)
	currentY += r.nextGaussian()
	series2_error.append(abs(r.nextGaussian()))
	
# Series 3 Outputs
#@OUTPUT Double[] series3_xvalues
#@OUTPUT Double[] series3_yvalues
#@OUTPUT Double[] series3_error
#@OUTPUT String series3_fillColor
#@OUTPUT String series3_strokeColor
#@OUTPUT Integer series3_strokeWidth

series3_strokeColor = "rgb(" + str(r.nextInt(255)) + "," + str(r.nextInt(255)) + "," + str(r.nextInt(255)) + ")"
series3_fillColor = series1_strokeColor
series3_strokeWidth = 1

series3_xvalues = []
series3_yvalues = []
series3_error = []

currentY = 0
for i in range(29):
	series3_xvalues.append(i)
	series3_yvalues.append(currentY)
	currentY += r.nextGaussian()
	series3_error.append(abs(r.nextGaussian()))