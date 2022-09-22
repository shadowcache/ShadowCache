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
filename = '../../../benchmarks/benchmark/test.csv'

# parse arguments
if len(sys.argv) > 1:
    filename = sys.argv[1]

X = load_csv(filename)

# Compute average error
print('Average Error of Est(Number)', np.divide(X[:, 3], X[:, 1]).mean() - 1.0)
print('Average Error of Est(Bytes)', np.divide(X[:, 4], X[:, 2]).mean() - 1.0)

fontsize = 12
num_subplots = 2
plt.figure(0, figsize=(16, 2 * 4))

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

plt.xlabel('#operation', fontsize=fontsize)

plt.savefig(filename.replace("csv", "png"), dpi=600, bbox_inches='tight')
