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
import alluxio.client.file.cache.ShadowCacheParameters;
import alluxio.client.file.cache.dataset.Dataset;
import alluxio.client.file.cache.dataset.DatasetEntry;
import alluxio.client.file.cache.dataset.GeneralDataset;
import alluxio.client.file.cache.dataset.generator.EntryGenerator;
import alluxio.client.file.cache.dataset.generator.MSREntryGenerator;
import alluxio.client.file.cache.dataset.generator.RandomEntryGenerator;
import alluxio.client.file.cache.dataset.generator.SequentialEntryGenerator;
import alluxio.client.file.cache.dataset.generator.TwitterEntryGenerator;
import alluxio.client.quota.CacheScope;
import alluxio.util.FormatUtils;

import java.util.*;

public class MultiScopeAccuracyBenchmark implements Benchmark {
  private final BenchmarkContext mBenchmarkContext;
  private final BenchmarkParameters mBenchmarkParameters;
  private final ShadowCache[] mShadowCacheList;
  private final HashMap<CacheScope,ShadowCache> scope2ShadowCache =  new HashMap<>();
  private final ShadowCache mIdealShadowCache;
  private Boolean isCCF = false;
  private Dataset<String> mDataset;

  public MultiScopeAccuracyBenchmark(BenchmarkContext benchmarkContext,
                           BenchmarkParameters benchmarkParameters) {
    mBenchmarkContext = benchmarkContext;
    mBenchmarkParameters = benchmarkParameters;
    ShadowCache.ShadowCacheType type = ShadowCache.ShadowCacheType.valueOf(benchmarkParameters.mShadowCacheType.toUpperCase());
    if(type != ShadowCache.ShadowCacheType.CCF) {
      long memoryInBits = FormatUtils.parseSpaceSize(benchmarkParameters.mMemoryBudget) * 8;
      benchmarkParameters.mMemoryBudget = memoryInBits / benchmarkParameters.mNumScope / 1024 / 8 +"kb";
      mShadowCacheList = new ShadowCache[benchmarkParameters.mNumScope];
      for(int i=0;i< benchmarkParameters.mNumScope;i++){
        mShadowCacheList[i] = ShadowCache.create(benchmarkParameters);
      }

    }else{
      isCCF=true;
      mShadowCacheList = new ShadowCache[1];
      mShadowCacheList[0] = ShadowCache.create(benchmarkParameters);
    }
    mIdealShadowCache = new IdealShadowCacheManager(benchmarkParameters);
    createDataset();
    for(int i=0;i<mShadowCacheList.length;i++){
      mShadowCacheList[i].stopUpdate();
    }
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
    int freeIdx = 0;
    Set<CacheScope> scopeSet = new HashSet<>();
    HashMap<CacheScope,Double> scopeNumARE = new HashMap<>();
    HashMap<CacheScope,Double> scopeByteARE = new HashMap<>();

    HashMap<CacheScope,Long> scopeNumFP = new HashMap<>();
    HashMap<CacheScope,Long> scopeNumFN = new HashMap<>();
    HashMap<CacheScope,Long> scopeByteFP = new HashMap<>();
    HashMap<CacheScope,Long> scopeByteFN = new HashMap<>();
    HashMap<CacheScope,Long> scopeTotalBytes = new HashMap<>();
    HashMap<CacheScope,Long> scopeOps = new HashMap<>();
    HashMap<CacheScope,Long> scopeErrCnt = new HashMap<>();

    long agingPeriod = mBenchmarkParameters.mWindowSize / mBenchmarkParameters.mAgeLevels;
    if (agingPeriod <= 0) {
      agingPeriod = 1;
    }
    System.out.printf("agingPeriod:%d\n", agingPeriod);

    System.out.println(mShadowCacheList[0].getSummary());
    mBenchmarkContext.mStream.println(
        "#operation\tScope\tReal\tReal(byte)\tEst\tEst(byte)\t");

    long startTick = System.currentTimeMillis();
    while (mDataset.hasNext() && opsCount < mBenchmarkParameters.mMaxEntries) {
      opsCount++;

      DatasetEntry<String> entry = mDataset.next();
      long scopeCount = scopeOps.getOrDefault(entry.getScopeInfo(),0l);
      scopeOps.put(entry.getScopeInfo(),scopeCount+1);

      PageId item = new PageId(entry.getScopeInfo().toString(), entry.getItem().hashCode());
      long startCacheTick = System.currentTimeMillis();
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
      if(!scopeSet.contains(entry.getScopeInfo())){
        scopeSet.add(entry.getScopeInfo());
      }
      int nread = shadowCache.get(item, entry.getSize(), entry.getScopeInfo());
      if (nread <= 0) {
        shadowCache.put(item, entry.getSize(), entry.getScopeInfo());
      }
      shadowCache.updateTimestamp(1);
      cacheDuration += (System.currentTimeMillis() - startCacheTick);

      // Aging
      if (opsCount % agingPeriod == 0) {
        agingCount++;
        long startAgingTick = System.currentTimeMillis();
        for(int i=0;i< mShadowCacheList.length;i++){
          mShadowCacheList[i].aging();
        }
        agingDuration += System.currentTimeMillis() - startAgingTick;
      }

      // update ideal cache
      int nread2 = mIdealShadowCache.get(item, entry.getSize(), entry.getScopeInfo());
      if (nread2 <= 0) {
        mIdealShadowCache.put(item, entry.getSize(), entry.getScopeInfo());
      }
      mIdealShadowCache.updateTimestamp(1);
      mIdealShadowCache.aging();

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

      long totalBytes = scopeTotalBytes.getOrDefault(entry.getScopeInfo(),0l);
      scopeTotalBytes.put(entry.getScopeInfo(),totalBytes+entry.getSize());

      // report
      if (opsCount % mBenchmarkParameters.mReportInterval == 0) {
        mIdealShadowCache.updateWorkingSetSize();
        if(isCCF){
          mShadowCacheList[0].updateWorkingSetSize();
        }else{
          for(int i=0;i< mBenchmarkParameters.mNumScope;i++){
            mShadowCacheList[i].updateWorkingSetSize();
          }
        }
        // long realNum = mDataset.getRealEntryNumber();
        // long realByte = mDataset.getRealEntrySize();
        long realNum;
        long realByte;
        long estNum;
        long estByte;
        for(CacheScope scope:scopeSet){
          ShadowCache shadowCacheScope;
          realByte = mIdealShadowCache.getShadowCacheBytes(scope);
          realNum = mIdealShadowCache.getShadowCachePages(scope);
          if(isCCF){
            shadowCacheScope = mShadowCacheList[0];
            estNum = shadowCacheScope.getShadowCachePages(scope);
            estByte = shadowCacheScope.getShadowCacheBytes(scope);
          }else{
            shadowCacheScope = scope2ShadowCache.get(scope);
            estNum = shadowCacheScope.getShadowCachePages();
            estByte = shadowCacheScope.getShadowCacheBytes();
          }
          mBenchmarkContext.mStream.printf("%d\t%s\t%d\t%d\t%d\t%d\n", opsCount,scope.getId(),
              realNum, realByte, estNum, estByte);
          //System.out.println("[+] total est bytes: " + shadowCacheScope.getShadowCacheBytes());
          // accumulate error
          if(realByte==0|| realNum==0){
            continue;
          }
          double numARE = scopeNumARE.getOrDefault(scope,0.0);
          double byteARE = scopeByteARE.getOrDefault(scope,0.0);
          scopeNumARE.put(scope,numARE + Math.abs(estNum / (double) realNum - 1.0));
          scopeByteARE.put(scope,byteARE + Math.abs(estByte / (double) realByte - 1.0));
          long errCnt = scopeErrCnt.getOrDefault(scope,0L);
          scopeErrCnt.put(scope,errCnt+1);
        }
      }
    }
    long totalDuration = (System.currentTimeMillis() - startTick);

    System.out.println(mShadowCacheList[0].getSummary());
    System.out.println();
    System.out.println("TotalTime(ms)\t" + totalDuration);
    System.out.println("TotalScope\t" + scopeSet.size());
    for(CacheScope testscope:scopeSet){
      System.out.print(testscope.getId()+" ");
      System.out.println();
    }

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

  @Override
  public boolean finish() {
    return false;
  }
}
