#!/bin/bash
set -ex

source bin/bench_one.sh

JAVA="java"
JAR="./target/working-set-size-estimation-1.0-SNAPSHOT-jar-with-dependencies.jar"
CLASS_NAME="alluxio.client.file.cache.benchmark.BenchmarkMain"
BENCHMARK="time_multi"
DATASET="multi" # optional: msr, twitter, random, sequential
SHADOW_CACHE="mbf" # optional: ccf, mbf
TRACE="/datasets/multi/multiScope_1d_16_v5.csv" # path to dataset
NUM_SCOPE=16
SCOPE_BITS=0
MAX_ENTRIES=125829120000 # 120000m
WINDOW_SIZE_RAW=3600000  # 1h
NUM_UNIQUE_ENTRIES=262144 # used for random & sequential benchmark
REPORT_DIR="/datasets/benchmarks/multi_scope"
REPORT_INTERVAL=1000
SIZE_BITS=32
SCOPE_BITS=0
TIME_DIVISOR=144
WINDOW_SIZE=`expr $WINDOW_SIZE_RAW / $TIME_DIVISOR`
NUM_BLOOMS=8


OPPO_AGING=false;

MEMORY="24mb";
TAGS_PER_BUCKET=4; TAG_BITS=8;
SIZE_ENCODE="NONE"; SIZE_BITS=32; SIZE_BUCKET_BITS=0;

CLOCK_BITS=4;
bench_one_mbf

