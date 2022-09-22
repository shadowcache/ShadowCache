#!/bin/bash
set +ex -u

source bin/bench_one.sh

JAVA="java"
JAR="./target/working-set-size-estimation-1.0-SNAPSHOT-jar-with-dependencies.jar"
CLASS_NAME="alluxio.client.file.cache.benchmark.BenchmarkMain"
BENCHMARK="accuracy"
MAX_ENTRIES=12582912 # 12m
WINDOW_SIZE=262144 # 256k
NUM_UNIQUE_ENTRIES=262144 # used for random & sequential benchmark
REPORT_DIR="/datasets/benchmarks/stability"
REPORT_INTERVAL=64

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
# disable replay speedup
TIME_DIVISOR=1

stability_bench() {
  # CCF
  NUM_SCOPE=0
  SHADOW_CACHE="ccf"
  TAGS_PER_BUCKET=4; TAG_BITS=8;
  SIZE_ENCODE="BUCKET"
  CLOCK_BITS=6
  SIZE_BITS=8
  SIZE_BUCKET_BITS=$(( MAX_SIZE - SIZE_BITS ))
  TOTAL_BYTES=$(( (TAG_BITS + SIZE_BITS + SCOPE_BITS + CLOCK_BITS) * WINDOW_ELEMENTS / 8 ))
  MEMORY="$(to_brief_string ${TOTAL_BYTES})b"
  echo "MEMORY=${MEMORY}"

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
  
  # SWAMP
  SHADOW_CACHE="SWAMP"
  CLOCK_BITS=8
  SIZE_BITS=32
  bench_one_swamp

  #SlidingSketch
  SHADOW_CACHE="bms"
  NUM_HASH=8
  bench_one_ss
}

# MSR
DATASET="msr" # optional: msr, twitter, random, sequential
TRACE="/datasets/msr/prxy_0.csv" # msr
WINDOW_ELEMENTS=32768
MAX_SIZE=16
stability_bench

# Twitter
DATASET="twitter"
TRACE="/datasets/twitter/cluster37-1h-new.csv" # twitter
WINDOW_ELEMENTS=131072
MAX_SIZE=16
stability_bench

# YCSB
DATASET="ycsb"
TRACE="/datasets/ycsb/ycsb-1m-10m-1m-concat6.csv"
MAX_ENTRIES=10000000
WINDOW_ELEMENTS=262144
MAX_SIZE=20
stability_bench

