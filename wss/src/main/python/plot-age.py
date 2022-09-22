import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import sys

fontsize = 12
num_subplots = 2
plt.figure(0, figsize=(9, 6))

X = [94,99,94,101,87,90,105,103,88,104,95,94,100,100,90,100,100,98,88,101,96,106,103,99,97,102,105,102,108,88,94,88,114,96,105,95,105,105,99,100,101,103,99,112,99,108,92,105,102,101,85,101,104,115,84,109,106,105,112,108,98,93,100,97,115,107,107,99,113,111,106,111,95,136,106,117,124,129,109,115,123,112,115,110,125,122,120,133,120,128,122,139,114,113,102,125,129,122,127,114,140,123,119,128,125,118,134,133,122,124,117,129,113,130,119,117,123,124,130,116,124,120,120,129,129,134,131,113,113,146,119,113,151,115,105,151,132,126,132,133,118,117,124,122,117,118,119,134,128,128,135,124,129,142,113,129,132,123,132,131,137,134,132,132,134,133,134,129,124,140,124,122,156,123,124,131,131,147,127,128,130,140,153,132,143,128,143,129,132,133,133,131,149,135,133,140,133,143,143,140,133,145,132,144,130,131,150,132,136,159,115,138,154,116,132,166,131,139,122,149,145,129,153,129,139,155,136,141,138,145,151,129,146,153,149,134,147,133,159,146,155,138,130,142,155,137,151,138,145,142,145,140,169,148,141,105
     ]
XX = [i for i in range(len(X))]


r = 0
for i in range(len(X)):
     r += X[i] * (i+1)

mrc_mem = [0.0 for i in range(len(X))]
mrc_hr = [0.0 for i in range(len(X))]
mem = 0
h = 0.0
for i in range(len(X)-1, -1, -1):
     mem += X[i]
     h += X[i] * (i+1)
     mrc_mem[len(X) - i - 1] = mem
     mrc_hr[len(X) - i - 1] = h / r


# 1. Number
plt.subplot(num_subplots, 1, 1)
plt.bar(XX, X, label='Number of items')
plt.ticklabel_format(axis="y", style="sci", scilimits=(0, 0))
plt.legend(loc='best', ncol=3, fontsize=fontsize)
plt.ylabel('Number of items', fontsize=fontsize)
plt.xlabel('#age', fontsize=fontsize)

# 1. mrc
plt.subplot(num_subplots, 1, 2)
plt.plot(mrc_mem, mrc_hr, label='mrc')
plt.ticklabel_format(axis="y", style="sci", scilimits=(0, 0))
plt.legend(loc='best', ncol=3, fontsize=fontsize)
plt.ylabel('Hit Ratio', fontsize=fontsize)
plt.xlabel('Cache Size', fontsize=fontsize)

plt.savefig('age-8.png', dpi=600, bbox_inches='tight')
