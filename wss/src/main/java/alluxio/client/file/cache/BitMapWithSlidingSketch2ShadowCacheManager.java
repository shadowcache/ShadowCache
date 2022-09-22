package alluxio.client.file.cache;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import alluxio.Constants;
import alluxio.client.file.cache.cuckoofilter.SlidingWindowType;
import alluxio.client.quota.CacheScope;
import alluxio.util.FormatUtils;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BitMapWithSlidingSketch2ShadowCacheManager implements ShadowCache{
  // 10 hash fuctions
  // 2 fileds: new old
  // bloom filter
  private final AtomicLong mTotalOnes = new AtomicLong(0);
  private final AtomicLong mTotalSize = new AtomicLong(0);
  private final AtomicLong mShadowCachePageHit = new AtomicLong(0);
  private final AtomicLong mShadowCacheByteHit = new AtomicLong(0);
  private final AtomicLong mShadowCachePageRead = new AtomicLong(0);
  private final AtomicLong mShadowCacheByteRead = new AtomicLong(0);
  private final ScheduledExecutorService mScheduler = Executors.newScheduledThreadPool(0);
  private final Lock lock = new ReentrantLock();
  private int mSizeBits;
  private int currIdx = 0;
  private int mNumBuckets;
  private long mMemoryInBits;
  private long mWindowSize;
  private long agingPeriod;
  private int numClearPerTime;
  private Funnel<PageId> mFunnel;
  private HashFunction mHashFunction;
  private int[] newTable;
  private int[] oldTable;


  public BitMapWithSlidingSketch2ShadowCacheManager(ShadowCacheParameters parameters){
    mWindowSize = parameters.mWindowSize;
    mSizeBits = parameters.mSizeBits;
    mFunnel = PageIdFunnel.FUNNEL;
    mMemoryInBits = FormatUtils.parseSpaceSize(parameters.mMemoryBudget) * 8;
    mNumBuckets = (int) (mMemoryInBits / (2*mSizeBits));
    agingPeriod = 1;
    numClearPerTime =  mNumBuckets / (int)mWindowSize;
    mHashFunction = Hashing.murmur3_32(32713);
    newTable = new int[mNumBuckets];
    oldTable = new int[mNumBuckets];
    if(parameters.mSlidingWindowType == SlidingWindowType.TIME_BASED){
      mScheduler.scheduleAtFixedRate(this::aging, agingPeriod, agingPeriod, MILLISECONDS);
    }
  }

  @Override
  public boolean put(PageId pageId, int size, CacheScope scope) {
    lock.lock();
    int pos =  bucketIndex(pageId,mHashFunction);
    if(newTable[pos]==0 && oldTable[pos]==0){
      mTotalOnes.incrementAndGet();

    }
    mTotalSize.addAndGet(-newTable[pos]);
    mTotalSize.addAndGet(size);
    newTable[pos] = size;

    lock.unlock();
    return true;
  }

  @Override
  public int get(PageId pageId, int bytesToRead, CacheScope scope) {
    mShadowCachePageRead.incrementAndGet();
    mShadowCacheByteRead.addAndGet(bytesToRead);
    lock.lock();

    int pos = bucketIndex(pageId,mHashFunction);
    if(newTable[pos]==0 && oldTable[pos]==0){
      lock.unlock();
      return 0;
    }

    mShadowCacheByteHit.addAndGet(bytesToRead);
    mShadowCachePageHit.incrementAndGet();
    lock.unlock();
    return bytesToRead;
  }

  @Override
  public boolean delete(PageId pageId) {
    return false;
  }

  @Override
  public void aging() {
    lock.lock();
    int endPos = (currIdx+numClearPerTime)%mNumBuckets;
    if(endPos<currIdx){
      agingRange(currIdx,mNumBuckets);
      agingRange(0,endPos);
    }else{
      agingRange(currIdx,endPos);
    }
    currIdx = endPos;
    lock.unlock();
  }

  private void agingRange(int start,int end){
    for(int i=start;i<end;i++){
      if(oldTable[i]!=0 && newTable[i]==0){
        mTotalOnes.decrementAndGet();
      }
      mTotalSize.addAndGet(-oldTable[i]);
      oldTable[i] = newTable[i];
      newTable[i] = 0;
    }
  }

  @Override
  public void updateWorkingSetSize() {

  }

  @Override
  public void stopUpdate() {
    mScheduler.shutdown();
  }

  @Override
  public void updateTimestamp(long increment) {

  }

  @Override
  public long getShadowCachePages() {
    long zeros = mNumBuckets - mTotalOnes.get();
    double pages = 0.;
    if (zeros == 0) {
      pages = mNumBuckets * Math.log(mNumBuckets);
    } else {
      pages = -mNumBuckets * Math.log(zeros / (double) mNumBuckets);
    }
    return (long) (pages); // 源码里并没有/hashNum，原作者也没考虑
  }

  @Override
  public long getShadowCachePages(CacheScope scope) {
    return 0;
  }

  @Override
  public long getShadowCacheBytes() {
    double pages = getShadowCachePages();
    double avePageSize = mTotalSize.get() / (double) mTotalOnes.get();
    // System.out.printf("[+] pages:%d bucket1Num:%d totalsize:%d\n",(long)pages,mBucketsSet.get(),mTotalSize.get());
    return (long) (pages * avePageSize);
  }

  @Override
  public long getShadowCacheBytes(CacheScope scope) {
    return 0;
  }

  @Override
  public long getShadowCachePageRead() {
    return mShadowCachePageRead.get();
  }

  @Override
  public long getShadowCachePageHit() {
    return mShadowCachePageHit.get();
  }

  @Override
  public long getShadowCacheByteRead() {
    return mShadowCacheByteRead.get();
  }

  @Override
  public long getShadowCacheByteHit() {
    return mShadowCacheByteHit.get();
  }

  @Override
  public double getFalsePositiveRatio() {
    return 0;
  }

  @Override
  public long getSpaceBits() {
    return mNumBuckets * mSizeBits * 2;
  }

  @Override
  public String getSummary() {
    return "bitmapWithClockSketch\bnumBuckets: " + mNumBuckets
        + "\nbitsPerSize: " + mSizeBits + "\nSizeInMB: "
        + (getSpaceBits() / 8.0 / Constants.MB);
  }

  private int bucketIndex(PageId pageId, HashFunction hashFunc) {
    return (int)Math.abs(hashFunc.newHasher().putObject(pageId, mFunnel).hash().asInt() % mNumBuckets);
  }
}
