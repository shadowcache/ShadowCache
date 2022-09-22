#!/bin/bash
set -ex

source bin/bench_one.sh

JAVA="java"
JAR="./target/working-set-size-estimation-1.0-SNAPSHOT-jar-with-dependencies.jar"
CLASS_NAME="alluxio.client.file.cache.benchmark.BenchmarkMain"
BENCHMARK="accuracy"
DATASET="msr" # optional: msr, twitter, ycsb, random, sequential
SHADOW_CACHE="mbf" # optional: ccf, mbf, bmc
TRACE="/datasets/msr/prxy_0.csv" # msr
MAX_ENTRIES=12582912 # 12m
WINDOW_SIZE=262144 # 256k
NUM_UNIQUE_ENTRIES=262144 # used for random & sequential benchmark
REPORT_DIR="/datasets/benchmarks/memory-mbf" # path to store logs & csvs
REPORT_INTERVAL=64 # report metrics every 64 time units
NUM_SCOPE=0
SCOPE_BITS=0
NUM_BLOOMS=1

OPPO_AGING=false; # true for CCF(oa); false for CCF
TAGS_PER_BUCKET=4; TAG_BITS=8;
SIZE_ENCODE="BUCKET"; SIZE_BITS=32; SIZE_BUCKET_BITS=12;
CLOCK_BITS=8;
TIME_DIVISOR=1

#
#twitter
#for memory in `seq 160 32 384`; do
#msr
for memory in `seq 40 8 96`; do
#ycsb
#for memory in `seq 320 64 768`; do
  NUM_BLOOMS=`expr $memory / 8`
  MEMORY="${memory}kb"
  bench_one_mbf
done