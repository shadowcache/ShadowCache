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

package alluxio.client.file.cache.benchmark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.beust.jcommander.JCommander;
import org.junit.Test;

public class BenchmarkParametersTest {
  @Test
  public void testBasic() {
    String[] argv = {"--benchmark", "test", "--memory", "1mb", "--window_size", "12345",
        "--clock_bits", "8", "--opportunistic_aging", "false"};
    BenchmarkParameters benchmarkParameters = new BenchmarkParameters();
    JCommander jc = JCommander.newBuilder().addObject(benchmarkParameters).build();
    jc.parse(argv);
    assertEquals("test", benchmarkParameters.mBenchmarkType);
    assertEquals("1mb", benchmarkParameters.mMemoryBudget);
    assertEquals(12345, benchmarkParameters.mWindowSize);
    assertEquals(8, benchmarkParameters.mClockBits);
    assertFalse(benchmarkParameters.mOpportunisticAging);
  }
}
