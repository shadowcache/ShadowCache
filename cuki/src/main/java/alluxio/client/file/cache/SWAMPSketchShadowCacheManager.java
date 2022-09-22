package alluxio.client.file.cache;

import alluxio.Constants;
import alluxio.client.file.cache.cuckoofilter.SlidingWindowType;
import alluxio.client.quota.CacheScope;
import alluxio.util.FormatUtils;
import alluxio.util.TinyTable.TinyTable;
import alluxio.util.TinyTable.TinyTableWithCounters;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SWAMPSketchShadowCacheManager implements ShadowCache{
  protected long mWindowSize;
  protected int mFingerPrintSize;
  protected int[] cyclicFingerBuffer;
  protected int mBucketCapacity;
  protected int mBitsPerSize;
  protected int mBucketNum;
  protected int mCycleBufferLen;
  protected double mLoadF; //said "recommend this be 0.2"
  protected HashFunction mHashFunction;
  protected Funnel<PageId> mFunnel;
  protected int curIdx;
  protected int ageIdx;
  protected long deleteTime;
  protected long addTime;
  private final ScheduledExecutorService mScheduler = Executors.newScheduledThreadPool(0);
  private final AtomicLong mShadowCachePageRead = new AtomicLong(0);
  private final AtomicLong mShadowCachePageHit = new AtomicLong(0);
  private final AtomicLong mShadowCacheByteRead = new AtomicLong(0);
  private final AtomicLong mShadowCacheByteHit = new AtomicLong(0);
  private final AtomicLong mBucketsSet = new AtomicLong(0);
  private final AtomicLong mTotalSize = new AtomicLong(0);
  private final SlidingWindowType mSlidingWindowType;
  protected boolean debugMode;

  protected Map<Integer,Integer> debugMapPageToCode;
  protected Set<Integer> debugCodeSet;

  // no scope
  protected TinyTable tinyTable;
  protected boolean noBucketMode;
  private final Lock lock = new ReentrantLock();
  public SWAMPSketchShadowCacheManager(ShadowCacheParameters params) {
    mWindowSize = params.mWindowSize;
    mFingerPrintSize = params.mTagBits;
    mBucketCapacity = params.mTagsPerBucket;
    mBitsPerSize = params.mSizeBits;
    mSlidingWindowType = params.mSlidingWindowType;
    noBucketMode = false;
    long memoryInBits = FormatUtils.parseSpaceSize(params.mMemoryBudget) * 8;
    // mCycleBufferLen = (int) memoryInBits/mFingerPrintSize;
    mCycleBufferLen = (int) (memoryInBits/2)/mFingerPrintSize;
    if(mCycleBufferLen>mWindowSize && mSlidingWindowType == SlidingWindowType.COUNT_BASED){
      mCycleBufferLen = (int)mWindowSize;
    }
    mBucketNum = (int)((memoryInBits-(mCycleBufferLen*mFingerPrintSize)) / (mBucketCapacity*(mFingerPrintSize+mBitsPerSize)+136));
    if(mBucketNum<=0){
      mBucketNum=16;
    }
    mLoadF = mBucketCapacity/(mCycleBufferLen/(double)mBucketNum);
    tinyTable = new TinyTable(mFingerPrintSize,mBitsPerSize,mBucketCapacity,mBucketNum);
    cyclicFingerBuffer = new int[(int)mCycleBufferLen];
    mHashFunction = Hashing.murmur3_32((int)System.currentTimeMillis());
    mFunnel = PageIdFunnel.FUNNEL;
    debugMapPageToCode = new HashMap<>();
    debugCodeSet =  new HashSet<>();
    debugMode = false;
    if(mSlidingWindowType == SlidingWindowType.TIME_BASED){
      long agingPeriod = mWindowSize / mCycleBufferLen;
      if(agingPeriod<=0){
        agingPeriod = 1;
      }
      mScheduler.scheduleAtFixedRate(this::timeAging,agingPeriod,agingPeriod, TimeUnit.MILLISECONDS);
    }
  }

  private void info(String s){
    if(debugMode){
      System.out.print(s);
    }
  }



  private void updateCurIdx(){
    if(curIdx==mCycleBufferLen-1){
      curIdx = 0;
    }else{
      curIdx = curIdx + 1;
    }
  }

  private void updateAgeIdx(){
    if(ageIdx==mCycleBufferLen-1){
      ageIdx = 0;
    }else{
      ageIdx = ageIdx + 1;
    }
  }

  @Override
  public boolean put(PageId pageId, int size, CacheScope scope) {
    //info(String.format("try to put page:%s\n",pageId.toString()));
    lock.lock();
    //System.out.println("[+] in put.");
    int hashcode = mHashFunction.newHasher().putObject(pageId,mFunnel).hash().asInt();
    int prev = cyclicFingerBuffer[curIdx];
    if(prev!=0){
      long start = System.currentTimeMillis();
      delete(prev);
      deleteTime+= System.currentTimeMillis()-start;
    }
    long bucketNum = tinyTable.getNum(hashcode);
    //System.out.println("bucketBUm:"+bucketNum);
    if(bucketNum>=63){
      updateCurIdx();
      lock.unlock();
      return false;
    }

    boolean isContain = tinyTable.containItemWithSize(hashcode);
    long start = System.currentTimeMillis();
    if(!tinyTable.addItem(hashcode,size)){
      addTime += System.currentTimeMillis()-start;;
      updateCurIdx();
      lock.unlock();
      return false;
    }
    addTime += System.currentTimeMillis()-start;
    cyclicFingerBuffer[curIdx] = hashcode;
    updateCurIdx();

    if(!isContain){
      mBucketsSet.addAndGet(1);
      mTotalSize.addAndGet(size);
    }
    lock.unlock();
    return true;
  }

  @Override
  public int get(PageId pageId, int bytesToRead, CacheScope scope) {
    mShadowCachePageRead.incrementAndGet();
    mShadowCacheByteRead.addAndGet(bytesToRead);
    int hashcode = mHashFunction.newHasher().putObject(pageId,mFunnel).hash().asInt();
    lock.lock();
    if(!tinyTable.containItemWithSize(hashcode)){
      lock.unlock();
      return 0;
    }
    lock.unlock();
    this.put(pageId,bytesToRead,scope);
    mShadowCachePageHit.incrementAndGet();
    mShadowCacheByteHit.addAndGet(bytesToRead);
    return bytesToRead;
  }


  public boolean delete(int hashcode){
    long prevSize = tinyTable.getItemSize(hashcode);
    if(prevSize!=-1) {
      tinyTable.RemoveItem(hashcode,(int)prevSize);
      if (!tinyTable.containItemWithSize(hashcode)) {
        mTotalSize.addAndGet(-prevSize);
        mBucketsSet.addAndGet(-1);
      }
      cyclicFingerBuffer[curIdx] = 0;
      return true;
    }
    return false;
  }
  @Override
  public boolean delete(PageId pageId) {

    return false;
  }

  private void timeAging(){
    lock.lock();
    //System.out.println("[+] in aging.");
    int prev = cyclicFingerBuffer[ageIdx];
    if(prev!=0){
      delete(prev);
    }
    updateAgeIdx();
    lock.unlock();
  }

  @Override
  public void aging() {

  }

  @Override
  public void updateWorkingSetSize() {

  }

  @Override
  public void stopUpdate() {

  }

  @Override
  public void updateTimestamp(long increment) {

  }

  @Override
  public long getShadowCachePages() {
    return mBucketsSet.get();
  }

  @Override
  public long getShadowCachePages(CacheScope scope) {
    return 0;
  }

  @Override
  public long getShadowCacheBytes() {
    return mTotalSize.get();
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
    // maybe not right, we should check this !
    return (long) mFingerPrintSize *mCycleBufferLen+ (long)mBucketNum*mBucketCapacity*(mFingerPrintSize+mBitsPerSize)+mBucketNum*136L;
  }

  @Override
  public String getSummary() {
    return "SWAMP: \nbitsPerTag: " + mFingerPrintSize
        + "\nbucketNum: "+ mBucketNum
        + "\nSizeInMB: " + getSpaceBits() / 8.0 / Constants.MB
        + "\nCycleBufferLen: "+ mCycleBufferLen
        + "\nDeleteTime: " +deleteTime
        + "\nTinyAddTime: " +addTime
        + "\nfindEmptyTime" +tinyTable.findEmptyTime
        + "\nscaleUpTime: "+tinyTable.addItemTime;
  }
}
