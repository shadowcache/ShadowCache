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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.beust.jcommander.JCommander;
import org.junit.Test;

public class ShadowCacheParametersTest {
  @Test
  public void testBasic() {
    String[] argv = {"--memory", "1mb", "--window_size", "12345", "--clock_bits", "8",
        "--opportunistic_aging", "false"};
    ShadowCacheParameters shadowCacheParameters = new ShadowCacheParameters();
    JCommander jc = JCommander.newBuilder().addObject(shadowCacheParameters).build();
    jc.parse(argv);
    assertEquals("1mb", shadowCacheParameters.mMemoryBudget);
    assertEquals(12345, shadowCacheParameters.mWindowSize);
    assertEquals(8, shadowCacheParameters.mClockBits);
    assertFalse(shadowCacheParameters.mOpportunisticAging);
  }
}
