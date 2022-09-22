import matplotlib.pyplot as plt
import pandas as pd
import numpy as np

file='G:/datasets/msr-cambridge1/prxy_0.csv'
nrows = 1024*1000

def load_csv(filename):
    df = pd.read_csv(filename, header=None, nrows=nrows)
    df = df.reset_index()
    return df.to_numpy()

X = load_csv(file)

num_subplots = 4
plt.figure(0, figsize=(16, 4 *4))

# 1. Object
plt.subplot(num_subplots, 1, 1)
plt.scatter(X[:,0], X[:,5], s=0.01, color='k', label='Offset')
plt.ticklabel_format(axis="y", style="sci", scilimits=(0,0))
plt.legend(loc='upper left', ncol=2)
plt.ylabel('Offset')

# 2. Size
plt.subplot(num_subplots, 1, 2)
plt.scatter(X[:,0], X[:,6], s=0.01, color='k', label='Size(Bytes)')
plt.ticklabel_format(axis="y", style="sci", scilimits=(0,0))
plt.legend(loc='upper left', ncol=2)
plt.ylabel('Size (Bytes)')

# 3. sliding window average size
num_groups = 64
size_per_group = 64 * 1024
window_size = 64 * 1024
total_size_ess = [0 for _ in range(0, num_groups)]
total_size_real = [0 for _ in range(0, num_groups)]
total_count = [0 for _ in range(0, num_groups)]
real_size = 0
est_size = 0
data = np.zeros((nrows, 3))
data_group = np.zeros((nrows, num_groups*2))
for i in range(0, nrows):
    size = X[i, 6]
    group = int(size / size_per_group)
    if i >= window_size:
        # evict one
        real_size -= X[i-window_size, 6]
        evict_group = int(X[i-window_size, 6] / size_per_group)
        total_size_real[evict_group] -= X[i-window_size, 6]
        evict_size = round(total_size_ess[evict_group] / total_count[evict_group])
        total_size_ess[evict_group] -= evict_size
        est_size -= evict_size
        total_count[evict_group] -= 1
    real_size += size
    est_size += size
    total_size_real[group] += size
    total_count[group] += 1
    total_size_ess[group] += size
    data[i][0] = i
    data[i][1] = real_size
    data[i][2] = est_size
    for j in range(num_groups):
        data_group[i][j*2] = total_size_real[j]
        data_group[i][j*2+1] = total_size_ess[j]

plt.subplot(num_subplots, 1, 3)
plt.scatter(data[:,0], data[:,1], s=0.01, color='k', label='Real(Bytes)')
plt.scatter(data[:,0], data[:,2], s=0.01, color='r', label='SlidingWindow(Bytes)')
plt.legend(loc='upper left', ncol=2)
plt.ylabel('Size (Bytes)')

## 4. per group sliding window size
plt.subplot(num_subplots, 1, 4)
for j in range(num_groups):
    if total_size_real[j] == 0:
        continue
    plt.plot(data[:,0], data_group[:,j*2], lw=1, ls='-', color='k', label='Real_'+str(j))
    plt.plot(data[:,0], data_group[:,j*2+1], lw=1, ls='--', color='c', label='ESS_'+str(j))
plt.legend(loc='upper left', ncol=2)
plt.ylabel('Size (Bytes)')

plt.xlabel('time')
pngfile = file.replace('csv', 'png')
plt.savefig(pngfile, dpi=300)
