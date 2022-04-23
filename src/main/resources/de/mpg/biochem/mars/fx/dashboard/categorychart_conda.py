#@ Float width
#@ Float height
#@OUTPUT String imgsrc

import marspylib as mars
import seaborn as sns
import pandas as pd
import matplotlib.pyplot as plt

data = {'Categories': ['Triangles', 'Squares', 'Circles'], 'Frequency': [20, 50, 30]}  

sns.set_theme(style="whitegrid")
df = pd.DataFrame(data)  
ax = sns.barplot(x="Categories", y="Frequency", data=df)

fig = plt.gcf()
fig.set_size_inches(width, height)
fig.tight_layout()

imgsrc = mars.figure_to_imgsrc(plt.gcf())