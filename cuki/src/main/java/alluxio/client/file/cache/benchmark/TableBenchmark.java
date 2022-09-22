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

package alluxio.client.file.cache.benchmark;

import alluxio.Constants;
import alluxio.client.file.cache.cuckoofilter.CuckooTable;
import alluxio.client.file.cache.cuckoofilter.SimpleCuckooTable;
import alluxio.collections.BitSet;

import java.util.Random;

public class TableBenchmark implements Benchmark {
  private static final int NUM_BITS = 125 * 1024 * Constants.MB;
  private static final int TAGS_PER_BUCKET = 4;

  private final BenchmarkContext mBenchmarkContext;
  private final BenchmarkParameters mBenchmarkParameters;
  private final CuckooTable mTable;
  private final int mBitsPerTag;
  private final int mNumBuckets;

  public TableBenchmark(BenchmarkContext benchmarkContext, BenchmarkParameters parameters) {
    mBenchmarkContext = benchmarkContext;
    mBenchmarkParameters = parameters;
    mBitsPerTag = parameters.mTagBits;
    mNumBuckets = NUM_BITS / TAGS_PER_BUCKET / mBitsPerTag;
    mTable = new SimpleCuckooTable(BitSet.createBitSet(parameters.mBitSetType, NUM_BITS),
        mNumBuckets, TAGS_PER_BUCKET, mBitsPerTag);
  }

  @Override
  public void run() {
    System.out.printf("tagsPerBucket: %d\n", TAGS_PER_BUCKET);
    System.out.printf("bitsPerTag: %d\n", mBitsPerTag);
    System.out.printf("bitsetType: %s\n", mBenchmarkParameters.mBitSetType);

    // 1. sequential read benchmark
    long startTick = System.currentTimeMillis();
    for (int i = 0; i < mNumBuckets; i++) {
      for (int j = 0; j < TAGS_PER_BUCKET; j++) {
        int tag = mTable.readTag(i, j);
      }
    }
    long duration1 = System.currentTimeMillis() - startTick;
    System.out.printf("sequential read: %d ms, %f Mops/sec\n", duration1,
        1000L * mNumBuckets * TAGS_PER_BUCKET / (double) duration1 / (1000 * 1000));

    // 2. random read benchmark
    startTick = System.currentTimeMillis();
    Random random = new Random(32749);
    for (int i = 0; i < mNumBuckets * TAGS_PER_BUCKET; i++) {
      int r = random.nextInt(mNumBuckets * TAGS_PER_BUCKET);
      int bucketIndex = r / TAGS_PER_BUCKET;
      int slotIndex = r % TAGS_PER_BUCKET;
      int tag = mTable.readTag(bucketIndex, slotIndex);
    }
    long duration2 = System.currentTimeMillis() - startTick;
    System.out.printf("random read: %d ms, %f Mops/sec\n", duration2,
        1000L * mNumBuckets * TAGS_PER_BUCKET / (double) duration2 / (1000 * 1000));


    // 3. sequential write benchmark
    startTick = System.currentTimeMillis();
    for (int i = 0; i < mNumBuckets; i++) {
      for (int j = 0; j < TAGS_PER_BUCKET; j++) {
        mTable.writeTag(i, j, 0x12345678);
      }
    }
    long duration3 = System.currentTimeMillis() - startTick;
    System.out.printf("sequential write: %d ms, %f Mops/sec\n", duration3,
        1000L * mNumBuckets * TAGS_PER_BUCKET / (double) duration3 / (1000 * 1000));

    // 4. random write benchmark
    startTick = System.currentTimeMillis();
    for (int i = 0; i < mNumBuckets * TAGS_PER_BUCKET; i++) {
      int r = random.nextInt(mNumBuckets * TAGS_PER_BUCKET);
      int bucketIndex = r / TAGS_PER_BUCKET;
      int slotIndex = r % TAGS_PER_BUCKET;
      mTable.writeTag(bucketIndex, slotIndex, 0x12345678);
    }
    long duration4 = System.currentTimeMillis() - startTick;
    System.out.printf("random write: %d ms, %f Mops/sec\n", duration4,
        1000L * mNumBuckets * TAGS_PER_BUCKET / (double) duration4 / (1000 * 1000));

    // 5. sequential set benchmark
    startTick = System.currentTimeMillis();
    for (int i = 0; i < mNumBuckets; i++) {
      for (int j = 0; j < TAGS_PER_BUCKET; j++) {
        mTable.set(i, j);
      }
    }
    long duration5 = System.currentTimeMillis() - startTick;
    System.out.printf("sequential set: %d ms, %f Mops/sec\n", duration5,
        1000L * mNumBuckets * TAGS_PER_BUCKET / (double) duration5 / (1000 * 1000));

    // 6. sequential clear benchmark
    startTick = System.currentTimeMillis();
    for (int i = 0; i < mNumBuckets; i++) {
      for (int j = 0; j < TAGS_PER_BUCKET; j++) {
        mTable.clear(i, j);
      }
    }
    long duration6 = System.currentTimeMillis() - startTick;
    System.out.printf("sequential clear: %d ms, %f Mops/sec\n", duration6,
        1000L * mNumBuckets * TAGS_PER_BUCKET / (double) duration6 / (1000 * 1000));
  }
}
