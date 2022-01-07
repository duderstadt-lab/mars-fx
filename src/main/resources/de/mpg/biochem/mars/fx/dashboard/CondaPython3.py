#@ Context scijavaContext
#@ MoleculeArchive archive
#@ Float width
#@ Float height
#@ String path

from matplotlib.figure import Figure
import numpy as np

# Data for plotting
t = np.arange(0.0, 2.0, 0.01)
s = 1 + np.sin(2 * np.pi * t)

fig = Figure()
ax = fig.subplots()
ax.plot(t,s)
ax.set(xlabel='time (s)', ylabel='voltage (mV)',
       title='About as simple as it gets, folks')
ax.grid()
fig.set_size_inches(width, height)
fig.savefig(sj.to_python(path), bbox_inches='tight', dpi=100)