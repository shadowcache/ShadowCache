#!/bin/bash
set +ex
PRESTO_HOME="/presto"
PRESTO="/presto/presto-cli/target/presto-cli-0.266-SNAPSHOT-executable.jar"
PRESTO_SERVER="/presto/presto-server/target/presto-server-0.266-SNAPSHOT/presto-server-0.266-SNAPSHOT/bin/launcher"
SQL_PATH="sqls"
PRESTO_CONFIG_PATH=/etcs/presto

MAX_SQL_NUM=5
OPS=1
bash restart-all.sh
# wait for server start
sleep 30s

start_tick=$(date +%s)
echo -e "start timestamp ${start_tick}"

for (( i=0; i < $MAX_SQL_NUM; i++ )); do
  echo "ops ${i}"
  ${PRESTO} --server slave017:8881 --catalog hive --schema s3tpcds10 -f sqls/query_7.sql > /dev/null &
  ${PRESTO} --server slave017:8881 --catalog hive --schema s3tpcds10 -f sqls/query_53.sql > /dev/null &
  ${PRESTO} --server slave017:8881 --catalog hive --schema s3tpcds10 -f sqls/query_68.sql > /dev/null &
  wait
  ${PRESTO} --server slave017:8881 --catalog hive --schema s3tpcds10 -f sqls/query_30.sql > /dev/null &
  ${PRESTO} --server slave017:8881 --catalog hive --schema s3tpcds10 -f sqls/query_73.sql > /dev/null & 
  ${PRESTO} --server slave017:8881 --catalog hive --schema s3tpcds10 -f sqls/query_57.sql > /dev/null & 
  wait 
  ${PRESTO} --server slave017:8881 --catalog hive --schema s3tpcds10 -f sqls/query_22.sql > /dev/null & 
  ${PRESTO} --server slave017:8881 --catalog hive --schema s3tpcds10 -f sqls/query_46.sql > /dev/null & 
  ${PRESTO} --server slave017:8881 --catalog hive --schema s3tpcds10 -f sqls/query_25.sql > /dev/null & 
  wait
done


end_tick=$(date +%s)

echo -e "start timestamp ${start_tick}, end timestamp ${end_tick}"
echo -e "cost $(( end_tick - start_tick )) s"