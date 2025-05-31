#@ Float width
#@ Float height
#@OUTPUT String imgsrc

import marspylib as mars
import seaborn as sns
import pandas as pd
import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

df = pd.DataFrame()
df['Position'] = np.concatenate((np.random.normal(0, 1, 1000), np.random.normal(5, 1, 1000), np.random.normal(-5, 1, 1000)))
df['group'] = np.concatenate((np.repeat(0, 1000), np.repeat(1, 1000), np.repeat(2, 1000)))

sns.set_theme(style="white")
ax = sns.histplot(data=df, x="Position", bins=100, hue="group", element="step", 
		  fill=False, palette=sns.color_palette("husl", 3)).set(title="Histogram")

fig = plt.gcf()
fig.set_size_inches(width, height)
fig.tight_layout()

imgsrc = mars.figure_to_imgsrc(fig)