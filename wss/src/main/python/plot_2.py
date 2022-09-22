#! python

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import sys


def load_csv(filename):
    df = pd.read_table(filename)
    return df.values


# 0             1       2           3   4
# #operation	Real	Real(bytes)	Est	Est(bytes)
#       5               6               7               8               9                  10
# RealRead(Page)	RealRead(Bytes)	RealHit(Page)	RealHit(Bytes)	EstHit(Page)	EstHit(Bytes)
filename = '../../../benchmarks/benchmark/test.csv'

# parse arguments
if len(sys.argv) > 1:
    filename = sys.argv[1]

X = load_csv(filename)

# Compute average error
print('Average Error of Est(Number)', np.divide(X[:, 3], X[:, 1]).mean() - 1.0)
print('Average Error of Est(Bytes)', np.divide(X[:, 4], X[:, 2]).mean() - 1.0)

fontsize = 12
num_subplots = 4
plt.figure(0, figsize=(16, num_subplots * 4))

# 1. Number
plt.subplot(num_subplots, 1, 1)
plt.plot(X[:, 0], X[:, 1], lw=1, ls='-', color='k', label='Real(Number)')
plt.plot(X[:, 0], X[:, 3], lw=1, ls='--', color='r', label='Est(Number)')
plt.ticklabel_format(axis="y", style="sci", scilimits=(0, 0))
plt.legend(loc='upper left', ncol=3, fontsize=fontsize)
plt.ylabel('WSS (Number)', fontsize=fontsize)

# 2. Bytes
plt.subplot(num_subplots, 1, 2)
plt.plot(X[:, 0], X[:, 2], lw=1, ls='-', color='k', label='Real(Bytes)')
plt.plot(X[:, 0], X[:, 4], lw=1, ls='--', color='r', label='Est(Bytes)')
plt.ticklabel_format(axis="y", style="sci", scilimits=(0, 0))
plt.legend(loc='upper left', ncol=3, fontsize=fontsize)
plt.ylabel('WSS (Bytes)', fontsize=fontsize)

# 3. Page Hit
plt.subplot(num_subplots, 1, 3)
plt.plot(X[:, 0], X[:, 5], lw=1, ls='-', color='k', label='CacheRead(Number)')
plt.plot(X[:, 0], X[:, 7], lw=1, ls=':', color='c', label='RealCacheHit(Number)')
plt.plot(X[:, 0], X[:, 9], lw=1, ls='--', color='r', label='EstCacheHit(Number)')
plt.ticklabel_format(axis="y", style="sci", scilimits=(0, 0))
plt.legend(loc='upper left', ncol=3, fontsize=fontsize)
plt.ylabel('Cache ', fontsize=fontsize)

# 4. Bytes Hit
plt.subplot(num_subplots, 1, 4)
plt.plot(X[:, 0], X[:, 6], lw=1, ls='-', color='k', label='CacheRead(Byte)')
plt.plot(X[:, 0], X[:, 8], lw=1, ls=':', color='c', label='RealCacheHit(Byte)')
plt.plot(X[:, 0], X[:, 10], lw=1, ls='--', color='r', label='EstCacheHit(Byte)')
plt.ticklabel_format(axis="y", style="sci", scilimits=(0, 0))
plt.legend(loc='upper left', ncol=3, fontsize=fontsize)
plt.ylabel('Cache ', fontsize=fontsize)

plt.xlabel('#operation', fontsize=fontsize)

plt.savefig(filename.replace("csv", "png"), dpi=600, bbox_inches='tight')
