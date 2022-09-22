#!/bin/bash
set -ex

source bin/bench_one.sh

JAVA="java"
JAR="./target/working-set-size-estimation-1.0-SNAPSHOT-jar-with-dependencies.jar"
CLASS_NAME="alluxio.client.file.cache.benchmark.BenchmarkMain"
BENCHMARK="time_multi"
DATASET="multi" # optional: msr, twitter, random, sequential
SHADOW_CACHE="bmc" # optional: ccf, mbf
TRACE="/datasets/multi/multiScope_1d_8_v5.csv" # path to dataset
NUM_SCOPE=8
SCOPE_BITS=0
#DATASET_NAME="multiScope_1d_16"
#TRACE="/datasets/cluster37.0" # twitter
MAX_ENTRIES=125829120000 # 120000m
WINDOW_SIZE_RAW=3600000  # 1h
NUM_UNIQUE_ENTRIES=262144 # used for random & sequential benchmark
REPORT_DIR="/datasets/benchmarks/multi_scope"
REPORT_INTERVAL=1000
SIZE_BITS=32
TIME_DIVISOR=144
WINDOW_SIZE=`expr $WINDOW_SIZE_RAW / $TIME_DIVISOR`

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

OPPO_AGING=false;

MEMORY="24mb";
SIZE_ENCODE="NONE"; 
SIZE_BITS=32;
CLOCK_BITS=4;
bench_one_bmc