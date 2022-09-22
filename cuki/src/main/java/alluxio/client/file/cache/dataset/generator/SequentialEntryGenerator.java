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

public class SequentialEntryGenerator implements EntryGenerator<String> {
  private final SequentialIntegerEntryGenerator mGenerator;

  public SequentialEntryGenerator(long numEntries) {
    mGenerator = new SequentialIntegerEntryGenerator(numEntries);
  }

  public SequentialEntryGenerator(long numEntries, int lowerBound, int upperBound) {
    mGenerator = new SequentialIntegerEntryGenerator(numEntries, lowerBound, upperBound);
  }

  public SequentialEntryGenerator(long numEntries, int lowerBound, int upperBound,
      int lowerBoundSize, int upperBoundSize, int numScopes) {
    mGenerator = new SequentialIntegerEntryGenerator(numEntries, lowerBound, upperBound,
        lowerBoundSize, upperBoundSize, numScopes);
  }

  @Override
  public DatasetEntry<String> next() {
    DatasetEntry<Integer> entry = mGenerator.next();
    return new DatasetEntry<>("" + entry.getItem(), entry.getSize(), entry.getScopeInfo());
  }

  @Override
  public boolean hasNext() {
    return mGenerator.hasNext();
  }
}
