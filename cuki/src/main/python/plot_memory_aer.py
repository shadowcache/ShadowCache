import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import sys

mbf_data = {
    "msr": np.array([
        [0, 64, 0.3655],
        [1, 128, 0.3660],
        [2, 256, 0.3663],
        [3, 512, 0.3659],
        [4, 1024, 0.3661],
    ]),
    "twitter": np.array([
        # [0, 256, 0.0830],
        [1, 512, 0.1654],
        [2, 1024, 0.1654],
        [3, 2048, 0.1655],
        [4, 4096, 0.1655],
    ]),
}

ccf_data = {
    "msr": np.array([
        [0, 64, 0.1114],
        [1, 128, 0.0250],
        [2, 256, 0.0236],
        [3, 512, 0.0235],
        [4, 1024, 0.0234],
    ]),
    "twitter": np.array([
        # [0, 256, 0.7541],
        [1, 512, 0.0184],
        [2, 1024, 0.0107],
        [3, 2048, 0.0091],
        [4, 4096, 0.0074],
    ]),
}

dataset = "twitter"

fontsize = 16
num_subplots = 1
plt.figure(0, figsize=(4, 3))

plt.subplot(num_subplots, 1, 1)
fig, ax = plt.subplots()
ax.set_xscale('log', base=2)
# ax.set_yscale('log', basey=2)
plt.plot(mbf_data[dataset][:, 1], mbf_data[dataset][:, 2], lw=3, linestyle='-', marker='o', markersize=10, color='c', label='MBF(Bytes)')
plt.plot(ccf_data[dataset][:, 1], ccf_data[dataset][:, 2], lw=3, linestyle='-', marker='^', markersize=10, color='r', label='CCF(Bytes)')
# plt.ticklabel_format(axis="y", style="sci", scilimits=(0, 0))
plt.legend(loc='best', ncol=3, fontsize=fontsize)
plt.ylabel('Average Relative Error', fontsize=fontsize)
plt.xlabel('Memory (KB)', fontsize=fontsize)
plt.tick_params(labelsize=fontsize)

fig_name = '../figs/' + dataset + '_memory_are_byte.png'
print(fig_name)
plt.savefig(fig_name, dpi=600, bbox_inches='tight')
