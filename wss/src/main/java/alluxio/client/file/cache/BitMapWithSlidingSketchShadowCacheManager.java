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

package alluxio.client.file.cache;

import static java.lang.Math.min;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import alluxio.Constants;
import alluxio.client.file.cache.cuckoofilter.SlidingWindowType;
import alluxio.client.quota.CacheScope;
import alluxio.util.FormatUtils;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BitMapWithSlidingSketchShadowCacheManager implements ShadowCache {
  protected int mBucketNum;
  protected int mBitsPerSize;
  protected int mBitsPerScope;
  protected int mWindowSize;
  protected int mHashNum;
  private double numClearPerTime;
  private double clears;
  protected Funnel<PageId> mFunnel;
  protected List<HashFunction> hashFuncs = new LinkedList<HashFunction>();
  protected int[] traceSizeNew;
  protected int[] traceSizeOld;
  private final AtomicLong mShadowCachePageRead = new AtomicLong(0);
  private final AtomicLong mShadowCachePageHit = new AtomicLong(0);
  private final AtomicLong mShadowCacheByteRead = new AtomicLong(0);
  private final AtomicLong mShadowCacheByteHit = new AtomicLong(0);
  private final AtomicLong mTotalOnes = new AtomicLong(0);
  private final AtomicLong mTotalSize = new AtomicLong(0);
  private final ScheduledExecutorService mScheduler = Executors.newScheduledThreadPool(0);
  private final Lock lock = new ReentrantLock();
  private int currIdx;

  public BitMapWithSlidingSketchShadowCacheManager(ShadowCacheParameters params) {
    mBitsPerSize = params.mSizeBits;
    mWindowSize = (int)params.mWindowSize;
    mFunnel = PageIdFunnel.FUNNEL;
    mBitsPerScope = params.mScopeBits;
    mHashNum = params.mNumHashFunctions;
    long memoryInBits = FormatUtils.parseSpaceSize(params.mMemoryBudget) * 8;
    mBucketNum = (int) (memoryInBits / ((mBitsPerSize) * 2));
    for (int i = 0; i < mHashNum; i++) {
      hashFuncs.add(Hashing.murmur3_32(i + 32713));
    }
    numClearPerTime = (double)mBucketNum / mWindowSize;
    traceSizeNew = new int[mBucketNum];
    traceSizeOld = new int[mBucketNum];
    int agingPeriod = 1;
    if(params.mSlidingWindowType == SlidingWindowType.TIME_BASED){
      mScheduler.scheduleAtFixedRate(this::aging, agingPeriod, agingPeriod, MILLISECONDS);
    }
  }

  @Override
  public boolean put(PageId pageId, int size, CacheScope scope) {
    //System.out.println("in put");
    for (HashFunction hashFunc : hashFuncs) {
      int pos = Math.abs(hashFunc.newHasher().putObject(pageId, mFunnel).hash().asInt() % mBucketNum);
      lock.lock();
      if(traceSizeNew[pos]==0){
        if(traceSizeOld[pos]==0){
          mTotalOnes.incrementAndGet();
        }
      }
      traceSizeNew[pos] = size;
      lock.unlock();
      // traceSizeOld[pos] = 0;
    }
    //System.out.println("put end");
    return true;
  }

  @Override
  public int get(PageId pageId, int bytesToRead, CacheScope scope) {
    //System.out.println("in get");
    mShadowCachePageRead.incrementAndGet();
    mShadowCacheByteRead.addAndGet(bytesToRead);
    for (HashFunction hashFunc : hashFuncs) {
      int pos = Math.abs(hashFunc.newHasher().putObject(pageId, mFunnel).hash().asInt() % mBucketNum);
      lock.lock();
      if (traceSizeNew[pos] == 0 && traceSizeOld[pos] == 0) {
        lock.unlock();
        //System.out.println("out get");
        return 0;
      }
      lock.unlock();
      //System.out.println("out get");
    }
    mShadowCachePageHit.incrementAndGet();
    mShadowCacheByteHit.addAndGet(bytesToRead);
    return bytesToRead;
  }

  @Override
  public void aging() {
    lock.lock();
    clears+=numClearPerTime;
    int clearsI  = (int)Math.floor(clears);
    clears -= clearsI;
    int endPos = (currIdx+clearsI)%mBucketNum;

    if(endPos<currIdx){
      agingRange(currIdx,mBucketNum);
      agingRange(0,endPos);
    }else{
      agingRange(currIdx,endPos);
    }
    currIdx = endPos;
    lock.unlock();
  }

  private void agingRange(int start,int end){
    for(int i=start;i<end;i++){
      if(traceSizeNew[i]==0 && traceSizeOld[i]!=0){
        mTotalOnes.decrementAndGet();
      }
      traceSizeOld[i] = traceSizeNew[i];
      traceSizeNew[i] = 0;
    }
  }

  @Override
  public boolean delete(PageId pageId) {
    lock.lock();
    for (HashFunction hashFunc : hashFuncs) {
      int pos = Math.abs(hashFunc.newHasher().putObject(pageId, mFunnel).hash().asInt() % mBucketNum);
      traceSizeNew[pos] = 0;
      traceSizeOld[pos] = 0;
    }
    lock.unlock();
    return true;
  }

  @Override
  public void updateWorkingSetSize() {}

  @Override
  public void stopUpdate() {
    mScheduler.shutdown();
  }

  @Override
  public void updateTimestamp(long increment) {}

  @Override
  public long getShadowCachePages() {
    double zeros = mBucketNum-mTotalOnes.get();
    double pages;


    if (zeros == 0) {
      pages = -mBucketNum/(double)mHashNum * Math.log(1.0 / mBucketNum);
    } else {
      pages = -mBucketNum/(double)mHashNum * Math.log(zeros / (double) mBucketNum);
    }
    return (long)pages;
  }

  @Override
  public long getShadowCachePages(CacheScope scope) {
    return 0;
  }

  @Override
  public long getShadowCacheBytes() {
    long sizeSum = 0;
    //long ones = mTotalOnes.get();
    long ones = 0;
    for (int i = 0; i < mBucketNum; i++) {
      // TODO- how to combine
      double traceSize = traceSizeNew[i] + traceSizeOld[i];
      if(traceSizeNew[i]!=0){
        ones++;
      }
      if(traceSizeOld[i]!=0){
        ones++;
      }
      sizeSum += traceSize;
    }
    double avgSize = (double)sizeSum/ones;

    return (long)avgSize*getShadowCachePages();
  }

  @Override
  public long getShadowCacheBytes(CacheScope scope) {
    return 0;
  }

  @Override
  public long getShadowCachePageRead() {
    return mShadowCachePageRead.get();
  }

  @Override
  public long getShadowCachePageHit() {
    return mShadowCachePageHit.get();
  }

  @Override
  public long getShadowCacheByteRead() {
    return mShadowCacheByteRead.get();
  }

  @Override
  public long getShadowCacheByteHit() {
    return mShadowCacheByteHit.get();
  }

  @Override
  public double getFalsePositiveRatio() {
    return 0;
  }

  @Override
  public long getSpaceBits() {
    return (long) mBucketNum * mBitsPerSize * 2;
  }

  @Override
  public String getSummary() {
    return "Sliding Sketch   numBuckets: " + mBucketNum
        + "\nbitsPerSize: " + mBitsPerSize
        + "\nSizeInMB: " + (getSpaceBits() / 8.0 / Constants.MB)
        + "\nClearItemsPerTime: " +  numClearPerTime
        + "\nHashNum: " + mHashNum;
  }
}
