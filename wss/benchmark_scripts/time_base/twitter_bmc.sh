#!/bin/bash
set -ex

source /home/lisimian/wss/wss-estimation/benchmark_scripts/bench_bmc.sh

JAVA="java"
JAR="./target/working-set-size-estimation-1.0-SNAPSHOT-jar-with-dependencies.jar"
CLASS_NAME="alluxio.client.file.cache.benchmark.BenchmarkMain"
BENCHMARK="time_accuracy"
DATASET="twitter" # optional: msr, twitter, random, sequential
SHADOW_CACHE="bmc" # optional: ccf, mbf
TRACE="/home/lisimian/datasets/twitter/cluster13-2d-new.csv" # msr
DATASET_NAME="cluster13_2d"
#TRACE="/datasets/cluster37.0" # twitter
MAX_ENTRIES=125829120000 # 120000m
WINDOW_SIZE=3600000  # 1h
NUM_UNIQUE_ENTRIES=262144 # used for random & sequential benchmark
REPORT_DIR="/datasets/benchmarks/time"
REPORT_INTERVAL=1000
SIZE_BITS=32
SCOPE_BITS=0
TIME_DIVISOR=40
WINDOW_SIZE=`expr $WINDOW_SIZE / $TIME_DIVISOR`

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

MEMORY="64mb";
TAGS_PER_BUCKET=4; TAG_BITS=8;
SIZE_ENCODE="NONE"; SIZE_BITS=32; SIZE_BUCKET_BITS=0;
CLOCK_BITS=8;
bench_one

