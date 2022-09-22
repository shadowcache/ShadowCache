#!/bin/bash
set +ex
FILE="$1"
#echo "${FILE}"
#ER=$(sed -n '50p' ${FILE} | sed -s 's/=/ /g' | awk '{printf "%s,%s,%s", $2,$4,$6}')
parse_log() {
  MEMORY=$(grep "mMemoryBudget" ${FILE} | tail -1 | awk '{print $2}')
  BYTE_ER=$(grep "FPR(Byte)" -A1 ${FILE} | tail -1 | sed -s 's/=/ /g' | awk '{printf "%s,%s,%s", $2,$4,$6}')
  #PAGE_ER=$(grep "FPR(Page)" -A1 ${FILE} | tail -1 | sed -s 's/=/ /g' | awk '{printf "%s,%s,%s", $2,$4,$6}')
  ARE=$(grep "Put/Get(ms)" -A1 ${FILE} | tail -1 | awk '{printf "%s", $7}')
  # echo -e "${MEMORY},${ARE},${BYTE_ER},${PAGE_ER}"
  echo -e "${MEMORY},${ARE},${BYTE_ER}"
}

for FILE in "$@"; do
  parse_log
done
