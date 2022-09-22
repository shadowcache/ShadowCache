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

public interface Dataset<T> {
  public DatasetEntry<T> next();

  public boolean hasNext();

  public int getRealEntryNumber();

  public int getRealEntryNumber(CacheScope scope);

  public int getRealEntrySize();

  public int getRealEntrySize(CacheScope scope);
}