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

import java.util.Objects;

public class DatasetEntry<T> {
  private T item;
  private int size;
  private CacheScope scopeInfo;
  private long timestamp;

  public DatasetEntry(T item, int size, CacheScope scopeInfo, long timestamp) {
    this.item = item;
    this.size = size;
    this.scopeInfo = scopeInfo;
    this.timestamp = timestamp;
  }

  public DatasetEntry(T item, int size, CacheScope scopeInfo) {
    this.item = item;
    this.size = size;
    this.scopeInfo = scopeInfo;
    this.timestamp = 0;
  }

  public T getItem() {
    return item;
  }

  public void setItem(T item) {
    this.item = item;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public CacheScope getScopeInfo() {
    return scopeInfo;
  }

  public void setScopeInfo(CacheScope scopeInfo) {
    this.scopeInfo = scopeInfo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    DatasetEntry<?> that = (DatasetEntry<?>) o;
    return size == that.size && Objects.equals(item, that.item)
        && Objects.equals(scopeInfo, that.scopeInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(item, size, scopeInfo);
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public String toString() {
    return "DatasetEntry{" + "item=" + item + ", size=" + size + ", scopeInfo=" + scopeInfo
        + ", timestamp=" + timestamp + '}';
  }
}
