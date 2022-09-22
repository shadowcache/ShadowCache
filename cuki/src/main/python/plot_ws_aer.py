import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import sys

mbf_data = {
    "msr": np.array([
        [0, 64, 0.4280],
        [1, 128, 0.4195],
        [2, 256, 0.3663],
        [3, 512, 0.2826],
        [3, 1024, 0.1823],
    ]),
    "twitter": np.array([
        [0, 64, 0.116378],
        [1, 128, 0.140683],
        [2, 256, 0.165441],
        [3, 512, 0.124802],
        [4, 1024, 0.079372],
    ]),
}

ccf_data = {
    "msr": np.array([
        [0, 64, 0.0273],
        [1, 128, 0.0258],
        [2, 256, 0.0236],
        [3, 512, 0.0192],
        [3, 1024, 0.0201],
    ]),
    "twitter": np.array([
        [0, 64, 0.008365],
        [1, 128, 0.006095],
        [2, 256, 0.010722],
        [3, 512, 0.024362],
        [4, 1024, 0.25234],
    ]),
}

dataset = "twitter"

fontsize = 16
num_subplots = 1
plt.figure(0, figsize=(4, 3))

# 2. Bytes
plt.subplot(num_subplots, 1, 1)
fig, ax = plt.subplots()
ax.set_xscale('log', base=2)
# ax.set_yscale('log', basey=2)
plt.plot(mbf_data[dataset][:, 1], mbf_data[dataset][:, 2], lw=3, linestyle='-', marker='o', markersize=10, color='c', label='MBF(Bytes)')
plt.plot(ccf_data[dataset][:, 1], ccf_data[dataset][:, 2], lw=3, linestyle='-', marker='^', markersize=10, color='r', label='CCF(Bytes)')
# plt.ticklabel_format(axis="y", style="sci", scilimits=(0, 0))
plt.legend(loc='best', ncol=3, fontsize=fontsize)
plt.ylabel('Average Relative Error', fontsize=fontsize)
plt.tick_params(labelsize=fontsize)
plt.xlabel('Window Size (K)', fontsize=fontsize)

fig_name = '../figs/' + dataset + '_ws_are_byte.png'
print(fig_name)
plt.savefig(fig_name, dpi=600, bbox_inches='tight')
