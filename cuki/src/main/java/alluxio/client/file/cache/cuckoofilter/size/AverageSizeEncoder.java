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

public class AverageSizeEncoder extends SizeEncoder {

  public AverageSizeEncoder(int maxSizeBits, int numBucketsBits) {
    super(maxSizeBits, numBucketsBits);
  }

  @Override
  public void access(int size) {
    // average the total size whenever an entry is accessed
    int group = getSizeGroup(size);
    long oldGroupSize = buckets[group].getSize();
    buckets[group].add(size);
    buckets[group].decrement();
    totalBytes.addAndGet(buckets[group].getSize() - oldGroupSize);
  }
}

