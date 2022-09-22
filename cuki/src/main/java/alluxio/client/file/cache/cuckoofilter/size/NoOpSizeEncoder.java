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

import java.util.concurrent.atomic.AtomicLong;

public class NoOpSizeEncoder implements ISizeEncoder {
  protected final int maxSizeBits;
  protected final int sizeMask;

  protected final AtomicLong totalBytes = new AtomicLong(0);
  protected final AtomicLong totalCounts = new AtomicLong(0);

  public NoOpSizeEncoder(int maxSizeBits) {
    this.maxSizeBits = maxSizeBits;
    this.sizeMask = (1 << maxSizeBits) - 1;
  }

  public void add(int size) {
    totalBytes.addAndGet(encode(size));
    totalCounts.incrementAndGet();
  }

  public int dec(int group) {
    totalBytes.addAndGet(-group);
    totalCounts.decrementAndGet();
    return group;
  }

  public long getTotalSize() {
    return totalBytes.get();
  }

  @Override
  public long getTotalCount() {
    return totalCounts.get();
  }

  @Override
  public void access(int size) {
    // no-op
  }

  @Override
  public int encode(int size) {
    return size & sizeMask;
  }

  @Override
  public String dumpInfo() {
    return "";
  }
}

