## Build

1. Clone

```bash
git clone git@git.nju.edu.cn:iluoeli/wss-estimation.git
```

2. Build

```bash
mvn assembly:assembly \
  -T 4C \
  -Dmaven.javadoc.skip=true \
  -DskipTests \
  -Dlicense.skip=true \
  -Dcheckstyle.skip=true \
  -Dfindbugs.skip=true
```

3. Test

```bash
java -cp ./target/working-set-size-estimation-1.0-SNAPSHOT-jar-with-dependencies.jar \
  alluxio.client.file.cache.benchmark.BenchmarkMain \
  --benchmark accuracy --shadow_cache sccf \
  --dataset msr --trace /datasets/msr-cambridge1/prxy_0.csv \
  --max_entries 12582912 --memory 32kb --window_size 262144 \
  --tag_bits 8 --tags_per_bucket 4 \
  --clock_bits 8 --opportunistic_aging false \
  --scope_bits 0 \
  --size_encode BUCKET --num_size_bucket_bits 4 --size_bucket_bits 10 --size_bits 4 \
  --report_interval 64 \
  --report_file /datasets/benchmarks/sccf-20211214/accuracy/msr/sccf-msr-12m-256k-32kb-t1_2-sBUCKET_4_10-c2_false.csv
```
