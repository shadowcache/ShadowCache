#!/bin/bash
set +ex

PRESTO_SERVER="/presto/presto-server/target/presto-server-0.266-SNAPSHOT/presto-server-0.266-SNAPSHOT/bin/launcher"
PRESTO_CONFIG_PATH=/etcs/presto

#stop
ssh  slave039 "${PRESTO_SERVER} --etc-dir=${PRESTO_CONFIG_PATH} stop"
ssh  slave040 "${PRESTO_SERVER} --etc-dir=${PRESTO_CONFIG_PATH} stop"
ssh  slave022 "${PRESTO_SERVER} --etc-dir=${PRESTO_CONFIG_PATH} stop"
ssh  slave024 "${PRESTO_SERVER} --etc-dir=${PRESTO_CONFIG_PATH} stop"
ssh  slave025 "${PRESTO_SERVER} --etc-dir=${PRESTO_CONFIG_PATH} stop"
${PRESTO_SERVER} --etc-dir=${PRESTO_CONFIG_PATH}  stop
sleep 30s

# FLUSH CONFIG
cp conf/hive.properties ${PRESTO_CONFIG_PATH}/catalog
scp conf/hive.properties slave039:${PRESTO_CONFIG_PATH}/catalog
scp conf/hive.properties slave040:${PRESTO_CONFIG_PATH}/catalog
scp conf/hive.properties slave022:${PRESTO_CONFIG_PATH}/catalog
scp conf/hive.properties slave024:${PRESTO_CONFIG_PATH}/catalog
scp conf/hive.properties slave025:${PRESTO_CONFIG_PATH}/catalog
scp conf/hive.properties slave026:${PRESTO_CONFIG_PATH}/catalog
scp conf/hive.properties slave028:${PRESTO_CONFIG_PATH}/catalog

# clear cache
ssh slave039 "rm -rf /mnt/ramdisk/alluxioclient/LOCAL"
ssh slave040 "rm -rf /mnt/ramdisk/alluxioclient/LOCAL"
ssh slave022 "rm -rf /mnt/ramdisk/alluxioclient/LOCAL"
ssh slave024 "rm -rf /mnt/ramdisk/alluxioclient/LOCAL"
ssh slave025 "rm -rf /mnt/ramdisk/alluxioclient/LOCAL"
ssh slave026 "rm -rf /mnt/ramdisk/alluxioclient/LOCAL"
ssh slave028 "rm -rf /mnt/ramdisk/alluxioclient/LOCAL"
rm -rf /mnt/ramdisk/alluxioclient/LOCAL



#start
${PRESTO_SERVER} --etc-dir=${PRESTO_CONFIG_PATH}  start
ssh  slave039 "${PRESTO_SERVER} --etc-dir=${PRESTO_CONFIG_PATH} start"
ssh  slave040 "${PRESTO_SERVER} --etc-dir=${PRESTO_CONFIG_PATH} start"
ssh  slave022 "${PRESTO_SERVER} --etc-dir=${PRESTO_CONFIG_PATH} start"
ssh  slave024 "${PRESTO_SERVER} --etc-dir=${PRESTO_CONFIG_PATH} start"
ssh  slave025 "${PRESTO_SERVER} --etc-dir=${PRESTO_CONFIG_PATH} start"

