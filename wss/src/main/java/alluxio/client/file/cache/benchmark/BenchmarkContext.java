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

import java.io.PrintStream;

public class BenchmarkContext {
  public PrintStream mStream;

  public BenchmarkContext(BenchmarkParameters parameters) {
    // init report file print stream
    if (parameters.mReportFile.equals("stdout")) {
      mStream = System.out;
    } else {
      try {
        mStream = new PrintStream(parameters.mReportFile);
      } catch (Exception e) {
        System.out.printf("Error: illegal report file %s\n", parameters.mReportFile);
        e.printStackTrace();
      }
    }
  }
}
