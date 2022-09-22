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
import alluxio.client.file.cache.cuckoofilter.SlidingWindowType;
import alluxio.client.file.cache.dataset.Dataset;
import alluxio.client.file.cache.dataset.DatasetEntry;
import alluxio.client.file.cache.dataset.GeneralDataset;
import alluxio.client.file.cache.dataset.generator.EntryGenerator;
import alluxio.client.file.cache.dataset.generator.MSREntryGenerator;
import alluxio.client.file.cache.dataset.generator.RandomEntryGenerator;
import alluxio.client.file.cache.dataset.generator.SequentialEntryGenerator;
import alluxio.client.file.cache.dataset.generator.TwitterEntryGenerator;
import alluxio.util.FormatUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TimeBasedAccuracyBenchmark implements Benchmark {
  private final BenchmarkContext mBenchmarkContext;
  private final BenchmarkParameters mBenchmarkParameters;
  private final ShadowCache mShadowCache;
  private final ShadowCache mIdealShadowCache;
  private final ScheduledExecutorService mScheduler = Executors.newScheduledThreadPool(0);
  long mStartTime = System.currentTimeMillis();
  long opsCount = 0;
  long agingCount = 0;
  long agingDuration = 0;
  long cacheDuration = 0;
  double numARE = 0.0;
  double byteARE = 0.0;
  double pageHitARE = 0.0;
  double byteHitARE = 0.0;
  long errCnt = 0;
  long numFP = 0; // number of pages are seen as existent in cache but in fact not
  long numFN = 0; // number of pages are seen as inexistent in cache but in fact existed
  long byteFP = 0; // number of bytes are seen as existent in cache but in fact not
  long byteFN = 0; // number of bytes are seen as inexistent in cache but in fact existed
  long totalBytes = 0; // number of bytes passed the shadow cache
  private EntryGenerator<String> mDataset;

  public TimeBasedAccuracyBenchmark(BenchmarkContext benchmarkContext,
      BenchmarkParameters benchmarkParameters) {
    mBenchmarkContext = benchmarkContext;
    mBenchmarkParameters = benchmarkParameters;
    // force use time-based sliding window
    benchmarkParameters.mSlidingWindowType = SlidingWindowType.TIME_BASED;
    mShadowCache = ShadowCache.create(benchmarkParameters);
    mIdealShadowCache = new IdealShadowCacheManager(benchmarkParameters);
    createDataset();
  }

  private void createDataset() {
    EntryGenerator<String> generator;
    switch (mBenchmarkParameters.mDataset) {
      case "sequential":
        generator = new SequentialEntryGenerator(mBenchmarkParameters.mMaxEntries, 1,
            (int) mBenchmarkParameters.mNumUniqueEntries + 1);
        break;
      case "msr":
        generator = new MSREntryGenerator(mBenchmarkParameters.mTrace);
        break;
      case "twitter":
        generator = new TwitterEntryGenerator(mBenchmarkParameters.mTrace);
        break;
      case "random":
      default:
        generator = new RandomEntryGenerator(mBenchmarkParameters.mMaxEntries, 1,
            (int) mBenchmarkParameters.mNumUniqueEntries + 1);
    }
    mDataset = generator;
  }

  @Override
  public boolean prepare() {
    return false;
  }

  @Override
  public void run() {
    long agingPeriod = mBenchmarkParameters.mWindowSize / mBenchmarkParameters.mAgeLevels;
    if (agingPeriod <= 0) {
      agingPeriod = 1;
    }
    System.out.printf("agingPeriod:%d\n", agingPeriod);

    System.out.println(mShadowCache.getSummary());
    mBenchmarkContext.mStream.println(
        "#operation\tReal\tReal(byte)\tEst\tEst(byte)\t" + "RealRead(Page)\tRealRead(Bytes)\t"
            + "RealHit(Page)\tRealHit(Bytes)\t" + "EstHit(Page)\tEstHit(Bytes)");
    mStartTime = System.currentTimeMillis();
    long mEndTime = mStartTime + FormatUtils.parseTimeSize(mBenchmarkParameters.mRunTime);
    long firstEntryArrivalTime = -1;
    mScheduler.scheduleAtFixedRate(this::reportStatistics, mBenchmarkParameters.mReportInterval,
        mBenchmarkParameters.mReportInterval, TimeUnit.MILLISECONDS);
    while (mDataset.hasNext() && opsCount < mBenchmarkParameters.mMaxEntries
        && System.currentTimeMillis() < mEndTime) {
      opsCount++;
      DatasetEntry<String> entry = mDataset.next();
      if (firstEntryArrivalTime < 0) {
        firstEntryArrivalTime = entry.getTimestamp();
      }

      // TODO(iluoeli): rate limiter
      long elapsedMillis =
          (System.currentTimeMillis() - mStartTime) * mBenchmarkParameters.mTimeDivisor;
      long millisToWait = ((entry.getTimestamp() - firstEntryArrivalTime) * 1000 - elapsedMillis);
      if (millisToWait > 0) {
        try {
          Thread.sleep(millisToWait/mBenchmarkParameters.mTimeDivisor);
        } catch (Exception e) {
          e.printStackTrace();
          System.exit(1);
        }
      }
      PageId item = new PageId(entry.getScopeInfo().toString(), entry.getItem().hashCode());

      // update ideal cache
      // mIdealShadowCache.updateWorkingSetSize();
      int nread2 = mIdealShadowCache.get(item, entry.getSize(), entry.getScopeInfo());
      if (nread2 <= 0) {
        mIdealShadowCache.put(item, entry.getSize(), entry.getScopeInfo());
      } else {
        entry.setSize(nread2);
      }

      // update shadow cache
      long startCacheTick = System.currentTimeMillis();
      int nread = mShadowCache.get(item, entry.getSize(), entry.getScopeInfo());
      if (nread <= 0) {
        mShadowCache.put(item, entry.getSize(), entry.getScopeInfo());
      }
      cacheDuration += (System.currentTimeMillis() - startCacheTick);

      // membership statistics
      if (nread > nread2) {
        // false positive
        numFP++;
        byteFP += entry.getSize();
      } else if (nread < nread2) {
        // false negative
        numFN++;
        byteFN += entry.getSize();
      }
      totalBytes += entry.getSize();
    }

    mScheduler.shutdown();
    mShadowCache.stopUpdate();

    long totalDuration = (System.currentTimeMillis() - mStartTime);
    System.out.println();
    System.out.println("TotalTime(ms)\t" + totalDuration);
    reportMetrics();
  }

  @Override
  public boolean finish() {
    return false;
  }

  private void reportStatistics() {
    long elapsedSeconds =
        (System.currentTimeMillis() - mStartTime) / mBenchmarkParameters.mReportInterval;
    mIdealShadowCache.updateWorkingSetSize();
    long realNum = mIdealShadowCache.getShadowCachePages();
    long realByte = mIdealShadowCache.getShadowCacheBytes();
    long realCachePagesRead = mIdealShadowCache.getShadowCachePageRead();
    long realCacheBytesRead = mIdealShadowCache.getShadowCacheByteRead();
    long realCachePagesHit = mIdealShadowCache.getShadowCachePageHit();
    long realCacheBytesHit = mIdealShadowCache.getShadowCacheByteHit();
    mShadowCache.updateWorkingSetSize();
    long estNum = mShadowCache.getShadowCachePages();
    long estByte = mShadowCache.getShadowCacheBytes();
    long estCachePagesHit = mShadowCache.getShadowCachePageHit();
    long estCacheBytesHit = mShadowCache.getShadowCacheByteHit();
    mBenchmarkContext.mStream.printf("%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\n", elapsedSeconds,
        realNum, realByte, estNum, estByte, realCachePagesRead, realCacheBytesRead,
        realCachePagesHit, realCacheBytesHit, estCachePagesHit, estCacheBytesHit);
    // accumulate error
    errCnt++;
    if(realNum != 0){
      numARE += Math.abs(estNum / (double) realNum - 1.0);
    }
    if(realByte != 0){
      byteARE += Math.abs(estByte / (double) realByte - 1.0);
    }
    if (estCacheBytesHit != 0) {
      pageHitARE += Math.abs(realCachePagesHit / (double) estCachePagesHit - 1.0);
      byteHitARE += Math.abs(realCacheBytesHit / (double) estCacheBytesHit - 1.0);
    }
    if (elapsedSeconds % 60 == 0) {
      System.out.printf("%d S......\n", elapsedSeconds);
      reportMetrics();
    }
  }

  private void reportMetrics() {
    long realCachePagesRead = mIdealShadowCache.getShadowCachePageRead();
    long realCacheBytesRead = mIdealShadowCache.getShadowCacheByteRead();
    long realCachePagesHit = mIdealShadowCache.getShadowCachePageHit();
    long realCacheBytesHit = mIdealShadowCache.getShadowCacheByteHit();
    long estCachePagesHit = mShadowCache.getShadowCachePageHit();
    long estCacheBytesHit = mShadowCache.getShadowCacheByteHit();
    double realPageHitRatio = realCachePagesHit / (double) realCachePagesRead;
    double estPageHitRatio = estCachePagesHit / (double) realCachePagesRead;
    double realByteHitRatio = realCacheBytesHit / (double) realCacheBytesRead;
    double estByteHitRatio = estCacheBytesHit / (double) realCacheBytesRead;
    double pageHitAREFinal = Math.abs(estPageHitRatio / realPageHitRatio - 1.0);
    double byteHitAREFinal = Math.abs(estByteHitRatio / realByteHitRatio - 1.0);

    System.out.println();
    System.out
        .println("Put/Get(ms)\tAging(ms)\tAgingCnt\tops/sec\tops/sec(aging)\tARE(Page)\tARE(Byte)"
            + "\tARE(PageHit)\tARE(ByteHit)\tFinalARE(PageHit)\tFinalARE(ByteHit)");
    System.out.printf("%d\t%d\t%d\t%.2f\t%.2f\t%.4f%%\t%.4f%%\t%.4f%%\t%.4f%%\t%.4f%%\t%.4f%%\n",
        cacheDuration, agingDuration, agingCount, opsCount * 1000 / (double) cacheDuration,
        opsCount * 1000 / (double) (cacheDuration + agingDuration), numARE * 100 / errCnt,
        byteARE * 100 / errCnt, pageHitARE * 100 / errCnt, byteHitARE * 100 / errCnt,
        pageHitAREFinal * 100, byteHitAREFinal * 100);

    System.out.println();
    System.out.println("FPR(Page)\tFNR(Page)\tER(Page)");
    System.out.printf("%d/%d=%.4f%%\t%d/%d=%.4f%%\t%d/%d=%.4f%%\n", numFP, opsCount,
        numFP * 100 / (double) opsCount, numFN, opsCount, numFN * 100 / (double) opsCount,
        numFP + numFN, opsCount, (numFP + numFN) * 100 / (double) opsCount);

    System.out.println();
    System.out.println("FPR(Byte)\tFNR(Byte)\tER(Byte)");
    System.out.printf("%d/%d=%.4f%%\t%d/%d=%.4f%%\t%d/%d=%.4f%%\n", byteFP, totalBytes,
        byteFP * 100 / (double) totalBytes, byteFN, totalBytes, byteFN * 100 / (double) totalBytes,
        byteFP + byteFN, totalBytes, (byteFP + byteFN) * 100 / (double) totalBytes);
  }
}
