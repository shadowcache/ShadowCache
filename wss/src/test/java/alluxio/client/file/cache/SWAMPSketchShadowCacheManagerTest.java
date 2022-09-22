package alluxio.client.file.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import alluxio.util.TinyTable.TinyTable;
import alluxio.util.TinyTable.TinyTableWithCounters;

import com.beust.jcommander.JCommander;

import org.junit.Test;

import java.util.Arrays;

public class SWAMPSketchShadowCacheManagerTest {
  @Test
  public void testCounterParse() {
      long a = SWAMPCounterSketchShadowCacheManager.intoCounter(12,0x7ffff);
      int[] b = SWAMPCounterSketchShadowCacheManager.parseCounter(a);
      System.out.print(Arrays.toString(b));
    }
  @Test
  public void testDelete(){
    TinyTableWithCounters tinyTableWithCounters = new TinyTableWithCounters(8,200,2);
    int[] b = new int[20];
    for(int i=0;i<20;i++){
      tinyTableWithCounters.StoreValue(i,10);
    }
    tinyTableWithCounters.RemoveValue(10);
    for(int i=0;i<20;i++){
      b[i] = (int)tinyTableWithCounters.GetValue(i);
      System.out.print(i+":"+b[i]+" ");
    }
    tinyTableWithCounters.StoreValue(10,1110);
    System.out.print(tinyTableWithCounters.GetValue(10));
  }
  @Test
  public void testAddSize(){
    TinyTable tinyTable = new TinyTable(8,16,4,4196);
    tinyTable.addItem(10,987);
    long a = tinyTable.getItemSize(10);
    System.out.print(a);
  }
}
