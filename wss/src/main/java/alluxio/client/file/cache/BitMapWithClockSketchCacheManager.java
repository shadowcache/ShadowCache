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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import alluxio.Constants;
import alluxio.client.file.cache.cuckoofilter.SlidingWindowType;
import alluxio.client.quota.CacheScope;
import alluxio.util.FormatUtils;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BitMapWithClockSketchCacheManager implements ShadowCache {
  protected static int mNumBuckets;
  protected static int mBitsPerSize;
  protected static int mBitsPerClock;
  private final AtomicLong mShadowCachePageRead = new AtomicLong(0);
  private final AtomicLong mShadowCachePageHit = new AtomicLong(0);
  private final AtomicLong mShadowCacheByteRead = new AtomicLong(0);
  private final AtomicLong mShadowCacheByteHit = new AtomicLong(0);
  private final AtomicLong mBucketsSet = new AtomicLong(0);
  private final AtomicLong mTotalSize = new AtomicLong(0);
  private final ScheduledExecutorService mScheduler = Executors.newScheduledThreadPool(0);
  protected int mBitsPerScope;
  protected long mWindowSize;
  protected Funnel<PageId> mFunnel;
  protected HashFunction mHashFunction;
  protected int[] clockTable;
  protected int[] sizeTable;
  // TODO - SCOPE 存储重复，能不能从某一个位置开始都存某个scope的pageid
  protected long[] scopeTable; // stores the fingerprint of scope
  protected int mSizeMask;
  protected long mScopeMask;
  private final Lock lock = new ReentrantLock();

  public BitMapWithClockSketchCacheManager(ShadowCacheParameters parameters) {
    mBitsPerClock = parameters.mClockBits;
    mBitsPerSize = parameters.mSizeBits;
    mWindowSize = parameters.mWindowSize;
    mBitsPerScope = parameters.mScopeBits;
    mFunnel = PageIdFunnel.FUNNEL;
    long memoryInBits = FormatUtils.parseSpaceSize(parameters.mMemoryBudget) * 8;
    mNumBuckets = (int) (memoryInBits
        / (parameters.mClockBits + parameters.mSizeBits + parameters.mScopeBits));
    mHashFunction = Hashing.murmur3_32(32713);
    clockTable = new int[mNumBuckets];
    sizeTable = new int[mNumBuckets];
    scopeTable = new long[mNumBuckets];
    mSizeMask = (mBitsPerSize < 32) ? (1 << mBitsPerSize) - 1 : 0xFFFFFFFF;
    mScopeMask = (mBitsPerScope < 64) ? (1 << mBitsPerScope) - 1 : -1L;
    long windowMs = parameters.mWindowSize;
    long agingPeriod = windowMs >> mBitsPerClock;
    if(parameters.mSlidingWindowType == SlidingWindowType.TIME_BASED){
      mScheduler.scheduleAtFixedRate(this::aging, agingPeriod, agingPeriod, MILLISECONDS);
    }
  }

  @Override
  public boolean put(PageId pageId, int size, CacheScope scope) {
    int pos = bucketIndex(pageId, mHashFunction);
    long scopefp = encodeScope(scope);
    lock.lock();
    if (clockTable[pos] == 0) {
      sizeTable[pos] = size & mSizeMask;
      scopeTable[pos] = scopefp & mScopeMask;
      clockTable[pos] = (1 << mBitsPerClock) - 1;
      mBucketsSet.incrementAndGet();
      mTotalSize.addAndGet(size);
    } else if (mBitsPerScope > 0 && scopeTable[pos] == scopefp) { // hit
      clockTable[pos] = (1 << mBitsPerClock) - 1;
    } else { // collision
      // no-op
    }
    lock.unlock();
    return true;
  }

  @Override
  public int get(PageId pageId, int bytesToRead, CacheScope scope) {
    mShadowCachePageRead.incrementAndGet();
    mShadowCacheByteRead.addAndGet(bytesToRead);
    long scopefp = encodeScope(scope);
    int pos = bucketIndex(pageId, mHashFunction);
    lock.lock();
    if (clockTable[pos] == 0 || (mBitsPerScope > 0 && scopefp != scopeTable[pos])) {
      lock.unlock();
      return 0;
    }
    // reset CLOCK
    clockTable[pos] = (1 << mBitsPerClock) - 1;
    lock.unlock();
    mShadowCachePageHit.incrementAndGet();
    mShadowCacheByteHit.addAndGet(bytesToRead);
    return bytesToRead;
  }

  @Override
  public boolean delete(PageId pageId) {
    int pos = bucketIndex(pageId, mHashFunction);
    lock.lock();
    clockTable[pos] = 0;
    lock.unlock();
    return false;
  }


  public void aging() {
    for (int i = 0; i < mNumBuckets; i++) {
      if (clockTable[i] > 1) {
        clockTable[i] -= 1;
      } else if (clockTable[i] == 1) {
        mBucketsSet.addAndGet(-1);
        mTotalSize.addAndGet(-sizeTable[i]);
        clockTable[i] = 0;
        sizeTable[i] = 0;
        scopeTable[i] = 0;
      }
    }
  }

  @Override
  public void updateWorkingSetSize() {
    // no-op
  }

  @Override
  public void stopUpdate() {
    mScheduler.shutdown();
  }

  @Override
  public void updateTimestamp(long increment) {
    // no-op
  }

  @Override
  public long getShadowCachePages() {
    long zeros = mNumBuckets - mBucketsSet.get();
    double pages = 0.;
    if (zeros == 0) {
      pages = mNumBuckets * Math.log(mNumBuckets);
    } else {
      pages = -mNumBuckets * Math.log(zeros / (double) mNumBuckets);
    }
    return (long) (pages); // 源码里并没有/hashNum，原作者也没考虑
  }

  @Override
  public long getShadowCachePages(CacheScope scope) {
    long ones = 0;
    long scopefp = encodeScope(scope);
    for (int i = 0; i < mNumBuckets; ++i) {
      if (clockTable[i] > 0 && scopefp == scopeTable[i]) {
        ones++;
      }
    }
    long zeros = mNumBuckets - ones;
    double pages = 0.;
    if (zeros == 0) {
      pages = mNumBuckets * Math.log(mNumBuckets);
    } else {
      pages = -mNumBuckets * Math.log(zeros / (double) mNumBuckets);
    }
    return (long) (pages); // 源码里并没有/hashNum，原作者也没考虑
  }

  @Override
  public long getShadowCacheBytes() {
    double pages = getShadowCachePages();
    double avePageSize = mTotalSize.get() / (double) mBucketsSet.get();
    return (long) (pages * avePageSize);
  }

  @Override
  public long getShadowCacheBytes(CacheScope scope) {
    long ones = 0;
    double totalSize = 0.;
    long scopefp = encodeScope(scope);
    for (int i = 0; i < mNumBuckets; ++i) {
      if (clockTable[i] > 0 && scopeTable[i] == scopefp) {
        ones++;
        totalSize += sizeTable[i];
      }
    }
    long zeros = mNumBuckets - ones;
    double pages = -mNumBuckets * Math.log(zeros / (double) mNumBuckets);
    double avePageSize = totalSize / (double) ones;
    return (long) (pages * avePageSize);
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
    return mNumBuckets * (mBitsPerScope + mBitsPerClock + mBitsPerSize);
  }

  @Override
  public String getSummary() {
    return "bitmapWithClockSketch\bnumBuckets: " + mNumBuckets + "\nbitsPerClock: " + mBitsPerClock
        + "\nbitsPerSize: " + mBitsPerSize + "\nbitsPerScope: " + mBitsPerScope + "\nSizeInMB: "
        + (mNumBuckets * mBitsPerClock / 8.0 / Constants.MB
            + mNumBuckets * mBitsPerSize / 8.0 / Constants.MB
            + mNumBuckets * mBitsPerScope / 8.0 / Constants.MB);
  }

  private int bucketIndex(PageId pageId, HashFunction hashFunc) {
    return Math.abs(hashFunc.newHasher().putObject(pageId, mFunnel).hash().asInt() % mNumBuckets);
  }

  private long encodeScope(CacheScope scope) {
    return scope.hashCode() & mScopeMask;
  }

}
