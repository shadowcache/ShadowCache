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

package alluxio.util;

import static org.junit.Assert.*;

import alluxio.collections.BitSet;
import alluxio.collections.SimpleBitSet;

import org.junit.Test;

public class BitsUtilsTest {
    @Test
    public void testHasZero4() {
        BitSet bitSet = new SimpleBitSet(128);
        assertTrue(BitsUtils.hasZero4(bitSet.get(0, 16)));
        bitSet.set(0, 16, 0x1234);
        assertFalse(BitsUtils.hasZero4(bitSet.get(0, 16)));
    }

    @Test
    public void testHasZero8() {
        BitSet bitSet = new SimpleBitSet(128);
        assertTrue(BitsUtils.hasZero8(bitSet.get(0, 32)));
        bitSet.set(0, 32, 0x12345678);
        assertFalse(BitsUtils.hasZero8(bitSet.get(0, 32)));
    }

    @Test
    public void testHasZero12() {
        BitSet bitSet = new SimpleBitSet(128);
        assertTrue(BitsUtils.hasZero12(bitSet.get(0, 48)));
        bitSet.set(0, 48);
        assertFalse(BitsUtils.hasZero12(bitSet.get(0, 48)));
    }

    @Test
    public void testHasZero16() {
        BitSet bitSet = new SimpleBitSet(128);
        assertTrue(BitsUtils.hasZero16(bitSet.get(0, 64)));
        bitSet.set(0, 64);
        assertFalse(BitsUtils.hasZero16(bitSet.get(0, 64)));
    }

    @Test
    public void testHasValue4() {
        BitSet bitSet = new SimpleBitSet(128);
        assertTrue(BitsUtils.hasValue4(bitSet.get(0, 16), 0));
        assertFalse(BitsUtils.hasValue4(bitSet.get(0, 16), 0x1));
        bitSet.set(0, 16, 0x1234);
        assertTrue(BitsUtils.hasValue4(bitSet.get(0, 16), 0x1));
        assertTrue(BitsUtils.hasValue4(bitSet.get(0, 16), 0x2));
        assertTrue(BitsUtils.hasValue4(bitSet.get(0, 16), 0x3));
        assertTrue(BitsUtils.hasValue4(bitSet.get(0, 16), 0x4));
    }

    @Test
    public void testHasValue8() {
        BitSet bitSet = new SimpleBitSet(128);
        assertTrue(BitsUtils.hasValue8(bitSet.get(0, 32), 0));
        assertFalse(BitsUtils.hasValue8(bitSet.get(0, 32), 0x12));
        bitSet.set(0, 32, 0x12345678);
        assertTrue(BitsUtils.hasValue8(bitSet.get(0, 32), 0x12));
        assertTrue(BitsUtils.hasValue8(bitSet.get(0, 32), 0x34));
        assertTrue(BitsUtils.hasValue8(bitSet.get(0, 32), 0x56));
        assertTrue(BitsUtils.hasValue8(bitSet.get(0, 32), 0x78));
    }

    @Test
    public void testHasValue12() {
        BitSet bitSet = new SimpleBitSet(128);
        assertTrue(BitsUtils.hasValue12(bitSet.get(0, 48), 0));
        assertFalse(BitsUtils.hasValue12(bitSet.get(0, 48), 0x123));
        bitSet.set(0, 48, 0x123456789abcL);
        assertTrue(BitsUtils.hasValue12(bitSet.get(0, 48), 0x123));
        assertTrue(BitsUtils.hasValue12(bitSet.get(0, 48), 0x456));
        assertTrue(BitsUtils.hasValue12(bitSet.get(0, 48), 0x789));
        assertTrue(BitsUtils.hasValue12(bitSet.get(0, 48), 0xabc));
    }

    @Test
    public void testHasValue16() {
        BitSet bitSet = new SimpleBitSet(128);
        assertTrue(BitsUtils.hasValue16(bitSet.get(0, 64), 0));
        assertFalse(BitsUtils.hasValue16(bitSet.get(0, 64), 0x1234));
        bitSet.set(0, 64, 0x123456789abcdef0L);
        assertTrue(BitsUtils.hasValue16(bitSet.get(0, 64), 0x1234));
        assertTrue(BitsUtils.hasValue16(bitSet.get(0, 64), 0x5678));
        assertTrue(BitsUtils.hasValue16(bitSet.get(0, 64), 0x9abc));
        assertTrue(BitsUtils.hasValue16(bitSet.get(0, 64), 0xdef0));
    }
}