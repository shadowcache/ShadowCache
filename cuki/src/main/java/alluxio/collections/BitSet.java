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

/**
 * An interface of BitSet supports get/set/clear.
 */
public interface BitSet {

  enum BitSetType {
    BUILTIN,
    DEFAULT
  }

  static BitSet createBitSet(BitSetType type, int nbits) {
    switch (type) {
      case BUILTIN:
        return new BuiltinBitSet(nbits);
      case DEFAULT:
      default:
        return new SimpleBitSet(nbits);
    }
  }

  /**
   * @param index the index of the bit to get
   * @return the bit value of the specified index
   */
  boolean get(int index);

  /**
   * @param startIndex the start index of the bit to get
   * @param length the length of bits to get
   * @return the bit value of the specified index
   */
  long get(int startIndex, int length);

  /**
   * Sets the bit at the specified index to {@code true}.
   *
   * @param index the index of the bit to be set
   */
  void set(int index);

  /**
   * @param startIndex the start index of the bit to get
   * @param length the length of bits to read
   */
  void set(int startIndex, int length);

  /**
   * @param startIndex the start index of the bit to set
   * @param length the length of bits to set
   * @param value the value to set
   */
  void set(int startIndex, int length, long value);

  /**
   * Sets the bit specified by the index to {@code false}.
   *
   * @param index the index of the bit to be cleared
   */
  void clear(int index);

  /**
   * @param startIndex the start index of the bit to clear
   * @param length the length of bits to clear
   */
  void clear(int startIndex, int length);

  /**
   * @return the number of bits currently in this bit set
   */
  int size();
}
