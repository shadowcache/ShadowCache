package alluxio.client.file.cache.benchmark;

import alluxio.client.file.cache.PageId;
import alluxio.client.file.cache.ShadowCache;
import alluxio.client.file.cache.dataset.Dataset;
import alluxio.client.file.cache.dataset.DatasetEntry;
import alluxio.client.file.cache.dataset.GeneralDataset;
import alluxio.client.file.cache.dataset.generator.EntryGenerator;
import alluxio.util.FormatUtils;
import alluxio.util.LRU;

import java.util.HashMap;

public class CacheAdaptionBenchmark implements Benchmark {
    private LRU itemLRU;
    private final BenchmarkContext mBenchmarkContext;
    private final BenchmarkParameters mBenchmarkParameters;
    private long maxMemoryBytes;
    private long memoryUsed;

    private final ShadowCache mShadowCache;
    private Dataset<String> mDataset;
    private HashMap<PageId, Integer> pageId2Size;


    CacheAdaptionBenchmark(BenchmarkContext benchmarkContext,
                           BenchmarkParameters benchmarkParameters) {
        mBenchmarkContext = benchmarkContext;
        mBenchmarkParameters = benchmarkParameters;
        mShadowCache = ShadowCache.create(benchmarkParameters);
        createDataset();
        mShadowCache.stopUpdate();
        itemLRU = new LRU();
        maxMemoryBytes = FormatUtils.parseSpaceSize(benchmarkParameters.mCacheMemory);
        pageId2Size = new HashMap<>();
        memoryUsed = 0;

    }

    private void createDataset() {
        EntryGenerator<String> generator = BenchmarkUtils.createGenerator(mBenchmarkParameters);
        mDataset = new GeneralDataset<>(generator, (int) mBenchmarkParameters.mWindowSize);
    }

    @Override
    public boolean prepare() {
        return false;
    }

    @Override
    public void run() {
        long opsCount = 0;
        long pageHitNum = 0;
        long totalByte = 0;
        long byteHitNum = 0;
        long maxEstByte = 0;
        long agingPeriod = mBenchmarkParameters.mWindowSize / mBenchmarkParameters.mAgeLevels;
        // first run with bigger cache
        System.out.printf("agingPeriod:%d\n", agingPeriod);
        System.out.println(mShadowCache.getSummary());

        mBenchmarkContext.mStream.println(
                "#operation\thitRatio(Page)\thitRatio(Bytes)");
        while (mDataset.hasNext() && opsCount < mBenchmarkParameters.mMaxEntries) {
            opsCount++;
            DatasetEntry<String> entry = mDataset.next();
            PageId item = new PageId(entry.getScopeInfo().toString(), entry.getItem().hashCode());
            totalByte += entry.getSize();
            if (!itemLRU.get(item)) {
                while (memoryUsed + entry.getSize() > maxMemoryBytes) {
                    PageId pollItem = itemLRU.poll();
                    long pollSize = pageId2Size.get(pollItem);
                    pageId2Size.remove(pollItem);
                    memoryUsed -= pollSize;
                }
                itemLRU.put(item);
                pageId2Size.put(item, entry.getSize());
                memoryUsed += entry.getSize();
            } else {
                pageHitNum++;
                byteHitNum += entry.getSize();
            }
            // insert into memory


            int nread = mShadowCache.get(item, entry.getSize(), entry.getScopeInfo());
            if (nread <= 0) {
                mShadowCache.put(item, entry.getSize(), entry.getScopeInfo());
            }
            mShadowCache.updateTimestamp(1);

            // Aging
            if (opsCount % agingPeriod == 0) {
                long startAgingTick = System.currentTimeMillis();
                mShadowCache.aging();
            }

            if (opsCount % mBenchmarkParameters.mReportInterval == 0) {
                mShadowCache.updateWorkingSetSize();
                long estByte = mShadowCache.getShadowCacheBytes();
                if (estByte > maxEstByte) {
                    maxEstByte = estByte;
                }

                mBenchmarkContext.mStream.printf("%d\t%f\t%f\n", opsCount, pageHitNum / (double) opsCount, byteHitNum / (double) totalByte);
                // accumulate error
            }

        }
        // get a ccf's cache , use it to run
        System.out.printf("[+] change cahce memory to:%d\n", maxEstByte);
        maxMemoryBytes = maxEstByte;
        memoryUsed = 0;
        itemLRU = new LRU();
        opsCount = 0;
        pageHitNum = 0;
        totalByte = 0;
        byteHitNum = 0;
        createDataset();


        while (mDataset.hasNext() && opsCount < mBenchmarkParameters.mMaxEntries) {
            opsCount++;
            DatasetEntry<String> entry = mDataset.next();
            PageId item = new PageId(entry.getScopeInfo().toString(), entry.getItem().hashCode());
            totalByte += entry.getSize();
            if (!itemLRU.get(item)) {
                while (memoryUsed + entry.getSize() > maxMemoryBytes) {
                    PageId pollItem = itemLRU.poll();
                    long pollSize = pageId2Size.get(pollItem);
                    pageId2Size.remove(pollItem);
                    memoryUsed -= pollSize;
                }
                itemLRU.put(item);
                pageId2Size.put(item, entry.getSize());
                memoryUsed += entry.getSize();
            } else {
                pageHitNum++;
                byteHitNum += entry.getSize();
            }

            if (opsCount % mBenchmarkParameters.mReportInterval == 0) {
                mShadowCache.updateWorkingSetSize();
                long estByte = mShadowCache.getShadowCacheBytes();
                if (estByte > maxEstByte) {
                    maxEstByte = estByte;
                }

                mBenchmarkContext.mStream.printf("%d\t%f\t%f\n", opsCount, pageHitNum / (double) opsCount, byteHitNum / (double) totalByte);
                // accumulate error
            }
        }
    }


    @Override
    public boolean finish() {
        return false;
    }
}
