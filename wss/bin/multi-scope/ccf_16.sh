#!/bin/bash
set -ex

source bin/bench_one.sh

JAVA="java"
JAR="./target/working-set-size-estimation-1.0-SNAPSHOT-jar-with-dependencies.jar"
CLASS_NAME="alluxio.client.file.cache.benchmark.BenchmarkMain"
BENCHMARK="time_multi"
DATASET="multi" # optional: msr, twitter, random, sequential
SHADOW_CACHE="ccf" # optional: ccf, mbf
TRACE="/datasets/multi/multiScope_1d_16_v5.csv" # path to dataset
NUM_SCOPE=16
SCOPE_BITS=5
#TRACE="/datasets/cluster37.0" # twitter
MAX_ENTRIES=125829120000 # 120000m
WINDOW_SIZE_RAW=3600000  # 1h
NUM_UNIQUE_ENTRIES=262144 # used for random & sequential benchmark
REPORT_DIR="/datasets/benchmarks/multi_scope"
REPORT_INTERVAL=1000


TIME_DIVISOR=144
WINDOW_SIZE=`expr $WINDOW_SIZE_RAW / $TIME_DIVISOR`



OPPO_AGING=false;
TAGS_PER_BUCKET=4; 
TAG_BITS=8;
SIZE_ENCODE="None"; 
SIZE_BUCKET_BITS=0;

SIZE_BITS=32;
MEMORY="24mb";
CLOCK_BITS=4;
NUM_SIZE_BUCKET_BITS=0;
bench_one_ccf