#@ Float width
#@ Float height
#@OUTPUT String imgsrc

import marspylib as mars
import seaborn as sns
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

sns.set_theme(style="ticks")

rs = np.random.RandomState(11)
x = rs.gamma(2, size=1000)
y = -.5 * x + rs.normal(size=1000)

sns.jointplot(x=x, y=y, kind="hex", color="#4CB391")

fig = plt.gcf()
fig.set_size_inches(width, height)
fig.tight_layout()

imgsrc = mars.figure_to_imgsrc(fig) 