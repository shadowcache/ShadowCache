package alluxio.client.file.cache.benchmark;

import alluxio.client.file.cache.PageId;
import alluxio.client.file.cache.ShadowCache;
import alluxio.client.file.cache.dataset.ClientDataset;
import alluxio.client.file.cache.dataset.Dataset;
import alluxio.client.file.cache.dataset.DatasetEntry;
import alluxio.client.file.cache.dataset.generator.EntryGenerator;

import java.util.LinkedList;
import java.util.List;

public class SlidingSketchThrouhputBenchmark implements Benchmark{
  private final BenchmarkContext mBenchmarkContext;
  private final BenchmarkParameters mBenchmarkParameters;
  private final int mNumThreads;
  private final ShadowCache mShadowCache;
  private final List<CacheClient> mClients = new LinkedList<>();
  private EntryGenerator<String> mEntryGenerator;

  public SlidingSketchThrouhputBenchmark(BenchmarkContext benchmarkContext, BenchmarkParameters parameters){
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
    long maxRead = count / mNumThreads;
    for (int i = 0; i < mNumThreads; i++) {
      mClients.add(new CacheClient(i, mShadowCache, clientDatasets.get(i),maxRead));
    }
    long duration = (System.currentTimeMillis() - startTick);

    System.out.printf("Prepare %d entries cost %d ms\n", count, duration);
    return true;
  }

  @Override
  public void run() {
    System.out.println();
    System.out.println("ConcurrencyBenchmark");
    System.out.println(mShadowCache.getSummary());
    System.out.printf("num_threads=%d\n", mNumThreads);
    long startTick = System.currentTimeMillis();

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

  @Override
  public boolean finish() {
    return Benchmark.super.finish();
  }

  private static class CacheClient implements Runnable {
    private final int mThreadId;
    private final ShadowCache mShadowCache;
    private final Dataset<String> mClientDataset;
    private final long mMaxRead;
    private long mOpsDone = 0;
    private long mRuntime = 0;

    public CacheClient(int threadId, ShadowCache shadowCache, Dataset<String> clientDataset,
                       long maxRead) {
      mThreadId = threadId;
      mShadowCache = shadowCache;
      mClientDataset = clientDataset;
      mMaxRead = maxRead;
    }

    @Override
    public void run() {
      runOneWindow();
    }

    private void runOneWindow() {
      long startTick = System.currentTimeMillis();
      int count = 0;
      while (mClientDataset.hasNext() && count < mMaxRead) {
        count++;
        DatasetEntry<String> entry = mClientDataset.next();
        PageId item = new PageId(entry.getScopeInfo().toString(), entry.getItem().hashCode());
        int nread = mShadowCache.get(item, entry.getSize(), entry.getScopeInfo());
        if (nread <= 0) {
          mShadowCache.put(item, entry.getSize(), entry.getScopeInfo());
        }
        mShadowCache.aging();
        mShadowCache.updateTimestamp(1);
      }
      mOpsDone += count;
      mRuntime += (System.currentTimeMillis() - startTick);
    }
  }
}
