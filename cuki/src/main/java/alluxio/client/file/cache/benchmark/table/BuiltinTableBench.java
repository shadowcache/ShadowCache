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

package alluxio.client.file.cache.benchmark.table;

import alluxio.Constants;
import alluxio.client.file.cache.cuckoofilter.CuckooTable;
import alluxio.client.file.cache.cuckoofilter.SimpleCuckooTable;
import alluxio.collections.BuiltinBitSet;

public class BuiltinTableBench {
  private static final int MAX_BIT_SIZE = 125 * Constants.MB;
  private static final int BITS_PER_TAG = 8;
  private static final int TAGS_PER_BUCKET = 4;
  private static final int NUM_BUCKETS = MAX_BIT_SIZE / BITS_PER_TAG / TAGS_PER_BUCKET;

  public static void main(String[] args) {
    BuiltinBitSet bits = new BuiltinBitSet(MAX_BIT_SIZE);
    BuiltinBitSet bits2 = new BuiltinBitSet(MAX_BIT_SIZE);
    CuckooTable builtinTable =
        new SimpleCuckooTable(bits, NUM_BUCKETS, TAGS_PER_BUCKET, BITS_PER_TAG);
    CuckooTable builtinTable2 =
        new SimpleCuckooTable(bits2, NUM_BUCKETS, TAGS_PER_BUCKET, BITS_PER_TAG);
    long st = System.currentTimeMillis();
    for (int i = 0; i < NUM_BUCKETS; i++) {
      for (int j = 0; j < TAGS_PER_BUCKET; j++) {
        builtinTable.readTag(i, j);
        int tag = builtinTable2.readTag(i, j);
        builtinTable2.writeTag(i, j, tag);
      }
    }
    long en = System.currentTimeMillis();
    long mills = (en - st);
    System.out.printf("Cost %d ms\n", mills);
  }
}
