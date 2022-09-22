import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
import utils

file='G:/datasets/cluster37.0'
nrows = 1024*1024

def load_csv(filename):
    df = pd.read_csv(filename, header=None, nrows=nrows)
    df = df.reset_index()
    return df.to_numpy()

# 2: item, 4: size
X = load_csv(file)
print(X[0])

num_subplots = 2
fontsize = 12
plt.figure(0, figsize=(4 * num_subplots, 3))

# 1. plot frequency cdf
ax = plt.subplot(1, num_subplots, 1)
freq_x, freq_cdf = utils.cdf(X[:, [2]])
plt.plot(freq_x, freq_cdf, lw=1, linestyle='-', marker='o', markersize=3, markerfacecolor='none',
         markevery=0.1, label="Twitter")
plt.xlabel('Item Occurrence', fontsize=fontsize)
plt.ylabel('CDF', fontsize=fontsize)
max_freq = max(freq_x)
print('max freq', max_freq)
# ax.set_xscale('log', base=10)
# plt.xlim(left=0)
# plt.ylim((-2, 2))
# x_ticks = np.arange(0, max_freq, 5000)
y_ticks = np.arange(0, 1.2, 0.2)
# plt.xticks(x_ticks)
plt.yticks(y_ticks)
plt.legend(loc='best', ncol=3, fontsize=fontsize)

# 2. plot size cdf
ax = plt.subplot(1, num_subplots, 2)
size_x, size_cdf = utils.cdf2(X[:, [4]])
plt.plot(size_x, size_cdf, lw=1, linestyle='-', marker='o', markersize=3, markerfacecolor='none',
         markevery=0.1, label="Twitter")
plt.xlabel('Request Size (Byte)', fontsize=fontsize)
plt.ylabel('CDF', fontsize=fontsize)
# ax.set_xscale('log', base=2)
# x_ticks = np.arange(0, 300*1024, 64*1024)
y_ticks = np.arange(0, 1.2, 0.2)
# plt.xticks(x_ticks)
plt.yticks(y_ticks)
plt.legend(loc='best', ncol=3, fontsize=fontsize)
print('size', size_x)

pngfile = 'imgs/dataset/twitter-cdf.png'
plt.savefig(pngfile, dpi=600, bbox_inches='tight')
