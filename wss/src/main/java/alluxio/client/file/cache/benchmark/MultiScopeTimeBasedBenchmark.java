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
import alluxio.client.file.cache.dataset.generator.*;
import alluxio.client.quota.CacheScope;
import alluxio.util.FormatUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MultiScopeTimeBasedBenchmark implements Benchmark {
  private final BenchmarkContext mBenchmarkContext;
  private final BenchmarkParameters mBenchmarkParameters;
  private final ShadowCache mIdealShadowCache;
  private final ScheduledExecutorService mScheduler = Executors.newScheduledThreadPool(0);
  private final ShadowCache[] mShadowCacheList;
  private final HashMap<CacheScope,ShadowCache> scope2ShadowCache;
  private final Set<CacheScope> scopeSet;
  HashMap<CacheScope,Double> scopeNumARE = new HashMap<>();
  HashMap<CacheScope,Double> scopeByteARE = new HashMap<>();

  HashMap<CacheScope,Long> scopeNumFP = new HashMap<>();
  HashMap<CacheScope,Long> scopeNumFN = new HashMap<>();
  HashMap<CacheScope,Long> scopeByteFP = new HashMap<>();
  HashMap<CacheScope,Long> scopeByteFN = new HashMap<>();
  HashMap<CacheScope,Long> scopeTotalBytes = new HashMap<>();
  HashMap<CacheScope,Long> scopeOps = new HashMap<>();
  HashMap<CacheScope,Long> scopeErrCnt = new HashMap<>();

  long mStartTime = System.currentTimeMillis();
  long opsCount = 0;
  long agingCount = 0;
  long agingDuration = 0;
  long cacheDuration = 0;
  private EntryGenerator<String> mDataset;
  private boolean isCCF = false;

  public MultiScopeTimeBasedBenchmark(BenchmarkContext benchmarkContext,
                                    BenchmarkParameters benchmarkParameters) {
    ShadowCache.ShadowCacheType type = ShadowCache.ShadowCacheType.valueOf(benchmarkParameters.mShadowCacheType.toUpperCase());

    mBenchmarkContext = benchmarkContext;
    mBenchmarkParameters = benchmarkParameters;
    // force use time-based sliding window
    benchmarkParameters.mSlidingWindowType = SlidingWindowType.TIME_BASED;
    if(type == ShadowCache.ShadowCacheType.CCF){
      isCCF = true;
      mShadowCacheList = new ShadowCache[1];
      mShadowCacheList[0] = ShadowCache.create(benchmarkParameters);
    }else{
      long memoryInBits = FormatUtils.parseSpaceSize(benchmarkParameters.mMemoryBudget) * 8;
      benchmarkParameters.mMemoryBudget = memoryInBits / benchmarkParameters.mNumScope / 1024 / 8 +"kb";
      mShadowCacheList = new ShadowCache[benchmarkParameters.mNumScope];
      for(int i=0;i< benchmarkParameters.mNumScope;i++){
        mShadowCacheList[i] = ShadowCache.create(benchmarkParameters);
      }
    }
    mIdealShadowCache = new IdealShadowCacheManager(benchmarkParameters);
    scope2ShadowCache = new HashMap<>();
    scopeSet = new HashSet<>();
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
      case "multi":
        generator = new MultiScopeEntryGenerator(mBenchmarkParameters.mTrace);
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
    int freeIdx = 0;
    System.out.printf("agingPeriod:%d\n", agingPeriod);

    System.out.println(mShadowCacheList[0].getSummary());
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
      long scopeCount = scopeOps.getOrDefault(entry.getScopeInfo(),0l);
      scopeOps.put(entry.getScopeInfo(),scopeCount+1);
      long totalBytes = scopeTotalBytes.getOrDefault(entry.getScopeInfo(),0l);
      scopeTotalBytes.put(entry.getScopeInfo(),totalBytes+entry.getSize());

      if(!scopeSet.contains(entry.getScopeInfo())){
        scopeSet.add(entry.getScopeInfo());
      }
      ShadowCache shadowCache;
      if(isCCF){
        shadowCache = mShadowCacheList[0];
      }else{
        shadowCache = scope2ShadowCache.get(entry.getScopeInfo());
        if(shadowCache==null){
          if(freeIdx>= mBenchmarkParameters.mNumScope){
            System.out.println("freeIdx >= mNumScope");
            System.exit(-1);
          }
          shadowCache = mShadowCacheList[freeIdx];
          scope2ShadowCache.put(entry.getScopeInfo(),shadowCache);
          freeIdx+=1;
        }
      }
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
      int nread = shadowCache.get(item, entry.getSize(), entry.getScopeInfo());
      if (nread <= 0) {
        shadowCache.put(item, entry.getSize(), entry.getScopeInfo());
      }
      cacheDuration += (System.currentTimeMillis() - startCacheTick);

      // membership statistics
      if (nread > nread2) {
        // false positive
        long numFP = scopeNumFP.getOrDefault(entry.getScopeInfo(),0L);
        long byteFP = scopeByteFP.getOrDefault(entry.getScopeInfo(),0L);
        scopeNumFP.put(entry.getScopeInfo(),numFP+1);
        scopeByteFP.put(entry.getScopeInfo(),byteFP+entry.getSize());
      } else if (nread < nread2) {
        // false negative
        long numFN = scopeNumFN.getOrDefault(entry.getScopeInfo(),0L);
        long byteFN = scopeByteFN.getOrDefault(entry.getScopeInfo(),0L);
        scopeNumFN.put(entry.getScopeInfo(),numFN+1);
        scopeByteFN.put(entry.getScopeInfo(),byteFN+entry.getSize());
      }
    }

    mScheduler.shutdown();
    for(int i=0;i<mShadowCacheList.length;i++){
      mShadowCacheList[i].stopUpdate();
    }

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
    for(int i=0;i< mShadowCacheList.length;i++){
      mShadowCacheList[i].updateWorkingSetSize();
    }

    long realNum;
    long realByte;
    long estNum;
    long estByte;
    for(CacheScope scope:scopeSet) {
      ShadowCache shadowCacheScope;
      realByte = mIdealShadowCache.getShadowCacheBytes(scope);
      realNum = mIdealShadowCache.getShadowCachePages(scope);
      if (isCCF) {
        shadowCacheScope = mShadowCacheList[0];
        estNum = shadowCacheScope.getShadowCachePages(scope);
        estByte = shadowCacheScope.getShadowCacheBytes(scope);
      } else {
        shadowCacheScope = scope2ShadowCache.get(scope);
        estNum = shadowCacheScope.getShadowCachePages();
        estByte = shadowCacheScope.getShadowCacheBytes();
      }
      mBenchmarkContext.mStream.printf("%d\t%s\t%d\t%d\t%d\t%d\n", opsCount, scope.getId(),
          realNum, realByte, estNum, estByte);
      // accumulate error
      if (realByte == 0 || realNum == 0) {
        continue;
      }
      double numARE = scopeNumARE.getOrDefault(scope, 0.0);
      double byteARE = scopeByteARE.getOrDefault(scope, 0.0);
      scopeNumARE.put(scope, numARE + Math.abs(estNum / (double) realNum - 1.0));
      scopeByteARE.put(scope, byteARE + Math.abs(estByte / (double) realByte - 1.0));
      long errCnt = scopeErrCnt.getOrDefault(scope, 0L);
      scopeErrCnt.put(scope, errCnt + 1);
    }
    if (elapsedSeconds % 60 == 0) {
      System.out.printf("%d S......\n", elapsedSeconds);
      reportMetrics();
    }
  }

  private void reportMetrics() {
    System.out.println();
    for(CacheScope scope:scopeSet){
      long numFN = scopeNumFN.getOrDefault(scope,0L);
      long byteFN = scopeByteFN.getOrDefault(scope,0L);
      long numFP = scopeNumFP.getOrDefault(scope,0L);
      long byteFP = scopeByteFP.getOrDefault(scope,0L);
      long scopeCount = scopeOps.get(scope);
      long totalBytes = scopeTotalBytes.get(scope);
      long errCnt = scopeErrCnt.getOrDefault(scope,0l);
      double numARE = scopeNumARE.getOrDefault(scope,0.0);
      double byteARE = scopeByteARE.getOrDefault(scope,0.0);

      System.out.println("Scope\tPut/Get(ms)\tAging(ms)\tAgingCnt\tops/sec\tops/sec(aging)\tARE(Page)\tARE(Byte)");
      System.out.printf("%s\t%d\t%d\t%d\t%.2f\t%.2f\t%.4f%%\t%.4f%%\n",
          scope.getId(),cacheDuration, agingDuration, agingCount, opsCount * 1000 / (double) cacheDuration,
          opsCount * 1000 / (double) (cacheDuration + agingDuration), numARE * 100 / errCnt,
          byteARE * 100 / errCnt);

      System.out.println();
      System.out.println("Scope\tFPR(Page)\tFNR(Page)\tER(Page)");
      System.out.printf("%s\t%d/%d=%.4f%%\t%d/%d=%.4f%%\t%d/%d=%.4f%%\n", scope.getId(),numFP, scopeCount,
          numFP * 100 / (double) scopeCount, numFN, scopeCount, numFN * 100 / (double) scopeCount,
          numFP + numFN, scopeCount, (numFP + numFN) * 100 / (double) scopeCount);

      System.out.println();

      System.out.println("FPR(Byte)\tFNR(Byte)\tER(Byte)");
      System.out.printf("%s\t%d/%d=%.4f%%\t%d/%d=%.4f%%\t%d/%d=%.4f%%\n",scope.getId(), byteFP ,scopeTotalBytes.get(scope),
          byteFP * 100 / (double) totalBytes, byteFN, totalBytes, byteFN * 100 / (double)totalBytes,
          byteFP + byteFN, totalBytes, (byteFP + byteFN) * 100 / (double)totalBytes);
    }
  }
}
