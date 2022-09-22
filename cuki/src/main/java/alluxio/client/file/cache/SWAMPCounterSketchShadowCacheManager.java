package alluxio.client.file.cache;

import alluxio.Constants;
import alluxio.client.quota.CacheScope;
import alluxio.util.FormatUtils;
import alluxio.util.TinyTable.RankIndexingTechnique.RankIndexingTechnique;
import alluxio.util.TinyTable.TinyTable;
import alluxio.util.TinyTable.TinyTableWithCounters;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.util.*;

public class SWAMPCounterSketchShadowCacheManager implements ShadowCache{
  protected long mWindowSize;
  protected int mFingerPrintSize;
  protected int[] cyclicFingerBuffer;
  protected int mBucketCapacity;
  protected int mBucketNum;
  protected static int mBitsPerSize = 16;
  protected static int mBitsPerCounter =  16;
  protected double mLoadF; //said "recommend this be 0.2"
  protected HashFunction mHashFunction;
  protected Funnel<PageId> mFunnel;
  protected int curIdx;
  protected long pageNum;
  protected long pageSize;
  protected long pageReadNum;
  protected long pageReadSize;
  protected long hitNum;
  protected long hitByte;
  protected boolean debugMode;

  protected Map<Integer,Integer> debugMapPageToCode;
  protected Set<Integer> debugCodeSet;
  protected HashMap<Integer,Long> debugmap;
  // no scope
  protected TinyTableWithCounters tinyTableWithCounters;
  protected TinyTable tinyTable;
  // we can use a hashcode to find string's 64bit long
  // and also filed
  // so we can define a hashcode's func
  // Objects.hash(name,a,a)
  public SWAMPCounterSketchShadowCacheManager(ShadowCacheParameters params) {
    mWindowSize = params.mWindowSize;
    mFingerPrintSize = params.mTagBits;
    mBucketCapacity = params.mTagsPerBucket;
    mBitsPerSize = params.mSizeBits;
    mBitsPerCounter = 16;

    long memoryInBits = FormatUtils.parseSpaceSize(params.mMemoryBudget) * 8;
    //mBucketNum = (int)((memoryInBits-(mWindowSize*mFingerPrintSize)) / (mBucketCapacity*(mFingerPrintSize+32)+136));
    //mBucketNum = (int)((memoryInBits-(mWindowSize*mFingerPrintSize)) / (mBucketCapacity*mFingerPrintSize))*2;
    mBucketNum = (int)((memoryInBits-(mWindowSize*mFingerPrintSize)) / (mBucketCapacity*(mFingerPrintSize+mBitsPerSize+mBitsPerCounter)+136));
    mLoadF = mBucketCapacity/(mWindowSize/(double)mBucketNum);
    tinyTableWithCounters = new TinyTableWithCounters(mFingerPrintSize,mBucketCapacity,mBucketNum);
    tinyTable = new TinyTable(mFingerPrintSize,mBucketCapacity,mBucketNum);
    cyclicFingerBuffer = new int[(int)mWindowSize];
    mHashFunction = Hashing.murmur3_32((int)System.currentTimeMillis());
    mFunnel = PageIdFunnel.FUNNEL;
    pageNum = 0;
    pageSize = 0;
    debugMapPageToCode = new HashMap<>();
    debugCodeSet =  new HashSet<>();
    debugMode = false;
    debugmap = new HashMap<>();
  }

  private void info(String s){
    if(debugMode){
      System.out.print(s);
    }
  }

  private void updateCurIdx(){
    if(curIdx==mWindowSize-1){
      curIdx = 0;
    }else{
      curIdx = curIdx + 1;
    }
  }

  public boolean put(PageId pageId,int size, CacheScope scope){
    int hashcode = mHashFunction.newHasher().putObject(pageId,mFunnel).hash().asInt();
    int prev = cyclicFingerBuffer[curIdx];
    pageReadNum++;
    pageReadSize++;
    delete(prev);
    int count = 0;
    long value = tinyTableWithCounters.GetValue(hashcode);
    long debugValue = debugmap.getOrDefault(hashcode,0L);
    if(value!=0){
      hitNum++;
      hitByte+=size;
      count = parseCounter(value)[0]+1;
    }else{
      pageNum ++;
      pageSize += size;
      count++;
    }
    tinyTableWithCounters.StoreValue(hashcode,intoCounter(count,size));
    debugmap.put(hashcode,intoCounter(count,size));
    cyclicFingerBuffer[curIdx] = hashcode;
    updateCurIdx();
    return true;
  }

  @Override
  public int get(PageId pageId, int bytesToRead, CacheScope scope) {
    return 0;
  }

  public boolean delete(int hashcode){
    long prevValue = tinyTableWithCounters.GetValue(hashcode);
    int[] cs = parseCounter(prevValue);
    if(prevValue!=0) {
      if(cs[0]==1){
        pageSize -= cs[1];
        pageNum -= 1;
        while(tinyTableWithCounters.ContainItem(hashcode)){
          tinyTableWithCounters.RemoveValue(hashcode);
        }
        debugmap.put(hashcode,0L);
      }else if(cs[0] != 0){
        tinyTableWithCounters.StoreValue(hashcode,intoCounter(cs[0]-1,cs[1]));
        debugmap.put(hashcode,intoCounter(cs[0]-1,cs[1]));
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
    return pageNum;
  }

  @Override
  public long getShadowCachePages(CacheScope scope) {
    return 0;
  }

  @Override
  public long getShadowCacheBytes() {
    return pageSize;
  }

  @Override
  public long getShadowCacheBytes(CacheScope scope) {
    return 0;
  }

  @Override
  public long getShadowCachePageRead() {
    return pageReadNum;
  }

  @Override
  public long getShadowCachePageHit() {
    return hitNum;
  }

  @Override
  public long getShadowCacheByteRead() {
    return pageReadSize;
  }

  @Override
  public long getShadowCacheByteHit() {
    return hitByte;
  }

  @Override
  public double getFalsePositiveRatio() {
    return 0;
  }

  @Override
  public long getSpaceBits() {
    // maybe not right, we should check this !
    return mFingerPrintSize*mWindowSize+ (long)mBucketNum*mBucketCapacity*(mFingerPrintSize+32)+mBucketNum* 136L;
  }

  @Override
  public String getSummary() {
    return "SWAMP: \nbitsPerTag: " + mFingerPrintSize
        + "\bbucketNum: "+ mBucketNum
        + "\nSizeInMB: " + getSpaceBits() / 8.0 / Constants.MB;
  }

  static public long intoCounter(int count,int size){
    return (long)count<<mBitsPerSize|size;
  }

  static public int[] parseCounter(long bits){
    long mask = (0x7fffffffffffffffL)>>(63-mBitsPerSize-mBitsPerCounter);
    int counter = (int)((bits&mask)>>mBitsPerSize);
    if(counter<0){
      System.out.print("-1");
      System.exit(-1);
    }
    mask = (0x7fffffffffffffffL)>>(63-mBitsPerSize);
    int size = (int)(bits&mask);
    if(size<0){
      System.out.print("-1");
      System.exit(-1);
    }

    return new int[]{counter,size};
  }
}
