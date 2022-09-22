# ShadowCache
This article will guide you run ShadowCache with presto to accelearte data access to under file systems.

## Code Organization of This Project
- ShadowCache-on-Alluxio: the cache system including ShadowCache.
- conf: the presto config files.
- scripts: some bash shells for experiment
- sqls: tpc-ds queries
- cuki: the approximate data structure and benchmarks of it

## environment
Java 1.8
maven 3.54
Hive 3.1.3
S3
Git

## compile
To compile alluxio, use command:

```shell
mvn clean install -Dmaven.javadoc.skip=true -DskipTests -Dlicense.skip=true -Dcheckstyle.skip=true -Dfindbugs.skip=true -Prelease
```

To compile presto, use command:

```shell
 ./mvnw clean install -T2C -DskipTests -Dlicense.skip=true -Dcheckstyle.skip=true -Dfindbugs.skip=true -pl '!presto-docs'
```

To compile only the approximate data structure Cuki, use command:
```shell
mvn assembly:assembly \
  -T 4C \
  -Dmaven.javadoc.skip=true \
  -DskipTests \
  -Dlicense.skip=true \
  -Dcheckstyle.skip=true \
  -Dfindbugs.skip=true
```

## Config

Conifg your presto cluster just as common, but replace the `hive.properties` with the `conf/hive.properties`. There are serveral parameters in this file, and these are the most important of ShadowCache:

| parameters                           | meanings                                                     |
| ------------------------------------ | ------------------------------------------------------------ |
| cache.enabled                        | whether to enable alluxio cache                              |
| cache.base-directory                 | the directory stores cache files, better be ramdisk          |
| cache.alluxio.max-cache-size         | the cache size alluxio use for each worker, ShadowCache will automatically tune cache size. Do not worry about that because ShadowCache will automatically tune cache size during runtime. |
| cache.alluxio.shadow-cache-enabled   | enable the working set size tracking                         |
| cache.alluxio.shadow-cache-window    | set the window size,for example 20m,20h,20d.                 |
| cache.alluxio.metrics-enabled        | enable report metrics                                        |
| cache.alluxio.cache-adaption-enabled | enable the ShadowCache adaptive cache optimazation           |
| cache.alluxio.shadow-cache-type      | could be either MULTIPLE_BLOOM_FILTER or CLOCK_CUCKOO_FILTER |

## Presto TPC-DS Test
Next, if the presto cluster run in common way. You could load the TPC-DS data in your hive but location in S3.

Run the scripts `scripts/test_s3_throughput.sql`, and you will see the presto UI are handling them.

## Cuki Test

You can run Cuki by command:
```
java -cp ./target/working-set-size-estimation-1.0-SNAPSHOT-jar-with-dependencies.jar \
  alluxio.client.file.cache.benchmark.BenchmarkMain \
  --benchmark accuracy --shadow_cache sccf \
  --dataset msr --trace /datasets/ycsb/xxx.csv \
  --max_entries 12582912 --memory 32kb --window_size 262144 \
  --tag_bits 8 --tags_per_bucket 4 \
  --clock_bits 8 --opportunistic_aging false \
  --scope_bits 0 \
  --size_encode BUCKET --num_size_bucket_bits 4 --size_bucket_bits 10 --size_bits 4 \
  --report_interval 64 \
  --report_file /datasets/benchmarks/sccf-20211214/accuracy/msr/sccf-msr-12m-256k-32kb-t1_2-sBUCKET_4_10-c2_false.csv
```

For furture comparing with other methods, we also prepare some bash files in `cuki/benchmark_scripts`.

You can download the MSR trace in http://iotta.snia.org/traces/block-io/388, and the Twitter trace in  https://github.com/twitter/cache-trace.