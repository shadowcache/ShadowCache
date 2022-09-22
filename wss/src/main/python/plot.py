import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import sys


def load_csv(filename):
    df = pd.read_table(filename)
    return df.values


# 0             1       2           3   4           5       6
# #operation	Real	Real(bytes)	MBF	MBF(bytes)	CCF	CCF(bytes)
# filename = '../benchmarks/benchmark/test.csv'
filename  = 'testTwitter37.csv'
# parse arguments
if len(sys.argv) > 1:
    filename = sys.argv[1]

X = load_csv(filename)

# Compute average error
print('Average Error of MBF(Number)', np.divide(X[:, 3], X[:, 1]).mean() - 1.0)
print('Average Error of CCF(Number)', np.divide(X[:, 5], X[:, 1]).mean() - 1.0)
print('Average Error of BMC(Number)', np.divide(X[:, 7], X[:, 1]).mean() - 1.0)

print('Average Error of MBF(Bytes)', np.divide(X[:, 4], X[:, 2]).mean() - 1.0)
print('Average Error of CCF(Bytes)', np.divide(X[:, 6], X[:, 2]).mean() - 1.0)
print('Average Error of BMC(Bytes)', np.divide(X[:, 8], X[:, 2]).mean() - 1.0)

fontsize = 12
num_subplots = 2
plt.figure(0, figsize=(12, num_subplots * 4))

# 1. Number
plt.subplot(num_subplots, 1, 1)
plt.plot(X[:, 0], X[:, 1], lw=1, ls='-', color='k', label='Exact(Number)')
plt.plot(X[:, 0], X[:, 3], lw=1, ls='--', color='c', label='MBF(Number)')
plt.plot(X[:, 0], X[:, 5], lw=1, ls=':', color='r', label='CCF(Number)')
plt.plot(X[:, 0], X[:, 7], lw=1, ls='-.', color='g', label='BMC(Number)')
plt.ticklabel_format(axis="y", style="sci", scilimits=(0, 0))
plt.legend(loc='best', ncol=3, fontsize=fontsize)
plt.ylabel('Working Set Size (Number)', fontsize=fontsize)
plt.xticks([])

# 2. Bytes
plt.subplot(num_subplots, 1, 2)
plt.plot(X[:, 0], X[:, 2], lw=1, ls='-', color='k', label='Exact(Bytes)')
plt.plot(X[:, 0], X[:, 4], lw=1, ls='--', color='c', label='MBF(Bytes)')
plt.plot(X[:, 0], X[:, 6], lw=1, ls=':', color='r', label='CCF(Bytes)')
plt.plot(X[:, 0], X[:, 8], lw=1, ls='-.', color='g', label='BMC(Bytes)')
plt.ticklabel_format(axis="y", style="sci", scilimits=(0, 0))
plt.legend(loc='best', ncol=3, fontsize=fontsize)
plt.ylabel('Working Set Size (Bytes)', fontsize=fontsize)

plt.xlabel('Time', fontsize=fontsize)

plt.savefig(filename.replace("csv", "png"), dpi=600, bbox_inches='tight')
