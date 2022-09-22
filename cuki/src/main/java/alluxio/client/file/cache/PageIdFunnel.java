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

package alluxio.client.file.cache;


import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;

/**
 * Funnel for PageId.
 */
public enum PageIdFunnel implements Funnel<PageId> {
  FUNNEL;

  /**
   * @param from source
   * @param into destination
   */
  public void funnel(PageId from, PrimitiveSink into) {
    into.putUnencodedChars(from.getFileId()).putLong(from.getPageIndex());
  }
}
