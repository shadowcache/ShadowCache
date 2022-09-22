#!/bin/bash
set -ex

source bin/bench_one.sh

JAVA="java"
JAR="./target/working-set-size-estimation-1.0-SNAPSHOT-jar-with-dependencies.jar"
CLASS_NAME="alluxio.client.file.cache.benchmark.BenchmarkMain"
BENCHMARK="accuracy"
DATASET="ycsb" # optional: msr, twitter, ycsb, random, sequential
SHADOW_CACHE="SWAMP" # optional: ccf, mbf, bmc
TRACE="/datasets/ycsb/ycsb-1m-10m-1m-concat6.csv"
MAX_ENTRIES=10000000 # 12m
WINDOW_SIZE=262144 # 256k
NUM_UNIQUE_ENTRIES=262144 # used for random & sequential benchmark
REPORT_DIR="/datasets/benchmarks/memory-swamp-ycsb-concat6"
REPORT_INTERVAL=64 # report metrics every 64 time units


OPPO_AGING=false; # true for CCF(oa); false for CCF
TAGS_PER_BUCKET=4; TAG_BITS=8;
SIZE_ENCODE="BUCKET"; SIZE_BITS=32; SIZE_BUCKET_BITS=12;
CLOCK_BITS=8;
TIME_DIVISOR=1;
memory=40;

#
#twitter
#for memory in `seq 160 32 384`; do
#msr
#for memory in `seq 40 8 96`; do
#ycsb
for memory in `seq 320 64 768`; do
  MEMORY="${memory}kb"
  bench_one_swamp
done
