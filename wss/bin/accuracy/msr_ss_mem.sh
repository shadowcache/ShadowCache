#!/bin/bash
set -ex

source bin/bench_one.sh

JAVA="java"
JAR="./target/working-set-size-estimation-1.0-SNAPSHOT-jar-with-dependencies.jar"
CLASS_NAME="alluxio.client.file.cache.benchmark.BenchmarkMain"
BENCHMARK="accuracy"
#DATASET="twitter" # optional: msr, twitter, random, sequential
DATASET="msr"
#DATASET="ycsb"
SHADOW_CACHE="bms" # optional: ccf, mbf

#TRACE="/home/lisimian/datasets/twitter/cluster37-1h-new.csv" # path to dataset
TRACE="/datasets/msr/prxy_0.csv" # msr
#TRACE="/home/lisimian/datasets/ycsb/ycsb-1m-10m-1m-concat6.csv"

#DATASET_NAME="cluster37-1h-new"
#DATASET_NAME="prxy_0"
MAX_ENTRIES=12582912 # 12m
WINDOW_SIZE_RAW=262144 # 256k
WINDOW_SIZE=262144 # 256k
NUM_UNIQUE_ENTRIES=262144 # used for random & sequential benchmark
REPORT_DIR="/datasets/benchmarks/memory-ss"
REPORT_INTERVAL=64
SIZE_BITS=32
TIME_DIVISOR=1
NUM_HASH=8
NUM_SCOPE=0
SCOPE_BITS=0


#
#twitter
#for memory in `seq 160 32 384`; do
#msr
for memory in `seq 40 8 96`; do
#ycsb
#for memory in `seq 320 64 768`; do
  MEMORY="${memory}kb"
  bench_one_ss
done