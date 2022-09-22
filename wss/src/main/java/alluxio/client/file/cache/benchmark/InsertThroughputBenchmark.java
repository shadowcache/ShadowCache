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

import alluxio.client.file.cache.PageId;
import alluxio.client.file.cache.ShadowCache;
import alluxio.client.file.cache.dataset.ClientDataset;
import alluxio.client.file.cache.dataset.Dataset;
import alluxio.client.file.cache.dataset.DatasetEntry;
import alluxio.client.file.cache.dataset.generator.EntryGenerator;

import java.util.LinkedList;
import java.util.List;

public class InsertThroughputBenchmark implements Benchmark {
  private final BenchmarkContext mBenchmarkContext;
  private final BenchmarkParameters mBenchmarkParameters;
  private final int mNumThreads;
  private final ShadowCache mShadowCache;
  private final List<CacheClient> mClients = new LinkedList<>();
  private EntryGenerator<String> mEntryGenerator;
  private long mNumPeriodToRun;

  public InsertThroughputBenchmark(BenchmarkContext benchmarkContext,
      BenchmarkParameters parameters) {
    mBenchmarkContext = benchmarkContext;
    mBenchmarkParameters = parameters;
    mNumThreads = parameters.mNumThreads;
    mEntryGenerator = BenchmarkUtils.createGenerator(parameters);
    mShadowCache = ShadowCache.create(parameters);
    mShadowCache.stopUpdate();
  }

  @Override
  public boolean prepare() {
    // dispatch entries to each client's dataset
    long startTick = System.currentTimeMillis();
    long count = 0;
    LinkedList<ClientDataset<String>> clientDatasets = new LinkedList<>();
    for (int i = 0; i < mNumThreads; i++) {
      clientDatasets.add(new ClientDataset<>());
    }
    int nextThread = 0;
    while (mEntryGenerator.hasNext() && count < mBenchmarkParameters.mMaxEntries) {
      clientDatasets.get(nextThread).insertEntry(mEntryGenerator.next());
      nextThread = (nextThread + 1) % mNumThreads;
      count++;
    }
    long periodSize = mBenchmarkParameters.mWindowSize / mBenchmarkParameters.mAgeLevels;
    mNumPeriodToRun = count / periodSize;
    long clientWindowSize =
        mBenchmarkParameters.mWindowSize / mNumThreads / mBenchmarkParameters.mAgeLevels;
    for (int i = 0; i < mNumThreads; i++) {
      mClients.add(new CacheClient(i, mShadowCache, clientDatasets.get(i), clientWindowSize));
    }
    long duration = (System.currentTimeMillis() - startTick);
    System.out.printf("Prepare %d entries cost %d ms\n", count, duration);
    System.out.printf("numPeriodToRun %d, clientPeriodSize %d\n", mNumPeriodToRun,
        clientWindowSize);
    return true;
  }

  @Override
  public void run() {
    System.out.println();
    System.out.println("ConcurrencyBenchmark");
    System.out.println(mShadowCache.getSummary());
    System.out.printf("num_threads=%d\n", mNumThreads);
    long startTick = System.currentTimeMillis();
    for (long w = 0; w < mNumPeriodToRun; w++) {
      List<Thread> threads = new LinkedList<>();
      for (int i = 0; i < mNumThreads; i++) {
        threads.add(new Thread(mClients.get(i)));
        threads.get(i).start();
      }
      for (int i = 0; i < mNumThreads; i++) {
        try {
          threads.get(i).join();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      mShadowCache.aging();
    }
    long opsdone = 0;
    long runtime = 0;
    for (int i = 0; i < mNumThreads; i++) {
      opsdone += mClients.get(i).mOpsDone;
      runtime += mClients.get(i).mRuntime;
    }
    long runtimePerClient = runtime / mNumThreads;
    long duration = (System.currentTimeMillis() - startTick);
    System.out.printf("Insert %d entries, cost %d ms, Throughput %d ops/sec\n", opsdone, duration,
        opsdone * 1000 / runtimePerClient);
  }

  private static class CacheClient implements Runnable {
    private final int mThreadId;
    private final ShadowCache mShadowCache;
    private final Dataset<String> mClientDataset;
    private final long mWindowSize;
    private long mOpsDone = 0;
    private long mRuntime = 0;

    public CacheClient(int threadId, ShadowCache shadowCache, Dataset<String> clientDataset,
        long windowSize) {
      mThreadId = threadId;
      mShadowCache = shadowCache;
      mClientDataset = clientDataset;
      mWindowSize = windowSize;
    }

    @Override
    public void run() {
      runOneWindow();
    }

    private void runOneWindow() {
      long startTick = System.currentTimeMillis();
      int count = 0;
      while (mClientDataset.hasNext() && count < mWindowSize) {
        count++;
        DatasetEntry<String> entry = mClientDataset.next();
        PageId item = new PageId(entry.getScopeInfo().toString(), entry.getItem().hashCode());
        mShadowCache.put(item, entry.getSize(), entry.getScopeInfo());
        mShadowCache.updateTimestamp(1);
      }
      mOpsDone += count;
      mRuntime += (System.currentTimeMillis() - startTick);
    }
  }
}
