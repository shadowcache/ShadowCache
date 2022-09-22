import matplotlib.pyplot as plt
import pandas as pd
import numpy as np


# compute freq cdf
def cdf(X):
    # count frequency
    items = {}
    num_items = len(X)
    for i in range(0, num_items):
        id = X[i, 0]
        if id not in items:
            items[id] = 0
        items[id] += 1
    # cumulate cdf
    num_unique_items = len(items)
    max_freq = max(items.values())
    freq_histo = np.zeros(max_freq+1, dtype=np.int32)
    for freq in items.values():
        freq_histo[freq] += 1
    freq_dist = freq_histo / float(num_unique_items)
    freq_cdf = np.cumsum(freq_dist)
    return np.array(range(0, max_freq+1)), freq_cdf

# compute cdf
def cdf2(X):
    # map item to frequency
    item2freq = {}
    num_items = len(X)
    for i in range(0, num_items):
        id = X[i, 0]
        if id not in item2freq:
            item2freq[id] = 0
        item2freq[id] += 1
    num_unique_items = len(X)
    freq_histo = np.array(list(item2freq.items()))
    sort_cols = freq_histo[:, 0].argsort()
    sorted_freq_histo = freq_histo[sort_cols, :]
    freq_dist = sorted_freq_histo[:, 1] / float(num_unique_items)
    freq_cdf = np.cumsum(freq_dist)
    return sorted_freq_histo[:, 0], freq_cdf
