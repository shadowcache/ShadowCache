#!/bin/bash
set +ex -u

source bin/bench_one.sh

JAVA="java"
JAR="./target/working-set-size-estimation-1.0-SNAPSHOT-jar-with-dependencies.jar"
CLASS_NAME="alluxio.client.file.cache.benchmark.BenchmarkMain"
BENCHMARK="time_accuracy"
MAX_ENTRIES=125829120000 # 120000m

NUM_UNIQUE_ENTRIES=262144 # used for random & sequential benchmark
REPORT_DIR="/datasets/benchmarks/time-stability"
REPORT_INTERVAL=1000

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

# disalbe multi-scope
NUM_SCOPE=0
SCOPE_BITS=0


stability_bench() {
  WINDOW_SIZE=`expr $WINDOW_SIZE_RAW / $TIME_DIVISOR`
  # CCF
  SHADOW_CACHE="ccf"
  TAGS_PER_BUCKET=4; TAG_BITS=8;
  SIZE_ENCODE="BUCKET"
  CLOCK_BITS=6
  SIZE_BITS=8
  SIZE_BUCKET_BITS=4

  OPPO_AGING=false
  bench_one_ccf
  
  # CCF(oa)
  OPPO_AGING=true;
  bench_one_ccf
  
  # MBF
  SHADOW_CACHE="mbf"
  NUM_BLOOMS=8
  bench_one_mbf
  
  # ClockSketch
  SHADOW_CACHE="bmc"
  CLOCK_BITS=8
  SIZE_BITS=32
  bench_one_bmc

  #SlidingSketch
  SHADOW_CACHE="bms"
  NUM_HASH=8
  bench_one_ss
}

# MSR
DATASET="msr" # optional: msr, twitter, random, sequential
TRACE="/datasets/msr/prxy_0.csv" # msr
WINDOW_SIZE_RAW=86400000 # 24h
TIME_DIVISOR=168
MEMORY="176kb"
stability_bench

# Twitter
DATASET="twitter"
TRACE="/datasets/twitter/cluster35-1d.csv" # twitter
WINDOW_SIZE_RAW=3600000  # 1h
TIME_DIVISOR=24
MEMORY="1408kb"
stability_bench



