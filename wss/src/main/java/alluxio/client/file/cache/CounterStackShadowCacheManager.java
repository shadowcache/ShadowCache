package alluxio.client.file.cache;

import alluxio.client.quota.CacheScope;
import alluxio.util.FormatUtils;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class CounterStackShadowCacheManager implements ShadowCache{
  private long mWindowSize;
  private long mTagBits;
  private long mCounterBuckets;
  private long mMemoryInBits;
  private final AtomicLong mShadowCachePageRead = new AtomicLong(0);
  private final AtomicLong mShadowCachePageHit = new AtomicLong(0);
  private final AtomicLong mShadowCacheByteRead = new AtomicLong(0);
  private final AtomicLong mShadowCacheByteHit = new AtomicLong(0);
  private final AtomicLong mBucketsSet = new AtomicLong(0);
  private final AtomicLong mTotalSize = new AtomicLong(0);
  public  CounterStackShadowCacheManager(ShadowCacheParameters parameters){
    mTagBits = parameters.mTagBits;
    mMemoryInBits = FormatUtils.parseSpaceSize(parameters.mMemoryBudget) * 8;
    mCounterBuckets = mMemoryInBits / (mWindowSize*mTagBits);

  }

  @Override
  public boolean put(PageId pageId, int size, CacheScope scope) {
    return false;
  }

  @Override
  public int get(PageId pageId, int bytesToRead, CacheScope scope) {
    return 0;
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
    return 0;
  }

  @Override
  public long getShadowCachePages(CacheScope scope) {
    return 0;
  }

  @Override
  public long getShadowCacheBytes() {
    return 0;
  }

  @Override
  public long getShadowCacheBytes(CacheScope scope) {
    return 0;
  }

  @Override
  public long getShadowCachePageRead() {
    return 0;
  }

  @Override
  public long getShadowCachePageHit() {
    return 0;
  }

  @Override
  public long getShadowCacheByteRead() {
    return 0;
  }

  @Override
  public long getShadowCacheByteHit() {
    return 0;
  }

  @Override
  public double getFalsePositiveRatio() {
    return 0;
  }

  @Override
  public long getSpaceBits() {
    return 0;
  }

  @Override
  public String getSummary() {
    return null;
  }
}
