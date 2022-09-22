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

import alluxio.client.file.cache.dataset.DatasetEntry;
import alluxio.client.file.cache.dataset.GeneralDataset;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TwitterEntryGeneratorTest {
  private static final String TWITTER_TRACE_SAMPLE_RELATIVE_PATH = "data/cluster37.0_100.csv";

  @Test
  public void testBasic() {
    String path = getClass().getResource("/").getPath() + TWITTER_TRACE_SAMPLE_RELATIVE_PATH;
    EntryGenerator<String> generator = new TwitterEntryGenerator(path);
    GeneralDataset<String> dataset = new GeneralDataset<>(generator, 100);
    int count = 0;
    while (dataset.hasNext()) {
      DatasetEntry<String> e = dataset.next();
      count++;
    }
    assertEquals(100, count);
    assertEquals(74, dataset.getRealEntryNumber());
  }
}
