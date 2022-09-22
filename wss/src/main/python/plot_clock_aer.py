import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import sys

ccf_data = {
    "msr": np.array([
        [2, 2, 0.0619],
        [3, 3, 0.0381],
        [4, 4, 0.0236],
        [5, 5, 0.0130],
        [6, 6, 0.0074],
    ]),
    "twitter": np.array([
        [2, 2, 0.0735],
        [3, 3, 0.0290],
        [4, 4, 0.0107],
        [5, 5, 0.0107],
        [6, 6, 0.0098],
    ]),
}

dataset = "msr"

fontsize = 16
num_subplots = 1
plt.figure(0, figsize=(4, 3))

# 2. Bytes
plt.subplot(num_subplots, 1, 1)
fig, ax = plt.subplots()
# ax.set_xscale('log', base=2)
# ax.set_yscale('log', basey=2)
plt.plot(ccf_data[dataset][:, 1], ccf_data[dataset][:, 2], lw=3, linestyle='-', marker='^', markersize=10, color='r', label='CCF(Bytes)')
# plt.ticklabel_format(axis="y", style="sci", scilimits=(0, 0))
plt.legend(loc='best', ncol=3, fontsize=fontsize)
plt.ylabel('Average Relative Error', fontsize=fontsize)
plt.tick_params(labelsize=fontsize)
plt.xlabel('Clock size (bit)', fontsize=fontsize)

fig_name = '../figs/' + dataset + '_clock_are_byte.png'
print(fig_name)
plt.savefig(fig_name, dpi=600, bbox_inches='tight')
