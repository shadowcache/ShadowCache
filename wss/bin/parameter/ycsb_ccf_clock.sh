#!/bin/bash
set -ex

source bin/bench_one.sh

JAVA="java"
JAR="./target/working-set-size-estimation-1.0-SNAPSHOT-jar-with-dependencies.jar"
CLASS_NAME="alluxio.client.file.cache.benchmark.BenchmarkMain"
BENCHMARK="accuracy"
DATASET="ycsb" # optional: msr, twitter, random, sequential
SHADOW_CACHE="ccf" # optional: ccf, mbf
TRACE="/datasets/ycsb/ycsb-1m-10m-1m-concat6.csv"
MAX_ENTRIES=10000000 # 10m
WINDOW_SIZE=262144 # 256k
NUM_UNIQUE_ENTRIES=262144 # used for random & sequential benchmark
REPORT_DIR="/datasets/benchmarks/clock-ccf-concat6"
REPORT_INTERVAL=64
SIZE_BITS=20
SCOPE_BITS=0

OPPO_AGING=true;
TAGS_PER_BUCKET=4; TAG_BITS=8;
SIZE_ENCODE="NONE"; SIZE_BITS=20; SIZE_BUCKET_BITS=0;
WINDOW_ELEMENTS=262144

CLOCK_BITS_LIST="$(seq 1 1 8)";
for CLOCK_BITS in ${CLOCK_BITS_LIST}; do
  TOTAL_BYTES=$(( (TAG_BITS + SIZE_BITS + SCOPE_BITS + CLOCK_BITS) * WINDOW_ELEMENTS / 8 ))
  MEMORY="$(to_brief_string ${TOTAL_BYTES})b"
  #echo "${TOTAL_BYTES}"
  #echo "${MEMORY}-c${CLOCK_BITS}"
  bench_one_ccf
done
