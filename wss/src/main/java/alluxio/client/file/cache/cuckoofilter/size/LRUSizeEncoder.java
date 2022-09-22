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

package alluxio.client.file.cache.cuckoofilter.size;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;

public class LRUSizeEncoder implements ISizeEncoder {
  private final int maxSizeBits;
  private final int numGroupBits;
  private final int numBucketBits;
  private final int numGroups;
  private final int numBuckets;
  private final int numBucketsPerGroup;
  private final int sizePerGroup;
  private final LRUGroup groups[];

  private final AtomicLong totalBytes = new AtomicLong(0);
  private final AtomicLong totalCounts = new AtomicLong(0);

  public LRUSizeEncoder(int maxSizeBits, int numGroupBits, int numBucketBits) {
    this.maxSizeBits = maxSizeBits;
    this.numGroupBits = numGroupBits;
    this.numGroups = (1 << numGroupBits);
    this.numBucketBits = numBucketBits;
    this.numBuckets = (1 << numBucketBits);
    this.numBucketsPerGroup = (1 << (numBucketBits - numGroupBits));
    this.sizePerGroup = (1 << (maxSizeBits - numGroupBits));
    this.groups = new LRUGroup[numGroups];
    for (int i = 0; i < numGroups; i++) {
      groups[i] = new LRUGroup(numBucketBits - numGroupBits, maxSizeBits - numGroupBits);
    }
  }

  public void add(int size) {
    totalBytes.addAndGet(size);
    totalCounts.incrementAndGet();
    groups[getGroup(size)].add(maskSize(size));
  }

  public int dec(int group) {
    int size = groups[group].dec();
    totalBytes.addAndGet(-size - sizePerGroup * group);
    totalCounts.decrementAndGet();
    return size;
  }

  @Override
  public void access(int size) {
    groups[getGroup(size)].access(maskSize(size));
  }

  public long getTotalSize() {
    return totalBytes.get();
  }

  @Override
  public long getTotalCount() {
    return totalCounts.get();
  }

  private int getGroup(int size) {
    return (size >> (maxSizeBits - numGroupBits));
  }

  private int maskSize(int size) {
    // return (size & ((1 << (maxSizeBits - numGroupBits)) - 1));
    return size % sizePerGroup;
  }

  @Override
  public int encode(int size) {
    return getGroup(size);
  }

  static class LRUGroup {
    private final int totalBits;
    private final int numBucketBits;
    private final int numBuckets;
    private final Bucket[] buckets;
    private final LinkedList<Integer> lruCache;

    LRUGroup(int numBucketBits, int totalBits) {
      this.totalBits = totalBits;
      this.numBucketBits = numBucketBits;
      this.numBuckets = (1 << numBucketBits);
      this.buckets = new Bucket[numBuckets];
      this.lruCache = new LinkedList<>();
      for (int i = 0; i < numBuckets; i++) {
        buckets[i] = new Bucket();
        lruCache.add(i);
      }
    }

    public void add(int size) {
      // add the accessed bucket to the tail
      int b = getBucket(size);
      buckets[b].add(size);
      lruCache.remove(Integer.valueOf(b));
      lruCache.add(b);
    }

    public int dec() {
      // dec the lru bucket
      int b = -1;
      for (int x : lruCache) {
        if (buckets[x].getCount() > 0) {
          b = x;
          break;
        }
      }
      if (b >= 0) {
        return buckets[b].decrement();
      } else {
        return 0;
      }
    }

    public void access(int size) {
      // TODO(iluoeli): optimize
      int b = getBucket(size);
      lruCache.remove(Integer.valueOf(b));
      lruCache.add(b);
    }

    public long getTotalCount() {
      long totalCount = 0;
      for (int i = 0; i < numBuckets; i++) {
        totalCount += buckets[i].getCount();
      }
      return totalCount;
    }

    public long getTotalSize() {
      long totalSize = 0;
      for (int i = 0; i < numBuckets; i++) {
        totalSize += buckets[i].getSize();
      }
      return totalSize;
    }

    private int getBucket(int size) {
      return size >> (totalBits - numBucketBits);
    }
  }

  @Override
  public String dumpInfo() {
    return "";
  }
}

