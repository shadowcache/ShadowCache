# How to run ShadowCache with Presto
This article will guide you run ShadowCache with presto to accelearte data access to under file systems.

## environment
Java 1.8
maven 3.54
maven wrapper
Hive
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

## TPC-DS test
Next, if the presto cluster run in common way. You could load the TPC-DS data in your hive but location in S3.

Run the scripts `scripts/test_s3_throughput.sql`, and you will see the presto UI are handling them.



