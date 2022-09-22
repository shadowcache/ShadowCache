#!/bin/bash
set +ex
# Usage:
# benchmark_scripts/parse-log.sh `ls -tr
# /datasets/benchmarks/20211216-bucket-newtwitter/accuracy/twitter/*.log`
parse_log() {
  MEMORY=$(grep "mMemoryBudget" ${FILE} | tail -1 | awk '{print $2}')
  ER=$(grep "FPR(Byte)" -A1 ${FILE} | tail -1 | sed -s 's/=/ /g' | awk '{printf "%s,%s,%s", $2,$4,$6}')
  AER=$(grep "Put/Get(ms)" -A1 ${FILE} | tail -1 | awk '{printf "%s,%s", $7,$11}')
  echo -e "${MEMORY},${AER},${ER}"
}

for FILE in "$@"; do
  parse_log
done
