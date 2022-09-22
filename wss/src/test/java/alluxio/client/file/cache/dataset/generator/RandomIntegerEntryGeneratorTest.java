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

import alluxio.client.file.cache.dataset.Dataset;
import alluxio.client.file.cache.dataset.DatasetEntry;
import alluxio.client.file.cache.dataset.GeneralDataset;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RandomIntegerEntryGeneratorTest {

  @Test
  public void testBasic() {
    RandomIntegerEntryGenerator generator = new RandomIntegerEntryGenerator(1000);
    Dataset<Integer> dataset = new GeneralDataset<>(generator, 64);
    int count = 0;
    while (dataset.hasNext()) {
      count++;
      DatasetEntry<Integer> entry = dataset.next();
      assertTrue(dataset.getRealEntryNumber() <= 64);
    }
    assertEquals(1000, count);
  }

}
