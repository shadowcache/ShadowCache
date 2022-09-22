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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public class LogSizeEncoder implements ISizeEncoder {
  protected final int sizeGroupBits;
  protected final int numBuckets;
  protected final int first;
  protected final int base;
  protected final int bias;
  protected final Bucket[] buckets;
  protected final int[] bucketBounds;

  protected final AtomicLong totalBytes = new AtomicLong(0);
  protected final AtomicLong totalCounts = new AtomicLong(0);

  public LogSizeEncoder(int numBucketsBits, int first, int base, int bias) {
    this.sizeGroupBits = numBucketsBits;
    this.numBuckets = (1 << numBucketsBits);
    this.first = first;
    this.base = base;
    this.bias = bias;
    this.buckets = new Bucket[numBuckets];
    this.bucketBounds = new int[numBuckets];
    int a = first;
    int m = 1;
    for (int i = 0; i < numBuckets; i++) {
      buckets[i] = new Bucket();
      bucketBounds[i] = a * m + bias;
      m *= base;
    }
    bucketBounds[numBuckets-1] = Integer.MAX_VALUE;
  }

  public void add(int size) {
    totalBytes.addAndGet(size);
    totalCounts.incrementAndGet();
    buckets[getSizeGroup(size)].add(size);
  }

  public int dec(int group) {
    int size = buckets[group].decrement();
    totalBytes.addAndGet(-size);
    totalCounts.decrementAndGet();
    return size;
  }

  public long getTotalSize() {
    return totalBytes.get();
  }

  @Override
  public long getTotalCount() {
    return totalCounts.get();
  }

  protected int getSizeGroup(int size) {
    return Math.abs(Arrays.binarySearch(bucketBounds, size)) - 1;
  }

  @Override
  public void access(int size) {
    // no-op
  }

  @Override
  public int encode(int size) {
    return getSizeGroup(size);
  }

  @Override
  public String dumpInfo() {
    StringBuilder stringBuilder = new StringBuilder();
    for (int i=0; i < numBuckets; i++) {
      stringBuilder.append(String.format("%d [%d, %d] <%d, %d>\n",
              i, (i > 0) ? bucketBounds[i-1] : 0, bucketBounds[i],
              buckets[i].getCount(), buckets[i].getSize()));
    }
    return stringBuilder.toString();
  }
}

