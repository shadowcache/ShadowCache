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

public class Bucket {
  private AtomicLong totalBytes = new AtomicLong(0);
  private AtomicLong count = new AtomicLong(0);

  public void add(int size) {
    totalBytes.addAndGet(size);
    count.incrementAndGet();
  }

  public int decrement() {
    int size = (int) getAverageSize();
    totalBytes.addAndGet(-size);
    count.decrementAndGet();
    return size;
  }

  public long getAverageSize() {
    return Math.round(totalBytes.get() / count.doubleValue());
  }

  public long getSize() {
    return totalBytes.get();
  }

  public long getCount() {
    return count.get();
  }
}
