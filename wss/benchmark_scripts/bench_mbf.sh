#!/bin/bash
set +ex

JAVA="java"
JAR="./target/working-set-size-estimation-1.0-SNAPSHOT-jar-with-dependencies.jar"
CLASS_NAME="alluxio.client.file.cache.benchmark.BenchmarkMain"

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

to_brief_string_time() {
  local size=$1
  echo "$(( size / 3600000 ))h"
}


bench_one() {
  local str_max_entries=$(to_brief_string ${MAX_ENTRIES})
  local str_window_size=$(to_brief_string ${WINDOW_SIZE_RAW})
  if [[ ${BENCHMARK} == "time_accuracy" ]]; then
    str_window_size=$(to_brief_string_time ${WINDOW_SIZE_RAW})
  fi
  mkdir -p "${REPORT_DIR}/${BENCHMARK}/${DATASET}/${DATASET_NAME}"
  local prefix="${REPORT_DIR}/${BENCHMARK}/${DATASET}/${DATASET_NAME}/${SHADOW_CACHE}-${DATASET}-${str_max_entries}-${str_window_size}-${MEMORY}-${NUM_BLOOMS}"
        local timestamp=$(date +'%Y%m%d_%H_%M_%S')
  REPORT_FILE="${prefix}.csv"
  LOG_FILE="${prefix}.log"
  echo "${REPORT_FILE}"
  echo "${LOG_FILE}"
  ${JAVA} -cp ${JAR} ${CLASS_NAME} \
    --benchmark ${BENCHMARK} \
    --shadow_cache ${SHADOW_CACHE} \
    --dataset ${DATASET} \
    --trace ${TRACE} \
    --max_entries ${MAX_ENTRIES} \
    --memory ${MEMORY} \
    --window_size ${WINDOW_SIZE} \
    --num_unique_entries ${NUM_UNIQUE_ENTRIES} \
    --time_divisor ${TIME_DIVISOR} \
    --num_blooms ${NUM_BLOOMS} \
    --report_file ${REPORT_FILE} \
    --report_interval ${REPORT_INTERVAL} \
    >> ${LOG_FILE}
}
