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

import static org.testng.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class SequentialIntegerEntryGeneratorTest {
  private static final long NUM_ENTRIES = 1024;
  private static final int LOWER_BOUND = 12;
  private static final int UPPER_BOUND = (int) (LOWER_BOUND + NUM_ENTRIES);

  private static SequentialIntegerEntryGenerator mGenerator;

  @Before
  public void init() {
    mGenerator = new SequentialIntegerEntryGenerator(NUM_ENTRIES, LOWER_BOUND, UPPER_BOUND);
  }

  @Test
  public void testGenerateSequentialItems() {
    for (int i = 0; i < NUM_ENTRIES * 4; i++) {
      assertEquals(LOWER_BOUND + (i % (UPPER_BOUND - LOWER_BOUND)),
          mGenerator.next().getItem().intValue());
    }
  }
}
