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

import alluxio.client.file.cache.ShadowCacheParameters;

import com.beust.jcommander.Parameter;

public class BenchmarkParameters extends ShadowCacheParameters {
  @Parameter(names = "--help")
  public boolean mHelp = false;

  @Parameter(names = "--benchmark", required = true)
  public String mBenchmarkType;

  @Parameter(names = "--dataset")
  public String mDataset = "random";

  @Parameter(names = "--trace")
  public String mTrace;

  @Parameter(names = "--max_entries")
  public long mMaxEntries = 65536;

  @Parameter(names = "--run_time")
  public String mRunTime = "7d";

  @Parameter(names = "--time_divisor")
  public long mTimeDivisor = 1;

  @Parameter(names = "--num_unique_entries")
  public long mNumUniqueEntries = 1024;

  @Parameter(names = "--report_file")
  public String mReportFile = "stdout";

  @Parameter(names = "--report_interval")
  public long mReportInterval = 64;

  @Parameter(names = "--num_threads")
  public int mNumThreads = 1;

  @Parameter(names = "--num_scope")
  public int mNumScope = 1;

  @Parameter(names = "--cache_memory")
  public String mCacheMemory = "2GB";
}
