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

import alluxio.Constants;
import alluxio.client.file.cache.cuckoofilter.SlidingWindowType;
import alluxio.client.quota.CacheScope;
import alluxio.util.LRU;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class IdealShadowCacheManager implements ShadowCache {
  private final Lock lock;
  private final LRU itemLRU;
  private final HashMap<PageId, ItemAttribute> itemToAttribute;
  private final HashMap<CacheScope, Integer> scopeToNumber;
  private final HashMap<CacheScope, Long> scopeToSize;

  private final AtomicLong mShadowCachePageRead = new AtomicLong(0);
  private final AtomicLong mShadowCachePageHit = new AtomicLong(0);
  private final AtomicLong mShadowCacheByteRead = new AtomicLong(0);
  private final AtomicLong mShadowCacheByteHit = new AtomicLong(0);

  private final int bitsPerItem;
  private final int bitsPerScope;
  private final int bitsPerSize;
  private final int bitsPerTimestamp;
  private final long windowSize;
  private long realNumber;
  private long realSize;
  private SlidingWindowType mSlidingWindowType;
  private long timestampNow;

  public IdealShadowCacheManager(ShadowCacheParameters params) {
    mSlidingWindowType = params.mSlidingWindowType;
    this.windowSize = params.mWindowSize;
    this.lock = new ReentrantLock();
    this.itemLRU = new LRU();
    this.itemToAttribute = new HashMap<>();
    this.scopeToNumber = new HashMap<>();
    this.scopeToSize = new HashMap<>();
    this.bitsPerItem = params.mPageBits;
    this.bitsPerScope = params.mScopeBits;
    this.bitsPerSize = params.mSizeBits;
    this.bitsPerTimestamp = 64;
    this.realNumber = 0;
    this.realSize = 0;
    this.timestampNow = 0;
  }

  @Override
  public boolean put(PageId pageId, int size, CacheScope scope) {
    boolean success;
    lock.lock();
    if (pageId == null) {
      return false;
    }
    ItemAttribute attribute = itemToAttribute.get(pageId);
    if (attribute == null) {
      itemToAttribute.put(pageId, new ItemAttribute(size, scope, getCurrentTimestamp()));
      success = itemLRU.put(pageId);
      scopeToSize.put(scope, scopeToSize.getOrDefault(scope, 0L) + size);
      scopeToNumber.put(scope, scopeToNumber.getOrDefault(scope, 0) + 1);
      realSize += size;
    } else {
      attribute.timeStamp = getCurrentTimestamp();
      success = itemLRU.put(pageId);
    }
    realNumber = itemLRU.getSize();
    lock.unlock();
    return success;
  }

  @Override
  public int get(PageId pageId, int bytesToRead, CacheScope scope) {
    lock.lock();
    // aging();
    int nread = 0;
    ItemAttribute attribute = itemToAttribute.get(pageId);
    if (attribute != null && attribute.timeStamp >= (getCurrentTimestamp() - windowSize)) {
      // on cache hit
      mShadowCachePageHit.incrementAndGet();
      bytesToRead = attribute.size;
      nread = bytesToRead;
      mShadowCacheByteHit.addAndGet(nread);
      attribute.timeStamp = getCurrentTimestamp();
      itemLRU.put(pageId);
    }
    lock.unlock();
    mShadowCachePageRead.incrementAndGet();
    mShadowCacheByteRead.addAndGet(bytesToRead);
    return nread;
  }

  @Override
  public boolean delete(PageId pageId) {
    lock.lock();
    boolean b1 = itemLRU.remove(pageId);
    boolean b2 = itemToAttribute.remove(pageId) != null;
    lock.unlock();
    return b1 && b2;
  }

  /**
   * Assert held lock
   */
  public void aging() {
    long oldTimestamp = getCurrentTimestamp() - windowSize;
    if (oldTimestamp > 0) {
      while (!itemLRU.isEmpty()) {
        PageId item = itemLRU.peek();
        ItemAttribute itemAttribute = itemToAttribute.get(item);
        if (itemAttribute.timeStamp < oldTimestamp) {
          itemLRU.poll();
          realSize -= itemAttribute.size;
          scopeToNumber.put(itemAttribute.scope,
              scopeToNumber.getOrDefault(itemAttribute.scope, 0) - 1);
          scopeToSize.put(itemAttribute.scope,
              scopeToSize.getOrDefault(itemAttribute.scope, 0L) - itemAttribute.size);
          itemToAttribute.remove(item);
        } else {
          break;
        }

      }
    }
    realNumber = itemLRU.getSize();
  }

  private long getCurrentTimestamp() {
    return (mSlidingWindowType == SlidingWindowType.TIME_BASED) ? System.currentTimeMillis()
        : timestampNow;
  }

  @Override
  public void updateWorkingSetSize() {
    lock.lock();
    aging();
    lock.unlock();
  }

  @Override
  public void stopUpdate() {
    // no-op
  }

  @Override
  public void updateTimestamp(long increment) {
    this.timestampNow += increment;
  }

  @Override
  public long getShadowCachePages() {
    return realNumber;
  }

  @Override
  public long getShadowCachePages(CacheScope scope) {
    return scopeToNumber.get(scope);
  }

  @Override
  public long getShadowCacheBytes() {
    return realSize;
  }

  @Override
  public long getShadowCacheBytes(CacheScope scope) {
    return scopeToSize.get(scope);
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
    long scopeNum = scopeToNumber.size();
    long space = realNumber * (bitsPerItem + bitsPerTimestamp + bitsPerScope + bitsPerTimestamp);
    space += scopeNum * (bitsPerScope * 2 + 32 + bitsPerSize);
    return space;
  }

  @Override
  public String getSummary() {
    return "MultipleBloomShadowCache:\nbitsPerItem: " + bitsPerItem + "\nbitsPerSize: "
        + bitsPerSize + "\nbitsPerScope: " + bitsPerScope + "\nSizeInMB: "
        + (windowSize * (bitsPerItem + bitsPerTimestamp + bitsPerScope + bitsPerTimestamp) / 8.0
            / Constants.MB
            + windowSize * (bitsPerScope * 2 + 32 + bitsPerSize) / 8.0 / Constants.MB);
  }

  private static class ItemAttribute {
    int size;
    CacheScope scope;
    long timeStamp;

    public ItemAttribute(int size, CacheScope scope, long timeStamp) {
      this.size = size;
      this.scope = scope;
      this.timeStamp = timeStamp;
    }
  }
}
