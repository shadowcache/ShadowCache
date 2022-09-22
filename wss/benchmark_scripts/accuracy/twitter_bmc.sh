#!/bin/bash
set -ex

source /home/lisimian/wss/wss-estimation/benchmark_scripts/bench_bmc.sh

JAVA="java"
JAR="./target/working-set-size-estimation-1.0-SNAPSHOT-jar-with-dependencies.jar"
CLASS_NAME="alluxio.client.file.cache.benchmark.BenchmarkMain"
BENCHMARK="accuracy"
DATASET="twitter" # optional: msr, twitter, ycsb, random, sequential
SHADOW_CACHE="bmc" # optional: ccf, mbf, bmc
TRACE="/home/lisimian/datasets/twitter/cluster37-1h-new.csv" # path to dataset
MAX_ENTRIES=12582912 # 12m
WINDOW_SIZE=262144 # 256k
NUM_UNIQUE_ENTRIES=262144 # used for random & sequential benchmark
REPORT_DIR="/datasets/benchmarks/clock-bmc" # path to store logs & csvs
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
TAGS_PER_BUCKET=4; TAG_BITS=8;
SIZE_ENCODE="BUCKET"; SIZE_BITS=32; SIZE_BUCKET_BITS=12;
CLOCK_BITS=8;

memory=32;
while(( ${memory}<=512 ))
do
    MEMORY="${memory}kb"
    memory=`expr $memory + 16`
    bench_one
done

memory=544
while(( ${memory}<=2048 ))
do
    MEMORY="${memory}kb"
    memory=`expr $memory + 64`
    bench_one
done

MEMORY="2048kb"
bench_one