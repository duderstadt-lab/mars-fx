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
df['X'] = np.random.normal(0, 1, 30)
df['Y'] = np.random.normal(0, 1, 30)
df['size'] = np.random.randint(2, 6, 30)

ax = sns.scatterplot(
    data=df, x="X", y="Y", hue="size", size="size",
    sizes=(20, 200), hue_norm=(0, 13), legend="full")
    
ax.set(title="Bubble chart")

fig = plt.gcf()
fig.set_size_inches(width, height)
fig.tight_layout()

imgsrc = mars.figure_to_imgsrc(fig) 