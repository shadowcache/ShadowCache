#!/bin/bash
set +ex -u

JAVA="java"
JAR="./target/working-set-size-estimation-1.0-SNAPSHOT-jar-with-dependencies.jar"
CLASS_NAME="alluxio.client.file.cache.benchmark.BenchmarkMain"

to_brief_string() {
  local size=$1
  if [[ ${size} -ge "1048576" && $(( size%1048576 )) == 0 ]]; then
    echo "$(( size / 1048576 ))m"
  elif [[ ${size} -ge "1024" && $(( size%1024 )) == 0 ]]; then
    echo "$(( size / 1024 ))k"
  else
    echo "${size}"
  fi
}

bench_one_ccf() {
  local str_max_entries=$(to_brief_string ${MAX_ENTRIES})
  local str_window_size=$(to_brief_string ${WINDOW_SIZE})
  mkdir -p "${REPORT_DIR}/${BENCHMARK}/${DATASET}"
  local tag_prefix="t${TAGS_PER_BUCKET}_${TAG_BITS}"
  local size_prefix="s${SIZE_ENCODE}_${SIZE_BITS}_${SIZE_BUCKET_BITS}"
  local clock_prefix="c${CLOCK_BITS}_${OPPO_AGING}"
  local prefix="${REPORT_DIR}/${BENCHMARK}/${DATASET}/${SHADOW_CACHE}-${DATASET}-${str_max_entries}-${str_window_size}-${MEMORY}-${tag_prefix}-${size_prefix}-${clock_prefix}"
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
    --clock_bits ${CLOCK_BITS} \
    --opportunistic_aging ${OPPO_AGING} \
    --report_file ${REPORT_FILE} \
    --report_interval ${REPORT_INTERVAL} \
    --size_encode ${SIZE_ENCODE} \
    --num_size_bucket_bits ${SIZE_BITS} \
    --size_bucket_bits ${SIZE_BUCKET_BITS} \
    --size_bits ${SIZE_BITS} \
    --num_scope ${NUM_SCOPE} \
    --scope_bits ${SCOPE_BITS} \
    --tag_bits ${TAG_BITS} \
    --tags_per_bucket ${TAGS_PER_BUCKET} \
    --bitset_type "default" \
    --time_divisor ${TIME_DIVISOR} \
    > ${LOG_FILE}
}

bench_one_mbf() {
  local str_max_entries=$(to_brief_string ${MAX_ENTRIES})
  local str_window_size=$(to_brief_string ${WINDOW_SIZE})
  mkdir -p "${REPORT_DIR}/${BENCHMARK}/${DATASET}"
  local prefix="${REPORT_DIR}/${BENCHMARK}/${DATASET}/${SHADOW_CACHE}-${DATASET}-${str_max_entries}-${str_window_size}-${MEMORY}-${NUM_BLOOMS}"
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
    --num_blooms ${NUM_BLOOMS} \
    --report_file ${REPORT_FILE} \
    --report_interval ${REPORT_INTERVAL} \
    --num_scope ${NUM_SCOPE} \
    --scope_bits ${SCOPE_BITS} \
    --time_divisor ${TIME_DIVISOR} \
    >> ${LOG_FILE}
}

# TODO: consider to rename bmc to clocksketch
bench_one_bmc() {
  local str_max_entries=$(to_brief_string ${MAX_ENTRIES})
  local str_window_size=$(to_brief_string ${WINDOW_SIZE})
  mkdir -p "${REPORT_DIR}/${BENCHMARK}/${DATASET}"
  local prefix="${REPORT_DIR}/${BENCHMARK}/${DATASET}/${SHADOW_CACHE}-${DATASET}-${str_max_entries}-${str_window_size}-${MEMORY}-${CLOCK_BITS}-${SIZE_BITS}"
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
    --clock_bits ${CLOCK_BITS} \
    --report_file ${REPORT_FILE} \
    --report_interval ${REPORT_INTERVAL} \
		--size_bits ${SIZE_BITS} \
		--time_divisor ${TIME_DIVISOR} \
    >> ${LOG_FILE}
}

bench_one_swamp() {
  local str_max_entries=$(to_brief_string ${MAX_ENTRIES})
  local str_window_size=$(to_brief_string ${WINDOW_SIZE})
  mkdir -p "${REPORT_DIR}/${BENCHMARK}/${DATASET}"
  local prefix="${REPORT_DIR}/${BENCHMARK}/${DATASET}/${SHADOW_CACHE}-${DATASET}-${str_max_entries}-${str_window_size}-${MEMORY}-${CLOCK_BITS}-${SIZE_BITS}"
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
    --clock_bits ${CLOCK_BITS} \
    --report_file ${REPORT_FILE} \
    --report_interval ${REPORT_INTERVAL} \
    --time_divisor ${TIME_DIVISOR} \
    --size_bits ${SIZE_BITS} \
    >> ${LOG_FILE}
}



bench_one_ss() {
    local str_max_entries=$(to_brief_string ${MAX_ENTRIES})
    local str_window_size=$(to_brief_string ${WINDOW_SIZE})
    mkdir -p "${REPORT_DIR}/${BENCHMARK}/${DATASET}"
    local prefix="${REPORT_DIR}/${BENCHMARK}/${DATASET}/${SHADOW_CACHE}-${DATASET}-${str_max_entries}-${str_window_size}-${MEMORY}-${SIZE_BITS}-${NUM_HASH}"
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
      --report_file ${REPORT_FILE} \
      --report_interval ${REPORT_INTERVAL} \
      --time_divisor ${TIME_DIVISOR} \
      --size_bits ${SIZE_BITS} \
      --scope_bits ${SCOPE_BITS} \
      --num_scope ${NUM_SCOPE} \
      --num_hash ${NUM_HASH} \
      >> ${LOG_FILE}
}

# TODO: add sliding sketch
