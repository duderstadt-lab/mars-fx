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
//#@OUTPUT String series1_fillColor
#@OUTPUT String series1_strokeColor
//#@OUTPUT Integer series1_strokeWidth

//Global Histogram Settings
title = "Histogram"
xLabel = "Position"
yLabel = "Frequency"
bins = 10
min = 0
max = 10

//Series 1 Settings
series1_fill = new Boolean(true)
series1_fillColor = "#add8e6"
series1_strokeColor = "#add8e6"
series1_strokeWidth = 3

series1_values = new Double[5]

series1_values[0] = 3
series1_values[1] = 3
series1_values[2] = 5
series1_values[3] = 7
series1_values[4] = 7
