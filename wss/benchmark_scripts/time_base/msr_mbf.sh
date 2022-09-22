#!/bin/bash
set -ex

source /home/lisimian/wss/wss-estimation/benchmark_scripts/bench_mbf.sh

JAVA="java"
JAR="./target/working-set-size-estimation-1.0-SNAPSHOT-jar-with-dependencies.jar"
CLASS_NAME="alluxio.client.file.cache.benchmark.BenchmarkMain"
BENCHMARK="time_accuracy"
DATASET="msr" # optional: msr, twitter, random, sequential
SHADOW_CACHE="mbf" # optional: ccf, mbf
TRACE="/home/lisimian/datasets/microsoft/MSR-Cambridge/prxy_1.csv" # msr
DATASET_NAME="prxy_1"
#TRACE="/datasets/cluster37.0" # twitter
MAX_ENTRIES=125829120000 # 120000m
WINDOW_SIZE_RAW=43200000  # 12h
NUM_UNIQUE_ENTRIES=262144 # used for random & sequential benchmark
REPORT_DIR="/datasets/benchmarks/time"
REPORT_INTERVAL=1000
SIZE_BITS=32
SCOPE_BITS=0
TIME_DIVISOR=80
WINDOW_SIZE=`expr $WINDOW_SIZE_RAW / $TIME_DIVISOR`
NUM_BLOOMS=8

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

MEMORY="320kb";
TAGS_PER_BUCKET=4; TAG_BITS=8;
SIZE_ENCODE="NONE"; SIZE_BITS=32; SIZE_BUCKET_BITS=0;

CLOCK_BITS=8;
bench_one

