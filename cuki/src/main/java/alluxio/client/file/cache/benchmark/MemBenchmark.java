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

import alluxio.client.file.cache.IdealShadowCacheManager;
import alluxio.client.file.cache.PageId;
import alluxio.client.file.cache.ShadowCache;
import alluxio.client.file.cache.dataset.Dataset;
import alluxio.client.file.cache.dataset.DatasetEntry;
import alluxio.client.file.cache.dataset.GeneralDataset;
import alluxio.client.file.cache.dataset.generator.EntryGenerator;

public class MemBenchmark implements Benchmark {
  private final BenchmarkContext mBenchmarkContext;
  private final BenchmarkParameters mBenchmarkParameters;
  private final ShadowCache mShadowCache;
  private Dataset<String> mDataset;

  public MemBenchmark(BenchmarkContext benchmarkContext,
                      BenchmarkParameters benchmarkParameters) {
    mBenchmarkContext = benchmarkContext;
    mBenchmarkParameters = benchmarkParameters;
    mShadowCache = ShadowCache.create(benchmarkParameters);
    createDataset();
    mShadowCache.stopUpdate();
  }

  private void createDataset() {
    EntryGenerator<String> generator = BenchmarkUtils.createGenerator(mBenchmarkParameters);
    mDataset = new GeneralDataset<>(generator, (int) mBenchmarkParameters.mWindowSize);
  }

  @Override
  public boolean prepare() {
    return false;
  }

  @Override
  public void run() {
    long opsCount = 0;
    long agingCount = 0;
    long agingDuration = 0;
    long cacheDuration = 0;
    long agingPeriod = mBenchmarkParameters.mWindowSize / mBenchmarkParameters.mAgeLevels;
    if (agingPeriod <= 0) {
      agingPeriod = 1;
    }
    System.out.printf("agingPeriod:%d\n", agingPeriod);

    System.out.println(mShadowCache.getSummary());
    mBenchmarkContext.mStream.println(
        "#operation\tReal\tReal(byte)\tEst\tEst(byte)\t" + "RealRead(Page)\tRealRead(Bytes)\t"
            + "RealHit(Page)\tRealHit(Bytes)\t" + "EstHit(Page)\tEstHit(Bytes)");

    long startTick = System.currentTimeMillis();
    while (mDataset.hasNext() && opsCount < mBenchmarkParameters.mMaxEntries) {
      opsCount++;
      DatasetEntry<String> entry = mDataset.next();

      PageId item = new PageId(entry.getScopeInfo().toString(), entry.getItem().hashCode());
      long startCacheTick = System.currentTimeMillis();
      int nread = mShadowCache.get(item, entry.getSize(), entry.getScopeInfo());
      if (nread <= 0) {
        mShadowCache.put(item, entry.getSize(), entry.getScopeInfo());
      }
      mShadowCache.updateTimestamp(1);
      cacheDuration += (System.currentTimeMillis() - startCacheTick);

      // Aging
      if (opsCount % agingPeriod == 0) {
        agingCount++;
        long startAgingTick = System.currentTimeMillis();
        mShadowCache.aging();
        agingDuration += System.currentTimeMillis() - startAgingTick;
      }

      // report
      if (opsCount % mBenchmarkParameters.mReportInterval == 0) {
        mShadowCache.updateWorkingSetSize();
        // long realNum = mDataset.getRealEntryNumber();
        // long realByte = mDataset.getRealEntrySize();
        long estNum = mShadowCache.getShadowCachePages();
        long estByte = mShadowCache.getShadowCacheBytes();
        long estCachePagesHit = mShadowCache.getShadowCachePageHit();
        long estCacheBytesHit = mShadowCache.getShadowCacheByteHit();

        mBenchmarkContext.mStream.printf("%d\t%d\t%d\t%d\t%d\n", opsCount, estNum, estByte, estCachePagesHit, estCacheBytesHit);
      }
    }

    long totalDuration = (System.currentTimeMillis() - startTick);

    System.out.println(mShadowCache.getSummary());
    System.out.println();
    System.out.println("TotalTime(ms)\t" + totalDuration);
    System.out.println();
    System.out
        .println("Put/Get(ms)\tAging(ms)\tAgingCnt\tops/sec\tops/sec(aging)");
    System.out.printf("%d\t%d\t%d\t%.2f\t%.2f\n",
        cacheDuration, agingDuration, agingCount, opsCount * 1000 / (double) cacheDuration,
        opsCount * 1000 / (double) (cacheDuration + agingDuration));


    if (mBenchmarkParameters.mVerbose) {
      System.out.println(mShadowCache.dumpDebugInfo());
    }
  }

  @Override
  public boolean finish() {
    return false;
  }
}
