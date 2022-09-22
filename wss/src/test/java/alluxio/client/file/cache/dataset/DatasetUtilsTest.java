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

import org.junit.Test;

import static org.junit.Assert.*;


public class DatasetUtilsTest {
  @Test
  public void testWindowsFileTimeToUnixSeconds() {
    long winFileTime = 128166391024154329L;
    long unixSeconds = 1172165502L;
    assertEquals(unixSeconds, DatasetUtils.WindowsFileTimeToUnixSeconds(winFileTime));
  }
}
