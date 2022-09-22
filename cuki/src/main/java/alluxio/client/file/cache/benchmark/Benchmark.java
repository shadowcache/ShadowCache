/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.client.file.cache.benchmark;

import java.util.NoSuchElementException;

public interface Benchmark {
  public static Benchmark create(BenchmarkParameters parameters) throws Exception {
    BenchmarkContext benchmarkContext = new BenchmarkContext(parameters);
    BenchmarkType type = BenchmarkType.valueOf(parameters.mBenchmarkType.toUpperCase());
    switch (type) {
      case ACCURACY:
        return new AccuracyBenchmark(benchmarkContext, parameters);
      case THROUGHPUT:
        return new ThroughputBenchmark(benchmarkContext, parameters);
      case SS_THROUGHPUT:
        return new SlidingSketchThrouhputBenchmark(benchmarkContext,parameters);
      case HITRATIO:
        return new HitRatioBenchmark(benchmarkContext, parameters);
      case INSERT_THROUGHPUT:
        return new InsertThroughputBenchmark(benchmarkContext, parameters);
      case TIME_ACCURACY:
        return new TimeBasedAccuracyBenchmark(benchmarkContext, parameters);
      case TABLE:
        return new TableBenchmark(benchmarkContext, parameters);
      case MULTI:
        return new MultiScopeAccuracyBenchmark(benchmarkContext,parameters);
      case ADAPTION:
        return new CacheAdaptionBenchmark(benchmarkContext,parameters);
      case TIME_MULTI:
        return new MultiScopeTimeBasedBenchmark(benchmarkContext,parameters);
      case MEM:
        return new MemBenchmark(benchmarkContext,parameters);
    }
    throw new NoSuchElementException();
  }

  default boolean prepare() {
    return true;
  }

  void run();

  default boolean finish() {
    return true;
  }

  enum BenchmarkType {
    ACCURACY, THROUGHPUT, HITRATIO, INSERT_THROUGHPUT, QUERY_THROUGHPUT, TIME_ACCURACY,
    TABLE, MULTI, TIME_MULTI, ADAPTION,SS_THROUGHPUT,MEM
  }
}
