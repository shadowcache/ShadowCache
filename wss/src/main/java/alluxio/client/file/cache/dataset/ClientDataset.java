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

package alluxio.client.file.cache.dataset;

import alluxio.client.quota.CacheScope;

import java.util.LinkedList;
import java.util.Queue;

public class ClientDataset<T> implements Dataset<T> {
  private final Queue<DatasetEntry<T>> mEntries = new LinkedList<>();

  public void insertEntry(DatasetEntry<T> entry) {
    mEntries.offer(entry);
  }

  @Override
  public DatasetEntry<T> next() {
    return mEntries.poll();
  }

  @Override
  public boolean hasNext() {
    return !mEntries.isEmpty();
  }

  @Override
  public int getRealEntryNumber() {
    return 0;
  }

  @Override
  public int getRealEntryNumber(CacheScope scope) {
    return 0;
  }

  @Override
  public int getRealEntrySize() {
    return 0;
  }

  @Override
  public int getRealEntrySize(CacheScope scope) {
    return 0;
  }
}
