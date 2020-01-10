#@ MoleculeArchive(required=false) archive
#@OUTPUT String xlabel
#@OUTPUT String ylabel
#@OUTPUT String title

//Set global outputs
xlabel = "X"
ylabel = "Y"
title = "Bubble chart"

//Series 1 Outputs
#@OUTPUT Double[] series1_xvalues
#@OUTPUT Double[] series1_yvalues
#@OUTPUT Double[] series1_size
#@OUTPUT String[] series1_label
#@OUTPUT String series1_markerColor

series1_markerColor = "lightgreen"

import java.util.Random

series1_xvalues = new Double[20]
series1_yvalues = new Double[20]
series1_size = new Double[20]

series1_label = new String[20]

Random r = new Random()

for (int i=0; i<20; i++) {
	series1_xvalues[i] = Double.valueOf(r.nextGaussian())
	series1_yvalues[i] = Double.valueOf(r.nextGaussian())
	series1_size[i] = 3 + Double.valueOf(r.nextGaussian()*4)
	series1_label[i] = "point " + i 
}