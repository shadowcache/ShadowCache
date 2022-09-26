# ShadowCache
ShadowCache is a an adaptive and lightweight approach to improve cache efficiency 
This is the repository for ShadowCache's implementation based on Alluxio.
This article will guide you run ShadowCache with presto to accelearte data access to under file systems.

## Code Organization of This Project
- alluxio: the cache system including ShadowCache.
- presto: we make few changes in presto, so we can config ShadowCache by Presto's configuration file.
- conf: the presto config files.
- scripts: some bash shells for experiment
- sqls: tpc-ds queries
- cuki: the approximate data structure and benchmarks of it

## Environment
- Java 1.8
- maven 3.54
- Hive 3.1.3
- S3
- Git

## Compile
To compile alluxio with ShadowCache, use command:

```shell
cd alluxio
mvn clean install -Dmaven.javadoc.skip=true -DskipTests -Dlicense.skip=true -Dcheckstyle.skip=true -Dfindbugs.skip=true -Prelease
```

To compile presto, use command:

```shell
cd presto
./mvnw clean install -T2C -DskipTests -Dlicense.skip=true -Dcheckstyle.skip=true -Dfindbugs.skip=true -pl '!presto-docs'
```

To compile only the approximate data structure Cuki, use command:
```shell
cd Cuki
mvn assembly:assembly \
  -T 4C \
  -Dmaven.javadoc.skip=true \
  -DskipTests \
  -Dlicense.skip=true \
  -Dcheckstyle.skip=true \
  -Dfindbugs.skip=true
```

## Config

Conifg your presto cluster just as common, but replace the `hive.properties` with the `conf/hive.properties`. There are serveral parameters in this file, we list some important parameters as follow:

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

You may want to use a monitor to capture the metrics, we provide a sample config as `conf/jvm.config` for you to use prometheus to monitor the alluxio client.

The important metrics of alluxio client are list as follows:

| Metrics                | Meanings                             |
| ---------------------- | ------------------------------------ |
| CacheBytesReadExternal | The bytes read from external storage |
| CacheBytesReadCache    | The bytes read from cache            |
| CacheSpaceAvailable    | The avaliable cache space size       |
| CacheSpaceUsage        | The usage cache space size           |



## Presto TPC-DS Test

Before the test, you need to clear the client cache or restart the presto cluster to make the result more precise.

Next, if the presto cluster run in common way. You could load the TPC-DS data in your hive but location in S3.

Run the scripts `scripts/test_s3_throughput.sql`,  and you will see the presto UI are handling them.

Open your prometheus UI, you will see the variation of the cache hit ratio.

## Cuki Test

You can run Cuki by command such as:
```
java -cp ./target/working-set-size-estimation-1.0-SNAPSHOT-jar-with-dependencies.jar \
  alluxio.client.file.cache.benchmark.BenchmarkMain \
  --benchmark accuracy --shadow_cache sccf \
  --dataset msr --trace /datasets/ycsb/xxx.csv \
  --max_entries 12582912 --memory 32kb --window_size 262144 \
  --tag_bits 8 --tags_per_bucket 4 \
  --clock_bits 8 --opportunistic_aging false \
  --scope_bits 0 \
  --size_encode None
  --size_bits 4 \
  --report_interval 64 \
  --report_file xxx.csv
```

The parameters about above command are list as follows ( `ccf` reprents  Cuki, Clock Cuckoo Filter; `mbf` represents Multiple Bloom Filters; `bmc` represents Clock Sketch; `bms` represents Sliding Sketch):

| parameters          | meanings                                                     |
| ------------------- | ------------------------------------------------------------ |
| benchmark           | can be accuracy / time_accuracy / throughput / adaption / time_multi. You may see the evaluation of WSS estimation for more information about different benchmarks. |
| shadow_cache        | the implementations of shadow cache, which can be ccf / mbf / bmc / bms / swamp |
| dataset             | the dataset, which can be msr / twitter / ycsb               |
| trace               | the path to your dataset                                     |
| tag_bits            | bits length used for fingerprint                             |
| tags_per_bucket     | slots number per bucket in cuckoo filter                     |
| clock_bits          | bits length used for clock                                   |
| opportunistic_aging | whether to enable Cuki-OA                                    |
| scope_bits          | bits length used for scope                                   |
| size_encode         | the enode methods of size field                              |
| size_bits           | bits length used for size field                              |
| report_interval     | the time interval for report metrics (seconds)               |
| report_file         | the output file path                                         |
| scope_bits          | bits length used for scope                                   |
| scope_bits          | bits length used for scope                                   |

For further comparing with other methods, we also prepare some bash files in `cuki/benchmark_scripts`.

We provide our YCSB trace ( in `cuki/datasets_tiny/` )for simple benchmarks. You can download the MSR trace in http://iotta.snia.org/traces/block-io/388, and the Twitter trace in  https://github.com/twitter/cache-trace.