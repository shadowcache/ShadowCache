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

import com.beust.jcommander.JCommander;

public class BenchmarkMain {
  private static final BenchmarkParameters mParameters = new BenchmarkParameters();

  public static void main(String[] args) throws Exception {
    JCommander jc = JCommander.newBuilder().addObject(mParameters).build();
    jc.parse(args);
    if (mParameters.mHelp) {
      jc.usage();
      System.exit(0);
    }
    Benchmark benchmark = Benchmark.create(mParameters);
    System.out.println(mParameters.toString());
    benchmark.prepare();
    benchmark.run();
    benchmark.finish();
  }
}
