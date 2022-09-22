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

import alluxio.client.file.cache.cuckoofilter.SlidingWindowType;
import alluxio.client.file.cache.cuckoofilter.size.SizeEncodeType;
import alluxio.collections.BitSet;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;

public class ShadowCacheParameters extends Parameters {
  @Parameter(names = "--shadow_cache")
  public String mShadowCacheType = "CCF";

  @Parameter(names = "--memory")
  public String mMemoryBudget = "1MB";

  @Parameter(names = "--window_type", converter = SlidingWindowTypeConverter.class)
  public SlidingWindowType mSlidingWindowType = SlidingWindowType.COUNT_BASED;

  @Parameter(names = "--window_size")
  public long mWindowSize = 65536;

  @Parameter(names = "--clock_bits")
  public int mClockBits = 4;

  @Parameter(names = "--verbose")
  public boolean mVerbose = false;

  // clock cuckoo specified parameters
  @Parameter(names = "--size_bits")
  public int mSizeBits = 16;

  @Parameter(names = "--scope_bits")
  public int mScopeBits = 8;

  @Parameter(names = "--opportunistic_aging", arity = 1)
  public boolean mOpportunisticAging = true;

  @Parameter(names = "--tag_bits")
  public int mTagBits = 8;

  @Parameter(names = "--tags_per_bucket")
  public int mTagsPerBucket = 4;

  @Parameter(names = "--size_encode", converter = SizeEncodeTypeConverter.class)
  public SizeEncodeType mSizeEncodeType = SizeEncodeType.NONE;

  @Parameter(names = "--num_size_bucket_bits")
  public int mNumSizeBucketBits = 8;

  @Parameter(names = "--size_bucket_bits")
  public int mSizeBucketBits = 12;

  @Parameter(names = "--size_bucket_truncate_bits")
  public int mSizeBucketTruncateBits = 0;

  @Parameter(names = "--bitset_type", converter = BitSetTypeConverter.class)
  public BitSet.BitSetType mBitSetType = BitSet.BitSetType.DEFAULT;

  // for log size encoder
  @Parameter(names = "--size_bucket_first")
  public int mSizeBucketFirst = 512;

  @Parameter(names = "--size_bucket_base")
  public int mSizeBucketBase = 2;

  @Parameter(names = "--size_bucket_bias")
  public int mSizeBucketBias = 1;

  // multiple bloom filter specified parameters
  @Parameter(names = "--num_blooms")
  public int mNumBloom = 4;

  // clock sketch
  @Parameter(names = "--num_hash")
  public int mNumHashFunctions = 3;

  // String key + page index
  @Parameter(names = "--page_bits")
  public int mPageBits = 8 * 16 + 64;


  // String key + page index
  @Parameter(names = "--freq_bits")
  public int mFreqBits = 4;

  @Parameter(names = "--key_bits")
  public int mKeyBits = 64;

  public int mAgeLevels = 0;

  static class SlidingWindowTypeConverter implements IStringConverter<SlidingWindowType> {
    @Override
    public SlidingWindowType convert(String s) {
      return SlidingWindowType.valueOf(s.toUpperCase());
    }
  }

  static class SizeEncodeTypeConverter implements IStringConverter<SizeEncodeType> {
    @Override
    public SizeEncodeType convert(String s) {
      return SizeEncodeType.valueOf(s.toUpperCase());
    }
  }

  static class BitSetTypeConverter implements IStringConverter<BitSet.BitSetType> {
    @Override
    public BitSet.BitSetType convert(String s) {
      return BitSet.BitSetType.valueOf(s.toUpperCase());
    }
  }
}
