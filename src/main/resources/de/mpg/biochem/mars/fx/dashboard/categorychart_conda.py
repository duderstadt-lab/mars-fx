#@ Float width
#@ Float height
#@OUTPUT String imgsrc

import marspylib as mars
import matplotlib
matplotlib.use('agg')
from matplotlib.figure import Figure
import numpy as np

# Data for plotting
t = np.arange(0.0, 2.0, 0.01)
s = 1 + np.sin(2 * np.pi * t)

fig = Figure()
ax = fig.subplots()
ax.plot(t,s)
ax.set(xlabel='time (s)', ylabel='voltage (mV)',
       title='Title')
ax.grid()
fig.set_size_inches(width, height)
imgsrc = mars.figure_to_imgsrc(fig)

