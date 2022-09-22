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

package alluxio.client.file.cache.dataset.generator;

import alluxio.client.file.cache.dataset.DatasetEntry;
import alluxio.client.quota.CacheScope;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class RandomIntegerEntryGenerator implements EntryGenerator<Integer> {
  private static final int DEFAULT_LOWER_BOUND = 0;
  private static final int DEFAULT_UPPER_BOUND = 1000;
  private static final int DEFAULT_LOWER_BOUND_SIZE = 0;
  private static final int DEFAULT_UPPER_BOUND_SIZE = 1024;
  private static final int DEFAULT_NUM_SCOPES = 64;
  private static final int DEFAULT_SEED = 32173;

  private final long numEntries;
  private final int lowerBound;
  private final int upperBound;
  private final int lowerBoundSize;
  private final int upperBoundSize;
  private final int numScopes;
  private final Random random;

  private final AtomicLong count;

  public RandomIntegerEntryGenerator(long numEntries) {
    this(numEntries, DEFAULT_LOWER_BOUND, DEFAULT_UPPER_BOUND, DEFAULT_LOWER_BOUND_SIZE,
        DEFAULT_UPPER_BOUND_SIZE, DEFAULT_NUM_SCOPES, DEFAULT_SEED);
  }

  public RandomIntegerEntryGenerator(long numEntries, int lowerBound, int upperBound) {
    this(numEntries, lowerBound, upperBound, DEFAULT_LOWER_BOUND_SIZE, DEFAULT_UPPER_BOUND_SIZE,
        DEFAULT_NUM_SCOPES, DEFAULT_SEED);
  }

  public RandomIntegerEntryGenerator(long numEntries, int lowerBound, int upperBound,
      int lowerBoundSize, int upperBoundSize, int numScopes, int seed) {
    this.numEntries = numEntries;
    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
    this.lowerBoundSize = lowerBoundSize;
    this.upperBoundSize = upperBoundSize;
    this.numScopes = numScopes;
    this.random = new Random(seed);
    this.count = new AtomicLong(0);
  }

  @Override
  public DatasetEntry<Integer> next() {
    int item = lowerBound + random.nextInt(upperBound - lowerBound);
    CacheScope scope = CacheScope.create("schema1.table" + (item % numScopes));
    int size = lowerBoundSize + (item * 31213) % (upperBoundSize - lowerBoundSize);
    if (size < 0) {
      size = -size;
    }
    count.incrementAndGet();
    return new DatasetEntry<>(item, size, scope);
  }

  @Override
  public boolean hasNext() {
    return count.longValue() < numEntries;
  }
}
