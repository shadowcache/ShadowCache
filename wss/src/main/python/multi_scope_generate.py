import pandas as pd
import numpy
import os
import re
import time

from pandas.core.indexes.base import Index


# 32 76253021 rows
def scope2(x):
    a = x.split('.')
    return ".".join(a[:-1])+a[-1]


def parse_time(window_time):
    return (window_time / WINDOWS_TICK - SEC_TO_UNIX_EPOCH)


def generate():
    #cost: 17.02873420715332
    dataset=pd.DataFrame()
    start = time.time()
    skipNum=4
    idx=0
    for filename in os.listdir(path):
        if idx >= scopeNum+skipNum:
            break
        if idx < skipNum:
            idx+=1
            continue
        print('[+] in dataset', filename)
        csv = pd.read_csv(path+'/'+filename,header=None,nrows=67108864,usecols=[0,4,5])
        startTimeStamp = parse_time(csv[0][0])
        print('[-] 7 day data num:',csv.shape[0])
        csv = csv[(parse_time(csv[0])-startTimeStamp)<=maxTimeStamp]
        scopeList = re.split("[.]",filename)[:-1]
        scopeName = datasetName+"."+"".join(scopeList)
        csv.insert(1,1,scopeName)
        print('[-] 1 day data num:',csv.shape[0])
        dataset = dataset.append(csv,ignore_index=True)
        # print(dataset)
        idx += 1

    dataset = dataset.sort_values(by=0)
    print('cost:',time.time()-start)
    print(dataset)
    dataset.to_csv(outPath+'/multiScope_'+maxTimeStampStr+'_'+str(scopeNum)+'.csv',header=None,index=None)

def replace_scope():
    #cost: 34.83781719207764 
    for filename in os.listdir(outPath):
        csv = pd.read_csv(outPath+'/'+filename,header=None)
        start = time.time()
        csv[1] = csv[1].apply(scope2)
        print('cost:',time.time()-start)
        print(csv[1])
        break

def find_max_timestamp():
    maxtime = 0
    maxstr = ''
    for filename in os.listdir(path):
        csv = pd.read_csv(path+'/'+filename,header=None,nrows=2)
        if csv[0][0] > maxtime:
            maxtime = csv[0][0]
            maxstr = filename
        print(csv[0][0])
    print(maxtime,maxstr)

def get_1d(filename):
    csv = pd.read_csv(path+'/'+filename,header=None,nrows=67108864)
    startTimeStamp = parse_time(csv[0][0])
    print('[-] 7 day data num:',csv.shape[0])
    csv = csv[(parse_time(csv[0])-startTimeStamp)<=maxTimeStamp]
    print('[-] 1 day data num:',csv.shape[0])
    csv.to_csv(h1Path+"/"+"proj_2_1d.csv",header = None,index = None)
    
def print_time(filename):
    csv = pd.read_csv(path+'/'+filename,header=None,nrows=67108864)
    startTimeStamp = parse_time(csv[0][0])
    print(parse_time(csv[0])-startTimeStamp)
path="D:/DevelopmentData/msr-data/unzip"
outPath="D:/DevelopmentData/msr-data/merge"
h1Path="D:/DevelopmentData/msr-data/data_1d"
datasetName = "msr"
scopeNum=8
maxTimeStamp=86400 # 1d
maxTimeStampStr='1d'
WINDOWS_TICK = 10000000
SEC_TO_UNIX_EPOCH = 11644473600

generate()
# find_max_timestamp()