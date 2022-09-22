#!/bin/bash
set -ex

source benchmark_scripts/bench_ccf.sh

JAVA="java"
JAR="./target/working-set-size-estimation-1.0-SNAPSHOT-jar-with-dependencies.jar"
CLASS_NAME="alluxio.client.file.cache.benchmark.BenchmarkMain"
BENCHMARK="accuracy"
DATASET="msr" # optional: msr, twitter, ycsb, random, sequential
SHADOW_CACHE="ccf" # optional: ccf, mbf, bmc
TRACE="/datasets/msr-cambridge1/prxy_0.csv" # path to dataset
MAX_ENTRIES=12582912 # 12m
WINDOW_SIZE=262144 # 256k
NUM_UNIQUE_ENTRIES=262144 # used for random & sequential benchmark
REPORT_DIR="/datasets/benchmarks/clock-ccf" # path to store logs & csvs
REPORT_INTERVAL=64 # report metrics every 64 time units
SCOPE_BITS=0 # disable multiple scope

to_brief_string() {
  local size=$1
  if [[ ${size} -ge "1048576" ]]; then
    echo "$(( size / 1048576 ))m"
  elif [[ ${size} -ge "1024" ]]; then
    echo "$(( size / 1024 ))k"
  else
    echo "${size}k"
  fi
}

OPPO_AGING=false; # true for CCF(oa); false for CCF
MEMORY="256kb";
TAGS_PER_BUCKET=4; TAG_BITS=8;
SIZE_ENCODE="BUCKET"; SIZE_BITS=8; SIZE_BUCKET_BITS=12;

CLOCK_BITS_LIST="1 2 3 4 5 6 7 8";
for CLOCK_BITS in ${CLOCK_BITS_LIST}; do
  bench_one
done
