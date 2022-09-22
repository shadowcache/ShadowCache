import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
import utils

file = 'G:/datasets/cluster37.0'
nrows = 1024 * 1024


def load_csv(filename):
    df = pd.read_csv(filename, header=None, nrows=nrows)
    df = df.reset_index()
    return df.to_numpy()


# 2: item, 4: size
# item foramt: scope1-scope2-scope3-scope4
X = load_csv(file)
print(X[0])

# parse scope
items = {}
scopes = [{} for _ in range(0, 5)]
for item in X[:, 2]:
    if item not in items:
        tokens = item.split('-')
        for i in range(0, len(tokens) - 1):
            scope = '-'.join(tokens[0:i + 1])
            if scope not in scopes[i]:
                scopes[i][scope] = 0
            scopes[i][scope] += 1
        items[item] = 0
for i in range(0, len(scopes)):
    print('Level:', i, ', num_scopes:', len(scopes[i]))

num_subplots = 5
fontsize = 12
plt.figure(0, figsize=(4 * 2, 3 * 4))
fig, axs = plt.subplots(2, 3)
fig.tight_layout()
# plt.subplots_adjust(wspace=0.2, hspace=0.2)

# plot per level scope
for i in range(0, len(scopes)):
    ax = plt.subplot((num_subplots + 1) / 2, 2, i + 1)
    scope_cardinality = np.array(list(scopes[i].items()))
    cardinality = [int(x) for x in scope_cardinality[:, 1]]
    plt.bar(range(0, len(scope_cardinality[:, 0])), cardinality, label="Twitter")
    plt.xlabel('Level' + str(i+1), fontsize=fontsize)
    plt.ylabel('Cardinality', fontsize=fontsize)
    plt.legend(loc='best', ncol=3, fontsize=fontsize)

pngfile = 'imgs/dataset/twitter-scope.png'
plt.savefig(pngfile, dpi=600, bbox_inches='tight')
