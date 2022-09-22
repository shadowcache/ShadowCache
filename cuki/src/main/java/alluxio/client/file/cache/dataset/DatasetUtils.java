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

public class DatasetUtils {
  public static final long WINDOWS_TICK = 10000000L;
  public static final long SEC_TO_UNIX_EPOCH = 11644473600L;

  public static long WindowsFileTimeToUnixSeconds(long winFileTime) {
    return (winFileTime / WINDOWS_TICK - SEC_TO_UNIX_EPOCH);
  }
}
