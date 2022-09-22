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

package alluxio.client.file.cache.benchmark.table;

import alluxio.Constants;

/**
 * Access 125MB bitarray or long array
 */
public class TableBench {
  static final long MAX_BYTES = 125 * Constants.MB;
  static final int BYTE_ARRAY_SIZE = (int) (MAX_BYTES);
  static final int LONG_ARRAY_SIZE = (int) (MAX_BYTES / 8);

  public static void main(String[] args) {
    ByteTable byteTable = new ByteTable((int) MAX_BYTES * 8);
    long t1 = System.currentTimeMillis();
    for (int i = 0; i < MAX_BYTES * 8; i++) {
      byteTable.get(i);
    }
    long t2 = System.currentTimeMillis();
    System.out.println("ByteTable cost " + (t2 - t1) + " ms");

    LongTable longTable = new LongTable((int) MAX_BYTES * 8);
    long t3 = System.currentTimeMillis();
    for (int i = 0; i < MAX_BYTES * 8; i++) {
      longTable.get(i);
    }
    long t4 = System.currentTimeMillis();
    System.out.println("LongTable cost " + (t2 - t1) + " ms");
  }

  interface BitTable {
    boolean get(int index);
  }

  static class ByteTable implements BitTable {
    private byte[] data;

    public ByteTable(int size) {
      int nbyte = (size + 7) / 8;
      data = new byte[nbyte];
    }

    @Override
    public boolean get(int index) {
      int i = index >> 3; // div 8
      int ii = index & 0x7;
      long bitmask = 1L << ii;
      return (data[i] & bitmask) != 0;
    }
  }

  static class LongTable implements BitTable {
    private long[] data;

    public LongTable(int size) {
      int nlong = (size + 63) / 64;
      data = new long[nlong];
    }

    @Override
    public boolean get(int index) {
      int i = index >> 6; // div 8
      int ii = index & 0x63;
      long bitmask = 1L << ii;
      return (data[i] & bitmask) != 0;
    }
  }
}
