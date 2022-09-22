#!/bin/bash
set -ex

source bin/bench_ccf.sh

JAVA="java"
JAR="./target/working-set-size-estimation-1.0-SNAPSHOT-jar-with-dependencies.jar"
CLASS_NAME="alluxio.client.file.cache.benchmark.BenchmarkMain"
BENCHMARK="time_accuracy"
DATASET="twitter" # optional: msr, twitter, random, sequential
SHADOW_CACHE="ccf" # optional: ccf, mbf
#TRACE="/datasets/msr-cambridge1/prxy_0.csv" # msr
TRACE="/datasets/cluster37.0" # twitter
MAX_ENTRIES=125829120000 # 12m
WINDOW_SIZE=3600000  # 1h
NUM_UNIQUE_ENTRIES=262144 # used for random & sequential benchmark
REPORT_DIR="/datasets/benchmarks/time-20211202"
REPORT_INTERVAL=64
SIZE_BITS=20
SCOPE_BITS=0

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

MEMORY="125mb";
TAGS_PER_BUCKET=4; TAG_BITS=8;
SIZE_ENCODE="NONE"; SIZE_BITS=18; SIZE_BUCKET_BITS=0;
CLOCK_BITS=6;
bench_one

