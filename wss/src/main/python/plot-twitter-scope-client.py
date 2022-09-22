import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
import utils

file = 'G:/datasets/cluster37.0'
nrows = 1024 * 1024 * 12


def load_csv(filename):
    df = pd.read_csv(filename, header=None, nrows=nrows)
    df = df.reset_index()
    return df.to_numpy()


# 2: item, 4: size, 5: client_id
X = load_csv(file)
print(X[0])

# parse scope
items = {}
scopes = {} # map scope to items
for i in range(len(X)):
    item = X[i, 2]
    client_id = X[i, 5]
    if client_id not in scopes:
        scopes[client_id] = {}
    if item not in scopes[client_id]:
        scopes[client_id][item] = 1
scope2count = {}
for (scope, items) in scopes.items():
    scope2count[scope] = len(items)
    print('ClientID:', scope, ', num_items:', len(items))

num_subplots = 1
fontsize = 12
plt.figure(0, figsize=(4, 3))

scope_cardinality = np.array(list(scope2count.items()))
cardinality = [int(x) for x in scope_cardinality[:, 1]]
plt.bar(range(0, len(scope_cardinality[:, 0])), cardinality, label="Twitter")
plt.xlabel('Client Id', fontsize=fontsize)
plt.ylabel('Cardinality', fontsize=fontsize)
plt.legend(loc='best', ncol=3, fontsize=fontsize)

pngfile = 'imgs/dataset/twitter-scope-client-12m.png'
plt.savefig(pngfile, dpi=600, bbox_inches='tight')
