#@ Float width
#@ Float height
#@OUTPUT String imgsrc

import marspylib as mars
import seaborn as sns
import pandas as pd
import numpy as np
import matplotlib.ticker as ticker
import matplotlib.pyplot as plt

d = []
for i in range(3):
   currentY = 0
   for row in range(30):
      d.append({'X': row, 'Y': currentY, 'group': i})
      currentY += np.random.normal(0, 1)

df = pd.DataFrame(d)

sns.set_theme(style="white")
ax = sns.pointplot(data=df, x="X", y="Y", hue="group", 
	palette=sns.color_palette("husl", 3))
ax.xaxis.set_major_locator(ticker.MultipleLocator(5))
ax.xaxis.set_major_formatter(ticker.ScalarFormatter())
ax.set(title="XY Chart")

fig = plt.gcf()
fig.set_size_inches(width, height)
fig.tight_layout()

imgsrc = mars.figure_to_imgsrc(fig)	