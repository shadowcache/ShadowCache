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

import alluxio.client.file.cache.cuckoofilter.ClockCuckooFilter;
import alluxio.client.file.cache.cuckoofilter.ConcurrentClockCuckooFilter;
import alluxio.client.file.cache.cuckoofilter.SlidingWindowType;
import alluxio.client.quota.CacheScope;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class is a shadow cache with {@link ClockCuckooFilter} implementation.
 */
public class ClockCuckooShadowCacheManager implements ShadowCache {
  private static final int SLOTS_PER_BUCKET = 4;
  private static final int BITS_PER_TAG = 8;

  private final ScheduledExecutorService mScheduler = Executors.newScheduledThreadPool(0);
  private final ConcurrentClockCuckooFilter<PageId> mFilter;
  private final AtomicLong mShadowCachePageRead = new AtomicLong(0);
  private final AtomicLong mShadowCachePageHit = new AtomicLong(0);
  private final AtomicLong mShadowCacheByteRead = new AtomicLong(0);
  private final AtomicLong mShadowCacheByteHit = new AtomicLong(0);
  private long mShadowCacheBytes = 0;
  private long mShadowCachePages = 0;

  /**
   * Create a ClockCuckooShadowCacheManager.
   *
   * @param conf the alluxio configuration
   */
  public ClockCuckooShadowCacheManager(ShadowCacheParameters conf) {
    long windowMs = conf.mWindowSize;
    int bitsPerClock = conf.mClockBits;
    mFilter = ConcurrentClockCuckooFilter.create(PageIdFunnel.FUNNEL, conf);
    long agingPeriod = windowMs >> bitsPerClock;
    if (conf.mSlidingWindowType == SlidingWindowType.TIME_BASED){
      mScheduler.scheduleAtFixedRate(this::aging, agingPeriod, agingPeriod, MILLISECONDS);
    }
  }

  @Override
  public boolean put(PageId pageId, int size, CacheScope cacheScope) {
    return updateClockCuckoo(pageId, size, cacheScope);
  }

  /**
   * Put a page into shadow cache if it is not existed.
   *
   * @param pageId page identifier
   * @param size page size
   * @param cacheScope cache scope
   * @return true if page is put successfully; false otherwise
   */
  private boolean updateClockCuckoo(PageId pageId, int size, CacheScope cacheScope) {
    boolean ok = true;
    if (!mFilter.mightContainAndResetClock(pageId)) {
      ok = mFilter.put(pageId, size, cacheScope);
      updateWorkingSetSize();
    }
    return ok;
  }

  @Override
  public int get(PageId pageId, int bytesToRead, CacheScope cacheScope) {
    boolean seen = mFilter.mightContainAndResetClock(pageId);
    if (seen) {
      mShadowCachePageHit.getAndIncrement();
      mShadowCacheByteHit.getAndAdd(bytesToRead);
    }
    mShadowCachePageRead.getAndIncrement();
    mShadowCacheByteRead.getAndAdd(bytesToRead);
    return seen ? bytesToRead : 0;
  }

  @Override
  public boolean delete(PageId pageId) {
    return mFilter.delete(pageId);
  }

  @Override
  public void aging() {
    mFilter.aging();
  }

  @Override
  public void updateWorkingSetSize() {
    mShadowCachePages = mFilter.approximateElementCount();
    mShadowCacheBytes = mFilter.approximateElementSize();
  }

  @Override
  public void stopUpdate() {
    mScheduler.shutdown();
  }

  @Override
  public void updateTimestamp(long increment) {
    mFilter.increaseOperationCount((int) increment);
  }

  @Override
  public long getShadowCachePages() {
    return mShadowCachePages;
  }

  @Override
  public long getShadowCachePages(CacheScope scope) {
    return mFilter.approximateElementCount(scope);
  }

  @Override
  public long getShadowCacheBytes() {
    return mShadowCacheBytes;
  }

  @Override
  public long getShadowCacheBytes(CacheScope scope) {
    return mFilter.approximateElementSize(scope);
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
    return mFilter.expectedFpp();
  }

  @Override
  public long getSpaceBits() {
    return (mFilter.getBitsPerTag() + mFilter.getBitsPerClock() + mFilter.getBitsPerSize()
        + mFilter.getBitsPerScope()) * mFilter.getTagsPerBucket() * mFilter.getNumBuckets();
  }

  @Override
  public String getSummary() {
    return "ClockCuckooShadowCache:\n" + mFilter.getSummary();
  }

  @Override
  public String dumpDebugInfo() {
    return mFilter.dumpDebugInfo();
  }
}
