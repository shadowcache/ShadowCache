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

package alluxio.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BuiltinBitSetTest {
  private static final long INT_MASK = 0xffffffffL;
  private static final long WORD_MASK = 0xffffffffffffffffL;

  @Test
  public void basicTest() {
    BitSet bitSet = new BuiltinBitSet(100);
    bitSet.set(9);
    assertTrue(bitSet.get(9));
    assertFalse(bitSet.get(8));
  }

  @Test
  public void rangeTest() {
    BitSet bitSet = new SimpleBitSet(128);
    // test set
    bitSet.set(0, bitSet.size());
    assertEquals(WORD_MASK, bitSet.get(0, 64));
    assertEquals(WORD_MASK, bitSet.get(64, 64));

    // test clear
    bitSet.clear(0, bitSet.size());
    assertEquals(0L, bitSet.get(0, 64));
    assertEquals(0L, bitSet.get(64, 64));

    // test one word
    bitSet.set(0, 32, INT_MASK);
    assertEquals(INT_MASK, bitSet.get(0, 32));

    bitSet.clear(0, bitSet.size());
    bitSet.set(0, 64, WORD_MASK);
    assertEquals(WORD_MASK, bitSet.get(0, 64));

    // test two words
    bitSet.clear(0, bitSet.size());
    bitSet.set(32, 64, WORD_MASK);
    assertEquals(WORD_MASK, bitSet.get(32, 64));

    for (int i=0; i <= 64; i++) {
      bitSet.clear(0, bitSet.size());
      bitSet.set(i, 64, WORD_MASK);
      assertEquals(WORD_MASK, bitSet.get(i, 64));
    }
  }
}
