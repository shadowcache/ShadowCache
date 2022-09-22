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

import alluxio.client.file.cache.dataset.generator.EntryGenerator;
import alluxio.client.quota.CacheScope;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A general dataset which is able to record the WSS of a recent interval.
 */
public class GeneralDataset<T> implements Dataset<T> {
  private final Lock lock;

  private final EntryGenerator<T> generator;
  private final int windowSize;

  private final AtomicLong count;
  private final Queue<DatasetEntry<T>> queue;
  private final HashMap<T, Integer> itemToCount;
  private final HashMap<T, Integer> itemToSize;
  private final HashMap<CacheScope, Integer> scopeToNumber;
  private final HashMap<CacheScope, Integer> scopeToSize;
  private int realNumber;
  private int realSize;

  public GeneralDataset(EntryGenerator<T> generator, int windowSize) {
    this.generator = generator;
    this.windowSize = windowSize;
    this.lock = new ReentrantLock();
    this.count = new AtomicLong(0);
    this.queue = new LinkedList<>();
    this.itemToCount = new HashMap<>();
    this.itemToSize = new HashMap<>();
    this.scopeToNumber = new HashMap<>();
    this.scopeToSize = new HashMap<>();
    this.realNumber = 0;
    this.realSize = 0;
  }

  @Override
  public DatasetEntry<T> next() {
    count.incrementAndGet();
    lock.lock();
    DatasetEntry<T> entry = generator.next();
    assert entry != null;
    queue.offer(entry);
    Integer cnt = itemToCount.get(entry.getItem());
    if (cnt != null && cnt > 0) {
      itemToCount.put(entry.getItem(), cnt + 1);
      // workaround: force the same item should have the same size
      entry.setSize(itemToSize.get(entry.getItem()));
    } else {
      itemToCount.put(entry.getItem(), 1);
      itemToSize.put(entry.getItem(), entry.getSize());
      scopeToSize.put(entry.getScopeInfo(),
          scopeToSize.getOrDefault(entry.getScopeInfo(), 0) + entry.getSize());
      scopeToNumber.put(entry.getScopeInfo(),
          scopeToNumber.getOrDefault(entry.getScopeInfo(), 0) + 1);
      realSize += entry.getSize();
    }
    // shrink window
    if (queue.size() > windowSize) {
      DatasetEntry<T> staleItem = queue.poll();
      assert staleItem != null;
      Integer itemCount = itemToCount.get(staleItem.getItem());
      assert itemCount != null && itemCount >= 1;
      if (itemCount == 1) {
        itemToCount.remove(staleItem.getItem());
        scopeToSize.put(staleItem.getScopeInfo(),
            scopeToSize.getOrDefault(staleItem.getScopeInfo(), 0) - staleItem.getSize());
        scopeToNumber.put(staleItem.getScopeInfo(),
            scopeToNumber.getOrDefault(staleItem.getScopeInfo(), 0) - 1);
        //
        realSize -= staleItem.getSize();
      } else {
        itemToCount.put(staleItem.getItem(), itemCount - 1);
      }
    }
    realNumber = itemToCount.size();
    lock.unlock();
    return entry;
  }

  @Override
  public boolean hasNext() {
    return generator.hasNext();
  }

  @Override
  public int getRealEntryNumber() {
    return realNumber;
  }

  @Override
  public int getRealEntryNumber(CacheScope scope) {
    return scopeToNumber.getOrDefault(scope, 0);
  }

  @Override
  public int getRealEntrySize() {
    return realSize;
  }

  @Override
  public int getRealEntrySize(CacheScope scope) {
    return scopeToSize.getOrDefault(scope, 0);
  }
}
